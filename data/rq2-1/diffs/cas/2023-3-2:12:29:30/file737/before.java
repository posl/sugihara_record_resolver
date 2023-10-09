package org.apereo.cas;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.support.SimpleTestUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.DefaultPrincipalElectionStrategy;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.resolvers.ChainingPrincipalResolver;
import org.apereo.cas.authentication.principal.resolvers.EchoingPrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.junit.EnabledIfListeningOnPort;

import lombok.val;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link PersonDirectoryPrincipalResolverLdapTests}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@SpringBootTest(classes = BasePrincipalAttributeRepositoryTests.SharedTestConfiguration.class,
    properties = {
    "cas.authn.attribute-repository.ldap[0].base-dn=dc=example,dc=org",
    "cas.authn.attribute-repository.ldap[0].ldap-url=ldap://localhost:10389",
    "cas.authn.attribute-repository.ldap[0].search-filter=cn={cnuser}",
    "cas.authn.attribute-repository.ldap[0].attributes.cn=cn",
    "cas.authn.attribute-repository.ldap[0].attributes.description=description",
    "cas.authn.attribute-repository.ldap[0].attributes.entryDN=entryDN",
    "cas.authn.attribute-repository.ldap[0].bind-dn=cn=Directory Manager",
    "cas.authn.attribute-repository.ldap[0].bind-credential=password",
    "cas.authn.attribute-repository.ldap[0].use-all-query-attributes=false",
    "cas.authn.attribute-repository.ldap[0].query-attributes.principal=cnuser",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[0].type=DN_ATTRIBUTE_ENTRY",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[1].type=MERGE_ENTRIES",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[2].type=FOLLOW_SEARCH_REFERRAL",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[3].type=FOLLOW_SEARCH_RESULT_REFERENCE",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[4].type=ACTIVE_DIRECTORY",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[5].type=MERGE_ENTRIES",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[6].type=RECURSIVE_ENTRY",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[7].type=RANGE_ENTRY",
    "cas.authn.attribute-repository.ldap[0].search-entry-handlers[8].type=PRIMARY_GROUP"
})
@Tag("LdapAttributes")
@EnabledIfListeningOnPort(port = 10389)
public class PersonDirectoryPrincipalResolverLdapTests {
    @Autowired
    @Qualifier(PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY)
    private IPersonAttributeDao attributeRepository;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Test
    public void verifyResolver() {
        val attributeMerger = CoreAuthenticationUtils.getAttributeMerger(casProperties.getAuthn().getAttributeRepository().getCore().getMerger());
        val resolver = CoreAuthenticationUtils.newPersonDirectoryPrincipalResolver(PrincipalFactoryUtils.newPrincipalFactory(),
            this.attributeRepository, attributeMerger, casProperties.getPersonDirectory());
        val p = resolver.resolve(new UsernamePasswordCredential("admin", "password"),
            Optional.of(CoreAuthenticationTestUtils.getPrincipal("admin")),
            Optional.of(new SimpleTestUsernamePasswordAuthenticationHandler()));
        assertNotNull(p);
        assertTrue(p.getAttributes().containsKey("description"));
        assertTrue(p.getAttributes().containsKey("entryDN"));
    }

    @Test
    public void verifyChainedResolver() {
        val resolver = CoreAuthenticationUtils.newPersonDirectoryPrincipalResolver(PrincipalFactoryUtils.newPrincipalFactory(),
            this.attributeRepository,
            CoreAuthenticationUtils.getAttributeMerger(casProperties.getAuthn().getAttributeRepository().getCore().getMerger()),
            casProperties.getPersonDirectory());
        val chain = new ChainingPrincipalResolver(new DefaultPrincipalElectionStrategy(), casProperties);
        chain.setChain(Arrays.asList(new EchoingPrincipalResolver(), resolver));
        val attributes = new HashMap<String, List<Object>>(2);
        attributes.put("a1", List.of("v1"));
        attributes.put("a2", List.of("v2"));
        val p = chain.resolve(new UsernamePasswordCredential("admin", "password"),
            Optional.of(CoreAuthenticationTestUtils.getPrincipal("admin", attributes)),
            Optional.of(new SimpleTestUsernamePasswordAuthenticationHandler()));
        assertNotNull(p);
        assertTrue(p.getAttributes().containsKey("cn"));
        assertTrue(p.getAttributes().containsKey("a1"));
        assertTrue(p.getAttributes().containsKey("a2"));
    }
}
