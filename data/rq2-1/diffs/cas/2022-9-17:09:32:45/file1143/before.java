package org.apereo.cas;

import org.apereo.cas.ticket.registry.EhCache3TerracottaTicketRegistryTests;
import org.apereo.cas.ticket.registry.EhCache3TicketRegistryTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


/**
 * Test suite that runs all test in a batch.
 *
 * @author Hal Deadman
 * @since 6.2.0
 */
@SelectClasses({
    EhCache3TicketRegistryTests.class,
    EhCache3TerracottaTicketRegistryTests.class
})
@Suite
public class AllTestsSuite {
}
