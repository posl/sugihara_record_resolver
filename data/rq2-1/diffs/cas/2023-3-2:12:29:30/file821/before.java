package org.apereo.cas.support.saml.metadata.resolver;

import org.apereo.cas.audit.AuditActionResolvers;
import org.apereo.cas.audit.AuditResourceResolvers;
import org.apereo.cas.audit.AuditableActions;
import org.apereo.cas.configuration.model.support.saml.idp.SamlIdPProperties;
import org.apereo.cas.couchdb.saml.CouchDbSamlMetadataDocument;
import org.apereo.cas.couchdb.saml.SamlMetadataDocumentCouchDbRepository;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlMetadataDocument;
import org.apereo.cas.support.saml.services.idp.metadata.cache.resolver.BaseSamlRegisteredServiceMetadataResolver;
import org.apereo.cas.util.LoggingUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.shibboleth.shared.resolver.CriteriaSet;
import org.apereo.inspektr.audit.annotation.Audit;
import org.opensaml.saml.metadata.resolver.MetadataResolver;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is {@link CouchDbSamlRegisteredServiceMetadataResolver}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 */
@Slf4j
public class CouchDbSamlRegisteredServiceMetadataResolver extends BaseSamlRegisteredServiceMetadataResolver {

    private final SamlMetadataDocumentCouchDbRepository couchDb;

    public CouchDbSamlRegisteredServiceMetadataResolver(final SamlIdPProperties idp, final OpenSamlConfigBean openSamlConfigBean,
                                                        final SamlMetadataDocumentCouchDbRepository couchDb) {
        super(idp, openSamlConfigBean);
        this.couchDb = couchDb;
    }

    @Audit(action = AuditableActions.SAML2_METADATA_RESOLUTION,
        actionResolverName = AuditActionResolvers.SAML2_METADATA_RESOLUTION_ACTION_RESOLVER,
        resourceResolverName = AuditResourceResolvers.SAML2_METADATA_RESOLUTION_RESOURCE_RESOLVER)
    @Override
    public Collection<MetadataResolver> resolve(final SamlRegisteredService service, final CriteriaSet criteriaSet) {
        return couchDb.getAll().stream().map(doc -> buildMetadataResolverFrom(service, doc)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public boolean supports(final SamlRegisteredService service) {
        try {
            val metadataLocation = service.getMetadataLocation();
            return metadataLocation.trim().startsWith("couchdb://");
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return false;
    }

    @Override
    public void saveOrUpdate(final SamlMetadataDocument document) {
        val coudbDbDocument = couchDb.findFirstByName(document.getName());
        if (coudbDbDocument == null) {
            couchDb.add(new CouchDbSamlMetadataDocument(document));
        } else {
            couchDb.update(coudbDbDocument.merge(document));
        }
    }

    @Override
    public boolean isAvailable(final SamlRegisteredService service) {
        return supports(service);
    }
}
