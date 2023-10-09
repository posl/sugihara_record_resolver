
package org.apereo.cas;

import org.apereo.cas.pm.ActiveDirectoryPasswordManagementServiceTests;
import org.apereo.cas.pm.LdapPasswordManagementServiceTests;
import org.apereo.cas.pm.OpenLdapPasswordManagementServiceTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This is {@link AllTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@SelectClasses({
    ActiveDirectoryPasswordManagementServiceTests.class,
    OpenLdapPasswordManagementServiceTests.class,
    LdapPasswordManagementServiceTests.class
})
@Suite
public class AllTestsSuite {
}
