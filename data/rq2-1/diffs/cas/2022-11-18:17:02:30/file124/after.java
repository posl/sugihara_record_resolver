package org.apereo.cas.ticket.registry;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.config.CasOAuth20ProtocolTicketCatalogConfiguration;
import org.apereo.cas.config.DynamoDbTicketRegistryConfiguration;
import org.apereo.cas.config.DynamoDbTicketRegistryTicketCatalogConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.services.RegisteredServiceCipherExecutor;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.accesstoken.OAuth20DefaultAccessTokenFactory;
import org.apereo.cas.ticket.code.OAuth20Code;
import org.apereo.cas.ticket.code.OAuth20DefaultOAuthCodeFactory;
import org.apereo.cas.ticket.expiration.NeverExpiresExpirationPolicy;
import org.apereo.cas.ticket.refreshtoken.OAuth20DefaultRefreshTokenFactory;
import org.apereo.cas.token.JwtBuilder;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.DefaultUniqueTicketIdGenerator;
import org.apereo.cas.util.TicketGrantingTicketIdGenerator;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.junit.EnabledIfListeningOnPort;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.core.SdkSystemSetting;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link DynamoDbTicketRegistryTests}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Tag("DynamoDb")
@Import({
    DynamoDbTicketRegistryConfiguration.class,
    DynamoDbTicketRegistryTicketCatalogConfiguration.class,
    CasOAuth20ProtocolTicketCatalogConfiguration.class
})
@TestPropertySource(
    properties = {
        "cas.ticket.registry.dynamo-db.endpoint=http://localhost:8000",
        "cas.ticket.registry.dynamo-db.drop-tables-on-startup=true",
        "cas.ticket.registry.dynamo-db.local-instance=true",
        "cas.ticket.registry.dynamo-db.region=us-east-1",
        "cas.authn.oauth.access-token.storage-name=test-oauthAccessTokensCache",
        "cas.authn.oauth.refresh-token.storage-name=test-oauthRefreshTokensCache",
        "cas.authn.oauth.code.storage-name=test-oauthCodesCache",
        "cas.authn.oauth.device-token.storage-name=test-oauthDeviceTokensCache",
        "cas.authn.oauth.device-user-code.storage-name=test-oauthDeviceUserCodesCache"
    })
@EnabledIfListeningOnPort(port = 8000)
@Getter
public class DynamoDbTicketRegistryTests extends BaseTicketRegistryTests {
    private static final int COUNT = 10_333;

    static {
        System.setProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property(), "AKIAIPPIGGUNIO74C63Z");
        System.setProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property(), "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");
    }

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier(TicketRegistry.BEAN_NAME)
    private TicketRegistry newTicketRegistry;

    @Autowired
    @Qualifier(ServicesManager.BEAN_NAME)
    private ServicesManager servicesManager;

    @RepeatedTest(2)
    public void verifyOAuthCodeCanBeAdded() throws Exception {
        val code = createOAuthCode();
        newTicketRegistry.addTicket(code);
        assertSame(1, newTicketRegistry.deleteTicket(code.getId()), "Wrong ticket count");
        assertNull(newTicketRegistry.getTicket(code.getId()));
    }

    @RepeatedTest(2)
    public void verifyAccessTokenCanBeAdded() throws Exception {
        val code = createOAuthCode();
        val jwtBuilder = new JwtBuilder(CipherExecutor.noOpOfSerializableToString(),
            servicesManager, RegisteredServiceCipherExecutor.noOp(), casProperties);
        val token = new OAuth20DefaultAccessTokenFactory(neverExpiresExpirationPolicyBuilder(), jwtBuilder, servicesManager)
            .create(RegisteredServiceTestUtils.getService(),
                RegisteredServiceTestUtils.getAuthentication(), new MockTicketGrantingTicket("casuser"),
                CollectionUtils.wrapSet("1", "2"), code.getId(), "clientId1234567", new HashMap<>(),
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        newTicketRegistry.addTicket(token);
        assertSame(1, newTicketRegistry.deleteTicket(token.getId()), "Wrong ticket count");
        assertNull(newTicketRegistry.getTicket(token.getId()));
    }

    @RepeatedTest(2)
    public void verifyRefreshTokenCanBeAdded() throws Exception {
        val token = new OAuth20DefaultRefreshTokenFactory(neverExpiresExpirationPolicyBuilder(), servicesManager)
            .create(RegisteredServiceTestUtils.getService(),
                RegisteredServiceTestUtils.getAuthentication(), new MockTicketGrantingTicket("casuser"),
                CollectionUtils.wrapSet("1", "2"),
                "clientId1234567", StringUtils.EMPTY, new HashMap<>(),
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        newTicketRegistry.addTicket(token);
        assertSame(1, newTicketRegistry.deleteTicket(token.getId()), "Wrong ticket count");
        assertNull(newTicketRegistry.getTicket(token.getId()));
    }

    @RepeatedTest(2)
    public void verifyLargeDataset() throws Exception {
        val ticketGrantingTicketToAdd = Stream.generate(() -> {
                val tgtId = new TicketGrantingTicketIdGenerator(10, StringUtils.EMPTY)
                    .getNewTicketId(TicketGrantingTicket.PREFIX);
                return new TicketGrantingTicketImpl(tgtId,
                    CoreAuthenticationTestUtils.getAuthentication(), NeverExpiresExpirationPolicy.INSTANCE);
            })
            .limit(COUNT);
        var stopwatch = new StopWatch();
        newTicketRegistry.addTicket(ticketGrantingTicketToAdd);
        stopwatch.start();
        assertEquals(COUNT, newTicketRegistry.stream().count());
        stopwatch.stop();
        var time = stopwatch.getTime(TimeUnit.SECONDS);
        assertTrue(time <= 15);
    }

    private OAuth20Code createOAuthCode() {
        return new OAuth20DefaultOAuthCodeFactory(new DefaultUniqueTicketIdGenerator(),
            neverExpiresExpirationPolicyBuilder(), servicesManager, CipherExecutor.noOpOfStringToString())
            .create(RegisteredServiceTestUtils.getService(),
                RegisteredServiceTestUtils.getAuthentication(), new MockTicketGrantingTicket("casuser"),
                CollectionUtils.wrapSet("1", "2"), "code-challenge",
                "code-challenge-method", "clientId1234567", new HashMap<>(),
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
    }
}
