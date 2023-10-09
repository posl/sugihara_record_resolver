/*
 * This file is generated by jOOQ.
 */
package stroom.security.impl.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.security.impl.db.jooq.Indexes;
import stroom.security.impl.db.jooq.Keys;
import stroom.security.impl.db.jooq.Stroom;
import stroom.security.impl.db.jooq.tables.records.AppPermissionRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AppPermission extends TableImpl<AppPermissionRecord> {

    private static final long serialVersionUID = 202952632;

    /**
     * The reference instance of <code>stroom.app_permission</code>
     */
    public static final AppPermission APP_PERMISSION = new AppPermission();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AppPermissionRecord> getRecordType() {
        return AppPermissionRecord.class;
    }

    /**
     * The column <code>stroom.app_permission.id</code>.
     */
    public final TableField<AppPermissionRecord, Long> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.app_permission.user_uuid</code>.
     */
    public final TableField<AppPermissionRecord, String> USER_UUID = createField(DSL.name("user_uuid"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.app_permission.permission</code>.
     */
    public final TableField<AppPermissionRecord, String> PERMISSION = createField(DSL.name("permission"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * Create a <code>stroom.app_permission</code> table reference
     */
    public AppPermission() {
        this(DSL.name("app_permission"), null);
    }

    /**
     * Create an aliased <code>stroom.app_permission</code> table reference
     */
    public AppPermission(String alias) {
        this(DSL.name(alias), APP_PERMISSION);
    }

    /**
     * Create an aliased <code>stroom.app_permission</code> table reference
     */
    public AppPermission(Name alias) {
        this(alias, APP_PERMISSION);
    }

    private AppPermission(Name alias, Table<AppPermissionRecord> aliased) {
        this(alias, aliased, null);
    }

    private AppPermission(Name alias, Table<AppPermissionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> AppPermission(Table<O> child, ForeignKey<O, AppPermissionRecord> key) {
        super(child, key, APP_PERMISSION);
    }

    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.APP_PERMISSION_APP_PERMISSION_USER_UUID_PERMISSION_IDX, Indexes.APP_PERMISSION_PRIMARY);
    }

    @Override
    public Identity<AppPermissionRecord, Long> getIdentity() {
        return Keys.IDENTITY_APP_PERMISSION;
    }

    @Override
    public UniqueKey<AppPermissionRecord> getPrimaryKey() {
        return Keys.KEY_APP_PERMISSION_PRIMARY;
    }

    @Override
    public List<UniqueKey<AppPermissionRecord>> getKeys() {
        return Arrays.<UniqueKey<AppPermissionRecord>>asList(Keys.KEY_APP_PERMISSION_PRIMARY, Keys.KEY_APP_PERMISSION_APP_PERMISSION_USER_UUID_PERMISSION_IDX);
    }

    @Override
    public List<ForeignKey<AppPermissionRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<AppPermissionRecord, ?>>asList(Keys.APP_PERMISSION_USER_UUID);
    }

    public StroomUser stroomUser() {
        return new StroomUser(this, Keys.APP_PERMISSION_USER_UUID);
    }

    @Override
    public AppPermission as(String alias) {
        return new AppPermission(DSL.name(alias), this);
    }

    @Override
    public AppPermission as(Name alias) {
        return new AppPermission(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AppPermission rename(String name) {
        return new AppPermission(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public AppPermission rename(Name name) {
        return new AppPermission(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Long, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}