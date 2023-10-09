package org.apereo.cas;

import org.apereo.cas.support.saml.web.idp.audit.SamlMetadataResolverAuditResourceResolverTests;
import org.apereo.cas.support.saml.web.idp.audit.SamlRequestAuditResourceResolverTests;
import org.apereo.cas.support.saml.web.idp.audit.SamlResponseAuditPrincipalIdProviderTests;
import org.apereo.cas.support.saml.web.idp.audit.SamlResponseAuditResourceResolverTests;
import org.apereo.cas.support.saml.web.idp.web.SamlIdPErrorControllerTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This is {@link AllTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0-RC3
 */
@SelectClasses({
    SamlIdPErrorControllerTests.class,
    SamlMetadataResolverAuditResourceResolverTests.class,
    SamlRequestAuditResourceResolverTests.class,
    SamlResponseAuditPrincipalIdProviderTests.class,
    SamlResponseAuditResourceResolverTests.class
})
@Suite
public class AllTestsSuite {
}
