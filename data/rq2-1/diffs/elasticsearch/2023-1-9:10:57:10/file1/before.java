/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.action.admin.cluster.allocation;

import org.elasticsearch.Version;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.routing.AllocationId;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.cluster.routing.allocation.allocator.DesiredBalanceStats;
import org.elasticsearch.common.collect.Iterators;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ChunkedToXContentObject;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DesiredBalanceResponse extends ActionResponse implements ChunkedToXContentObject {

    private final DesiredBalanceStats stats;
    private final Map<String, Map<Integer, DesiredShards>> routingTable;

    public DesiredBalanceResponse(DesiredBalanceStats stats, Map<String, Map<Integer, DesiredShards>> routingTable) {
        this.stats = stats;
        this.routingTable = routingTable;
    }

    public static DesiredBalanceResponse from(StreamInput in) throws IOException {
        return new DesiredBalanceResponse(
            DesiredBalanceStats.readFrom(in),
            in.readImmutableMap(StreamInput::readString, v -> v.readImmutableMap(StreamInput::readVInt, DesiredShards::from))
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        stats.writeTo(out);
        out.writeMap(
            routingTable,
            StreamOutput::writeString,
            (shardsOut, shards) -> shardsOut.writeMap(
                shards,
                StreamOutput::writeVInt,
                (desiredShardsOut, desiredShards) -> desiredShards.writeTo(desiredShardsOut)
            )
        );
    }

    @Override
    public Iterator<? extends ToXContent> toXContentChunked(ToXContent.Params params) {
        return Iterators.concat(Iterators.single((builder, p) -> {
            builder.startObject();
            builder.startObject("stats");
            stats.toXContent(builder, p);
            builder.endObject();
            return builder.startObject("routing_table");
        }), routingTable.entrySet().stream().map(indexEntry -> (ToXContent) (builder, p) -> {
            builder.startObject(indexEntry.getKey());
            for (Map.Entry<Integer, DesiredShards> shardEntry : indexEntry.getValue().entrySet()) {
                builder.field(String.valueOf(shardEntry.getKey()));
                shardEntry.getValue().toXContent(builder, p);
            }
            return builder.endObject();
        }).iterator(), Iterators.single((builder, p) -> builder.endObject().endObject()));
    }

    public DesiredBalanceStats getStats() {
        return stats;
    }

    public Map<String, Map<Integer, DesiredShards>> getRoutingTable() {
        return routingTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof DesiredBalanceResponse that
            && Objects.equals(stats, that.stats)
            && Objects.equals(routingTable, that.routingTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stats, routingTable);
    }

    @Override
    public String toString() {
        return "DesiredBalanceResponse{stats=" + stats + ", routingTable=" + routingTable + "}";
    }

    public record DesiredShards(List<ShardView> current, ShardAssignmentView desired) implements Writeable, ToXContentObject {

        public static DesiredShards from(StreamInput in) throws IOException {
            return new DesiredShards(in.readList(ShardView::from), ShardAssignmentView.from(in));
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeList(current);
            desired.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.startArray("current");
            for (ShardView shardView : current) {
                shardView.toXContent(builder, params);
            }
            builder.endArray();
            desired.toXContent(builder.field("desired"), params);
            return builder.endObject();
        }

    }

    public record ShardView(
        ShardRoutingState state,
        boolean primary,
        String node,
        boolean nodeIsDesired,
        @Nullable String relocatingNode,
        boolean relocatingNodeIsDesired,
        int shardId,
        String index,
        @Nullable Double forecastedWriteLoad,
        @Nullable Long forecastedShardSizeInBytes
    ) implements Writeable, ToXContentObject {

        private static final Version ADD_FORECASTS_VERSION = Version.V_8_7_0;

        public static ShardView from(StreamInput in) throws IOException {
            if (in.getVersion().onOrAfter(ADD_FORECASTS_VERSION)) {
                return new ShardView(
                    ShardRoutingState.fromValue(in.readByte()),
                    in.readBoolean(),
                    in.readOptionalString(),
                    in.readBoolean(),
                    in.readOptionalString(),
                    in.readBoolean(),
                    in.readVInt(),
                    in.readString(),
                    in.readOptionalDouble(),
                    in.readOptionalLong()
                );
            } else {
                var shardView = new ShardView(
                    ShardRoutingState.fromValue(in.readByte()),
                    in.readBoolean(),
                    in.readOptionalString(),
                    in.readBoolean(),
                    in.readOptionalString(),
                    in.readBoolean(),
                    in.readVInt(),
                    in.readString(),
                    null,
                    null
                );
                in.readOptionalWriteable(AllocationId::new);
                return shardView;
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeByte(state.value());
            out.writeBoolean(primary);
            out.writeOptionalString(node);
            out.writeBoolean(nodeIsDesired);
            out.writeOptionalString(relocatingNode);
            out.writeBoolean(relocatingNodeIsDesired);
            out.writeVInt(shardId);
            out.writeString(index);
            if (out.getVersion().onOrAfter(ADD_FORECASTS_VERSION)) {
                out.writeOptionalDouble(forecastedWriteLoad);
                out.writeOptionalLong(forecastedShardSizeInBytes);
            } else {
                out.writeMissingWriteable(AllocationId.class);
            }
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject()
                .field("state", state.toString())
                .field("primary", primary)
                .field("node", node)
                .field("node_is_desired", nodeIsDesired)
                .field("relocating_node", relocatingNode)
                .field("relocating_node_is_desired", relocatingNodeIsDesired)
                .field("shard_id", shardId)
                .field("index", index)
                .field("forecasted_write_load", forecastedWriteLoad)
                .field("forecasted_shard_size_in_bytes", forecastedShardSizeInBytes)
                .endObject();
        }
    }

    public record ShardAssignmentView(Set<String> nodeIds, int total, int unassigned, int ignored) implements Writeable, ToXContentObject {

        public static ShardAssignmentView from(StreamInput in) throws IOException {
            return new ShardAssignmentView(in.readSet(StreamInput::readString), in.readVInt(), in.readVInt(), in.readVInt());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeCollection(nodeIds, StreamOutput::writeString);
            out.writeVInt(total);
            out.writeVInt(unassigned);
            out.writeVInt(ignored);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject()
                .array("node_ids", nodeIds.toArray(String[]::new))
                .field("total", total)
                .field("unassigned", unassigned)
                .field("ignored", ignored)
                .endObject();
        }
    }

}
