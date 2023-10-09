package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.core.DefaultCouchDbConnectorFactory;
import org.apereo.cas.couchdb.tickets.TicketRepository;
import org.apereo.cas.ticket.TicketCatalog;
import org.apereo.cas.ticket.registry.CouchDbTicketRegistry;
import org.apereo.cas.ticket.registry.NoOpTicketRegistryCleaner;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistryCleaner;
import org.apereo.cas.ticket.serialization.TicketSerializationManager;
import org.apereo.cas.util.CoreTicketUtils;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.ektorp.impl.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CouchDbTicketRegistryConfiguration}.
 *
 * @author Timur Duehr
 * @since 5.3.0
 * @deprecated Since 7
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.TicketRegistry, module = "couchdb")
@AutoConfiguration
@Deprecated(since = "7.0.0")
public class CouchDbTicketRegistryConfiguration {

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "ticketRegistryCouchDbFactory")
    public CouchDbConnectorFactory ticketRegistryCouchDbFactory(final CasConfigurationProperties casProperties,
                                                                @Qualifier("defaultObjectMapperFactory")
                                                                final ObjectMapperFactory objectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getTicket().getRegistry().getCouchDb(), objectMapperFactory);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "ticketRegistryCouchDbRepository")
    public TicketRepository ticketRegistryCouchDbRepository(final CasConfigurationProperties casProperties,
                                                            @Qualifier("ticketRegistryCouchDbFactory")
                                                            final CouchDbConnectorFactory ticketRegistryCouchDbFactory) {
        val couchDbProperties = casProperties.getTicket().getRegistry().getCouchDb();
        val ticketRepository = new TicketRepository(ticketRegistryCouchDbFactory.getCouchDbConnector(), couchDbProperties.isCreateIfNotExists());
        ticketRepository.initStandardDesignDocument();
        return ticketRepository;
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "couchDbTicketRegistry")
    public TicketRegistry ticketRegistry(final CasConfigurationProperties casProperties,
                                         @Qualifier(TicketCatalog.BEAN_NAME)
                                         final TicketCatalog ticketCatalog,
                                         @Qualifier(TicketSerializationManager.BEAN_NAME)
                                         final TicketSerializationManager ticketSerializationManager,
                                         @Qualifier("ticketRegistryCouchDbRepository")
                                         final TicketRepository ticketRegistryCouchDbRepository) {
        val couchDb = casProperties.getTicket().getRegistry().getCouchDb();
        val cipherExecutor = CoreTicketUtils.newTicketRegistryCipherExecutor(couchDb.getCrypto(), "couch-db");
        return new CouchDbTicketRegistry(cipherExecutor, ticketSerializationManager, ticketCatalog, ticketRegistryCouchDbRepository, couchDb.getRetries());
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "couchDbTicketRegistryCleaner")
    @Lazy(false)
    public TicketRegistryCleaner ticketRegistryCleaner() {
        return NoOpTicketRegistryCleaner.getInstance();
    }
}
