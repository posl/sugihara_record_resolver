package io.activej.dataflow.node;

import io.activej.datastream.consumer.StreamConsumer;

public interface PartitionedStreamConsumerFactory<T> {
	StreamConsumer<T> get(int partitionIndex, int maxPartitions);
}
