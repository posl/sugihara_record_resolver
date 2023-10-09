package org.apereo.cas;

import org.apereo.cas.validation.AuthenticationPolicyAwareServiceTicketValidationAuthorizerTests;
import org.apereo.cas.validation.Cas10ProtocolValidationSpecificationTests;
import org.apereo.cas.validation.Cas20ProtocolValidationSpecificationTests;
import org.apereo.cas.validation.Cas20WithoutProxyingValidationSpecificationTests;
import org.apereo.cas.validation.CasProtocolVersionValidationSpecificationTests;
import org.apereo.cas.validation.ChainingCasProtocolValidationSpecificationTests;
import org.apereo.cas.validation.ImmutableAssertionTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This is {@link AllTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0-RC3
 */
@SelectClasses({
    Cas20ProtocolValidationSpecificationTests.class,
    ImmutableAssertionTests.class,
    CasProtocolVersionValidationSpecificationTests.class,
    ChainingCasProtocolValidationSpecificationTests.class,
    AuthenticationPolicyAwareServiceTicketValidationAuthorizerTests.class,
    Cas10ProtocolValidationSpecificationTests.class,
    Cas20WithoutProxyingValidationSpecificationTests.class
})
@Suite
public class AllTestsSuite {
}
