/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables.records;


import stroom.meta.impl.db.jooq.tables.MetaRetentionTracker;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaRetentionTrackerRecord extends UpdatableRecordImpl<MetaRetentionTrackerRecord> implements Record3<String, String, Long> {

    private static final long serialVersionUID = 619300394;

    /**
     * Setter for <code>stroom.meta_retention_tracker.retention_rules_version</code>.
     */
    public void setRetentionRulesVersion(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.meta_retention_tracker.retention_rules_version</code>.
     */
    public String getRetentionRulesVersion() {
        return (String) get(0);
    }

    /**
     * Setter for <code>stroom.meta_retention_tracker.rule_age</code>.
     */
    public void setRuleAge(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.meta_retention_tracker.rule_age</code>.
     */
    public String getRuleAge() {
        return (String) get(1);
    }

    /**
     * Setter for <code>stroom.meta_retention_tracker.last_run_time</code>.
     */
    public void setLastRunTime(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.meta_retention_tracker.last_run_time</code>.
     */
    public Long getLastRunTime() {
        return (Long) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<String, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<String, String, Long> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<String, String, Long> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return MetaRetentionTracker.META_RETENTION_TRACKER.RETENTION_RULES_VERSION;
    }

    @Override
    public Field<String> field2() {
        return MetaRetentionTracker.META_RETENTION_TRACKER.RULE_AGE;
    }

    @Override
    public Field<Long> field3() {
        return MetaRetentionTracker.META_RETENTION_TRACKER.LAST_RUN_TIME;
    }

    @Override
    public String component1() {
        return getRetentionRulesVersion();
    }

    @Override
    public String component2() {
        return getRuleAge();
    }

    @Override
    public Long component3() {
        return getLastRunTime();
    }

    @Override
    public String value1() {
        return getRetentionRulesVersion();
    }

    @Override
    public String value2() {
        return getRuleAge();
    }

    @Override
    public Long value3() {
        return getLastRunTime();
    }

    @Override
    public MetaRetentionTrackerRecord value1(String value) {
        setRetentionRulesVersion(value);
        return this;
    }

    @Override
    public MetaRetentionTrackerRecord value2(String value) {
        setRuleAge(value);
        return this;
    }

    @Override
    public MetaRetentionTrackerRecord value3(Long value) {
        setLastRunTime(value);
        return this;
    }

    @Override
    public MetaRetentionTrackerRecord values(String value1, String value2, Long value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MetaRetentionTrackerRecord
     */
    public MetaRetentionTrackerRecord() {
        super(MetaRetentionTracker.META_RETENTION_TRACKER);
    }

    /**
     * Create a detached, initialised MetaRetentionTrackerRecord
     */
    public MetaRetentionTrackerRecord(String retentionRulesVersion, String ruleAge, Long lastRunTime) {
        super(MetaRetentionTracker.META_RETENTION_TRACKER);

        set(0, retentionRulesVersion);
        set(1, ruleAge);
        set(2, lastRunTime);
    }
}