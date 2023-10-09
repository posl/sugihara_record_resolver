/*
 * This file is generated by jOOQ.
 */
package stroom.index.impl.db.jooq;


import stroom.index.impl.db.jooq.tables.IndexShard;
import stroom.index.impl.db.jooq.tables.IndexVolume;
import stroom.index.impl.db.jooq.tables.IndexVolumeGroup;
import stroom.index.impl.db.jooq.tables.records.IndexShardRecord;
import stroom.index.impl.db.jooq.tables.records.IndexVolumeGroupRecord;
import stroom.index.impl.db.jooq.tables.records.IndexVolumeRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * stroom.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<IndexShardRecord> KEY_INDEX_SHARD_PRIMARY = Internal.createUniqueKey(IndexShard.INDEX_SHARD, DSL.name("KEY_index_shard_PRIMARY"), new TableField[] { IndexShard.INDEX_SHARD.ID }, true);
    public static final UniqueKey<IndexVolumeRecord> KEY_INDEX_VOLUME_NODE_NAME_PATH = Internal.createUniqueKey(IndexVolume.INDEX_VOLUME, DSL.name("KEY_index_volume_node_name_path"), new TableField[] { IndexVolume.INDEX_VOLUME.FK_INDEX_VOLUME_GROUP_ID, IndexVolume.INDEX_VOLUME.NODE_NAME, IndexVolume.INDEX_VOLUME.PATH }, true);
    public static final UniqueKey<IndexVolumeRecord> KEY_INDEX_VOLUME_PRIMARY = Internal.createUniqueKey(IndexVolume.INDEX_VOLUME, DSL.name("KEY_index_volume_PRIMARY"), new TableField[] { IndexVolume.INDEX_VOLUME.ID }, true);
    public static final UniqueKey<IndexVolumeGroupRecord> KEY_INDEX_VOLUME_GROUP_NAME = Internal.createUniqueKey(IndexVolumeGroup.INDEX_VOLUME_GROUP, DSL.name("KEY_index_volume_group_name"), new TableField[] { IndexVolumeGroup.INDEX_VOLUME_GROUP.NAME }, true);
    public static final UniqueKey<IndexVolumeGroupRecord> KEY_INDEX_VOLUME_GROUP_PRIMARY = Internal.createUniqueKey(IndexVolumeGroup.INDEX_VOLUME_GROUP, DSL.name("KEY_index_volume_group_PRIMARY"), new TableField[] { IndexVolumeGroup.INDEX_VOLUME_GROUP.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<IndexShardRecord, IndexVolumeRecord> INDEX_SHARD_FK_VOLUME_ID = Internal.createForeignKey(IndexShard.INDEX_SHARD, DSL.name("index_shard_fk_volume_id"), new TableField[] { IndexShard.INDEX_SHARD.FK_VOLUME_ID }, Keys.KEY_INDEX_VOLUME_PRIMARY, new TableField[] { IndexVolume.INDEX_VOLUME.ID }, true);
    public static final ForeignKey<IndexVolumeRecord, IndexVolumeGroupRecord> INDEX_VOLUME_GROUP_LINK_FK_GROUP_NAME = Internal.createForeignKey(IndexVolume.INDEX_VOLUME, DSL.name("index_volume_group_link_fk_group_name"), new TableField[] { IndexVolume.INDEX_VOLUME.FK_INDEX_VOLUME_GROUP_ID }, Keys.KEY_INDEX_VOLUME_GROUP_PRIMARY, new TableField[] { IndexVolumeGroup.INDEX_VOLUME_GROUP.ID }, true);
}