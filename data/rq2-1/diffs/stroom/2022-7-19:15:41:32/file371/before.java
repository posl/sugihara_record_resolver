/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables;


import stroom.meta.impl.db.jooq.Indexes;
import stroom.meta.impl.db.jooq.Keys;
import stroom.meta.impl.db.jooq.Stroom;
import stroom.meta.impl.db.jooq.tables.records.MetaFeedRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaFeed extends TableImpl<MetaFeedRecord> {

    private static final long serialVersionUID = -1979554485;

    /**
     * The reference instance of <code>stroom.meta_feed</code>
     */
    public static final MetaFeed META_FEED = new MetaFeed();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetaFeedRecord> getRecordType() {
        return MetaFeedRecord.class;
    }

    /**
     * The column <code>stroom.meta_feed.id</code>.
     */
    public final TableField<MetaFeedRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.meta_feed.name</code>.
     */
    public final TableField<MetaFeedRecord, String> NAME = createField(DSL.name("name"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * Create a <code>stroom.meta_feed</code> table reference
     */
    public MetaFeed() {
        this(DSL.name("meta_feed"), null);
    }

    /**
     * Create an aliased <code>stroom.meta_feed</code> table reference
     */
    public MetaFeed(String alias) {
        this(DSL.name(alias), META_FEED);
    }

    /**
     * Create an aliased <code>stroom.meta_feed</code> table reference
     */
    public MetaFeed(Name alias) {
        this(alias, META_FEED);
    }

    private MetaFeed(Name alias, Table<MetaFeedRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetaFeed(Name alias, Table<MetaFeedRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> MetaFeed(Table<O> child, ForeignKey<O, MetaFeedRecord> key) {
        super(child, key, META_FEED);
    }

    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.META_FEED_NAME, Indexes.META_FEED_PRIMARY);
    }

    @Override
    public Identity<MetaFeedRecord, Integer> getIdentity() {
        return Keys.IDENTITY_META_FEED;
    }

    @Override
    public UniqueKey<MetaFeedRecord> getPrimaryKey() {
        return Keys.KEY_META_FEED_PRIMARY;
    }

    @Override
    public List<UniqueKey<MetaFeedRecord>> getKeys() {
        return Arrays.<UniqueKey<MetaFeedRecord>>asList(Keys.KEY_META_FEED_PRIMARY, Keys.KEY_META_FEED_NAME);
    }

    @Override
    public MetaFeed as(String alias) {
        return new MetaFeed(DSL.name(alias), this);
    }

    @Override
    public MetaFeed as(Name alias) {
        return new MetaFeed(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaFeed rename(String name) {
        return new MetaFeed(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaFeed rename(Name name) {
        return new MetaFeed(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
