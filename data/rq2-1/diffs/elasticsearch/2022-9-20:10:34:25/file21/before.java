/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.slm.action;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.AckedClusterStateUpdateTask;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateUpdateTask;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.reservedstate.ReservedClusterStateHandler;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.slm.SnapshotLifecycleMetadata;
import org.elasticsearch.xpack.core.slm.SnapshotLifecyclePolicyMetadata;
import org.elasticsearch.xpack.core.slm.action.DeleteSnapshotLifecycleAction;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TransportDeleteSnapshotLifecycleAction extends TransportMasterNodeAction<
    DeleteSnapshotLifecycleAction.Request,
    DeleteSnapshotLifecycleAction.Response> {

    @Inject
    public TransportDeleteSnapshotLifecycleAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            DeleteSnapshotLifecycleAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            DeleteSnapshotLifecycleAction.Request::new,
            indexNameExpressionResolver,
            DeleteSnapshotLifecycleAction.Response::new,
            ThreadPool.Names.SAME
        );
    }

    @Override
    protected void masterOperation(
        Task task,
        DeleteSnapshotLifecycleAction.Request request,
        ClusterState state,
        ActionListener<DeleteSnapshotLifecycleAction.Response> listener
    ) throws Exception {
        submitUnbatchedTask("delete-snapshot-lifecycle-" + request.getLifecycleId(), new DeleteSnapshotPolicyTask(request, listener) {
            @Override
            protected DeleteSnapshotLifecycleAction.Response newResponse(boolean acknowledged) {
                return new DeleteSnapshotLifecycleAction.Response(acknowledged);
            }
        });
    }

    /**
     * Extracted extension of {@link AckedClusterStateUpdateTask} with only the execute method
     * implementation, so that the execute() transformation can be reused for {@link ReservedSnapshotAction}
     */
    public static class DeleteSnapshotPolicyTask extends AckedClusterStateUpdateTask {
        private final DeleteSnapshotLifecycleAction.Request request;

        DeleteSnapshotPolicyTask(
            DeleteSnapshotLifecycleAction.Request request,
            ActionListener<DeleteSnapshotLifecycleAction.Response> listener
        ) {
            super(request, listener);
            this.request = request;
        }

        /**
         * Used by the {@link ReservedClusterStateHandler} for SLM
         * {@link ReservedSnapshotAction}
         */
        DeleteSnapshotPolicyTask(String policyId) {
            this(new DeleteSnapshotLifecycleAction.Request(policyId), null);
        }

        @Override
        public ClusterState execute(ClusterState currentState) {
            SnapshotLifecycleMetadata snapMeta = currentState.metadata().custom(SnapshotLifecycleMetadata.TYPE);
            if (snapMeta == null) {
                throw new ResourceNotFoundException("snapshot lifecycle policy not found: {}", request.getLifecycleId());
            }
            // Check that the policy exists in the first place
            snapMeta.getSnapshotConfigurations()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getPolicy().getId().equals(request.getLifecycleId()))
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("snapshot lifecycle policy not found: {}", request.getLifecycleId()));

            Map<String, SnapshotLifecyclePolicyMetadata> newConfigs = snapMeta.getSnapshotConfigurations()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().equals(request.getLifecycleId()) == false)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Metadata metadata = currentState.metadata();
            return ClusterState.builder(currentState)
                .metadata(
                    Metadata.builder(metadata)
                        .putCustom(
                            SnapshotLifecycleMetadata.TYPE,
                            new SnapshotLifecycleMetadata(
                                newConfigs,
                                snapMeta.getOperationMode(),
                                snapMeta.getStats().removePolicy(request.getLifecycleId())
                            )
                        )
                )
                .build();
        }
    }

    @SuppressForbidden(reason = "legacy usage of unbatched task") // TODO add support for batching here
    private void submitUnbatchedTask(@SuppressWarnings("SameParameterValue") String source, ClusterStateUpdateTask task) {
        clusterService.submitUnbatchedStateUpdateTask(source, task);
    }

    @Override
    protected ClusterBlockException checkBlock(DeleteSnapshotLifecycleAction.Request request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected Optional<String> reservedStateHandlerName() {
        return Optional.of(ReservedSnapshotAction.NAME);
    }

    @Override
    protected Set<String> modifiedKeys(DeleteSnapshotLifecycleAction.Request request) {
        return Set.of(request.getLifecycleId());
    }
}
