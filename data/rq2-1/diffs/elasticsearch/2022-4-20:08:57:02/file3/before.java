/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.shutdown;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.metadata.NodesShutdownMetadata;
import org.elasticsearch.cluster.metadata.SingleNodeShutdownMetadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.junit.Before;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.elasticsearch.cluster.metadata.NodesShutdownMetadata.TYPE;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TransportDeleteShutdownNodeActionTests extends ESTestCase {
    private ClusterService clusterService;
    private TransportDeleteShutdownNodeAction action;

    @Before
    public void init() {
        // TODO: it takes almost 2 seconds to create these mocks....WHY?!?
        var threadPool = mock(ThreadPool.class);
        var transportService = mock(TransportService.class);
        clusterService = mock(ClusterService.class);
        var actionFilters = mock(ActionFilters.class);
        var indexNameExpressionResolver = mock(IndexNameExpressionResolver.class);
        action = new TransportDeleteShutdownNodeAction(
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            indexNameExpressionResolver
        );
    }

    public void testNoop() throws Exception {
        var singleNodeMetadata = mock(SingleNodeShutdownMetadata.class);
        var nodesShutdownMetadata = new NodesShutdownMetadata(Map.of("node1", singleNodeMetadata));
        var metadata = Metadata.builder().putCustom(TYPE, nodesShutdownMetadata).build();
        var clusterStateWithShutdown = ClusterState.builder(ClusterState.EMPTY_STATE).metadata(metadata).build();

        var request = new DeleteShutdownNodeAction.Request("node1");
        action.masterOperation(null, request, clusterStateWithShutdown, ActionListener.noop());
        var updateTaskCapture = ArgumentCaptor.forClass(ClusterStateUpdateTask.class);
        verify(clusterService).submitStateUpdateTask(any(), updateTaskCapture.capture(), any());
        ClusterState gotState = updateTaskCapture.getValue().execute(ClusterState.EMPTY_STATE);
        assertThat(gotState, sameInstance(ClusterState.EMPTY_STATE));
    }
}
