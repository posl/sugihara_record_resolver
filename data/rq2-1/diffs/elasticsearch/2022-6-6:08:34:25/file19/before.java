/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.license;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateTaskExecutor;
import org.elasticsearch.cluster.ClusterStateTaskListener;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.xpack.core.XPackPlugin;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StartTrialClusterTask implements ClusterStateTaskListener {

    private static final String ACKNOWLEDGEMENT_HEADER = "This API initiates a free 30-day trial for all platinum features. "
        + "By starting this trial, you agree that it is subject to the terms and conditions at"
        + " https://www.elastic.co/legal/trial_license/. To begin your free trial, call /start_trial again and specify "
        + "the \"acknowledge=true\" parameter.";

    private static final Map<String, String[]> ACK_MESSAGES = Collections.singletonMap(
        "security",
        new String[] { "With a trial license, X-Pack security features are available, but are not enabled by default." }
    );

    static final String TASK_SOURCE = "started trial license";

    private final Logger logger;
    private final String clusterName;
    private final PostStartTrialRequest request;
    private final ActionListener<PostStartTrialResponse> listener;
    private final Clock clock;

    StartTrialClusterTask(
        Logger logger,
        String clusterName,
        Clock clock,
        PostStartTrialRequest request,
        ActionListener<PostStartTrialResponse> listener
    ) {
        this.logger = logger;
        this.clusterName = clusterName;
        this.request = request;
        this.listener = listener;
        this.clock = clock;
    }

    @Override
    public void clusterStateProcessed(ClusterState oldState, ClusterState newState) {
        assert false : "never called";
    }

    private LicensesMetadata execute(
        LicensesMetadata currentLicensesMetadata,
        DiscoveryNodes discoveryNodes,
        ClusterStateTaskExecutor.TaskContext<StartTrialClusterTask> taskContext
    ) {
        assert taskContext.getTask() == this;
        final var listener = ActionListener.runBefore(
            this.listener,
            () -> { logger.debug("started self generated trial license: {}", currentLicensesMetadata); }
        );
        if (request.isAcknowledged() == false) {
            taskContext.success(
                listener.delegateFailure(
                    (l, s) -> l.onResponse(
                        new PostStartTrialResponse(PostStartTrialResponse.Status.NEED_ACKNOWLEDGEMENT, ACK_MESSAGES, ACKNOWLEDGEMENT_HEADER)
                    )
                )
            );
            return currentLicensesMetadata;
        } else if (currentLicensesMetadata == null || currentLicensesMetadata.isEligibleForTrial()) {
            long issueDate = clock.millis();
            long expiryDate = issueDate + LicenseService.NON_BASIC_SELF_GENERATED_LICENSE_DURATION.getMillis();

            License.Builder specBuilder = License.builder()
                .uid(UUID.randomUUID().toString())
                .issuedTo(clusterName)
                .issueDate(issueDate)
                .type(request.getType())
                .expiryDate(expiryDate);
            if (License.LicenseType.isEnterprise(request.getType())) {
                specBuilder.maxResourceUnits(LicenseService.SELF_GENERATED_LICENSE_MAX_RESOURCE_UNITS);
            } else {
                specBuilder.maxNodes(LicenseService.SELF_GENERATED_LICENSE_MAX_NODES);
            }
            License selfGeneratedLicense = SelfGeneratedLicense.create(specBuilder, discoveryNodes);
            LicensesMetadata newLicensesMetadata = new LicensesMetadata(selfGeneratedLicense, Version.CURRENT);
            taskContext.success(
                listener.delegateFailure(
                    (delegate, ignored) -> delegate.onResponse(new PostStartTrialResponse(PostStartTrialResponse.Status.UPGRADED_TO_TRIAL))
                )
            );
            return newLicensesMetadata;
        } else {
            taskContext.success(
                listener.delegateFailure(
                    (l, s) -> l.onResponse(new PostStartTrialResponse(PostStartTrialResponse.Status.TRIAL_ALREADY_ACTIVATED))
                )
            );
            return currentLicensesMetadata;
        }
    }

    @Override
    public void onFailure(@Nullable Exception e) {
        logger.error("unexpected failure during [" + TASK_SOURCE + "]", e);
        listener.onFailure(e);
    }

    static class Executor implements ClusterStateTaskExecutor<StartTrialClusterTask> {

        @Override
        public ClusterState execute(ClusterState currentState, List<TaskContext<StartTrialClusterTask>> taskContexts) throws Exception {
            XPackPlugin.checkReadyForXPackCustomMetadata(currentState);
            final LicensesMetadata originalLicensesMetadata = currentState.metadata().custom(LicensesMetadata.TYPE);
            var currentLicensesMetadata = originalLicensesMetadata;
            for (final var taskContext : taskContexts) {
                currentLicensesMetadata = taskContext.getTask().execute(currentLicensesMetadata, currentState.nodes(), taskContext);
            }
            if (currentLicensesMetadata == originalLicensesMetadata) {
                return currentState;
            } else {
                return ClusterState.builder(currentState)
                    .metadata(Metadata.builder(currentState.metadata()).putCustom(LicensesMetadata.TYPE, currentLicensesMetadata))
                    .build();
            }
        }
    }

}
