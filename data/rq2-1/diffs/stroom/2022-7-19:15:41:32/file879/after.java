/*
 * Copyright 2018 Crown Copyright
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

import stroom.job.api.ScheduledJobsBinder;
import stroom.util.RunnableWrapper;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.HasSystemInfoBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class ShardModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IndexShardSearcherCache.class).to(IndexShardSearcherCacheImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(IndexShardSearcherCacheImpl.class);

        ScheduledJobsBinder.create(binder())
                .bindJobTo(IndexSearcherCacheRefresh.class, builder -> builder
                        .name("Index Searcher Cache Refresh")
                        .description("Job to refresh index shard searchers in the cache")
                        .schedule(PERIODIC, "10m"));

        HasSystemInfoBinder.create(binder())
                .bind(IndexShardSystemInfo.class);
    }

    private static class IndexSearcherCacheRefresh extends RunnableWrapper {

        @Inject
        IndexSearcherCacheRefresh(final IndexShardSearcherCache indexShardSearcherCache) {
            super(indexShardSearcherCache::refresh);
        }
    }
}
