package org.apereo.cas.support.saml.idp.metadata;

import org.apereo.cas.couchdb.saml.CouchDbSamlIdPMetadataDocument;
import org.apereo.cas.couchdb.saml.SamlIdPMetadataCouchDbRepository;
import org.apereo.cas.support.saml.idp.metadata.generator.BaseSamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

/**
 * This is {@link CouchDbSamlIdPMetadataGenerator}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@Deprecated(since = "7.0.0")
public class CouchDbSamlIdPMetadataGenerator extends BaseSamlIdPMetadataGenerator {

    private final SamlIdPMetadataCouchDbRepository couchDb;

    public CouchDbSamlIdPMetadataGenerator(final SamlIdPMetadataGeneratorConfigurationContext context,
                                           final SamlIdPMetadataCouchDbRepository couchDb) {
        super(context);
        this.couchDb = couchDb;
    }

    @Override
    protected SamlIdPMetadataDocument finalizeMetadataDocument(final SamlIdPMetadataDocument doc,
                                                               final Optional<SamlRegisteredService> registeredService) {
        var couchDoc = registeredService.isPresent()
            ? couchDb.getForService(registeredService, getAppliesToFor(registeredService))
            : couchDb.getForAll();
        if (couchDoc == null) {
            couchDoc = new CouchDbSamlIdPMetadataDocument(doc);
            couchDoc.setAppliesTo(getAppliesToFor(registeredService));
            couchDb.add(couchDoc);
        } else {
            couchDb.update(couchDoc.merge(doc));
        }
        return couchDoc;
    }

    @Override
    public Pair<String, String> buildSelfSignedEncryptionCert(final Optional<SamlRegisteredService> registeredService) throws Exception {
        return generateCertificateAndKey();
    }

    @Override
    public Pair<String, String> buildSelfSignedSigningCert(final Optional<SamlRegisteredService> registeredService) throws Exception {
        return generateCertificateAndKey();
    }
}
