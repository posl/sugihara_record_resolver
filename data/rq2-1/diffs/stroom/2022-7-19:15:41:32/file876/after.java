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

package stroom.search.impl.shard;

import stroom.cache.api.CacheManager;
import stroom.cache.api.ICache;
import stroom.index.impl.IndexShardService;
import stroom.index.impl.IndexShardWriter;
import stroom.index.impl.IndexShardWriterCache;
import stroom.index.shared.IndexShard;
import stroom.search.impl.SearchException;
import stroom.security.api.SecurityContext;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.task.api.ThreadPoolImpl;
import stroom.task.shared.ThreadPool;
import stroom.util.concurrent.ThreadUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogExecutionTime;
import stroom.util.shared.Clearable;

import org.apache.lucene.index.IndexWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class IndexShardSearcherCacheImpl implements IndexShardSearcherCache, Clearable {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(IndexShardSearcherCacheImpl.class);
    private static final String CACHE_NAME = "Index Shard Searcher Cache";

    private final CacheManager cacheManager;
    private final IndexShardService indexShardService;
    private final IndexShardWriterCache indexShardWriterCache;
    private final AtomicLong closing = new AtomicLong();
    private final Executor executor;
    private final TaskContextFactory taskContextFactory;
    private final TaskContext taskContext;
    private final Provider<IndexShardSearchConfig> indexShardSearchConfigProvider;
    private final SecurityContext securityContext;

    private volatile ICache<Key, IndexShardSearcher> cache;

    @Inject
    IndexShardSearcherCacheImpl(final CacheManager cacheManager,
                                final IndexShardService indexShardService,
                                final IndexShardWriterCache indexShardWriterCache,
                                final ExecutorProvider executorProvider,
                                final TaskContextFactory taskContextFactory,
                                final TaskContext taskContext,
                                final Provider<IndexShardSearchConfig> indexShardSearchConfigProvider,
                                final SecurityContext securityContext) {
        this.cacheManager = cacheManager;
        this.indexShardService = indexShardService;
        this.indexShardWriterCache = indexShardWriterCache;
        this.taskContextFactory = taskContextFactory;
        this.taskContext = taskContext;
        this.indexShardSearchConfigProvider = indexShardSearchConfigProvider;
        this.securityContext = securityContext;

        final ThreadPool threadPool = new ThreadPoolImpl("Index Shard Searcher Cache", 3);
        executor = executorProvider.get(threadPool);
    }

    private ICache<Key, IndexShardSearcher> getCache() {
        ICache<Key, IndexShardSearcher> result = cache;
        if (result == null) {
            synchronized (this) {
                result = cache;
                if (result == null) {
                    result = cacheManager.create(CACHE_NAME,
                            () -> indexShardSearchConfigProvider.get().getIndexShardSearcherCache(),
                            this::create,
                            this::destroy);
                    cache = result;
                }
            }
        }

        return result;
    }

    private IndexShardSearcher create(final Key key) {
        if (key == null) {
            throw new NullPointerException("Null key supplied");
        }

        try {
            final IndexShard indexShard = indexShardService.loadById(key.indexShardId);
            if (indexShard == null) {
                throw new SearchException("Unable to find index shard with id = " + key.indexShardId);
            }

            return new IndexShardSearcher(indexShard, key.indexWriter);
        } catch (final RuntimeException e) {
            LOGGER.error(e::getMessage, e);
            throw e;
        }
    }

    @Override
    public IndexShardSearcher get(final Long indexShardId) {
        final IndexWriter indexWriter = getWriter(indexShardId);
        final Key key = new Key(indexShardId, indexWriter);
        return getCache().get(key);
    }

//    @Override
//    public boolean isCached(final Long indexShardId) {
//        final IndexWriter indexWriter = getWriter(indexShardId);
//        final Key key = new Key(indexShardId, indexWriter);
//        return getCache().optionalGet(key).isPresent();
//    }
//
//    private void destroy(RemovalNotification<Key, IndexShardSearcher> notification) {
//        destroy(notification.getKey(), notification.getValue());
//    }

    private void destroy(final Key key, final Object value) {
        securityContext.asProcessingUser(() -> {
            if (value instanceof IndexShardSearcher) {
                final IndexShardSearcher indexShardSearcher = (IndexShardSearcher) value;

                closing.incrementAndGet();

                final Runnable runnable = taskContextFactory.context("Closing searcher", taskContext -> {
                    try {
                        taskContext.info(() -> "Closing searcher for index shard " + key.indexShardId);

                        indexShardSearcher.destroy();
                    } finally {
                        closing.decrementAndGet();
                    }
                });
                CompletableFuture.runAsync(runnable, executor);
            }
        });
    }

    private IndexWriter getWriter(final Long indexShardId) {
        IndexWriter indexWriter = null;

        // Load the current index shard.
        final IndexShardWriter indexShardWriter = indexShardWriterCache.getWriterByShardId(indexShardId);
        if (indexShardWriter != null) {
            indexWriter = indexShardWriter.getWriter();
        }

        return indexWriter;
    }

    @Override
    public void clear() {
        LOGGER.info(() -> "Clearing index shard searcher cache");
        final LogExecutionTime logExecutionTime = new LogExecutionTime();

        ScheduledExecutorService executor = null;

        try {
            // Close any remaining writers.
            if (this.cache != null) {
                this.cache.clear();
            }

            // Report on closing progress.
            if (closing.get() > 0) {
                // Create a scheduled executor for us to continually log close progress.
                executor = Executors.newSingleThreadScheduledExecutor();
                // Start logging action progress.
                executor.scheduleAtFixedRate(
                        () ->
                                LOGGER.info(() ->
                                        "Waiting for " + closing.get() + " readers to close"),
                        10,
                        10,
                        TimeUnit.SECONDS);

                while (closing.get() > 0) {
                    ThreadUtil.sleep(500);
                }
            }
        } finally {
            if (executor != null) {
                // Shut down the progress logging executor.
                executor.shutdown();
            }
        }

        LOGGER.info(() -> "Finished clearing index shard searcher cache in " + logExecutionTime);
    }

//    @Override
//    public long getMaxOpenShards() {
//        return maxOpenShards;
//    }
//
//    @Override
//    public void setMaxOpenShards(final long maxOpenShards) {
//        if (this.maxOpenShards != maxOpenShards) {
//            synchronized (this) {
//                final LoadingCache<Key, IndexShardSearcher> result = cache;
//                this.maxOpenShards = maxOpenShards;
//                cache = null;
//
//                if (result != null) {
//                    // Clean up.
//                    CacheUtil.clear(result);
//                }
//            }
//        }
//    }

    /**
     * This is called by the lifecycle service and remove writers that are past their TTL, TTI or LRU
     * items that exceed the capacity.
     */
    @Override
    public void refresh() {
        taskContext.info(() -> "Refreshing index shard searcher cache");
        final ICache<Key, IndexShardSearcher> cache = getCache();
        if (cache != null) {
            LOGGER.logDurationIfDebugEnabled(() ->
                            cache.asMap().values().forEach(v -> {
                                if (v != null) {
                                    try {
                                        v.getSearcherManager().maybeRefresh();
                                    } catch (final IOException e) {
                                        LOGGER.error(e::getMessage, e);
                                    }
                                }
                            }),
                    "refresh()");
        } else {
            LOGGER.debug(() -> "Cache is null");
        }
    }

    public static class Key {

        private final long indexShardId;
        private final IndexWriter indexWriter;

        Key(final long indexShardId, final IndexWriter indexWriter) {
            this.indexShardId = indexShardId;
            this.indexWriter = indexWriter;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Key key = (Key) o;
            return indexShardId == key.indexShardId &&
                    Objects.equals(indexWriter, key.indexWriter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indexShardId, indexWriter);
        }
    }
}
