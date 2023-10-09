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

package stroom.search.solr;

import stroom.datasource.api.v2.DataSourceProvider;
import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.job.api.ScheduledJobsBinder;
import stroom.search.solr.indexing.SolrIndexingElementModule;
import stroom.search.solr.search.SolrSearchResponseCreatorManager;
import stroom.search.solr.search.StroomSolrIndexQueryResourceImpl;
import stroom.search.solr.shared.SolrIndexDoc;
import stroom.util.RunnableWrapper;
import stroom.util.entityevent.EntityEvent;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.CRON;
import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class SolrSearchModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new SolrIndexingElementModule());

        bind(SolrIndexCache.class).to(SolrIndexCacheImpl.class);
        bind(SolrIndexClientCache.class).to(SolrIndexClientCacheImpl.class);
        bind(SolrIndexStore.class).to(SolrIndexStoreImpl.class);
        bind(SolrIndexService.class).to(SolrIndexServiceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), DataSourceProvider.class)
                .addBinding(SolrIndexServiceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), EntityEvent.Handler.class)
                .addBinding(SolrIndexCacheImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(SolrIndexCacheImpl.class)
                .addBinding(SolrIndexClientCacheImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(SolrIndexStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(SolrIndexStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(SolrIndexDoc.DOCUMENT_TYPE, SolrIndexStoreImpl.class);

        RestResourcesBinder.create(binder())
                .bind(SolrIndexResourceImpl.class)
                .bind(StroomSolrIndexQueryResourceImpl.class);

        ScheduledJobsBinder.create(binder())
                .bindJobTo(DataRetention.class, builder -> builder
                        .name("Solr Index Retention")
                        .description("Logically delete indexed documents in Solr indexes based on the specified " +
                                "deletion query")
                        .schedule(CRON, "0 2 *"))
                .bindJobTo(EvictExpiredElements.class, builder -> builder
                        .name("Evict expired elements")
                        .schedule(PERIODIC, "10s")
                        .managed(false))
                .bindJobTo(SolrIndexOptimiseExecutorJob.class, builder -> builder
                        .name("Solr Index Optimise")
                        .description("Optimise Solr indexes")
                        .schedule(CRON, "0 3 *"));
    }

    private static class DataRetention extends RunnableWrapper {

        @Inject
        DataRetention(final SolrIndexRetentionExecutor dataRetentionExecutor) {
            super(dataRetentionExecutor::exec);
        }
    }

    private static class EvictExpiredElements extends RunnableWrapper {

        @Inject
        EvictExpiredElements(final SolrSearchResponseCreatorManager manager) {
            super(manager::evictExpiredElements);
        }
    }

    private static class SolrIndexOptimiseExecutorJob extends RunnableWrapper {

        @Inject
        SolrIndexOptimiseExecutorJob(final SolrIndexOptimiseExecutor executor) {
            super(executor::exec);
        }
    }
}
