/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.shutdown;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.AcknowledgedTransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateTaskConfig;
import org.elasticsearch.cluster.ClusterStateTaskExecutor;
import org.elasticsearch.cluster.ClusterStateTaskListener;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.metadata.NodesShutdownMetadata;
import org.elasticsearch.cluster.metadata.SingleNodeShutdownMetadata;
import org.elasticsearch.cluster.routing.RerouteService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.shutdown.DeleteShutdownNodeAction.Request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.cluster.metadata.NodesShutdownMetadata.getShutdownsOrEmpty;

public class TransportDeleteShutdownNodeAction extends AcknowledgedTransportMasterNodeAction<Request> {
    private static final Logger logger = LogManager.getLogger(TransportDeleteShutdownNodeAction.class);

    private final DeleteShutdownNodeExecutor executor = new DeleteShutdownNodeExecutor();

    private static boolean deleteShutdownNodeState(Map<String, SingleNodeShutdownMetadata> shutdownMetadata, Request request) {
        if (shutdownMetadata.containsKey(request.getNodeId()) == false) {
            // noop, the node has already been removed by the time we got to this update task
            return false;
        }

        logger.info("removing shutdown record for node [{}]", request.getNodeId());
        shutdownMetadata.remove(request.getNodeId());
        return true;
    }

    private static void ackAndReroute(Request request, ActionListener<AcknowledgedResponse> listener, RerouteService rerouteService) {
        rerouteService.reroute("node registered for removal from cluster", Priority.URGENT, new ActionListener<>() {
            @Override
            public void onResponse(ClusterState clusterState) {}

            @Override
            public void onFailure(Exception e) {
                logger.warn(() -> "failed to reroute after deleting node [" + request.getNodeId() + "] shutdown", e);
            }
        });
        listener.onResponse(AcknowledgedResponse.TRUE);
    }

    // package private for tests
    record DeleteShutdownNodeTask(Request request, ActionListener<AcknowledgedResponse> listener) implements ClusterStateTaskListener {
        @Override
        public void onFailure(Exception e) {
            logger.error(new ParameterizedMessage("failed to delete shutdown for node [{}]", request.getNodeId()), e);
            listener.onFailure(e);
        }
    }

    // package private for tests
    class DeleteShutdownNodeExecutor implements ClusterStateTaskExecutor<DeleteShutdownNodeTask> {
        @Override
        public ClusterState execute(ClusterState currentState, List<TaskContext<DeleteShutdownNodeTask>> taskContexts) throws Exception {
            var shutdownMetadata = new HashMap<>(getShutdownsOrEmpty(currentState).getAllNodeMetadataMap());
            boolean changed = false;
            for (final var taskContext : taskContexts) {
                var request = taskContext.getTask().request();
                try {
                    changed |= deleteShutdownNodeState(shutdownMetadata, request);
                } catch (Exception e) {
                    taskContext.onFailure(e);
                    continue;
                }
                var reroute = clusterService.getRerouteService();
                taskContext.success(taskContext.getTask().listener().delegateFailure((l, s) -> ackAndReroute(request, l, reroute)));
            }
            if (changed == false) {
                return currentState;
            }
            return ClusterState.builder(currentState)
                .metadata(
                    Metadata.builder(currentState.metadata())
                        .putCustom(NodesShutdownMetadata.TYPE, new NodesShutdownMetadata(shutdownMetadata))
                )
                .build();
        }
    }

    @Inject
    public TransportDeleteShutdownNodeAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            DeleteShutdownNodeAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            Request::new,
            indexNameExpressionResolver,
            ThreadPool.Names.SAME
        );
    }

    @Override
    protected void masterOperation(Task task, Request request, ClusterState state, ActionListener<AcknowledgedResponse> listener)
        throws Exception {
        { // This block solely to ensure this NodesShutdownMetadata isn't accidentally used in the cluster state update task below
            NodesShutdownMetadata nodesShutdownMetadata = state.metadata().custom(NodesShutdownMetadata.TYPE);
            if (nodesShutdownMetadata == null || nodesShutdownMetadata.getAllNodeMetadataMap().get(request.getNodeId()) == null) {
                throw new ResourceNotFoundException("node [" + request.getNodeId() + "] is not currently shutting down");
            }
        }

        var deleteTask = new DeleteShutdownNodeTask(request, listener);
        var taskConfig = ClusterStateTaskConfig.build(Priority.URGENT, request.masterNodeTimeout());
        clusterService.submitStateUpdateTask("delete-node-shutdown-" + request.getNodeId(), deleteTask, taskConfig, executor);
    }

    @Override
    protected ClusterBlockException checkBlock(Request request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
