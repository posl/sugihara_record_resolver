/*
 * This file is generated by jOOQ.
 */
package stroom.security.identity.db.jooq.tables;


import stroom.security.identity.db.jooq.Keys;
import stroom.security.identity.db.jooq.Stroom;
import stroom.security.identity.db.jooq.tables.records.JsonWebKeyRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row12;
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
public class JsonWebKey extends TableImpl<JsonWebKeyRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.json_web_key</code>
     */
    public static final JsonWebKey JSON_WEB_KEY = new JsonWebKey();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<JsonWebKeyRecord> getRecordType() {
        return JsonWebKeyRecord.class;
    }

    /**
     * The column <code>stroom.json_web_key.id</code>.
     */
    public final TableField<JsonWebKeyRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.json_web_key.version</code>.
     */
    public final TableField<JsonWebKeyRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.create_time_ms</code>.
     */
    public final TableField<JsonWebKeyRecord, Long> CREATE_TIME_MS = createField(DSL.name("create_time_ms"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.create_user</code>.
     */
    public final TableField<JsonWebKeyRecord, String> CREATE_USER = createField(DSL.name("create_user"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.update_time_ms</code>.
     */
    public final TableField<JsonWebKeyRecord, Long> UPDATE_TIME_MS = createField(DSL.name("update_time_ms"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.update_user</code>.
     */
    public final TableField<JsonWebKeyRecord, String> UPDATE_USER = createField(DSL.name("update_user"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.fk_token_type_id</code>.
     */
    public final TableField<JsonWebKeyRecord, Integer> FK_TOKEN_TYPE_ID = createField(DSL.name("fk_token_type_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.key_id</code>.
     */
    public final TableField<JsonWebKeyRecord, String> KEY_ID = createField(DSL.name("key_id"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.json_web_key.json</code>.
     */
    public final TableField<JsonWebKeyRecord, String> JSON = createField(DSL.name("json"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>stroom.json_web_key.expires_on_ms</code>.
     */
    public final TableField<JsonWebKeyRecord, Long> EXPIRES_ON_MS = createField(DSL.name("expires_on_ms"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.json_web_key.comments</code>.
     */
    public final TableField<JsonWebKeyRecord, String> COMMENTS = createField(DSL.name("comments"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>stroom.json_web_key.enabled</code>.
     */
    public final TableField<JsonWebKeyRecord, Boolean> ENABLED = createField(DSL.name("enabled"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.inline("0", SQLDataType.BOOLEAN)), this, "");

    private JsonWebKey(Name alias, Table<JsonWebKeyRecord> aliased) {
        this(alias, aliased, null);
    }

    private JsonWebKey(Name alias, Table<JsonWebKeyRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.json_web_key</code> table reference
     */
    public JsonWebKey(String alias) {
        this(DSL.name(alias), JSON_WEB_KEY);
    }

    /**
     * Create an aliased <code>stroom.json_web_key</code> table reference
     */
    public JsonWebKey(Name alias) {
        this(alias, JSON_WEB_KEY);
    }

    /**
     * Create a <code>stroom.json_web_key</code> table reference
     */
    public JsonWebKey() {
        this(DSL.name("json_web_key"), null);
    }

    public <O extends Record> JsonWebKey(Table<O> child, ForeignKey<O, JsonWebKeyRecord> key) {
        super(child, key, JSON_WEB_KEY);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<JsonWebKeyRecord, Integer> getIdentity() {
        return (Identity<JsonWebKeyRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<JsonWebKeyRecord> getPrimaryKey() {
        return Keys.KEY_JSON_WEB_KEY_PRIMARY;
    }

    @Override
    public List<ForeignKey<JsonWebKeyRecord, ?>> getReferences() {
        return Arrays.asList(Keys.JSON_WEB_KEY_FK_TOKEN_TYPE_ID);
    }

    private transient TokenType _tokenType;

    /**
     * Get the implicit join path to the <code>stroom.token_type</code> table.
     */
    public TokenType tokenType() {
        if (_tokenType == null)
            _tokenType = new TokenType(this, Keys.JSON_WEB_KEY_FK_TOKEN_TYPE_ID);

        return _tokenType;
    }

    @Override
    public TableField<JsonWebKeyRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    @Override
    public JsonWebKey as(String alias) {
        return new JsonWebKey(DSL.name(alias), this);
    }

    @Override
    public JsonWebKey as(Name alias) {
        return new JsonWebKey(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public JsonWebKey rename(String name) {
        return new JsonWebKey(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public JsonWebKey rename(Name name) {
        return new JsonWebKey(name, null);
    }

    // -------------------------------------------------------------------------
    // Row12 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row12<Integer, Integer, Long, String, Long, String, Integer, String, String, Long, String, Boolean> fieldsRow() {
        return (Row12) super.fieldsRow();
    }
}