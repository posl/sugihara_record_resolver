/*
 * This file is generated by jOOQ.
 */
package stroom.security.impl.db.jooq.tables.records;


import stroom.security.impl.db.jooq.tables.StroomUser;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class StroomUserRecord extends UpdatableRecordImpl<StroomUserRecord> implements Record10<Integer, Integer, Long, String, Long, String, String, String, Boolean, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>stroom.stroom_user.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>stroom.stroom_user.version</code>.
     */
    public void setVersion(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.version</code>.
     */
    public Integer getVersion() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>stroom.stroom_user.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>stroom.stroom_user.create_user</code>.
     */
    public void setCreateUser(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.create_user</code>.
     */
    public String getCreateUser() {
        return (String) get(3);
    }

    /**
     * Setter for <code>stroom.stroom_user.update_time_ms</code>.
     */
    public void setUpdateTimeMs(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.update_time_ms</code>.
     */
    public Long getUpdateTimeMs() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>stroom.stroom_user.update_user</code>.
     */
    public void setUpdateUser(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.update_user</code>.
     */
    public String getUpdateUser() {
        return (String) get(5);
    }

    /**
     * Setter for <code>stroom.stroom_user.name</code>.
     */
    public void setName(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.name</code>.
     */
    public String getName() {
        return (String) get(6);
    }

    /**
     * Setter for <code>stroom.stroom_user.uuid</code>.
     */
    public void setUuid(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.uuid</code>.
     */
    public String getUuid() {
        return (String) get(7);
    }

    /**
     * Setter for <code>stroom.stroom_user.is_group</code>.
     */
    public void setIsGroup(Boolean value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.is_group</code>.
     */
    public Boolean getIsGroup() {
        return (Boolean) get(8);
    }

    /**
     * Setter for <code>stroom.stroom_user.enabled</code>.
     */
    public void setEnabled(Boolean value) {
        set(9, value);
    }

    /**
     * Getter for <code>stroom.stroom_user.enabled</code>.
     */
    public Boolean getEnabled() {
        return (Boolean) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, Integer, Long, String, Long, String, String, String, Boolean, Boolean> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, Integer, Long, String, Long, String, String, String, Boolean, Boolean> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return StroomUser.STROOM_USER.ID;
    }

    @Override
    public Field<Integer> field2() {
        return StroomUser.STROOM_USER.VERSION;
    }

    @Override
    public Field<Long> field3() {
        return StroomUser.STROOM_USER.CREATE_TIME_MS;
    }

    @Override
    public Field<String> field4() {
        return StroomUser.STROOM_USER.CREATE_USER;
    }

    @Override
    public Field<Long> field5() {
        return StroomUser.STROOM_USER.UPDATE_TIME_MS;
    }

    @Override
    public Field<String> field6() {
        return StroomUser.STROOM_USER.UPDATE_USER;
    }

    @Override
    public Field<String> field7() {
        return StroomUser.STROOM_USER.NAME;
    }

    @Override
    public Field<String> field8() {
        return StroomUser.STROOM_USER.UUID;
    }

    @Override
    public Field<Boolean> field9() {
        return StroomUser.STROOM_USER.IS_GROUP;
    }

    @Override
    public Field<Boolean> field10() {
        return StroomUser.STROOM_USER.ENABLED;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getVersion();
    }

    @Override
    public Long component3() {
        return getCreateTimeMs();
    }

    @Override
    public String component4() {
        return getCreateUser();
    }

    @Override
    public Long component5() {
        return getUpdateTimeMs();
    }

    @Override
    public String component6() {
        return getUpdateUser();
    }

    @Override
    public String component7() {
        return getName();
    }

    @Override
    public String component8() {
        return getUuid();
    }

    @Override
    public Boolean component9() {
        return getIsGroup();
    }

    @Override
    public Boolean component10() {
        return getEnabled();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getVersion();
    }

    @Override
    public Long value3() {
        return getCreateTimeMs();
    }

    @Override
    public String value4() {
        return getCreateUser();
    }

    @Override
    public Long value5() {
        return getUpdateTimeMs();
    }

    @Override
    public String value6() {
        return getUpdateUser();
    }

    @Override
    public String value7() {
        return getName();
    }

    @Override
    public String value8() {
        return getUuid();
    }

    @Override
    public Boolean value9() {
        return getIsGroup();
    }

    @Override
    public Boolean value10() {
        return getEnabled();
    }

    @Override
    public StroomUserRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public StroomUserRecord value2(Integer value) {
        setVersion(value);
        return this;
    }

    @Override
    public StroomUserRecord value3(Long value) {
        setCreateTimeMs(value);
        return this;
    }

    @Override
    public StroomUserRecord value4(String value) {
        setCreateUser(value);
        return this;
    }

    @Override
    public StroomUserRecord value5(Long value) {
        setUpdateTimeMs(value);
        return this;
    }

    @Override
    public StroomUserRecord value6(String value) {
        setUpdateUser(value);
        return this;
    }

    @Override
    public StroomUserRecord value7(String value) {
        setName(value);
        return this;
    }

    @Override
    public StroomUserRecord value8(String value) {
        setUuid(value);
        return this;
    }

    @Override
    public StroomUserRecord value9(Boolean value) {
        setIsGroup(value);
        return this;
    }

    @Override
    public StroomUserRecord value10(Boolean value) {
        setEnabled(value);
        return this;
    }

    @Override
    public StroomUserRecord values(Integer value1, Integer value2, Long value3, String value4, Long value5, String value6, String value7, String value8, Boolean value9, Boolean value10) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached StroomUserRecord
     */
    public StroomUserRecord() {
        super(StroomUser.STROOM_USER);
    }

    /**
     * Create a detached, initialised StroomUserRecord
     */
    public StroomUserRecord(Integer id, Integer version, Long createTimeMs, String createUser, Long updateTimeMs, String updateUser, String name, String uuid, Boolean isGroup, Boolean enabled) {
        super(StroomUser.STROOM_USER);

        setId(id);
        setVersion(version);
        setCreateTimeMs(createTimeMs);
        setCreateUser(createUser);
        setUpdateTimeMs(updateTimeMs);
        setUpdateUser(updateUser);
        setName(name);
        setUuid(uuid);
        setIsGroup(isGroup);
        setEnabled(enabled);
    }
}
