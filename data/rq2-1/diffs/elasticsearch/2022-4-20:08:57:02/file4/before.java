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
import org.elasticsearch.cluster.metadata.SingleNodeShutdownMetadata.Type;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.junit.Before;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class TransportPutShutdownNodeActionTests extends ESTestCase {

    private ClusterService clusterService;
    private TransportPutShutdownNodeAction action;

    @Before
    public void init() {
        // TODO: it takes almost 2 seconds to create these mocks....WHY?!?
        var threadPool = mock(ThreadPool.class);
        var transportService = mock(TransportService.class);
        clusterService = mock(ClusterService.class);
        var actionFilters = mock(ActionFilters.class);
        var indexNameExpressionResolver = mock(IndexNameExpressionResolver.class);
        action = new TransportPutShutdownNodeAction(
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            indexNameExpressionResolver
        );
    }

    public void testNoop() throws Exception {
        var type = randomFrom(Type.REMOVE, Type.REPLACE, Type.RESTART);
        var allocationDelay = type == Type.RESTART ? TimeValue.timeValueMinutes(randomIntBetween(1, 3)) : null;
        var targetNodeName = type == Type.REPLACE ? randomAlphaOfLength(5) : null;
        var request = new PutShutdownNodeAction.Request("node1", type, "sunsetting", allocationDelay, targetNodeName);
        action.masterOperation(null, request, ClusterState.EMPTY_STATE, ActionListener.noop());
        var updateTaskCapture = ArgumentCaptor.forClass(ClusterStateUpdateTask.class);
        verify(clusterService).submitStateUpdateTask(any(), updateTaskCapture.capture(), any());
        ClusterState stableState = updateTaskCapture.getValue().execute(ClusterState.EMPTY_STATE);

        // run the request again, there should be no call to submit an update task
        clearInvocations(clusterService);
        action.masterOperation(null, request, stableState, ActionListener.noop());
        verifyNoInteractions(clusterService);

        // run the request again with empty state, the update task should return the same state
        action.masterOperation(null, request, ClusterState.EMPTY_STATE, ActionListener.noop());
        verify(clusterService).submitStateUpdateTask(any(), updateTaskCapture.capture(), any());
        ClusterState gotState = updateTaskCapture.getValue().execute(stableState);
        assertThat(gotState, sameInstance(stableState));
    }
}
