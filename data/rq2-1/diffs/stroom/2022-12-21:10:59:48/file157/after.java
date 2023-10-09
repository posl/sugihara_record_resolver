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

package stroom.processor.impl;


import stroom.dashboard.expression.v1.ValuesConsumer;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.docrefinfo.api.DocRefInfoService;
import stroom.entity.shared.ExpressionCriteria;
import stroom.pipeline.shared.PipelineDoc;
import stroom.processor.api.ProcessorTaskService;
import stroom.processor.shared.ProcessorTask;
import stroom.processor.shared.ProcessorTaskFields;
import stroom.processor.shared.ProcessorTaskSummary;
import stroom.searchable.api.Searchable;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.util.shared.ResultPage;

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ProcessorTaskServiceImpl implements ProcessorTaskService, Searchable {

    private static final String PERMISSION = PermissionNames.MANAGE_PROCESSORS_PERMISSION;

    private final ProcessorTaskDao processorTaskDao;
    private final DocRefInfoService docRefInfoService;
    private final SecurityContext securityContext;

    @Inject
    ProcessorTaskServiceImpl(final ProcessorTaskDao processorTaskDao,
                             final DocRefInfoService docRefInfoService,
                             final SecurityContext securityContext) {
        this.processorTaskDao = processorTaskDao;
        this.docRefInfoService = docRefInfoService;
        this.securityContext = securityContext;
    }

    @Override
    public ResultPage<ProcessorTask> find(final ExpressionCriteria criteria) {
        return securityContext.secureResult(PERMISSION, () -> {
            final ResultPage<ProcessorTask> resultPage = processorTaskDao.find(criteria);
            resultPage.getValues().forEach(processorTask -> {
                final DocRef docRef = new DocRef(PipelineDoc.DOCUMENT_TYPE,
                        processorTask.getProcessorFilter().getPipelineUuid());
                final Optional<String> name = docRefInfoService.name(docRef);
                processorTask.getProcessorFilter().setPipelineName(name.orElse(null));
            });
            return resultPage;
        });
    }

    @Override
    public ResultPage<ProcessorTaskSummary> findSummary(final ExpressionCriteria criteria) {
        return securityContext.secureResult(PERMISSION, () ->
                processorTaskDao.findSummary(criteria));
    }

    @Override
    public void search(final ExpressionCriteria criteria,
                       final AbstractField[] fields,
                       final ValuesConsumer consumer) {
        securityContext.secure(PERMISSION, () ->
                processorTaskDao.search(criteria, fields, consumer));
    }

    @Override
    public DocRef getDocRef() {
        if (securityContext.hasAppPermission(PERMISSION)) {
            return ProcessorTaskFields.PROCESSOR_TASK_PSEUDO_DOC_REF;
        }
        return null;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource
                .builder()
                .fields(ProcessorTaskFields.getFields())
                .timeField(ProcessorTaskFields.CREATE_TIME)
                .build();
    }
}
