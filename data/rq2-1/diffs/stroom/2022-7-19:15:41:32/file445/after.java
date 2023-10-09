/*
 * This file is generated by jOOQ.
 */
package stroom.processor.impl.db.jooq;


import stroom.processor.impl.db.jooq.tables.ProcessorTask;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in stroom.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index PROCESSOR_TASK_PROCESSOR_TASK_META_ID_IDX = Internal.createIndex(DSL.name("processor_task_meta_id_idx"), ProcessorTask.PROCESSOR_TASK, new OrderField[] { ProcessorTask.PROCESSOR_TASK.META_ID }, false);
    public static final Index PROCESSOR_TASK_PROCESSOR_TASK_STATUS_IDX = Internal.createIndex(DSL.name("processor_task_status_idx"), ProcessorTask.PROCESSOR_TASK, new OrderField[] { ProcessorTask.PROCESSOR_TASK.STATUS }, false);
}