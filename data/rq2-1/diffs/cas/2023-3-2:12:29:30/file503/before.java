package org.apereo.cas.ticket.registry;

import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationHandlersConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationMetadataConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPolicyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasPersonDirectoryConfiguration;
import org.apereo.cas.config.CasTicketCatalogConfigurationValuesProvider;
import org.apereo.cas.config.HazelcastTicketRegistryConfiguration;
import org.apereo.cas.config.HazelcastTicketRegistryTicketCatalogConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;

import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dmitriy Kopylenko
 * @since 4.2.0
 */
@SpringBootTest(classes = {
    HazelcastTicketRegistryConfiguration.class,
    CasCoreTicketsConfiguration.class,
    CasCoreTicketIdGeneratorsConfiguration.class,
    RefreshAutoConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    HazelcastTicketRegistryTicketCatalogConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasPersonDirectoryConfiguration.class,
    CasCoreLogoutConfiguration.class,
    CasCoreAuthenticationConfiguration.class,
    CasCoreServicesAuthenticationConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreAuthenticationPolicyConfiguration.class,
    CasCoreAuthenticationMetadataConfiguration.class,
    CasCoreAuthenticationSupportConfiguration.class,
    CasCoreAuthenticationHandlersConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCoreConfiguration.class,
    CasCoreNotificationsConfiguration.class,
    CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreWebConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class
},
    properties = {
        "cas.ticket.registry.hazelcast.cluster.core.instance-name=samplelocalhostinstance",
        "cas.ticket.registry.hazelcast.cluster.network.port=5702"
    })
@Slf4j
@Tag("Hazelcast")
public class DefaultHazelcastInstanceConfigurationTests {
    @Autowired
    @Qualifier("casTicketRegistryHazelcastInstance")
    private HazelcastInstance hzInstance;

    @Test
    public void correctHazelcastInstanceIsCreated() {
        assertNotNull(this.hzInstance);
        val config = this.hzInstance.getConfig();
        assertFalse(config.getNetworkConfig().getJoin().getMulticastConfig().isEnabled());
        assertEquals(List.of("localhost"), config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers());
        assertTrue(config.getNetworkConfig().isPortAutoIncrement());
        assertTrue(config.getManagementCenterConfig().isScriptingEnabled());
        assertEquals(5702, config.getNetworkConfig().getPort());
        val mapConfigs = config.getMapConfigs();
        mapConfigs.forEach((key, value) -> LOGGER.info("Hazelcast map key [{}]", key));
        assertTrue(mapConfigs.containsKey(CasTicketCatalogConfigurationValuesProvider.STORAGE_NAME_PROXY_TICKET));
        assertTrue(mapConfigs.containsKey(CasTicketCatalogConfigurationValuesProvider.STORAGE_NAME_PROXY_GRANTING_TICKETS));
        assertTrue(mapConfigs.containsKey(CasTicketCatalogConfigurationValuesProvider.STORAGE_NAME_SERVICE_TICKETS));
        assertTrue(mapConfigs.containsKey(CasTicketCatalogConfigurationValuesProvider.STORAGE_NAME_TICKET_GRANTING_TICKETS));
        assertTrue(mapConfigs.containsKey(CasTicketCatalogConfigurationValuesProvider.STORAGE_NAME_TRANSIENT_SESSION_TICKETS));
    }

    @AfterEach
    public void shutdownHz() {
        LOGGER.info("Shutting down hazelcast instance [{}]", this.hzInstance.getConfig().getInstanceName());
        this.hzInstance.shutdown();
        while (this.hzInstance.getLifecycleService().isRunning()) {
            LOGGER.info("Waiting for instances to shut down");
        }
    }
}
