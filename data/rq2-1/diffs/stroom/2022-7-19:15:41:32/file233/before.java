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

package stroom.dashboard.impl;

import stroom.dashboard.expression.v1.FunctionFactory;
import stroom.dashboard.shared.DashboardDoc;
import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;

public class DashboardModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DashboardStore.class).to(DashboardStoreImpl.class);
        bind(DashboardService.class).to(DashboardServiceImpl.class);
        bind(FunctionFactory.class).asEagerSingleton();
        bind(FunctionService.class).to(FunctionServiceImpl.class).asEagerSingleton();

        GuiceUtil.buildMultiBinder(binder(), Clearable.class).addBinding(ActiveQueriesManager.class);

        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(DashboardStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(DashboardStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(DashboardDoc.DOCUMENT_TYPE, DashboardStoreImpl.class);

        RestResourcesBinder.create(binder())
                .bind(DashboardResourceImpl.class);
    }
}
