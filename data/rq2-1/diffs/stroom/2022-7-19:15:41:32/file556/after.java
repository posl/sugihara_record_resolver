/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables.records;


import stroom.proxy.repo.db.jooq.tables.Source;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record7;
import org.jooq.Row7;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SourceRecord extends UpdatableRecordImpl<SourceRecord> implements Record7<Long, Long, Long, Boolean, Boolean, Integer, Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>source.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>source.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>source.file_store_id</code>.
     */
    public void setFileStoreId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>source.file_store_id</code>.
     */
    public Long getFileStoreId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>source.fk_feed_id</code>.
     */
    public void setFkFeedId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>source.fk_feed_id</code>.
     */
    public Long getFkFeedId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>source.examined</code>.
     */
    public void setExamined(Boolean value) {
        set(3, value);
    }

    /**
     * Getter for <code>source.examined</code>.
     */
    public Boolean getExamined() {
        return (Boolean) get(3);
    }

    /**
     * Setter for <code>source.deleted</code>.
     */
    public void setDeleted(Boolean value) {
        set(4, value);
    }

    /**
     * Getter for <code>source.deleted</code>.
     */
    public Boolean getDeleted() {
        return (Boolean) get(4);
    }

    /**
     * Setter for <code>source.item_count</code>.
     */
    public void setItemCount(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>source.item_count</code>.
     */
    public Integer getItemCount() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>source.new_position</code>.
     */
    public void setNewPosition(Long value) {
        set(6, value);
    }

    /**
     * Getter for <code>source.new_position</code>.
     */
    public Long getNewPosition() {
        return (Long) get(6);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record7 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row7<Long, Long, Long, Boolean, Boolean, Integer, Long> fieldsRow() {
        return (Row7) super.fieldsRow();
    }

    @Override
    public Row7<Long, Long, Long, Boolean, Boolean, Integer, Long> valuesRow() {
        return (Row7) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return Source.SOURCE.ID;
    }

    @Override
    public Field<Long> field2() {
        return Source.SOURCE.FILE_STORE_ID;
    }

    @Override
    public Field<Long> field3() {
        return Source.SOURCE.FK_FEED_ID;
    }

    @Override
    public Field<Boolean> field4() {
        return Source.SOURCE.EXAMINED;
    }

    @Override
    public Field<Boolean> field5() {
        return Source.SOURCE.DELETED;
    }

    @Override
    public Field<Integer> field6() {
        return Source.SOURCE.ITEM_COUNT;
    }

    @Override
    public Field<Long> field7() {
        return Source.SOURCE.NEW_POSITION;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public Long component2() {
        return getFileStoreId();
    }

    @Override
    public Long component3() {
        return getFkFeedId();
    }

    @Override
    public Boolean component4() {
        return getExamined();
    }

    @Override
    public Boolean component5() {
        return getDeleted();
    }

    @Override
    public Integer component6() {
        return getItemCount();
    }

    @Override
    public Long component7() {
        return getNewPosition();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public Long value2() {
        return getFileStoreId();
    }

    @Override
    public Long value3() {
        return getFkFeedId();
    }

    @Override
    public Boolean value4() {
        return getExamined();
    }

    @Override
    public Boolean value5() {
        return getDeleted();
    }

    @Override
    public Integer value6() {
        return getItemCount();
    }

    @Override
    public Long value7() {
        return getNewPosition();
    }

    @Override
    public SourceRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public SourceRecord value2(Long value) {
        setFileStoreId(value);
        return this;
    }

    @Override
    public SourceRecord value3(Long value) {
        setFkFeedId(value);
        return this;
    }

    @Override
    public SourceRecord value4(Boolean value) {
        setExamined(value);
        return this;
    }

    @Override
    public SourceRecord value5(Boolean value) {
        setDeleted(value);
        return this;
    }

    @Override
    public SourceRecord value6(Integer value) {
        setItemCount(value);
        return this;
    }

    @Override
    public SourceRecord value7(Long value) {
        setNewPosition(value);
        return this;
    }

    @Override
    public SourceRecord values(Long value1, Long value2, Long value3, Boolean value4, Boolean value5, Integer value6, Long value7) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SourceRecord
     */
    public SourceRecord() {
        super(Source.SOURCE);
    }

    /**
     * Create a detached, initialised SourceRecord
     */
    public SourceRecord(Long id, Long fileStoreId, Long fkFeedId, Boolean examined, Boolean deleted, Integer itemCount, Long newPosition) {
        super(Source.SOURCE);

        setId(id);
        setFileStoreId(fileStoreId);
        setFkFeedId(fkFeedId);
        setExamined(examined);
        setDeleted(deleted);
        setItemCount(itemCount);
        setNewPosition(newPosition);
    }
}
