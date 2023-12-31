/*
 * This file is generated by jOOQ.
 */
package stroom.annotation.impl.db.jooq.tables.records;


import stroom.annotation.impl.db.jooq.tables.AnnotationEntry;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AnnotationEntryRecord extends UpdatableRecordImpl<AnnotationEntryRecord> implements Record9<Long, Integer, Long, String, Long, String, Long, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>stroom.annotation_entry.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>stroom.annotation_entry.version</code>.
     */
    public void setVersion(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.version</code>.
     */
    public Integer getVersion() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>stroom.annotation_entry.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>stroom.annotation_entry.create_user</code>.
     */
    public void setCreateUser(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.create_user</code>.
     */
    public String getCreateUser() {
        return (String) get(3);
    }

    /**
     * Setter for <code>stroom.annotation_entry.update_time_ms</code>.
     */
    public void setUpdateTimeMs(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.update_time_ms</code>.
     */
    public Long getUpdateTimeMs() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>stroom.annotation_entry.update_user</code>.
     */
    public void setUpdateUser(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.update_user</code>.
     */
    public String getUpdateUser() {
        return (String) get(5);
    }

    /**
     * Setter for <code>stroom.annotation_entry.fk_annotation_id</code>.
     */
    public void setFkAnnotationId(Long value) {
        set(6, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.fk_annotation_id</code>.
     */
    public Long getFkAnnotationId() {
        return (Long) get(6);
    }

    /**
     * Setter for <code>stroom.annotation_entry.type</code>.
     */
    public void setType(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.type</code>.
     */
    public String getType() {
        return (String) get(7);
    }

    /**
     * Setter for <code>stroom.annotation_entry.data</code>.
     */
    public void setData(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.annotation_entry.data</code>.
     */
    public String getData() {
        return (String) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<Long, Integer, Long, String, Long, String, Long, String, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<Long, Integer, Long, String, Long, String, Long, String, String> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return AnnotationEntry.ANNOTATION_ENTRY.ID;
    }

    @Override
    public Field<Integer> field2() {
        return AnnotationEntry.ANNOTATION_ENTRY.VERSION;
    }

    @Override
    public Field<Long> field3() {
        return AnnotationEntry.ANNOTATION_ENTRY.CREATE_TIME_MS;
    }

    @Override
    public Field<String> field4() {
        return AnnotationEntry.ANNOTATION_ENTRY.CREATE_USER;
    }

    @Override
    public Field<Long> field5() {
        return AnnotationEntry.ANNOTATION_ENTRY.UPDATE_TIME_MS;
    }

    @Override
    public Field<String> field6() {
        return AnnotationEntry.ANNOTATION_ENTRY.UPDATE_USER;
    }

    @Override
    public Field<Long> field7() {
        return AnnotationEntry.ANNOTATION_ENTRY.FK_ANNOTATION_ID;
    }

    @Override
    public Field<String> field8() {
        return AnnotationEntry.ANNOTATION_ENTRY.TYPE;
    }

    @Override
    public Field<String> field9() {
        return AnnotationEntry.ANNOTATION_ENTRY.DATA;
    }

    @Override
    public Long component1() {
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
    public Long component7() {
        return getFkAnnotationId();
    }

    @Override
    public String component8() {
        return getType();
    }

    @Override
    public String component9() {
        return getData();
    }

    @Override
    public Long value1() {
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
    public Long value7() {
        return getFkAnnotationId();
    }

    @Override
    public String value8() {
        return getType();
    }

    @Override
    public String value9() {
        return getData();
    }

    @Override
    public AnnotationEntryRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value2(Integer value) {
        setVersion(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value3(Long value) {
        setCreateTimeMs(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value4(String value) {
        setCreateUser(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value5(Long value) {
        setUpdateTimeMs(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value6(String value) {
        setUpdateUser(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value7(Long value) {
        setFkAnnotationId(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value8(String value) {
        setType(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord value9(String value) {
        setData(value);
        return this;
    }

    @Override
    public AnnotationEntryRecord values(Long value1, Integer value2, Long value3, String value4, Long value5, String value6, Long value7, String value8, String value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AnnotationEntryRecord
     */
    public AnnotationEntryRecord() {
        super(AnnotationEntry.ANNOTATION_ENTRY);
    }

    /**
     * Create a detached, initialised AnnotationEntryRecord
     */
    public AnnotationEntryRecord(Long id, Integer version, Long createTimeMs, String createUser, Long updateTimeMs, String updateUser, Long fkAnnotationId, String type, String data) {
        super(AnnotationEntry.ANNOTATION_ENTRY);

        setId(id);
        setVersion(version);
        setCreateTimeMs(createTimeMs);
        setCreateUser(createUser);
        setUpdateTimeMs(updateTimeMs);
        setUpdateUser(updateUser);
        setFkAnnotationId(fkAnnotationId);
        setType(type);
        setData(data);
    }
}
