package org.apereo.cas;

import org.apereo.cas.support.pac4j.DelegatedClientJacksonModuleTests;
import org.apereo.cas.support.pac4j.authentication.DelegatedClientAuthenticationMetaDataPopulatorTests;
import org.apereo.cas.support.pac4j.authentication.attributes.GroovyAttributeConverterTests;
import org.apereo.cas.support.pac4j.authentication.handler.support.DelegatedClientAuthenticationHandlerTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This is {@link AllTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@SelectClasses({
    GroovyAttributeConverterTests.class,
    DelegatedClientAuthenticationHandlerTests.class,
    DelegatedClientJacksonModuleTests.class,
    DelegatedClientAuthenticationMetaDataPopulatorTests.class
})
@Suite
public class AllTestsSuite {
}
