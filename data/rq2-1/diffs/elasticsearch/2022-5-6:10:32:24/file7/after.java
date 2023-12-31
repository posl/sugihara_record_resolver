/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.coordination;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MasterHistoryServiceTests extends ESTestCase {

    public void testGetRemoteHistory() throws Exception {
        MasterHistoryService masterHistoryService = createMasterHistoryService();
        List<DiscoveryNode> remoteHistory = masterHistoryService.getRemoteMasterHistory();
        assertNull(remoteHistory);
        DiscoveryNode masterNode = new DiscoveryNode(UUID.randomUUID().toString(), buildNewFakeTransportAddress(), Version.CURRENT);
        List<DiscoveryNode> masterHistory = new ArrayList<>();
        masterHistory.add(masterNode);
        masterHistory.add(null);
        masterHistory.add(masterNode);
        masterHistory.add(null);
        masterHistory.add(masterNode);
        masterHistory.add(null);
        masterHistory.add(masterNode);
        masterHistoryService.remoteHistoryOrException = new MasterHistoryService.RemoteHistoryOrException(masterHistory);
        remoteHistory = masterHistoryService.getRemoteMasterHistory();
        assertThat(remoteHistory, equalTo(masterHistory));
        Exception exception = new Exception("Something happened");
        masterHistoryService.remoteHistoryOrException = new MasterHistoryService.RemoteHistoryOrException(exception);
        assertThat(
            expectThrows(Exception.class, masterHistoryService::getRemoteMasterHistory).getMessage(),
            containsString("Something happened")
        );
    }

    private static MasterHistoryService createMasterHistoryService() throws Exception {
        var clusterService = mock(ClusterService.class);
        when(clusterService.getSettings()).thenReturn(Settings.EMPTY);
        ThreadPool threadPool = mock(ThreadPool.class);
        when(threadPool.relativeTimeInMillis()).thenReturn(System.currentTimeMillis());
        TransportService transportService = mock(TransportService.class);
        return new MasterHistoryService(transportService, threadPool, clusterService);
    }
}
