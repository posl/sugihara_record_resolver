/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cluster.coordination;

import org.elasticsearch.cluster.node.DiscoveryNode;

public interface LeaderHeartbeatService {
    LeaderHeartbeatService NO_OP = new LeaderHeartbeatService() {
        @Override
        public void start(DiscoveryNode currentLeader, long term) {

        }

        @Override
        public void stop() {

        }
    };

    void start(DiscoveryNode currentLeader, long term);

    void stop();
}
