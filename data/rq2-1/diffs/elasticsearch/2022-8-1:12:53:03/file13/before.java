/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.apikey;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.test.XContentTestUtils;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.security.action.apikey.ApiKey;
import org.elasticsearch.xpack.core.security.action.apikey.GetApiKeyResponse;
import org.elasticsearch.xpack.core.security.action.apikey.GrantApiKeyAction;
import org.elasticsearch.xpack.core.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.xpack.security.SecurityOnTrialLicenseRestTestCase;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.xpack.core.security.authc.AuthenticationServiceField.RUN_AS_USER_HEADER;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration Rest Tests relating to API Keys.
 * Tested against a trial license
 */
public class ApiKeyRestIT extends SecurityOnTrialLicenseRestTestCase {

    private static final String SYSTEM_USER = "system_user";
    private static final SecureString SYSTEM_USER_PASSWORD = new SecureString("system-user-password".toCharArray());
    private static final String END_USER = "end_user";
    private static final SecureString END_USER_PASSWORD = new SecureString("end-user-password".toCharArray());
    private static final String MANAGE_OWN_API_KEY_USER = "manage_own_api_key_user";

    @Before
    public void createUsers() throws IOException {
        createUser(SYSTEM_USER, SYSTEM_USER_PASSWORD, List.of("system_role"));
        createRole("system_role", Set.of("grant_api_key"));
        createUser(END_USER, END_USER_PASSWORD, List.of("user_role"));
        createRole("user_role", Set.of("monitor"));
        createUser(MANAGE_OWN_API_KEY_USER, END_USER_PASSWORD, List.of("manage_own_api_key_role"));
        createRole("manage_own_api_key_role", Set.of("manage_own_api_key"));
    }

    @After
    public void cleanUp() throws IOException {
        deleteUser(SYSTEM_USER);
        deleteUser(END_USER);
        deleteUser(MANAGE_OWN_API_KEY_USER);
        deleteRole("system_role");
        deleteRole("user_role");
        deleteRole("manage_own_api_key_role");
        invalidateApiKeysForUser(END_USER);
        invalidateApiKeysForUser(MANAGE_OWN_API_KEY_USER);
    }

    @SuppressWarnings({ "unchecked" })
    public void testAuthenticateResponseApiKey() throws IOException {
        final String expectedApiKeyName = "my-api-key-name";
        final Map<String, String> expectedApiKeyMetadata = Map.of("not", "returned");
        final Map<String, Object> createApiKeyRequestBody = Map.of("name", expectedApiKeyName, "metadata", expectedApiKeyMetadata);

        final Request createApiKeyRequest = new Request("POST", "_security/api_key");
        createApiKeyRequest.setJsonEntity(XContentTestUtils.convertToXContent(createApiKeyRequestBody, XContentType.JSON).utf8ToString());

        final Response createApiKeyResponse = adminClient().performRequest(createApiKeyRequest);
        final Map<String, Object> createApiKeyResponseMap = responseAsMap(createApiKeyResponse); // keys: id, name, api_key, encoded
        final String actualApiKeyId = (String) createApiKeyResponseMap.get("id");
        final String actualApiKeyName = (String) createApiKeyResponseMap.get("name");
        final String actualApiKeyEncoded = (String) createApiKeyResponseMap.get("encoded"); // Base64(id:api_key)
        assertThat(actualApiKeyId, not(emptyString()));
        assertThat(actualApiKeyName, equalTo(expectedApiKeyName));
        assertThat(actualApiKeyEncoded, not(emptyString()));

        doTestAuthenticationWithApiKey(expectedApiKeyName, actualApiKeyId, actualApiKeyEncoded);
    }

    public void testGrantApiKeyForOtherUserWithPassword() throws IOException {
        Request request = new Request("POST", "_security/api_key/grant");
        request.setOptions(
            RequestOptions.DEFAULT.toBuilder()
                .addHeader("Authorization", UsernamePasswordToken.basicAuthHeaderValue(SYSTEM_USER, SYSTEM_USER_PASSWORD))
        );
        final Map<String, Object> requestBody = Map.ofEntries(
            Map.entry("grant_type", "password"),
            Map.entry("username", END_USER),
            Map.entry("password", END_USER_PASSWORD.toString()),
            Map.entry("api_key", Map.of("name", "test_api_key_password"))
        );
        request.setJsonEntity(XContentTestUtils.convertToXContent(requestBody, XContentType.JSON).utf8ToString());

        final Response response = client().performRequest(request);
        final Map<String, Object> responseBody = entityAsMap(response);

        assertThat(responseBody.get("name"), equalTo("test_api_key_password"));
        assertThat(responseBody.get("id"), notNullValue());
        assertThat(responseBody.get("id"), instanceOf(String.class));

        ApiKey apiKey = getApiKey((String) responseBody.get("id"));
        assertThat(apiKey.getUsername(), equalTo(END_USER));
    }

    public void testGrantApiKeyForOtherUserWithAccessToken() throws IOException {
        final Tuple<String, String> token = super.createOAuthToken(END_USER, END_USER_PASSWORD);
        final String accessToken = token.v1();

        final Request request = new Request("POST", "_security/api_key/grant");
        request.setOptions(
            RequestOptions.DEFAULT.toBuilder()
                .addHeader("Authorization", UsernamePasswordToken.basicAuthHeaderValue(SYSTEM_USER, SYSTEM_USER_PASSWORD))
        );
        final Map<String, Object> requestBody = Map.ofEntries(
            Map.entry("grant_type", "access_token"),
            Map.entry("access_token", accessToken),
            Map.entry("api_key", Map.of("name", "test_api_key_token", "expiration", "2h"))
        );
        request.setJsonEntity(XContentTestUtils.convertToXContent(requestBody, XContentType.JSON).utf8ToString());

        final Instant before = Instant.now();
        final Response response = client().performRequest(request);
        final Instant after = Instant.now();
        final Map<String, Object> responseBody = entityAsMap(response);

        assertThat(responseBody.get("name"), equalTo("test_api_key_token"));
        assertThat(responseBody.get("id"), notNullValue());
        assertThat(responseBody.get("id"), instanceOf(String.class));

        ApiKey apiKey = getApiKey((String) responseBody.get("id"));
        assertThat(apiKey.getUsername(), equalTo(END_USER));

        Instant minExpiry = before.plus(2, ChronoUnit.HOURS);
        Instant maxExpiry = after.plus(2, ChronoUnit.HOURS);
        assertThat(apiKey.getExpiration(), notNullValue());
        assertThat(apiKey.getExpiration(), greaterThanOrEqualTo(minExpiry));
        assertThat(apiKey.getExpiration(), lessThanOrEqualTo(maxExpiry));
    }

    public void testGrantApiKeyWithoutApiKeyNameWillFail() throws IOException {
        Request request = new Request("POST", "_security/api_key/grant");
        request.setOptions(
            RequestOptions.DEFAULT.toBuilder()
                .addHeader("Authorization", UsernamePasswordToken.basicAuthHeaderValue(SYSTEM_USER, SYSTEM_USER_PASSWORD))
        );
        final Map<String, Object> requestBody = Map.ofEntries(
            Map.entry("grant_type", "password"),
            Map.entry("username", END_USER),
            Map.entry("password", END_USER_PASSWORD.toString())
        );
        request.setJsonEntity(XContentTestUtils.convertToXContent(requestBody, XContentType.JSON).utf8ToString());

        final ResponseException e = expectThrows(ResponseException.class, () -> client().performRequest(request));

        assertEquals(400, e.getResponse().getStatusLine().getStatusCode());
        assertThat(e.getMessage(), containsString("api key name is required"));
    }

    public void testGrantApiKeyWithOnlyManageOwnApiKeyPrivilegeFails() throws IOException {
        final Request request = new Request("POST", "_security/api_key/grant");
        request.setOptions(
            RequestOptions.DEFAULT.toBuilder()
                .addHeader("Authorization", UsernamePasswordToken.basicAuthHeaderValue(MANAGE_OWN_API_KEY_USER, END_USER_PASSWORD))
        );
        final Map<String, Object> requestBody = Map.ofEntries(
            Map.entry("grant_type", "password"),
            Map.entry("username", MANAGE_OWN_API_KEY_USER),
            Map.entry("password", END_USER_PASSWORD.toString()),
            Map.entry("api_key", Map.of("name", "test_api_key_password"))
        );
        request.setJsonEntity(XContentTestUtils.convertToXContent(requestBody, XContentType.JSON).utf8ToString());

        final ResponseException e = expectThrows(ResponseException.class, () -> client().performRequest(request));

        assertEquals(403, e.getResponse().getStatusLine().getStatusCode());
        assertThat(e.getMessage(), containsString("action [" + GrantApiKeyAction.NAME + "] is unauthorized for user"));
    }

    public void testUpdateApiKey() throws IOException {
        final var apiKeyName = "my-api-key-name";
        final Map<String, Object> apiKeyMetadata = Map.of("not", "returned");
        final Map<String, Object> createApiKeyRequestBody = Map.of("name", apiKeyName, "metadata", apiKeyMetadata);

        final Request createApiKeyRequest = new Request("POST", "_security/api_key");
        createApiKeyRequest.setJsonEntity(XContentTestUtils.convertToXContent(createApiKeyRequestBody, XContentType.JSON).utf8ToString());
        createApiKeyRequest.setOptions(
            RequestOptions.DEFAULT.toBuilder()
                .addHeader("Authorization", headerFromRandomAuthMethod(MANAGE_OWN_API_KEY_USER, END_USER_PASSWORD))
        );

        final Response createApiKeyResponse = client().performRequest(createApiKeyRequest);
        final Map<String, Object> createApiKeyResponseMap = responseAsMap(createApiKeyResponse); // keys: id, name, api_key, encoded
        final var apiKeyId = (String) createApiKeyResponseMap.get("id");
        final var apiKeyEncoded = (String) createApiKeyResponseMap.get("encoded"); // Base64(id:api_key)
        assertThat(apiKeyId, not(emptyString()));
        assertThat(apiKeyEncoded, not(emptyString()));

        doTestUpdateApiKey(apiKeyName, apiKeyId, apiKeyEncoded, apiKeyMetadata);
    }

    public void testGrantTargetCanUpdateApiKey() throws IOException {
        final var request = new Request("POST", "_security/api_key/grant");
        request.setOptions(
            RequestOptions.DEFAULT.toBuilder()
                .addHeader("Authorization", UsernamePasswordToken.basicAuthHeaderValue(SYSTEM_USER, SYSTEM_USER_PASSWORD))
        );
        final var apiKeyName = "test_api_key_password";
        final Map<String, Object> requestBody = Map.ofEntries(
            Map.entry("grant_type", "password"),
            Map.entry("username", MANAGE_OWN_API_KEY_USER),
            Map.entry("password", END_USER_PASSWORD.toString()),
            Map.entry("api_key", Map.of("name", apiKeyName))
        );
        request.setJsonEntity(XContentTestUtils.convertToXContent(requestBody, XContentType.JSON).utf8ToString());

        final Response response = client().performRequest(request);
        final Map<String, Object> createApiKeyResponseMap = responseAsMap(response); // keys: id, name, api_key, encoded
        final var apiKeyId = (String) createApiKeyResponseMap.get("id");
        final var apiKeyEncoded = (String) createApiKeyResponseMap.get("encoded"); // Base64(id:api_key)
        assertThat(apiKeyId, not(emptyString()));
        assertThat(apiKeyEncoded, not(emptyString()));

        doTestUpdateApiKey(apiKeyName, apiKeyId, apiKeyEncoded, null);
    }

    public void testGrantorCannotUpdateApiKeyOfGrantTarget() throws IOException {
        final var request = new Request("POST", "_security/api_key/grant");
        final var apiKeyName = "test_api_key_password";
        final Map<String, Object> requestBody = Map.ofEntries(
            Map.entry("grant_type", "password"),
            Map.entry("username", MANAGE_OWN_API_KEY_USER),
            Map.entry("password", END_USER_PASSWORD.toString()),
            Map.entry("api_key", Map.of("name", apiKeyName))
        );
        request.setJsonEntity(XContentTestUtils.convertToXContent(requestBody, XContentType.JSON).utf8ToString());
        final Response response = adminClient().performRequest(request);

        final Map<String, Object> createApiKeyResponseMap = responseAsMap(response); // keys: id, name, api_key, encoded
        final var apiKeyId = (String) createApiKeyResponseMap.get("id");
        final var apiKeyEncoded = (String) createApiKeyResponseMap.get("encoded"); // Base64(id:api_key)
        assertThat(apiKeyId, not(emptyString()));
        assertThat(apiKeyEncoded, not(emptyString()));

        final var updateApiKeyRequest = new Request("PUT", "_security/api_key/" + apiKeyId);
        updateApiKeyRequest.setJsonEntity(XContentTestUtils.convertToXContent(Map.of(), XContentType.JSON).utf8ToString());
        final ResponseException e = expectThrows(ResponseException.class, () -> adminClient().performRequest(updateApiKeyRequest));

        assertEquals(404, e.getResponse().getStatusLine().getStatusCode());
        assertThat(e.getMessage(), containsString("no API key owned by requesting user found for ID [" + apiKeyId + "]"));
    }

    private void doTestAuthenticationWithApiKey(final String apiKeyName, final String apiKeyId, final String apiKeyEncoded)
        throws IOException {
        final var authenticateRequest = new Request("GET", "_security/_authenticate");
        authenticateRequest.setOptions(authenticateRequest.getOptions().toBuilder().addHeader("Authorization", "ApiKey " + apiKeyEncoded));

        final Response authenticateResponse = client().performRequest(authenticateRequest);
        assertOK(authenticateResponse);
        final Map<String, Object> authenticate = responseAsMap(authenticateResponse); // keys: username, roles, full_name, etc

        // If authentication type is API_KEY, authentication.api_key={"id":"abc123","name":"my-api-key"}. No encoded, api_key, or metadata.
        // If authentication type is other, authentication.api_key not present.
        assertThat(authenticate, hasEntry("api_key", Map.of("id", apiKeyId, "name", apiKeyName)));
    }

    private void doTestUpdateApiKey(
        final String apiKeyName,
        final String apiKeyId,
        final String apiKeyEncoded,
        final Map<String, Object> oldMetadata
    ) throws IOException {
        final var updateApiKeyRequest = new Request("PUT", "_security/api_key/" + apiKeyId);
        final boolean updated = randomBoolean();
        final Map<String, Object> expectedApiKeyMetadata = updated ? Map.of("not", "returned (changed)", "foo", "bar") : oldMetadata;
        final Map<String, Object> updateApiKeyRequestBody = expectedApiKeyMetadata == null
            ? Map.of()
            : Map.of("metadata", expectedApiKeyMetadata);
        updateApiKeyRequest.setJsonEntity(XContentTestUtils.convertToXContent(updateApiKeyRequestBody, XContentType.JSON).utf8ToString());

        final Response updateApiKeyResponse = doUpdateUsingRandomAuthMethod(updateApiKeyRequest);

        assertOK(updateApiKeyResponse);
        final Map<String, Object> updateApiKeyResponseMap = responseAsMap(updateApiKeyResponse);
        assertEquals(updated, updateApiKeyResponseMap.get("updated"));
        expectMetadata(apiKeyId, expectedApiKeyMetadata == null ? Map.of() : expectedApiKeyMetadata);
        // validate authentication still works after update
        doTestAuthenticationWithApiKey(apiKeyName, apiKeyId, apiKeyEncoded);
    }

    private Response doUpdateUsingRandomAuthMethod(Request updateApiKeyRequest) throws IOException {
        final boolean useRunAs = randomBoolean();
        if (useRunAs) {
            updateApiKeyRequest.setOptions(RequestOptions.DEFAULT.toBuilder().addHeader(RUN_AS_USER_HEADER, MANAGE_OWN_API_KEY_USER));
            return adminClient().performRequest(updateApiKeyRequest);
        } else {
            updateApiKeyRequest.setOptions(
                RequestOptions.DEFAULT.toBuilder()
                    .addHeader("Authorization", headerFromRandomAuthMethod(MANAGE_OWN_API_KEY_USER, END_USER_PASSWORD))
            );
            return client().performRequest(updateApiKeyRequest);
        }
    }

    private String headerFromRandomAuthMethod(final String username, final SecureString password) throws IOException {
        final boolean useBearerTokenAuth = randomBoolean();
        if (useBearerTokenAuth) {
            final Tuple<String, String> token = super.createOAuthToken(username, password);
            return "Bearer " + token.v1();
        } else {
            return UsernamePasswordToken.basicAuthHeaderValue(username, password);
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void expectMetadata(final String apiKeyId, final Map<String, Object> expectedMetadata) throws IOException {
        final var request = new Request("GET", "_security/api_key/");
        request.addParameter("id", apiKeyId);
        final Response response = adminClient().performRequest(request);
        assertOK(response);
        try (XContentParser parser = responseAsParser(response)) {
            final var apiKeyResponse = GetApiKeyResponse.fromXContent(parser);
            assertThat(apiKeyResponse.getApiKeyInfos().length, equalTo(1));
            assertThat(apiKeyResponse.getApiKeyInfos()[0].getMetadata(), equalTo(expectedMetadata));
        }
    }
}
