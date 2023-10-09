/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.statistics.impl.sql;

import stroom.task.api.TaskContextFactory;
import stroom.util.sysinfo.HasSystemInfo;
import stroom.util.sysinfo.SystemInfoResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class SQLStatisticCacheImpl implements SQLStatisticCache, HasSystemInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLStatisticCacheImpl.class);

    /**
     * By default we only want to hold a million values in memory before
     * flushing.
     */
    private static final int DEFAULT_MAX_SIZE = 1000000;

    private final Provider<SQLStatisticFlushTaskHandler> sqlStatisticFlushTaskHandlerProvider;
    private final Executor executor;
    private final TaskContextFactory taskContextFactory;

    private volatile SQLStatisticAggregateMap map = new SQLStatisticAggregateMap();
    private final ReentrantLock mapLock = new ReentrantLock();
    // private final ReentrantLock flushLock = new ReentrantLock();
    private final LinkedBlockingQueue<SQLStatisticAggregateMap> flushQueue = new LinkedBlockingQueue<>(1);

    private final int maxSize;

    @Inject
    public SQLStatisticCacheImpl(final Provider<SQLStatisticFlushTaskHandler> sqlStatisticFlushTaskHandlerProvider,
                                 final Executor executor,
                                 final TaskContextFactory taskContextFactory) {
        this.maxSize = DEFAULT_MAX_SIZE;
        this.sqlStatisticFlushTaskHandlerProvider = sqlStatisticFlushTaskHandlerProvider;
        this.executor = executor;
        this.taskContextFactory = taskContextFactory;
    }

    public SQLStatisticCacheImpl() {
        this(DEFAULT_MAX_SIZE);
    }

    public SQLStatisticCacheImpl(final int maxSize) {
        this.maxSize = maxSize;
        this.sqlStatisticFlushTaskHandlerProvider = null;
        this.executor = null;
        this.taskContextFactory = null;
    }

    @Override
    public void add(final SQLStatisticAggregateMap aggregateMap) {
        mapLock.lock();
        try {
            // If we need to flush then switch out the map.
            if (this.map.size() > maxSize) {
                final SQLStatisticAggregateMap flushMap = this.map;
                // Switch out the current map under lock.
                LOGGER.debug("add() - Switch out the current map under lock. {}", flushMap);

                this.map = aggregateMap;

                // Try a non-blocking flush
                doFlush(false, flushMap);
            } else {
                this.map.add(aggregateMap);
            }
        } finally {
            mapLock.unlock();
        }
    }

    @Override
    public void flush() {
        flush(false);
    }

    public void flush(final boolean block) {
        SQLStatisticAggregateMap flushMap;

        mapLock.lock();
        try {
            // Switch out the current map under lock.
            flushMap = this.map;
            this.map = new SQLStatisticAggregateMap();
        } finally {
            mapLock.unlock();
        }

        if (flushMap.size() > 0) {
            // Flush the original map.
            doFlush(block, flushMap);
        }
    }

    private void doFlush(final boolean block, final SQLStatisticAggregateMap flushMap) {
        if (sqlStatisticFlushTaskHandlerProvider != null && taskContextFactory != null && executor != null) {
            try {
                LOGGER.debug("doFlush() - Locking {}", flushMap);

                flushQueue.put(flushMap);

                final Runnable runnable = taskContextFactory.context(
                        "Flush SQL Statistic Cache",
                        taskContext ->
                                sqlStatisticFlushTaskHandlerProvider.get().exec(flushMap));

                if (block) {
                    try {
                        runnable.run();
                    } finally {
                        flushQueue.poll();
                    }

                } else {
                    // Flush the original map.
                    CompletableFuture
                            .runAsync(runnable, executor)
                            .whenComplete((r, t) -> {
                                if (t == null) {
                                    LOGGER.debug("doFlush() - Unlocking");
                                } else {
                                    LOGGER.error("doFlush() - Unlocking", t);
                                }
                                flushQueue.poll();
                            });
                }

            } catch (final InterruptedException e) {
                LOGGER.error(MarkerFactory.getMarker("FATAL"), "doFlush() - Not expecting InterruptedException", e);

                // Continue to interrupt this thread.
                Thread.currentThread().interrupt();
            }
        }
    }

    void shutdown() {
        // Do a final blocking flush.
        //TODO run as proc user
        flush(true);
    }

    public void execute() {
        // Kick off a flush
        flush(false);
    }

    @Override
    public SystemInfoResult getSystemInfo() {
        return SystemInfoResult.builder().name(getSystemInfoName())
                .addDetail("mapAge", map.getAge().toString())
                .addDetail("countMapSize", map.countEntrySet().size())
                .addDetail("valueMapSize", map.valueEntrySet().size())
                .build();
    }
}
