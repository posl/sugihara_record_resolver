/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.profile;

import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.admin.indices.get.GetIndexAction;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.test.rest.ObjectPath;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.security.action.profile.GetProfileAction;
import org.elasticsearch.xpack.core.security.action.profile.GetProfileRequest;
import org.elasticsearch.xpack.core.security.action.profile.GetProfilesResponse;
import org.elasticsearch.xpack.core.security.action.profile.Profile;
import org.elasticsearch.xpack.core.security.action.profile.SetProfileEnabledAction;
import org.elasticsearch.xpack.core.security.action.profile.SetProfileEnabledRequest;
import org.elasticsearch.xpack.core.security.action.profile.SuggestProfilesAction;
import org.elasticsearch.xpack.core.security.action.profile.SuggestProfilesRequest;
import org.elasticsearch.xpack.core.security.action.profile.SuggestProfilesResponse;
import org.elasticsearch.xpack.core.security.action.profile.UpdateProfileDataAction;
import org.elasticsearch.xpack.core.security.action.profile.UpdateProfileDataRequest;
import org.elasticsearch.xpack.core.security.action.user.ProfileHasPrivilegesAction;
import org.elasticsearch.xpack.core.security.action.user.ProfileHasPrivilegesRequest;
import org.elasticsearch.xpack.core.security.action.user.ProfileHasPrivilegesResponse;
import org.elasticsearch.xpack.core.security.action.user.PutUserAction;
import org.elasticsearch.xpack.core.security.action.user.PutUserRequest;
import org.elasticsearch.xpack.core.security.authc.Authentication;
import org.elasticsearch.xpack.core.security.authc.AuthenticationTestHelper;
import org.elasticsearch.xpack.core.security.authz.AuthorizationEngine.PrivilegesToCheck;
import org.elasticsearch.xpack.core.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.core.security.user.User;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.test.SecuritySettingsSourceField.TEST_PASSWORD_SECURE_STRING;
import static org.elasticsearch.xpack.security.support.SecuritySystemIndices.INTERNAL_SECURITY_PROFILE_INDEX_8;
import static org.elasticsearch.xpack.security.support.SecuritySystemIndices.SECURITY_PROFILE_ALIAS;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class ProfileIntegTests extends AbstractProfileIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal, Settings otherSettings) {
        final Settings.Builder builder = Settings.builder().put(super.nodeSettings(nodeOrdinal, otherSettings));
        // This setting tests that the setting is registered
        builder.put("xpack.security.authc.domains.my_domain.realms", "file");
        return builder.build();
    }

    public void testProfileIndexAutoCreation() {
        // Index does not exist yet
        assertThat(getProfileIndexResponse().getIndices(), not(hasItemInArray(INTERNAL_SECURITY_PROFILE_INDEX_8)));

        // Trigger index creation by indexing
        var indexResponse = client().prepareIndex(randomFrom(INTERNAL_SECURITY_PROFILE_INDEX_8, SECURITY_PROFILE_ALIAS))
            .setSource(Map.of("user_profile", Map.of("uid", randomAlphaOfLength(22))))
            .get();
        assertThat(indexResponse.status().getStatus(), equalTo(201));

        final GetIndexResponse getIndexResponse = getProfileIndexResponse();
        assertThat(getIndexResponse.getIndices(), hasItemInArray(INTERNAL_SECURITY_PROFILE_INDEX_8));
        var aliases = getIndexResponse.getAliases().get(INTERNAL_SECURITY_PROFILE_INDEX_8);
        assertThat(aliases, hasSize(1));
        assertThat(aliases.get(0).alias(), equalTo(SECURITY_PROFILE_ALIAS));

        final Settings settings = getIndexResponse.getSettings().get(INTERNAL_SECURITY_PROFILE_INDEX_8);
        assertThat(settings.get("index.number_of_shards"), equalTo("1"));
        assertThat(settings.get("index.auto_expand_replicas"), equalTo("0-1"));
        assertThat(settings.get("index.routing.allocation.include._tier_preference"), equalTo("data_content"));

        final Map<String, Object> mappings = getIndexResponse.getMappings().get(INTERNAL_SECURITY_PROFILE_INDEX_8).getSourceAsMap();

        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>) mappings.get("properties");
        assertThat(properties, hasKey("user_profile"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> userProfileProperties = (Map<String, Object>) ((Map<String, Object>) properties.get("user_profile")).get(
            "properties"
        );

        assertThat(userProfileProperties.keySet(), hasItems("uid", "enabled", "last_synchronized", "user", "labels", "application_data"));
    }

    public void testActivateProfile() {
        final Profile profile1 = doActivateProfile(RAC_USER_NAME, TEST_PASSWORD_SECURE_STRING);
        assertThat(profile1.user().username(), equalTo(RAC_USER_NAME));
        assertThat(profile1.user().roles(), contains(RAC_ROLE));
        assertThat(profile1.user().realmName(), equalTo("file"));
        assertThat(profile1.user().domainName(), equalTo("my_domain"));
        assertThat(profile1.user().email(), nullValue());
        assertThat(profile1.user().fullName(), nullValue());
        // Get by ID immediately should get the same document and content as the response to activate
        assertThat(getProfile(profile1.uid(), Set.of()), equalTo(profile1));

        // activate again should be getting the same profile
        final Profile profile2 = doActivateProfile(RAC_USER_NAME, TEST_PASSWORD_SECURE_STRING);
        assertThat(profile2.uid(), equalTo(profile1.uid()));
        // Get by ID immediately should get the same document and content as the response to activate
        assertThat(getProfile(profile2.uid(), Set.of()), equalTo(profile2));

        // Since file and native realms are not in the same domain, the new profile must be a different one
        final Profile profile3 = doActivateProfile(RAC_USER_NAME, NATIVE_RAC_USER_PASSWORD);
        assertThat(profile3.uid(), not(equalTo(profile1.uid()))); // NOT the same profile as the file user
        assertThat(profile3.user().username(), equalTo(RAC_USER_NAME));
        assertThat(profile3.user().realmName(), equalTo("index"));
        assertThat(profile3.user().domainName(), nullValue());
        assertThat(profile3.user().email(), equalTo(RAC_USER_NAME + "@example.com"));
        assertThat(profile3.user().fullName(), nullValue());
        assertThat(profile3.user().roles(), contains(RAC_ROLE));
        assertThat(profile3.labels(), anEmptyMap());
        // Get by ID immediately should get the same document and content as the response to activate
        assertThat(getProfile(profile3.uid(), Set.of()), equalTo(profile3));

        // Manually inserting some application data
        client().prepareUpdate(randomFrom(INTERNAL_SECURITY_PROFILE_INDEX_8, SECURITY_PROFILE_ALIAS), "profile_" + profile3.uid())
            .setDoc("""
                {
                    "user_profile": {
                      "labels": {
                        "my_app": {
                          "tag": "prod"
                        }
                      },
                      "application_data": {
                        "my_app": {
                          "theme": "default"
                        }
                      }
                    }
                  }
                """, XContentType.JSON)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL)
            .get();

        // Above manual update should be successful
        final Profile profile4 = getProfile(profile3.uid(), Set.of("my_app"));
        assertThat(profile4.uid(), equalTo(profile3.uid()));
        assertThat(profile4.labels(), equalTo(Map.of("my_app", Map.of("tag", "prod"))));
        assertThat(profile4.applicationData(), equalTo(Map.of("my_app", Map.of("theme", "default"))));

        // Update native rac user
        final PutUserRequest putUserRequest1 = new PutUserRequest();
        putUserRequest1.username(RAC_USER_NAME);
        putUserRequest1.roles(RAC_ROLE, "superuser");
        putUserRequest1.email(null);
        putUserRequest1.fullName("Native RAC User");
        assertThat(client().execute(PutUserAction.INSTANCE, putUserRequest1).actionGet().created(), is(false));

        // Activate again should see the updated user info
        final Profile profile5 = doActivateProfile(RAC_USER_NAME, NATIVE_RAC_USER_PASSWORD);
        assertThat(profile5.uid(), equalTo(profile3.uid()));
        assertThat(profile5.user().email(), nullValue());
        assertThat(profile5.user().fullName(), equalTo("Native RAC User"));
        assertThat(profile5.user().roles(), containsInAnyOrder(RAC_ROLE, "superuser"));
        // Re-activate should not change labels
        assertThat(profile5.labels(), equalTo(Map.of("my_app", Map.of("tag", "prod"))));
        // Get by ID immediately should get the same document and content as the response to activate
        assertThat(getProfile(profile5.uid(), Set.of()), equalTo(profile5));
        // Re-activate should not change application data
        assertThat(getProfile(profile5.uid(), Set.of("my_app")).applicationData(), equalTo(Map.of("my_app", Map.of("theme", "default"))));
    }

    public void testUpdateProfileData() {
        final Profile profile1 = doActivateProfile(RAC_USER_NAME, TEST_PASSWORD_SECURE_STRING);

        final UpdateProfileDataRequest updateProfileDataRequest1 = new UpdateProfileDataRequest(
            profile1.uid(),
            Map.of("app1", List.of("tab1", "tab2")),
            Map.of("app1", Map.of("name", "app1", "type", "app")),
            -1,
            -1,
            WriteRequest.RefreshPolicy.WAIT_UNTIL
        );
        client().execute(UpdateProfileDataAction.INSTANCE, updateProfileDataRequest1).actionGet();

        final Profile profile2 = getProfile(profile1.uid(), Set.of("app1", "app2"));

        assertThat(profile2.uid(), equalTo(profile1.uid()));
        assertThat(profile2.labels(), equalTo(Map.of("app1", List.of("tab1", "tab2"))));
        assertThat(profile2.applicationData(), equalTo(Map.of("app1", Map.of("name", "app1", "type", "app"))));

        // Update again should be incremental
        final UpdateProfileDataRequest updateProfileDataRequest2 = new UpdateProfileDataRequest(
            profile1.uid(),
            null,
            Map.of("app1", Map.of("name", "app1_take2", "active", false), "app2", Map.of("name", "app2")),
            -1,
            -1,
            WriteRequest.RefreshPolicy.WAIT_UNTIL
        );
        client().execute(UpdateProfileDataAction.INSTANCE, updateProfileDataRequest2).actionGet();

        final Profile profile3 = getProfile(profile1.uid(), Set.of("app1", "app2"));
        assertThat(profile3.uid(), equalTo(profile1.uid()));
        assertThat(profile3.labels(), equalTo(profile2.labels()));
        assertThat(
            profile3.applicationData(),
            equalTo(Map.of("app1", Map.of("name", "app1_take2", "type", "app", "active", false), "app2", Map.of("name", "app2")))
        );

        // Activate profile again should not affect the data section
        doActivateProfile(RAC_USER_NAME, TEST_PASSWORD_SECURE_STRING);
        final Profile profile4 = getProfile(profile1.uid(), Set.of("app1", "app2"));
        assertThat(profile4.labels(), equalTo(profile3.labels()));
        assertThat(profile4.applicationData(), equalTo(profile3.applicationData()));

        // Update non-existent profile should throw error
        final UpdateProfileDataRequest updateProfileDataRequest3 = new UpdateProfileDataRequest(
            "not-" + profile1.uid(),
            null,
            Map.of("foo", "bar"),
            -1,
            -1,
            WriteRequest.RefreshPolicy.WAIT_UNTIL
        );
        expectThrows(
            DocumentMissingException.class,
            () -> client().execute(UpdateProfileDataAction.INSTANCE, updateProfileDataRequest3).actionGet()
        );
    }

    public void testSuggestProfilesWithName() {
        final ProfileService profileService = getInstanceFromRandomNode(ProfileService.class);

        final Map<String, String> users = Map.of(
            "user_foo",
            "Very Curious User Foo",
            "user_bar",
            "Super Curious Admin Bar",
            "user_baz",
            "Very Anxious User Baz",
            "user_qux",
            "Super Anxious Admin Qux"
        );
        users.forEach((key, value) -> {
            final Authentication authentication = AuthenticationTestHelper.builder()
                .realm()
                .user(new User(key, new String[] { "rac_role" }, value, key.substring(5) + "email@example.org", Map.of(), true))
                .build();
            final PlainActionFuture<Profile> future = new PlainActionFuture<>();
            profileService.activateProfile(authentication, future);
            future.actionGet();
        });

        final SuggestProfilesResponse.ProfileHit[] profiles1 = doSuggest("");
        assertThat(extractUsernames(profiles1), equalTo(users.keySet()));

        final SuggestProfilesResponse.ProfileHit[] profiles2 = doSuggest(randomFrom("super admin", "admin super"));
        assertThat(extractUsernames(profiles2), equalTo(Set.of("user_bar", "user_qux")));

        // Prefix match on full name
        final SuggestProfilesResponse.ProfileHit[] profiles3 = doSuggest("ver");
        assertThat(extractUsernames(profiles3), equalTo(Set.of("user_foo", "user_baz")));

        // Prefix match on the username
        final SuggestProfilesResponse.ProfileHit[] profiles4 = doSuggest("user");
        assertThat(extractUsernames(profiles4), equalTo(users.keySet()));
        // Documents scored higher are those with matches in more fields
        assertThat(extractUsernames(Arrays.copyOfRange(profiles4, 0, 2)), equalTo(Set.of("user_foo", "user_baz")));

        // Match of different terms on different fields
        final SuggestProfilesResponse.ProfileHit[] profiles5 = doSuggest(randomFrom("admin very", "very admin"));
        assertThat(extractUsernames(profiles5), equalTo(users.keySet()));

        // Match email
        final SuggestProfilesResponse.ProfileHit[] profiles6 = doSuggest(randomFrom("fooem", "fooemail"));
        assertThat(extractUsernames(profiles6), equalTo(Set.of("user_foo")));

        final SuggestProfilesResponse.ProfileHit[] profiles7 = doSuggest("example.org");
        assertThat(extractUsernames(profiles7), equalTo(users.keySet()));

        // Fuzzy match
        final SuggestProfilesResponse.ProfileHit[] profiles8 = doSuggest("Kurious One");
        assertThat(extractUsernames(profiles8), equalTo(Set.of("user_foo", "user_bar")));
    }

    public void testSuggestProfileWithData() {
        final ProfileService profileService = getInstanceFromRandomNode(ProfileService.class);
        final PlainActionFuture<Profile> future1 = new PlainActionFuture<>();
        profileService.activateProfile(AuthenticationTestHelper.builder().realm().build(), future1);
        final Profile profile = future1.actionGet();

        final PlainActionFuture<AcknowledgedResponse> future2 = new PlainActionFuture<>();
        profileService.updateProfileData(
            new UpdateProfileDataRequest(
                profile.uid(),
                Map.of("spaces", "demo"),
                Map.of("app1", Map.of("name", "app1", "status", "prod"), "app2", Map.of("name", "app2", "status", "dev")),
                -1,
                -1,
                WriteRequest.RefreshPolicy.WAIT_UNTIL
            ),
            future2
        );
        assertThat(future2.actionGet().isAcknowledged(), is(true));

        // No application data is requested
        final SuggestProfilesResponse.ProfileHit[] profileHits1 = doSuggest("");
        assertThat(profileHits1.length, equalTo(1));
        final Profile profileResult1 = profileHits1[0].profile();
        assertThat(profileResult1.uid(), equalTo(profile.uid()));
        assertThat(profileResult1.labels(), equalTo(Map.of("spaces", "demo")));
        assertThat(profileResult1.applicationData(), anEmptyMap());

        // Request a single key of application data
        final SuggestProfilesResponse.ProfileHit[] profileHits2 = doSuggest("", Set.of("app1"));
        assertThat(profileHits2.length, equalTo(1));
        final Profile profileResult2 = profileHits2[0].profile();
        assertThat(profileResult2.uid(), equalTo(profile.uid()));
        assertThat(profileResult2.labels(), equalTo(Map.of("spaces", "demo")));
        assertThat(profileResult2.applicationData(), equalTo(Map.of("app1", Map.of("name", "app1", "status", "prod"))));

        // Request multiple keys
        final SuggestProfilesResponse.ProfileHit[] profileHits3 = doSuggest(
            "",
            randomFrom(Set.of("*"), Set.of("app*"), Set.of("app1", "app2"))
        );
        assertThat(profileHits3.length, equalTo(1));
        final Profile profileResult3 = profileHits3[0].profile();
        assertThat(profileResult3.uid(), equalTo(profile.uid()));
        assertThat(profileResult3.labels(), equalTo(Map.of("spaces", "demo")));
        assertThat(
            profileResult3.applicationData(),
            equalTo(Map.of("app1", Map.of("name", "app1", "status", "prod"), "app2", Map.of("name", "app2", "status", "dev")))
        );
    }

    public void testSuggestProfilesWithHint() throws IOException {
        final ProfileService profileService = getInstanceFromRandomNode(ProfileService.class);
        final List<String> spaces = List.of("space1", "space2", "space3", "space4", "*");
        final List<Profile> profiles = spaces.stream().map(space -> {
            final PlainActionFuture<Profile> future1 = new PlainActionFuture<>();
            final String lastName = randomAlphaOfLengthBetween(3, 8);
            final Authentication authentication = AuthenticationTestHelper.builder()
                .realm()
                .user(
                    new User(
                        "user_" + lastName,
                        new String[] { "rac_role" },
                        "User " + lastName,
                        "user_" + lastName + "@example.org",
                        Map.of(),
                        true
                    )
                )
                .build();
            profileService.activateProfile(authentication, future1);
            final Profile profile = future1.actionGet();
            final PlainActionFuture<AcknowledgedResponse> future2 = new PlainActionFuture<>();
            profileService.updateProfileData(
                new UpdateProfileDataRequest(
                    profile.uid(),
                    Map.of("kibana", Map.of("space", space)),
                    Map.of(),
                    -1,
                    -1,
                    WriteRequest.RefreshPolicy.WAIT_UNTIL
                ),
                future2
            );
            assertThat(future2.actionGet().isAcknowledged(), is(true));
            final PlainActionFuture<Profile> future3 = new PlainActionFuture<>();
            profileService.getProfile(profile.uid(), Set.of(), future3);
            return future3.actionGet();
        }).toList();

        // Default order of last synchronized timestamp
        final List<Profile> profileHits1 = Arrays.stream(doSuggest("")).map(SuggestProfilesResponse.ProfileHit::profile).toList();
        assertThat(
            profileHits1.stream().sorted(Comparator.comparingLong(Profile::lastSynchronized).reversed()).toList(),
            equalTo(profileHits1)
        );

        // uid hint is a should clause which does not exclude records but ranks matching ones higher
        final Profile hintedProfile2 = randomFrom(profiles);
        final List<Profile> profileHits2 = Arrays.stream(
            doSuggest(Set.of(), "user", new SuggestProfilesRequest.Hint(List.of(hintedProfile2.uid()), null))
        ).map(SuggestProfilesResponse.ProfileHit::profile).toList();
        assertThat(profileHits2.size(), equalTo(5));
        // First record has the matching uid
        assertThat(profileHits2.get(0).uid(), equalTo(hintedProfile2.uid()));
        // Rest follows order of last synced
        assertThat(
            profileHits2.stream().skip(1).sorted(Comparator.comparingLong(Profile::lastSynchronized).reversed()).toList(),
            equalTo(profileHits2.subList(1, profileHits2.size()))
        );

        // labels hint is also a should clause which does not exclude records but ranks matching ones higher
        final Profile hintedProfile3 = randomFrom(profiles);
        final String hintedSpace3 = ObjectPath.evaluate(hintedProfile3.labels(), "kibana.space");
        final List<Profile> profileHits3 = Arrays.stream(
            doSuggest(Set.of(), "user", new SuggestProfilesRequest.Hint(null, Map.of("kibana.space", hintedSpace3)))
        ).map(SuggestProfilesResponse.ProfileHit::profile).toList();
        assertThat(profileHits3.size(), equalTo(5));
        // First record has the matching labels
        assertThat(profileHits3.get(0).labels(), equalTo(Map.of("kibana", Map.of("space", hintedSpace3))));
        assertThat(profileHits3.get(0).uid(), equalTo(hintedProfile3.uid()));
        // Rest follows order of last synced
        assertThat(
            profileHits3.stream().skip(1).sorted(Comparator.comparingLong(Profile::lastSynchronized).reversed()).toList(),
            equalTo(profileHits3.subList(1, profileHits3.size()))
        );

        // Both uid and labels hints
        final List<Profile> hintedProfiles = randomSubsetOf(2, profiles);
        final Profile hintedProfile4 = randomFrom(hintedProfiles);
        final Object hintedSpace4 = ObjectPath.evaluate(hintedProfile4.labels(), "kibana.space");
        final List<Profile> profileHits4 = Arrays.stream(
            doSuggest(
                Set.of(),
                "user",
                new SuggestProfilesRequest.Hint(hintedProfiles.stream().map(Profile::uid).toList(), Map.of("kibana.space", hintedSpace4))
            )
        ).map(SuggestProfilesResponse.ProfileHit::profile).toList();
        assertThat(profileHits4.size(), equalTo(5));
        // First record has both matching uid and labels
        assertThat(profileHits4.get(0).labels(), equalTo(Map.of("kibana", Map.of("space", hintedSpace4))));
        assertThat(profileHits4.get(0).uid(), equalTo(hintedProfile4.uid()));
        // Second record has only matching uid
        assertThat(
            profileHits4.get(1).uid(),
            equalTo(hintedProfiles.stream().filter(p -> false == p.equals(hintedProfile4)).findFirst().orElseThrow().uid())
        );
        // Rest follows order of last synced
        assertThat(
            profileHits4.stream().skip(2).sorted(Comparator.comparingLong(Profile::lastSynchronized).reversed()).toList(),
            equalTo(profileHits4.subList(2, profileHits4.size()))
        );

        // A record will not be included if name does not match even when it has matching hint
        final Profile hintedProfile5 = randomFrom(profiles);
        final List<Profile> profileHits5 = Arrays.stream(
            doSuggest(
                Set.of(),
                hintedProfile5.user().fullName().substring(5),
                new SuggestProfilesRequest.Hint(profiles.stream().map(Profile::uid).toList(), Map.of("kibana.space", spaces))
            )
        ).map(SuggestProfilesResponse.ProfileHit::profile).toList();
        assertThat(profileHits5.size(), equalTo(1));
        assertThat(profileHits5.get(0).uid(), equalTo(hintedProfile5.uid()));
    }

    public void testProfileAPIsWhenIndexNotCreated() {
        // Ensure index does not exist
        assertThat(getProfileIndexResponse().getIndices(), not(hasItemInArray(INTERNAL_SECURITY_PROFILE_INDEX_8)));

        // Get Profile by ID returns empty result
        final GetProfilesResponse getProfilesResponse = client().execute(
            GetProfileAction.INSTANCE,
            new GetProfileRequest(randomAlphaOfLength(20), Set.of())
        ).actionGet();
        assertThat(getProfilesResponse.getProfiles(), arrayWithSize(0));

        // Ensure index does not exist
        assertThat(getProfileIndexResponse().getIndices(), not(hasItemInArray(INTERNAL_SECURITY_PROFILE_INDEX_8)));

        // Search returns empty result
        final SuggestProfilesResponse.ProfileHit[] profiles1 = doSuggest("");
        assertThat(profiles1, emptyArray());

        // Has privilege returns empty response
        ProfileHasPrivilegesResponse profileHasPrivilegesResponse = client().execute(
            ProfileHasPrivilegesAction.INSTANCE,
            new ProfileHasPrivilegesRequest(
                randomList(1, 3, () -> randomAlphaOfLength(20)),
                new PrivilegesToCheck(
                    new String[] { "monitor" },
                    new RoleDescriptor.IndicesPrivileges[0],
                    new RoleDescriptor.ApplicationResourcePrivileges[] {
                        RoleDescriptor.ApplicationResourcePrivileges.builder()
                            .application("test-app")
                            .resources("some/resource")
                            .privileges("write")
                            .build() }
                )
            )
        ).actionGet();
        assertThat(profileHasPrivilegesResponse.hasPrivilegeUids(), emptyIterable());
        assertThat(profileHasPrivilegesResponse.errorUids(), emptyIterable());

        // Ensure index does not exist
        assertThat(getProfileIndexResponse().getIndices(), not(hasItemInArray(INTERNAL_SECURITY_PROFILE_INDEX_8)));

        // Updating profile data results into doc missing exception
        // But the index is created in the process
        final DocumentMissingException e1 = expectThrows(
            DocumentMissingException.class,
            () -> client().execute(
                UpdateProfileDataAction.INSTANCE,
                new UpdateProfileDataRequest(
                    randomAlphaOfLength(20),
                    null,
                    Map.of(randomAlphaOfLengthBetween(3, 8), randomAlphaOfLengthBetween(3, 8)),
                    -1,
                    -1,
                    WriteRequest.RefreshPolicy.WAIT_UNTIL
                )
            ).actionGet()
        );

        // TODO: The index is created after the update call regardless. Should it not do that?
        assertThat(getProfileIndexResponse().getIndices(), hasItemInArray(INTERNAL_SECURITY_PROFILE_INDEX_8));
    }

    public void testSetEnabled() {
        final Profile profile1 = doActivateProfile(RAC_USER_NAME, TEST_PASSWORD_SECURE_STRING);

        final SuggestProfilesResponse.ProfileHit[] profileHits1 = doSuggest(RAC_USER_NAME);
        assertThat(profileHits1, arrayWithSize(1));
        assertThat(profileHits1[0].profile().uid(), equalTo(profile1.uid()));

        // Disable the profile
        final SetProfileEnabledRequest setProfileEnabledRequest1 = new SetProfileEnabledRequest(
            profile1.uid(),
            false,
            WriteRequest.RefreshPolicy.IMMEDIATE
        );
        client().execute(SetProfileEnabledAction.INSTANCE, setProfileEnabledRequest1).actionGet();

        // No longer visible to search
        final SuggestProfilesResponse.ProfileHit[] profileHits2 = doSuggest(RAC_USER_NAME);
        assertThat(profileHits2, emptyArray());

        // But can still direct get
        final Profile profile2 = getProfile(profile1.uid(), Set.of());
        assertThat(profile2.uid(), equalTo(profile1.uid()));
        assertThat(profile2.enabled(), is(false));

        // but not check privileges
        ProfileHasPrivilegesResponse profileHasPrivilegesResponse = client().execute(
            ProfileHasPrivilegesAction.INSTANCE,
            new ProfileHasPrivilegesRequest(
                List.of(profile1.uid()),
                new PrivilegesToCheck(
                    new String[] { "cluster:monitor/state" },
                    new RoleDescriptor.IndicesPrivileges[0],
                    new RoleDescriptor.ApplicationResourcePrivileges[0]
                )
            )
        ).actionGet();
        assertThat(profileHasPrivilegesResponse.hasPrivilegeUids(), emptyIterable());
        assertThat(profileHasPrivilegesResponse.errorUids(), emptyIterable());

        // Enable again for search
        final SetProfileEnabledRequest setProfileEnabledRequest2 = new SetProfileEnabledRequest(
            profile1.uid(),
            true,
            WriteRequest.RefreshPolicy.IMMEDIATE
        );
        client().execute(SetProfileEnabledAction.INSTANCE, setProfileEnabledRequest2).actionGet();
        final SuggestProfilesResponse.ProfileHit[] profileHits3 = doSuggest(RAC_USER_NAME);
        assertThat(profileHits3, arrayWithSize(1));
        assertThat(profileHits3[0].profile().uid(), equalTo(profile1.uid()));

        // Enable or disable non-existing profile will throw error
        final SetProfileEnabledRequest setProfileEnabledRequest3 = new SetProfileEnabledRequest(
            "not-" + profile1.uid(),
            randomBoolean(),
            WriteRequest.RefreshPolicy.IMMEDIATE
        );
        expectThrows(
            DocumentMissingException.class,
            () -> client().execute(SetProfileEnabledAction.INSTANCE, setProfileEnabledRequest3).actionGet()
        );
    }

    private SuggestProfilesResponse.ProfileHit[] doSuggest(String name) {
        return doSuggest(name, Set.of());
    }

    private SuggestProfilesResponse.ProfileHit[] doSuggest(String name, Set<String> dataKeys) {
        return doSuggest(dataKeys, name, null);
    }

    private SuggestProfilesResponse.ProfileHit[] doSuggest(Set<String> dataKeys, String name, SuggestProfilesRequest.Hint hint) {
        final SuggestProfilesRequest suggestProfilesRequest = new SuggestProfilesRequest(dataKeys, name, 10, hint);
        final SuggestProfilesResponse suggestProfilesResponse = client().execute(SuggestProfilesAction.INSTANCE, suggestProfilesRequest)
            .actionGet();
        assertThat(suggestProfilesResponse.getTotalHits().relation, is(TotalHits.Relation.EQUAL_TO));
        return suggestProfilesResponse.getProfileHits();
    }

    private Set<String> extractUsernames(SuggestProfilesResponse.ProfileHit[] profileHits) {
        return Arrays.stream(profileHits)
            .map(SuggestProfilesResponse.ProfileHit::profile)
            .map(Profile::user)
            .map(Profile.ProfileUser::username)
            .collect(Collectors.toUnmodifiableSet());
    }

    private GetIndexResponse getProfileIndexResponse() {
        final GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(".*");
        return client().execute(GetIndexAction.INSTANCE, getIndexRequest).actionGet();
    }
}
