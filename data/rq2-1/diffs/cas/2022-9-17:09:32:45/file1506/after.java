package org.apereo.cas;

import org.apereo.cas.scim.v2.ScimV2PrincipalAttributeMapperTests;
import org.apereo.cas.scim.v2.ScimV2PrincipalProvisionerTests;
import org.apereo.cas.web.flow.PrincipalScimV2ProvisionerActionTests;
import org.apereo.cas.web.flow.PrincipalScimV2ProvisionerActionWithScimServerTests;
import org.apereo.cas.web.flow.ScimWebflowConfigurerTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This is {@link AllScimTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@SelectClasses({
    PrincipalScimV2ProvisionerActionWithScimServerTests.class,
    ScimWebflowConfigurerTests.class,
    ScimV2PrincipalProvisionerTests.class,
    ScimV2PrincipalAttributeMapperTests.class,
    PrincipalScimV2ProvisionerActionTests.class
})
@Suite
public class AllScimTestsSuite {
}
