/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq;


import stroom.proxy.repo.db.jooq.tables.Aggregate;
import stroom.proxy.repo.db.jooq.tables.ForwardAggregate;
import stroom.proxy.repo.db.jooq.tables.ForwardSource;
import stroom.proxy.repo.db.jooq.tables.Source;
import stroom.proxy.repo.db.jooq.tables.SourceItem;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in the default schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index AGGREGATE_FEED_TYPE_INDEX = Internal.createIndex(DSL.name("aggregate_feed_type_index"), Aggregate.AGGREGATE, new OrderField[] { Aggregate.AGGREGATE.FEED_NAME, Aggregate.AGGREGATE.TYPE_NAME }, false);
    public static final Index NEW_POSITION_AGGREGATE_INDEX = Internal.createIndex(DSL.name("new_position_aggregate_index"), Aggregate.AGGREGATE, new OrderField[] { Aggregate.AGGREGATE.NEW_POSITION }, true);
    public static final Index NEW_POSITION_FORWARD_AGGREGATE_INDEX = Internal.createIndex(DSL.name("new_position_forward_aggregate_index"), ForwardAggregate.FORWARD_AGGREGATE, new OrderField[] { ForwardAggregate.FORWARD_AGGREGATE.NEW_POSITION }, true);
    public static final Index NEW_POSITION_FORWARD_SOURCE_INDEX = Internal.createIndex(DSL.name("new_position_forward_source_index"), ForwardSource.FORWARD_SOURCE, new OrderField[] { ForwardSource.FORWARD_SOURCE.NEW_POSITION }, true);
    public static final Index NEW_POSITION_SOURCE_INDEX = Internal.createIndex(DSL.name("new_position_source_index"), Source.SOURCE, new OrderField[] { Source.SOURCE.NEW_POSITION }, true);
    public static final Index NEW_POSITION_SOURCE_ITEM_INDEX = Internal.createIndex(DSL.name("new_position_source_item_index"), SourceItem.SOURCE_ITEM, new OrderField[] { SourceItem.SOURCE_ITEM.NEW_POSITION }, true);
    public static final Index RETRY_POSITION_FORWARD_AGGREGATE_INDEX = Internal.createIndex(DSL.name("retry_position_forward_aggregate_index"), ForwardAggregate.FORWARD_AGGREGATE, new OrderField[] { ForwardAggregate.FORWARD_AGGREGATE.RETRY_POSITION }, true);
    public static final Index RETRY_POSITION_FORWARD_SOURCE_INDEX = Internal.createIndex(DSL.name("retry_position_forward_source_index"), ForwardSource.FORWARD_SOURCE, new OrderField[] { ForwardSource.FORWARD_SOURCE.RETRY_POSITION }, true);
    public static final Index SOURCE_PATH_INDEX = Internal.createIndex(DSL.name("source_path_index"), Source.SOURCE, new OrderField[] { Source.SOURCE.PATH }, true);
}
