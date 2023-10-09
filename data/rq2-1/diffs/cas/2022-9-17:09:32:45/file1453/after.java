package org.apereo.cas.support.saml.web.idp.metadata;

import org.apereo.cas.audit.AuditableContext;
import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.SamlUtils;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.cache.SamlRegisteredServiceCachingMetadataResolver;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.web.BaseCasActuatorEndpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.criterion.SatisfyAnyCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.metadata.criteria.entity.impl.EvaluableEntityRoleEntityDescriptorCriterion;
import org.opensaml.saml.saml2.common.TimeBoundSAMLObject;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This is {@link SamlRegisteredServiceCachedMetadataEndpoint}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Slf4j
@Endpoint(id = "samlIdPRegisteredServiceMetadataCache", enableByDefault = false)
public class SamlRegisteredServiceCachedMetadataEndpoint extends BaseCasActuatorEndpoint {
    private final SamlRegisteredServiceCachingMetadataResolver cachingMetadataResolver;

    private final ServicesManager servicesManager;

    private final AuditableExecution registeredServiceAccessStrategyEnforcer;

    private final OpenSamlConfigBean openSamlConfigBean;

    public SamlRegisteredServiceCachedMetadataEndpoint(final CasConfigurationProperties casProperties,
                                                       final SamlRegisteredServiceCachingMetadataResolver cachingMetadataResolver,
                                                       final ServicesManager servicesManager,
                                                       final AuditableExecution registeredServiceAccessStrategyEnforcer,
                                                       final OpenSamlConfigBean openSamlConfigBean) {
        super(casProperties);
        this.cachingMetadataResolver = cachingMetadataResolver;
        this.servicesManager = servicesManager;
        this.registeredServiceAccessStrategyEnforcer = registeredServiceAccessStrategyEnforcer;
        this.openSamlConfigBean = openSamlConfigBean;
    }

    /**
     * Invalidate.
     *
     * @param serviceId the service id
     */
    @DeleteOperation
    @Operation(summary = "Invalidate SAML2 metadata cache using an entity id.", parameters = @Parameter(name = "serviceId"))
    public void invalidate(
        @Nullable
        final String serviceId) {
        if (StringUtils.isBlank(serviceId)) {
            cachingMetadataResolver.invalidate();
            LOGGER.info("Cleared SAML2 registered service metadata cache");
        } else {
            val registeredService = findRegisteredService(serviceId);
            val criteriaSet = new CriteriaSet();
            criteriaSet.add(new EntityIdCriterion(serviceId));
            criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
            cachingMetadataResolver.invalidate(registeredService, criteriaSet);
            LOGGER.info("Invalidated SAML2 registered service metadata cache entry for [{}]", registeredService);
        }
    }

    /**
     * Gets cached metadata object.
     *
     * @param serviceId the service id
     * @param entityId  the entity id
     * @return the cached metadata object
     */
    @ReadOperation(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get SAML2 cached metadata for a SAML2 registered service", parameters = {
        @Parameter(name = "serviceId", required = true),
        @Parameter(name = "entityId")
    })
    public Map<String, Object> getCachedMetadataObject(final String serviceId,
                                                       @Nullable
                                                       final String entityId) {
        try {
            val registeredService = findRegisteredService(serviceId);
            val criteriaSet = new CriteriaSet();
            if (StringUtils.isNotBlank(entityId)) {
                criteriaSet.add(new EntityIdCriterion(entityId));
                criteriaSet.add(new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
            } else {
                criteriaSet.add(new EvaluableEntityRoleEntityDescriptorCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME));
                criteriaSet.add(new SatisfyAnyCriterion(true));
            }
            val metadataResolverResult = cachingMetadataResolver.resolve(registeredService, criteriaSet);
            val iteration = metadataResolverResult.getMetadataResolver().resolve(criteriaSet).spliterator();
            return StreamSupport.stream(iteration, false)
                .filter(TimeBoundSAMLObject::isValid)
                .map(entity -> {
                    val details = CollectionUtils.wrap(
                        "cachedInstant", metadataResolverResult.getCachedInstant(),
                        "metadata", SamlUtils.transformSamlObject(openSamlConfigBean, entity).toString());
                    return Pair.of(entity.getEntityID(), details);
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
            return CollectionUtils.wrap("error", e.getMessage());
        }
    }

    private SamlRegisteredService findRegisteredService(final String serviceId) {
        var matchedServices = (Collection<RegisteredService>) null;
        if (NumberUtils.isCreatable(serviceId)) {
            val id = Long.parseLong(serviceId);
            matchedServices = List.of(servicesManager.findServiceBy(id, SamlRegisteredService.class));
        } else {
            matchedServices = servicesManager.findServiceBy(svc -> svc instanceof SamlRegisteredService
                                                                   && (svc.getName().equalsIgnoreCase(serviceId) || svc.getServiceId().equalsIgnoreCase(serviceId)));
        }
        if (matchedServices.isEmpty()) {
            throw new IllegalArgumentException("Unable to locate service " + serviceId);
        }
        val registeredService = (SamlRegisteredService) matchedServices.iterator().next();
        val ctx = AuditableContext.builder()
            .registeredService(registeredService)
            .build();
        val result = this.registeredServiceAccessStrategyEnforcer.execute(ctx);
        result.throwExceptionIfNeeded();
        return registeredService;
    }
}
