package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.ticket.serialization.TicketSerializationExecutionPlanConfigurer;
import org.apereo.cas.uma.ticket.permission.DefaultUmaPermissionTicket;
import org.apereo.cas.uma.ticket.permission.UmaPermissionTicket;
import org.apereo.cas.util.serialization.AbstractJacksonBackedStringSerializer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CasOAuthUmaTicketSerializationConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.OAuth, module = "uma")
@AutoConfiguration
public class CasOAuthUmaTicketSerializationConfiguration {

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public TicketSerializationExecutionPlanConfigurer oauthUmaTicketSerializationExecutionPlanConfigurer() {
        return plan -> {
            plan.registerTicketSerializer(new UmaPermissionTicketStringSerializer());
            plan.registerTicketSerializer(UmaPermissionTicket.class.getName(), new UmaPermissionTicketStringSerializer());
        };
    }

    private static class UmaPermissionTicketStringSerializer extends AbstractJacksonBackedStringSerializer<DefaultUmaPermissionTicket> {
        private static final long serialVersionUID = -2198623586274810263L;

        @Override
        public Class<DefaultUmaPermissionTicket> getTypeToSerialize() {
            return DefaultUmaPermissionTicket.class;
        }
    }
}
