/*
 * Copyright 2017 Crown Copyright
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

package stroom.index.impl;

import stroom.docref.DocRef;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexException;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShard.IndexShardStatus;
import stroom.index.shared.IndexShardKey;
import stroom.node.api.NodeInfo;
import stroom.security.api.SecurityContext;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.util.concurrent.StripedLock;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.ResultPage;

import org.apache.lucene.store.LockObtainFailedException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class IndexShardWriterCacheImpl implements IndexShardWriterCache {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(IndexShardWriterCacheImpl.class);

    private final NodeInfo nodeInfo;
    private final IndexShardService indexShardService;
    private final IndexStructureCache indexStructureCache;
    private final IndexShardManager indexShardManager;
    private final Provider<IndexConfig> indexConfigProvider;

    private final Map<Long, IndexShardWriter> openWritersByShardId = new ConcurrentHashMap<>();
    private final Map<IndexShardKey, IndexShardWriter> openWritersByShardKey = new ConcurrentHashMap<>();
    private final StripedLock existingShardQueryLocks = new StripedLock();
    private final AtomicLong closing = new AtomicLong();
    private final IndexShardWriterExecutorProvider executorProvider;
    private final TaskContextFactory taskContextFactory;
    private final TaskContext taskContext;
    private final SecurityContext securityContext;

    private volatile Settings settings;

    @Inject
    public IndexShardWriterCacheImpl(final NodeInfo nodeInfo,
                                     final IndexShardService indexShardService,
                                     final Provider<IndexConfig> indexConfigProvider,
                                     final IndexStructureCache indexStructureCache,
                                     final IndexShardManager indexShardManager,
                                     final IndexShardWriterExecutorProvider executorProvider,
                                     final TaskContextFactory taskContextFactory,
                                     final TaskContext taskContext,
                                     final SecurityContext securityContext) {
        this.nodeInfo = nodeInfo;
        this.indexShardService = indexShardService;
        this.indexConfigProvider = indexConfigProvider;
        this.indexStructureCache = indexStructureCache;
        this.indexShardManager = indexShardManager;
        this.executorProvider = executorProvider;
        this.taskContextFactory = taskContextFactory;
        this.taskContext = taskContext;
        this.securityContext = securityContext;
    }

    @Override
    public IndexShardWriter getWriterByShardId(final long indexShardId) {
        return openWritersByShardId.get(indexShardId);
    }

    @Override
    public IndexShardWriter getWriterByShardKey(final IndexShardKey indexShardKey) {
        return openWritersByShardKey.compute(indexShardKey, (k, v) -> {
            // If there is already a value in this map for the provided key just return the value.
            if (v != null) {
                return v;
            }

            // Make sure we have room to add a new writer.
            makeRoom();

            IndexShardWriter indexShardWriter = openExistingShard(k);
            if (indexShardWriter == null) {
                indexShardWriter = openNewShard(k);
            }

            if (indexShardWriter == null) {
                throw new IndexException("Unable to create writer for " + indexShardKey);
            }

            openWritersByShardId.put(indexShardWriter.getIndexShardId(), indexShardWriter);

            return indexShardWriter;
        });
    }

    /**
     * Finds an existing shard for the specified key and opens a writer for it.
     */
    private IndexShardWriter openExistingShard(final IndexShardKey indexShardKey) {
        // Get all index shards that are owned by this node.
        final FindIndexShardCriteria criteria = FindIndexShardCriteria.matchAll();
        criteria.getNodeNameSet().add(nodeInfo.getThisNodeName());
        criteria.getIndexUuidSet().add(indexShardKey.getIndexUuid());
        criteria.getPartition().setString(indexShardKey.getPartition());

        // Don't allow us to try to open more than one existing shard for the same index, node and
        // partition at the same time.
        final Lock lock = existingShardQueryLocks.getLockForKey(criteria);
        lock.lock();
        try {
            final ResultPage<IndexShard> indexShardResultPage = indexShardService.find(criteria);
            for (final IndexShard indexShard : indexShardResultPage.getValues()) {
                // Look for non deleted, non full, non corrupt index shards.
                if (IndexShardStatus.CLOSED.equals(indexShard.getStatus())) {
                    // Get the index fields.
                    final IndexStructure indexStructure = indexStructureCache.get(new DocRef(IndexDoc.DOCUMENT_TYPE,
                            indexShardKey.getIndexUuid()));
                    if (indexStructure != null
                            && indexShard.getDocumentCount() < indexStructure.getIndex().getMaxDocsPerShard()) {
                        final IndexShardWriter indexShardWriter = openWriter(indexShardKey, indexShard);
                        if (indexShardWriter != null) {
                            return indexShardWriter;
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }

        return null;
    }

    /**
     * Creates a new index shard writer for the specified key and opens a writer for it.
     */
    private IndexShardWriter openNewShard(final IndexShardKey indexShardKey) {
        final IndexShard indexShard = indexShardService.createIndexShard(indexShardKey, nodeInfo.getThisNodeName());
        return openWriter(indexShardKey, indexShard);
    }

    /**
     * We expect to get lock exceptions as writers are removed from the open writers cache and closed
     * asynchronously via `removeElementsExceedingTTLandTTI`. If this happens we expect this exception and
     * will return null from this method so that the calling code will create a new shard instead.
     * This means more shards are created but stops closing shards from blocking indexing.
     *
     * @param indexShardKey
     * @param indexShard
     * @return
     */
    private IndexShardWriter openWriter(final IndexShardKey indexShardKey, final IndexShard indexShard) {
        final long indexShardId = indexShard.getId();

        // Get the index fields.
        final IndexStructure indexStructure = indexStructureCache.get(new DocRef(IndexDoc.DOCUMENT_TYPE,
                indexShardKey.getIndexUuid()));

        // Create the writer.
        final int ramBufferSizeMB = getRamBufferSize();

        // Mark the index shard as opening.
        LOGGER.debug(() -> "Opening " + indexShardId);
        LOGGER.trace(() -> "Opening " + indexShardId + " - " + indexShardKey);
        indexShardManager.setStatus(indexShardId, IndexShardStatus.OPENING);

        try {
            final IndexShardWriter indexShardWriter = new IndexShardWriterImpl(indexShardManager,
                    indexStructure,
                    indexShardKey,
                    indexShard,
                    ramBufferSizeMB);

            // We have opened the index so update the DB object.
            indexShardManager.setStatus(indexShardId, IndexShardStatus.OPEN);

            // Output some debug.
            LOGGER.debug(() ->
                    "Opened " + indexShardId + " in " +
                            (System.currentTimeMillis() - indexShardWriter.getCreationTime()) + "ms");

            return indexShardWriter;

        } catch (final LockObtainFailedException t) {
            // We expect to get lock exceptions as writers are removed from the open writers cache and closed
            // asynchronously via `removeElementsExceedingTTLandTTI`. If this happens we expect this exception
            // and will return null from this method so that the calling code will create a new shard instead.
            // This means more shards are created but stops closing shards from blocking indexing.
            LOGGER.debug(() -> "Error opening " + indexShardId, t);
            LOGGER.trace(t::getMessage, t);

        } catch (final IOException | RuntimeException e) {
            // Something unexpected went wrong.
            LOGGER.error(() -> "Setting index shard status to corrupt because (" + e + ")", e);
            indexShardManager.setStatus(indexShardId, IndexShardStatus.CORRUPT);
        }

        return null;
    }

    private int getRamBufferSize() {
        int ramBufferSizeMB = 1024;
        if (indexConfigProvider != null) {
            ramBufferSizeMB = indexConfigProvider.get().getRamBufferSizeMB();
        }
        return ramBufferSizeMB;
    }

    @Override
    public void flush(final long indexShardId) {
        final IndexShardWriter indexShardWriter = openWritersByShardId.get(indexShardId);
        if (indexShardWriter != null) {
            LOGGER.debug(() -> "Flush index shard " + indexShardId);
            indexShardWriter.flush();
        }
    }

    /**
     * This is called by the lifecycle service and will call flush on all open writers.
     */
    @Override
    public void flushAll() {
        LOGGER.logDurationIfDebugEnabled(() -> {
            try {
                final Set<IndexShardWriter> openWriters = new HashSet<>(openWritersByShardKey.values());
                if (openWriters.size() > 0) {
                    // Flush all writers.
                    final CountDownLatch countDownLatch = new CountDownLatch(openWriters.size());
                    openWriters.forEach(indexShardWriter -> {
                        try {
                            flush(indexShardWriter, executorProvider.getAsyncExecutor())
                                    .thenRun(countDownLatch::countDown);
                        } catch (final RuntimeException e) {
                            LOGGER.error(e::getMessage, e);
                            countDownLatch.countDown();
                        }
                    });
                    countDownLatch.await();
                }
            } catch (final InterruptedException e) {
                LOGGER.trace(e::getMessage, e);
                // Continue to interrupt this thread.
                Thread.currentThread().interrupt();
            }
        }, "flushAll()");
    }

    /**
     * This method should ensure there is enough room in the map to add a new item by removing the LRU items
     * until we have less items than the max capacity.
     */
    private void makeRoom() {
        removeElementsExceedingCore();
        removeElementsExceedingMax();
    }

    /**
     * This is called by the lifecycle service and remove writers that are past their TTL, TTI or LRU
     * items that exceed the capacity.
     */
    @Override
    public void sweep() {
        LOGGER.logDurationIfDebugEnabled(
                () -> {
                    removeElementsExceedingTTLandTTI();
                    removeElementsExceedingCore();
                },
                "sweep()");
    }

    private void removeElementsExceedingTTLandTTI() {
        final Settings settings = getSettings();

        // Deal with TTL and TTI.
        long overflow = openWritersByShardKey.size() - settings.minItems;
        if (overflow > 0 && (settings.timeToLive > 0 || settings.timeToIdle > 0)) {
            final long now = System.currentTimeMillis();

            // Get a set of candidates for removal that are currently exceeding TTL or TTI.
            final Set<IndexShardWriter> candidates = Collections.newSetFromMap(new ConcurrentHashMap<>());

            // Add open writers that are currently exceeding TTL or TTI.
            openWritersByShardKey.values().parallelStream().forEach(indexShardWriter -> {
                if (settings.timeToLive > 0 && indexShardWriter.getCreationTime() < now - settings.timeToLive) {
                    candidates.add(indexShardWriter);
                } else if (settings.timeToIdle > 0 && indexShardWriter.getLastUsedTime() < now - settings.timeToIdle) {
                    candidates.add(indexShardWriter);
                }
            });

            // Close candidates in LRU order.
            final List<IndexShardWriter> lruList = getLeastRecentlyUsedList(candidates);
            while (overflow > 0 && lruList.size() > 0) {
                final IndexShardWriter indexShardWriter = lruList.remove(0);
                overflow--;
                close(indexShardWriter, executorProvider.getAsyncExecutor());
            }
        }
    }

    private void removeElementsExceedingCore() {
        final Settings settings = getSettings();
        trim(settings.coreItems, executorProvider.getAsyncExecutor());
    }

    private void removeElementsExceedingMax() {
        final Settings settings = getSettings();
        trim(settings.maxItems, executorProvider.getSyncExecutor());
    }

    private void trim(final long trimSize, final Executor executor) {
        // Deal with exceeding trim size.
        long overflow = openWritersByShardKey.size() - trimSize;
        if (overflow > 0) {
            // Get LRU list.
            final List<IndexShardWriter> lruList = getLeastRecentlyUsedList(openWritersByShardKey.values());
            while (overflow > 0 && lruList.size() > 0) {
                final IndexShardWriter indexShardWriter = lruList.remove(0);
                overflow--;
                close(indexShardWriter, executor);
            }
        }
    }

    private List<IndexShardWriter> getLeastRecentlyUsedList(final Collection<IndexShardWriter> items) {
        return items.stream()
                .sorted(Comparator.comparingLong(IndexShardWriter::getLastUsedTime))
                .collect(Collectors.toList());
    }

    @Override
    public void close(final IndexShardWriter indexShardWriter) {
        taskContext.info(() -> "Closing index shard writer for shard: " + indexShardWriter.getIndexShardId());
        close(indexShardWriter, executorProvider.getAsyncExecutor());
    }

    @Override
    public void delete(final long indexShardId) {
        indexShardManager.setStatus(indexShardId, IndexShardStatus.DELETED);
        openWritersByShardKey.values().forEach(indexShardWriter -> {
            if (indexShardWriter.getIndexShardId() == indexShardId) {
                close(indexShardWriter);
            }
        });
    }

    private CompletableFuture<IndexShardWriter> flush(final IndexShardWriter indexShardWriter,
                                                      final Executor executor) {
        final Supplier<IndexShardWriter> supplier = taskContextFactory.contextResult(
                "Flushing writer", taskContext -> {
                    try {
                        taskContext.info(() ->
                                "Flushing writer for index shard " + indexShardWriter.getIndexShardId());

                        // Flush the shard.
                        indexShardWriter.flush();
                    } catch (final RuntimeException e) {
                        LOGGER.error(e::getMessage, e);
                    }

                    return indexShardWriter;
                });
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private void close(final IndexShardWriter indexShardWriter,
                       final Executor executor) {
        final long indexShardId = indexShardWriter.getIndexShardId();

        // Remove the shard from the map.
        openWritersByShardKey.compute(indexShardWriter.getIndexShardKey(), (indexShardKey, v) -> {
            // If there is no value associated with the key or the value is not the one we expect it to be then
            // just return the current value.
            if (v == null || v != indexShardWriter) {
                return v;
            }

            try {
                // Set the status of the shard to closing so it won't be used again immediately when removed
                // from the map.
                indexShardManager.setStatus(indexShardId, IndexShardStatus.CLOSING);

                try {
                    // Close the shard.
                    final Supplier<IndexShardWriter> supplier = taskContextFactory.contextResult(
                            "Closing writer", taskContext -> {
                                try {
                                    try {
                                        LOGGER.debug(() ->
                                                "Closing " + indexShardId);
                                        LOGGER.trace(() ->
                                                "Closing " + indexShardId + " - " + indexShardKey.toString());

                                        taskContext.info(() -> "Closing writer for index shard " + indexShardId);

                                        // Close the shard.
                                        indexShardWriter.close();
                                    } finally {
                                        // Remove the writer from ones that can be used by readers.
                                        openWritersByShardId.remove(indexShardId);

                                        // Update the shard status.
                                        indexShardManager.setStatus(indexShardId, IndexShardStatus.CLOSED);
                                    }
                                } catch (final RuntimeException e) {
                                    LOGGER.error(e::getMessage, e);
                                }

                                return null;
                            });
                    final CompletableFuture<IndexShardWriter> completableFuture = CompletableFuture.supplyAsync(
                            supplier,
                            executor);
                    completableFuture.thenRun(closing::decrementAndGet);
                    completableFuture.exceptionally(t -> {
                        LOGGER.error(t::getMessage, t);
                        closing.decrementAndGet();
                        return null;
                    });
                    closing.incrementAndGet();
                } catch (final RuntimeException e) {
                    LOGGER.error(e::getMessage, e);
                    closing.decrementAndGet();
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e::getMessage, e);
            }

            // Return null to remove the writer from the map.
            return null;
        });
    }

    synchronized void startup() {
        securityContext.asProcessingUser(() -> {
            LOGGER.info(() -> "Index shard writer cache startup");
            LOGGER.logDurationIfDebugEnabled(() -> {
                // Make sure all open shards are marked as closed.
                final FindIndexShardCriteria criteria = FindIndexShardCriteria.matchAll();
                criteria.getNodeNameSet().add(nodeInfo.getThisNodeName());
                criteria.getIndexShardStatusSet().add(IndexShardStatus.OPEN);
                criteria.getIndexShardStatusSet().add(IndexShardStatus.OPENING);
                criteria.getIndexShardStatusSet().add(IndexShardStatus.CLOSING);
                final ResultPage<IndexShard> indexShardResultPage = indexShardService.find(criteria);
                for (final IndexShard indexShard : indexShardResultPage.getValues()) {
                    clean(indexShard);
                }
            }, "Index shard writer cache startup");
        });
    }

    @Override
    public synchronized void shutdown() {
        securityContext.asProcessingUser(() -> {
            LOGGER.info(() -> "Index shard writer cache shutdown");
            LOGGER.logDurationIfDebugEnabled(() -> {
                ScheduledExecutorService executor = null;

                try {
                    // Close any remaining writers.
                    openWritersByShardKey.values()
                            .forEach(indexShardWriter ->
                                    close(indexShardWriter, executorProvider.getAsyncExecutor()));

                    // Report on closing progress.
                    if (closing.get() > 0) {
                        // Create a scheduled executor for us to continually log index shard writer action progress.
                        executor = Executors.newSingleThreadScheduledExecutor();
                        // Start logging action progress.
                        executor.scheduleAtFixedRate(() ->
                                        LOGGER.info(() ->
                                                "Waiting for " + closing.get() + " index shards to close"),
                                10,
                                10,
                                TimeUnit.SECONDS);

                        while (closing.get() > 0) {
                            Thread.sleep(500);
                        }
                    }
                } catch (final InterruptedException e) {
                    LOGGER.error(e::getMessage, e);
                    // Continue to interrupt this thread.
                    Thread.currentThread().interrupt();
                } finally {
                    if (executor != null) {
                        // Shut down the progress logging executor.
                        executor.shutdown();
                    }
                }
            }, "Index shard writer cache shutdown");
        });
    }

    private void clean(final IndexShard indexShard) {
        try {
            LOGGER.info(() -> "Changing shard status to closed (" + indexShard + ")");
            indexShard.setStatus(IndexShardStatus.CLOSED);
            indexShardService.setStatus(indexShard.getId(), IndexShardStatus.CLOSED);
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
        }

        try {
            LOGGER.info(() -> "Clearing any lingering locks (" + indexShard + ")");
            final Path dir = IndexShardUtil.getIndexPath(indexShard);
            LockFactoryFactory.clean(dir);
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
        }
    }

    private Settings getSettings() {
        if (settings == null || settings.creationTime < (System.currentTimeMillis() - 60_000)) {
            final IndexCacheConfig indexCacheConfig = indexConfigProvider.get()
                    .getIndexWriterConfig()
                    .getIndexCacheConfig();
            final long timeToLive = indexCacheConfig.getTimeToLive() != null
                    ? indexCacheConfig.getTimeToLive().toMillis()
                    : 0L;
            final long timeToIdle = indexCacheConfig.getTimeToIdle() != null
                    ? indexCacheConfig.getTimeToIdle().toMillis()
                    : 0L;
            final long minItems = Math.max(0, indexCacheConfig.getMinItems());
            final long coreItems = Math.max(minItems, indexCacheConfig.getCoreItems());
            final long maxItems = Math.max(coreItems, indexCacheConfig.getMaxItems());

            settings = new Settings(
                    System.currentTimeMillis(),
                    timeToLive,
                    timeToIdle,
                    minItems,
                    coreItems,
                    maxItems);
        }

        return settings;
    }

    private static class Settings {

        private final long creationTime;
        private final long timeToLive;
        private final long timeToIdle;
        private final long minItems;
        private final long coreItems;
        private final long maxItems;

        Settings(final long creationTime,
                 final long timeToLive,
                 final long timeToIdle,
                 final long minItems,
                 final long coreItems,
                 final long maxItems) {
            this.creationTime = creationTime;
            this.timeToLive = timeToLive;
            this.timeToIdle = timeToIdle;
            this.minItems = minItems;
            this.coreItems = coreItems;
            this.maxItems = maxItems;
        }
    }
}
