package stroom.statistics.api;

public enum InternalStatisticKey {
    BENCHMARK_CLUSTER("Benchmark-Cluster Test"),
    CPU("CPU"),
    EVENTS_PER_SECOND("EPS"),
    HEAP_HISTOGRAM_BYTES("Heap Histogram Bytes"),
    HEAP_HISTOGRAM_INSTANCES("Heap Histogram Instances"),
    MEMORY("Memory"),
    METADATA_STREAMS_RECEIVED("Meta Data-Streams Received"),
    METADATA_STREAM_SIZE("Meta Data-Stream Size"),
    PIPELINE_STREAM_PROCESSOR("PipelineStreamProcessor"),
    STREAM_TASK_QUEUE_SIZE("Stream Task Queue Size"),
    VOLUMES("Volumes");

    private final String keyName;

    InternalStatisticKey(final String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return keyName;
    }
}
