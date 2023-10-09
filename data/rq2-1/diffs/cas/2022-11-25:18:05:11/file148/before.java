package org.apereo.cas.audit.spi;

import org.apereo.cas.util.DateTimeUtils;

import lombok.val;
import org.apereo.inspektr.audit.AuditActionContext;
import org.apereo.inspektr.audit.AuditTrailManager;
import org.apereo.inspektr.audit.FilterAndDelegateAuditTrailManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link FilterAndDelegateAuditTrailManagerTests}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Tag("Audits")
@SuppressWarnings("JavaUtilDate")
public class FilterAndDelegateAuditTrailManagerTests {

    @Test
    public void verifyExcludeOperationForAllActions() {
        val ctx = new AuditActionContext("casuser", "TEST", "TEST",
            "CAS", new Date(), "1.2.3.4",
            "1.2.3.4", UUID.randomUUID().toString());
        val mock = new MockAuditTrailManager();
        val mgr = new FilterAndDelegateAuditTrailManager(List.of(mock), List.of("*"), List.of("TES.+"));
        mgr.record(ctx);
        assertTrue(mock.getAuditRecords().isEmpty());
    }
    
    @Test
    public void verifyOperationForAllActions() {
        val ctx = new AuditActionContext("casuser", "TEST", "TEST",
            "CAS", new Date(), "1.2.3.4",
            "1.2.3.4", UUID.randomUUID().toString());
        val mock = new MockAuditTrailManager();
        val mgr = new FilterAndDelegateAuditTrailManager(List.of(mock), List.of("*"), List.of());
        mgr.record(ctx);
        assertFalse(mock.getAuditRecords().isEmpty());
    }

    @Test
    public void verifyOperationForAllSupportedActions() {
        val ctx = new AuditActionContext("casuser", "TEST", "TEST",
            "CAS", new Date(), "1.2.3.4",
            "1.2.3.4", UUID.randomUUID().toString());
        val mock = new MockAuditTrailManager();
        val mgr = new FilterAndDelegateAuditTrailManager(List.of(mock), List.of("TEST.*"), List.of());
        mgr.record(ctx);
        assertFalse(mock.getAuditRecords().isEmpty());
    }

    @Test
    public void verifyOperationForUnmatchedActions() {
        val ctx = new AuditActionContext("casuser", "TEST", "TEST",
            "CAS", new Date(), "1.2.3.4",
            "1.2.3.4", UUID.randomUUID().toString());
        val mock = new MockAuditTrailManager();
        val mgr = new FilterAndDelegateAuditTrailManager(List.of(mock), List.of("PASSED.*"), List.of());
        mgr.record(ctx);
        assertTrue(mock.getAuditRecords().isEmpty());
    }

    @Test
    public void verifyAuditRecordsSinceDate() {
        val ctx = new AuditActionContext("casuser", "TEST", "TEST",
            "CAS",
            DateTimeUtils.dateOf(LocalDateTime.now(ZoneOffset.UTC).plusDays(1)),
            "1.2.3.4",
            "1.2.3.4", UUID.randomUUID().toString());
        val mock = new MockAuditTrailManager();
        val mgr = new FilterAndDelegateAuditTrailManager(List.of(mock), List.of("TEST.*"), List.of());
        mgr.record(ctx);
        assertFalse(mock.getAuditRecords().isEmpty());
        val criteria = Map.<AuditTrailManager.WhereClauseFields, Object>of(AuditTrailManager.WhereClauseFields.DATE, LocalDate.now(ZoneOffset.UTC));
        assertEquals(1, mgr.getAuditRecords(criteria).size());
        assertDoesNotThrow(mgr::removeAll);
    }
}
