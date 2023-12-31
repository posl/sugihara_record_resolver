/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.datastreams;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.LocalNodeMasterListener;
import org.elasticsearch.cluster.metadata.DataStream;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexMode;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.threadpool.Scheduler;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.index.mapper.DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER;

/**
 * A component that updates the 'index.time_series.end_time' index setting for the most recently added backing index of a tsdb data stream.
 */
public class UpdateTimeSeriesRangeService extends AbstractLifecycleComponent implements LocalNodeMasterListener {

    private static final Logger LOGGER = LogManager.getLogger(UpdateTimeSeriesRangeService.class);

    private final ThreadPool threadPool;
    private final ClusterService clusterService;

    volatile TimeValue pollInterval;
    volatile Scheduler.Cancellable job;
    private final AtomicBoolean running = new AtomicBoolean(false);

    UpdateTimeSeriesRangeService(Settings settings, ThreadPool threadPool, ClusterService clusterService) {
        this.pollInterval = DataStreamsPlugin.TIME_SERIES_POLL_INTERVAL.get(settings);
        this.threadPool = threadPool;
        this.clusterService = clusterService;
        clusterService.getClusterSettings().addSettingsUpdateConsumer(DataStreamsPlugin.TIME_SERIES_POLL_INTERVAL, this::setPollInterval);
    }

    void perform(Runnable onComplete) {
        if (running.compareAndSet(false, true)) {
            LOGGER.debug("starting tsdb update task");
            submitUnbatchedTask("update_tsdb_data_stream_end_times", new ClusterStateUpdateTask(Priority.URGENT) {
                @Override
                public ClusterState execute(ClusterState currentState) throws Exception {
                    return updateTimeSeriesTemporalRange(currentState, Instant.now());
                }

                @Override
                public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
                    running.set(false);
                    onComplete.run();
                }

                @Override
                public void onFailure(Exception e) {
                    running.set(false);
                    LOGGER.warn("failed to update tsdb data stream end times", e);
                    onComplete.run();
                }

            });
        } else {
            LOGGER.debug("not starting tsdb update task, because another execution is still running");
        }
    }

    @SuppressForbidden(reason = "legacy usage of unbatched task") // TODO add support for batching here
    private void submitUnbatchedTask(@SuppressWarnings("SameParameterValue") String source, ClusterStateUpdateTask task) {
        clusterService.submitUnbatchedStateUpdateTask(source, task);
    }

    void setPollInterval(TimeValue newValue) {
        LOGGER.info(
            "updating [{}] setting from [{}] to [{}]",
            DataStreamsPlugin.TIME_SERIES_POLL_INTERVAL.getKey(),
            pollInterval,
            newValue
        );
        this.pollInterval = newValue;

        // Only re-schedule if we've been scheduled, this should only be the case on elected master node.
        if (job != null) {
            unschedule();
            scheduleTask();
        }
    }

    ClusterState updateTimeSeriesTemporalRange(ClusterState current, Instant now) {
        Metadata.Builder mBuilder = null;
        for (DataStream dataStream : current.metadata().dataStreams().values()) {
            if (dataStream.getIndexMode() != IndexMode.TIME_SERIES) {
                continue;
            }
            if (dataStream.isReplicated()) {
                continue;
            }

            // getWriteIndex() selects the latest added index:
            Index head = dataStream.getWriteIndex();
            IndexMetadata im = current.metadata().getIndexSafe(head);
            Instant currentEnd = IndexSettings.TIME_SERIES_END_TIME.get(im.getSettings());
            TimeValue lookAheadTime = IndexSettings.LOOK_AHEAD_TIME.get(im.getSettings());
            Instant newEnd = now.plus(lookAheadTime.getMillis(), ChronoUnit.MILLIS).plus(pollInterval.getMillis(), ChronoUnit.MILLIS);
            if (newEnd.isAfter(currentEnd)) {
                try {
                    Settings settings = Settings.builder()
                        .put(IndexSettings.TIME_SERIES_END_TIME.getKey(), DEFAULT_DATE_TIME_FORMATTER.format(newEnd))
                        .build();
                    LOGGER.debug(
                        "updating [{}] setting from [{}] to [{}] for data stream [{}]",
                        IndexSettings.TIME_SERIES_END_TIME.getKey(),
                        currentEnd,
                        newEnd,
                        dataStream.getName()
                    );
                    if (mBuilder == null) {
                        mBuilder = Metadata.builder(current.metadata());
                    }
                    mBuilder.updateSettings(settings, head.getName());
                    // Verify that all temporal ranges of each backing index is still valid:
                    dataStream.validate(mBuilder::get);
                } catch (Exception e) {
                    LOGGER.error(
                        () -> new ParameterizedMessage(
                            "unable to update [{}] for data stream [{}] and backing index [{}]",
                            IndexSettings.TIME_SERIES_END_TIME.getKey(),
                            dataStream.getName(),
                            head.getName()
                        ),
                        e
                    );
                }
            }
        }

        if (mBuilder != null) {
            return ClusterState.builder(current).metadata(mBuilder).build();
        } else {
            return current;
        }
    }

    void scheduleTask() {
        if (job == null) {
            LOGGER.debug("schedule tsdb update task");
            job = threadPool.scheduleWithFixedDelay(
                () -> perform(() -> LOGGER.debug("completed tsdb update task")),
                pollInterval,
                ThreadPool.Names.SAME
            );
        }
    }

    void unschedule() {
        if (job != null) {
            job.cancel();
            job = null;
        }
    }

    @Override
    protected void doStart() {
        clusterService.addLocalNodeMasterListener(this);
    }

    @Override
    protected void doStop() {
        unschedule();
    }

    @Override
    protected void doClose() throws IOException {
        unschedule();
    }

    @Override
    public void onMaster() {
        scheduleTask();
    }

    @Override
    public void offMaster() {
        unschedule();
    }
}
