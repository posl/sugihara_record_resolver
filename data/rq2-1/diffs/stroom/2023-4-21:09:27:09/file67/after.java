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

package stroom.test;

import stroom.docref.DocRef;
import stroom.meta.api.MetaService;
import stroom.node.api.NodeInfo;
import stroom.pipeline.shared.TextConverterDoc.TextConverterType;
import stroom.processor.api.ProcessorResult;
import stroom.processor.impl.DataProcessorTaskHandler;
import stroom.processor.impl.ProcessorTaskQueueManager;
import stroom.processor.impl.ProcessorTaskTestHelper;
import stroom.processor.shared.ProcessorTask;
import stroom.processor.shared.ProcessorTaskList;
import stroom.test.common.StroomPipelineTestFileUtil;
import stroom.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonTranslationTestHelper {

    public static final String FEED_NAME = "TEST_FEED";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonTranslationTestHelper.class);
    private static final String DIR = "CommonTranslationTest/";
    public static final Path INVALID_RESOURCE_NAME = StroomPipelineTestFileUtil.getTestResourcesFile(
            DIR + "Invalid.in");
    public static final Path VALID_RESOURCE_NAME = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "NetworkMonitoringSample.in");
    //    private static final Path CSV = StroomPipelineTestFileUtil.getTestResourcesFile(DIR + "CSV.ds");
    private static final Path CSV_WITH_HEADING = StroomPipelineTestFileUtil.getTestResourcesFile(
            DIR + "CSVWithHeading.ds");
    private static final Path XSLT_HOST_NAME_TO_LOCATION = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "SampleRefData-HostNameToLocation.xsl");
    private static final Path XSLT_HOST_NAME_TO_IP = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "SampleRefData-HostNameToIP.xsl");
    private static final Path XSLT_NETWORK_MONITORING = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "NetworkMonitoring.xsl");
    private static final Path REFDATA_HOST_NAME_TO_LOCATION = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "SampleRefData-HostNameToLocation.in");
    private static final Path REFDATA_HOST_NAME_TO_IP = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "SampleRefData-HostNameToIP.in");

    private static final String REFFEED_HOSTNAME_TO_LOCATION = "HOSTNAME_TO_LOCATION";
    private static final String REFFEED_HOSTNAME_TO_IP = "HOSTNAME_TO_IP";

    private static final String ID_TO_USER = "ID_TO_USER";
    private static final Path EMPLOYEE_REFERENCE_XSL = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "EmployeeReference.xsl");
    private static final Path EMPLOYEE_REFERENCE_CSV = StroomPipelineTestFileUtil
            .getTestResourcesFile(DIR + "EmployeeReference.in");

    private final NodeInfo nodeInfo;
    private final ProcessorTaskQueueManager processorTaskQueueManager;
    private final StoreCreationTool storeCreationTool;
    private final MetaService metaService;
    private final Provider<DataProcessorTaskHandler> dataProcessorTaskHandlerProvider;
    private final ProcessorTaskTestHelper processorTaskTestHelper;

    @Inject
    CommonTranslationTestHelper(final NodeInfo nodeInfo,
                                final ProcessorTaskQueueManager processorTaskQueueManager,
                                final ProcessorTaskTestHelper processorTaskTestHelper,
                                final StoreCreationTool storeCreationTool,
                                final MetaService metaService,
                                final Provider<DataProcessorTaskHandler> dataProcessorTaskHandlerProvider) {
        this.nodeInfo = nodeInfo;
        this.processorTaskQueueManager = processorTaskQueueManager;
        this.processorTaskTestHelper = processorTaskTestHelper;
        this.storeCreationTool = storeCreationTool;
        this.metaService = metaService;
        this.dataProcessorTaskHandlerProvider = dataProcessorTaskHandlerProvider;
    }

    public List<ProcessorResult> processAll() {
        // Force creation of stream tasks.
        processorTaskTestHelper.createAndQueueTasks();

        // We have to process 1 task at a time to ensure the ref data gets processed first.
        final List<ProcessorResult> results = new ArrayList<>();
        ProcessorTaskList processorTasks = processorTaskQueueManager.assignTasks(nodeInfo.getThisNodeName(), 1);
        while (processorTasks.getList().size() > 0) {
            for (final ProcessorTask processorTask : processorTasks.getList()) {
                results.add(process(processorTask));
            }
            processorTasks = processorTaskQueueManager.assignTasks(nodeInfo.getThisNodeName(), 1);
        }

        return results;
    }

    public ProcessorResult process(final ProcessorTask processorTask) {
        final DataProcessorTaskHandler dataProcessorTaskHandler = dataProcessorTaskHandlerProvider.get();
        return dataProcessorTaskHandler.exec(processorTask);
    }

    public void setup() {
        setup(FEED_NAME, Collections.singletonList(VALID_RESOURCE_NAME));
    }

    public void setup(final List<Path> dataLocations) {
        setup(FEED_NAME, dataLocations);
    }

    public void setup(final String feedName, final Path dataLocation) {
        setup(feedName, Collections.singletonList(dataLocation));
    }

    public void setup(final String feedName, final List<Path> dataLocations) {
        // commonTestControl.setup();

        // Setup the feed definitions.
        final DocRef hostNameToIP = storeCreationTool.addReferenceData(REFFEED_HOSTNAME_TO_IP,
                TextConverterType.DATA_SPLITTER, CSV_WITH_HEADING, XSLT_HOST_NAME_TO_IP, REFDATA_HOST_NAME_TO_IP);
        final DocRef hostNameToLocation = storeCreationTool.addReferenceData(REFFEED_HOSTNAME_TO_LOCATION,
                TextConverterType.DATA_SPLITTER, CSV_WITH_HEADING, XSLT_HOST_NAME_TO_LOCATION,
                REFDATA_HOST_NAME_TO_LOCATION);
        final DocRef idToUser = storeCreationTool.addReferenceData(ID_TO_USER, TextConverterType.DATA_SPLITTER,
                CSV_WITH_HEADING, EMPLOYEE_REFERENCE_XSL, EMPLOYEE_REFERENCE_CSV);

        final Set<DocRef> referenceFeeds = new HashSet<>();
        referenceFeeds.add(hostNameToIP);
        referenceFeeds.add(hostNameToLocation);
        referenceFeeds.add(idToUser);

        dataLocations.forEach(dataLocation -> {
            try {
                LOGGER.info("Adding data from file {}", FileUtil.getCanonicalPath(dataLocation));
                storeCreationTool.addEventData(feedName, TextConverterType.DATA_SPLITTER, CSV_WITH_HEADING,
                        XSLT_NETWORK_MONITORING, dataLocation, referenceFeeds);
            } catch (final IOException e) {
                throw new UncheckedIOException(String.format("Error adding event data for file %s",
                        FileUtil.getCanonicalPath(dataLocation)), e);
            }
        });

        assertThat(metaService.getLockCount()).isEqualTo(0);
    }
}
