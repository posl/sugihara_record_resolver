/*
 * This file is generated by jOOQ.
 */
package stroom.analytics.impl.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import stroom.analytics.impl.db.jooq.Indexes;
import stroom.analytics.impl.db.jooq.Keys;
import stroom.analytics.impl.db.jooq.Stroom;
import stroom.analytics.impl.db.jooq.tables.records.AnalyticNotificationRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AnalyticNotification extends TableImpl<AnalyticNotificationRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.analytic_notification</code>
     */
    public static final AnalyticNotification ANALYTIC_NOTIFICATION = new AnalyticNotification();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AnalyticNotificationRecord> getRecordType() {
        return AnalyticNotificationRecord.class;
    }

    /**
     * The column <code>stroom.analytic_notification.uuid</code>.
     */
    public final TableField<AnalyticNotificationRecord, String> UUID = createField(DSL.name("uuid"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.version</code>.
     */
    public final TableField<AnalyticNotificationRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.create_time_ms</code>.
     */
    public final TableField<AnalyticNotificationRecord, Long> CREATE_TIME_MS = createField(DSL.name("create_time_ms"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.create_user</code>.
     */
    public final TableField<AnalyticNotificationRecord, String> CREATE_USER = createField(DSL.name("create_user"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.update_time_ms</code>.
     */
    public final TableField<AnalyticNotificationRecord, Long> UPDATE_TIME_MS = createField(DSL.name("update_time_ms"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.update_user</code>.
     */
    public final TableField<AnalyticNotificationRecord, String> UPDATE_USER = createField(DSL.name("update_user"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.analytic_uuid</code>.
     */
    public final TableField<AnalyticNotificationRecord, String> ANALYTIC_UUID = createField(DSL.name("analytic_uuid"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.analytic_notification.config</code>.
     */
    public final TableField<AnalyticNotificationRecord, String> CONFIG = createField(DSL.name("config"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>stroom.analytic_notification.enabled</code>.
     */
    public final TableField<AnalyticNotificationRecord, Boolean> ENABLED = createField(DSL.name("enabled"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.inline("0", SQLDataType.BOOLEAN)), this, "");

    private AnalyticNotification(Name alias, Table<AnalyticNotificationRecord> aliased) {
        this(alias, aliased, null);
    }

    private AnalyticNotification(Name alias, Table<AnalyticNotificationRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.analytic_notification</code> table
     * reference
     */
    public AnalyticNotification(String alias) {
        this(DSL.name(alias), ANALYTIC_NOTIFICATION);
    }

    /**
     * Create an aliased <code>stroom.analytic_notification</code> table
     * reference
     */
    public AnalyticNotification(Name alias) {
        this(alias, ANALYTIC_NOTIFICATION);
    }

    /**
     * Create a <code>stroom.analytic_notification</code> table reference
     */
    public AnalyticNotification() {
        this(DSL.name("analytic_notification"), null);
    }

    public <O extends Record> AnalyticNotification(Table<O> child, ForeignKey<O, AnalyticNotificationRecord> key) {
        super(child, key, ANALYTIC_NOTIFICATION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.ANALYTIC_NOTIFICATION_ANALYTIC_NOTIFICATION_ANALYTIC_UUID_IDX);
    }

    @Override
    public UniqueKey<AnalyticNotificationRecord> getPrimaryKey() {
        return Keys.KEY_ANALYTIC_NOTIFICATION_PRIMARY;
    }

    @Override
    public TableField<AnalyticNotificationRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    @Override
    public AnalyticNotification as(String alias) {
        return new AnalyticNotification(DSL.name(alias), this);
    }

    @Override
    public AnalyticNotification as(Name alias) {
        return new AnalyticNotification(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AnalyticNotification rename(String name) {
        return new AnalyticNotification(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public AnalyticNotification rename(Name name) {
        return new AnalyticNotification(name, null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<String, Integer, Long, String, Long, String, String, String, Boolean> fieldsRow() {
        return (Row9) super.fieldsRow();
    }
}