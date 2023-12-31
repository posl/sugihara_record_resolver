/*
 * This file is generated by jOOQ.
 */
package stroom.explorer.impl.db.jooq.tables;


import stroom.explorer.impl.db.jooq.Keys;
import stroom.explorer.impl.db.jooq.Stroom;
import stroom.explorer.impl.db.jooq.tables.records.ExplorerNodeRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
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
public class ExplorerNode extends TableImpl<ExplorerNodeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.explorer_node</code>
     */
    public static final ExplorerNode EXPLORER_NODE = new ExplorerNode();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ExplorerNodeRecord> getRecordType() {
        return ExplorerNodeRecord.class;
    }

    /**
     * The column <code>stroom.explorer_node.id</code>.
     */
    public final TableField<ExplorerNodeRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.explorer_node.type</code>.
     */
    public final TableField<ExplorerNodeRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.explorer_node.uuid</code>.
     */
    public final TableField<ExplorerNodeRecord, String> UUID = createField(DSL.name("uuid"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.explorer_node.name</code>.
     */
    public final TableField<ExplorerNodeRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.explorer_node.tags</code>.
     */
    public final TableField<ExplorerNodeRecord, String> TAGS = createField(DSL.name("tags"), SQLDataType.VARCHAR(255), this, "");

    private ExplorerNode(Name alias, Table<ExplorerNodeRecord> aliased) {
        this(alias, aliased, null);
    }

    private ExplorerNode(Name alias, Table<ExplorerNodeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.explorer_node</code> table reference
     */
    public ExplorerNode(String alias) {
        this(DSL.name(alias), EXPLORER_NODE);
    }

    /**
     * Create an aliased <code>stroom.explorer_node</code> table reference
     */
    public ExplorerNode(Name alias) {
        this(alias, EXPLORER_NODE);
    }

    /**
     * Create a <code>stroom.explorer_node</code> table reference
     */
    public ExplorerNode() {
        this(DSL.name("explorer_node"), null);
    }

    public <O extends Record> ExplorerNode(Table<O> child, ForeignKey<O, ExplorerNodeRecord> key) {
        super(child, key, EXPLORER_NODE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<ExplorerNodeRecord, Integer> getIdentity() {
        return (Identity<ExplorerNodeRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ExplorerNodeRecord> getPrimaryKey() {
        return Keys.KEY_EXPLORER_NODE_PRIMARY;
    }

    @Override
    public List<UniqueKey<ExplorerNodeRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_EXPLORER_NODE_EXPLORER_NODE_TYPE_UUID);
    }

    @Override
    public ExplorerNode as(String alias) {
        return new ExplorerNode(DSL.name(alias), this);
    }

    @Override
    public ExplorerNode as(Name alias) {
        return new ExplorerNode(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ExplorerNode rename(String name) {
        return new ExplorerNode(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ExplorerNode rename(Name name) {
        return new ExplorerNode(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, String, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
