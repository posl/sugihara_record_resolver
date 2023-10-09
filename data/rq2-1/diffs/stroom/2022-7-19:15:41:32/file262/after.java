/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables;


import stroom.data.store.impl.fs.db.jooq.Keys;
import stroom.data.store.impl.fs.db.jooq.Stroom;
import stroom.data.store.impl.fs.db.jooq.tables.records.FsVolumeStateRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FsVolumeState extends TableImpl<FsVolumeStateRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.fs_volume_state</code>
     */
    public static final FsVolumeState FS_VOLUME_STATE = new FsVolumeState();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FsVolumeStateRecord> getRecordType() {
        return FsVolumeStateRecord.class;
    }

    /**
     * The column <code>stroom.fs_volume_state.id</code>.
     */
    public final TableField<FsVolumeStateRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.fs_volume_state.version</code>.
     */
    public final TableField<FsVolumeStateRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.fs_volume_state.bytes_used</code>.
     */
    public final TableField<FsVolumeStateRecord, Long> BYTES_USED = createField(DSL.name("bytes_used"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.fs_volume_state.bytes_free</code>.
     */
    public final TableField<FsVolumeStateRecord, Long> BYTES_FREE = createField(DSL.name("bytes_free"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.fs_volume_state.bytes_total</code>.
     */
    public final TableField<FsVolumeStateRecord, Long> BYTES_TOTAL = createField(DSL.name("bytes_total"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.fs_volume_state.update_time_ms</code>.
     */
    public final TableField<FsVolumeStateRecord, Long> UPDATE_TIME_MS = createField(DSL.name("update_time_ms"), SQLDataType.BIGINT, this, "");

    private FsVolumeState(Name alias, Table<FsVolumeStateRecord> aliased) {
        this(alias, aliased, null);
    }

    private FsVolumeState(Name alias, Table<FsVolumeStateRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.fs_volume_state</code> table reference
     */
    public FsVolumeState(String alias) {
        this(DSL.name(alias), FS_VOLUME_STATE);
    }

    /**
     * Create an aliased <code>stroom.fs_volume_state</code> table reference
     */
    public FsVolumeState(Name alias) {
        this(alias, FS_VOLUME_STATE);
    }

    /**
     * Create a <code>stroom.fs_volume_state</code> table reference
     */
    public FsVolumeState() {
        this(DSL.name("fs_volume_state"), null);
    }

    public <O extends Record> FsVolumeState(Table<O> child, ForeignKey<O, FsVolumeStateRecord> key) {
        super(child, key, FS_VOLUME_STATE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<FsVolumeStateRecord, Integer> getIdentity() {
        return (Identity<FsVolumeStateRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<FsVolumeStateRecord> getPrimaryKey() {
        return Keys.KEY_FS_VOLUME_STATE_PRIMARY;
    }

    @Override
    public TableField<FsVolumeStateRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    @Override
    public FsVolumeState as(String alias) {
        return new FsVolumeState(DSL.name(alias), this);
    }

    @Override
    public FsVolumeState as(Name alias) {
        return new FsVolumeState(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FsVolumeState rename(String name) {
        return new FsVolumeState(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FsVolumeState rename(Name name) {
        return new FsVolumeState(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Integer, Integer, Long, Long, Long, Long> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}