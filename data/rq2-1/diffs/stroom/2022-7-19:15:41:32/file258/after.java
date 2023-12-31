/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables;


import stroom.data.store.impl.fs.db.jooq.Keys;
import stroom.data.store.impl.fs.db.jooq.Stroom;
import stroom.data.store.impl.fs.db.jooq.tables.records.FsFeedPathRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FsFeedPath extends TableImpl<FsFeedPathRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.fs_feed_path</code>
     */
    public static final FsFeedPath FS_FEED_PATH = new FsFeedPath();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FsFeedPathRecord> getRecordType() {
        return FsFeedPathRecord.class;
    }

    /**
     * The column <code>stroom.fs_feed_path.id</code>.
     */
    public final TableField<FsFeedPathRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.fs_feed_path.name</code>.
     */
    public final TableField<FsFeedPathRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.fs_feed_path.path</code>.
     */
    public final TableField<FsFeedPathRecord, String> PATH = createField(DSL.name("path"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    private FsFeedPath(Name alias, Table<FsFeedPathRecord> aliased) {
        this(alias, aliased, null);
    }

    private FsFeedPath(Name alias, Table<FsFeedPathRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.fs_feed_path</code> table reference
     */
    public FsFeedPath(String alias) {
        this(DSL.name(alias), FS_FEED_PATH);
    }

    /**
     * Create an aliased <code>stroom.fs_feed_path</code> table reference
     */
    public FsFeedPath(Name alias) {
        this(alias, FS_FEED_PATH);
    }

    /**
     * Create a <code>stroom.fs_feed_path</code> table reference
     */
    public FsFeedPath() {
        this(DSL.name("fs_feed_path"), null);
    }

    public <O extends Record> FsFeedPath(Table<O> child, ForeignKey<O, FsFeedPathRecord> key) {
        super(child, key, FS_FEED_PATH);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<FsFeedPathRecord, Integer> getIdentity() {
        return (Identity<FsFeedPathRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<FsFeedPathRecord> getPrimaryKey() {
        return Keys.KEY_FS_FEED_PATH_PRIMARY;
    }

    @Override
    public List<UniqueKey<FsFeedPathRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_FS_FEED_PATH_NAME);
    }

    @Override
    public FsFeedPath as(String alias) {
        return new FsFeedPath(DSL.name(alias), this);
    }

    @Override
    public FsFeedPath as(Name alias) {
        return new FsFeedPath(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FsFeedPath rename(String name) {
        return new FsFeedPath(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FsFeedPath rename(Name name) {
        return new FsFeedPath(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
