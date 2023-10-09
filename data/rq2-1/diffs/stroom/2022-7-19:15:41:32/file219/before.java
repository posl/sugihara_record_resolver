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

package stroom.dashboard.client.main;

import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.DashboardResource;
import stroom.dashboard.shared.DashboardSearchRequest;
import stroom.dashboard.shared.DashboardSearchResponse;
import stroom.dashboard.shared.SearchBusPollRequest;
import stroom.dispatch.client.ApplicationInstanceIdProvider;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.security.client.api.event.LogoutEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Singleton
public class SearchBus {

    private static final DashboardResource DASHBOARD_RESOURCE = GWT.create(DashboardResource.class);

    private static final int ONE_SECOND = 1000;
    private static final int DEFAULT_POLL_INTERVAL = ONE_SECOND;
    private static final int QUICK_POLL_INTERVAL = 10;

    private final RestFactory restFactory;
    private final ApplicationInstanceIdProvider applicationInstanceIdProvider;

    private final Map<DashboardQueryKey, SearchModel> activeSearchMap = new HashMap<>();
    private final Timer pollingTimer;
    private int delayMillis = DEFAULT_POLL_INTERVAL;
    private boolean forcePoll;
    private boolean polling;

    @Inject
    public SearchBus(final EventBus eventBus,
                     final RestFactory restFactory,
                     final ApplicationInstanceIdProvider applicationInstanceIdProvider) {
        this.restFactory = restFactory;
        this.applicationInstanceIdProvider = applicationInstanceIdProvider;

        pollingTimer = new Timer() {
            @Override
            public void run() {
                doPoll();
            }
        };

        // Listen for logout events.
        eventBus.addHandler(LogoutEvent.getType(), event -> reset());
    }

    private void reset() {
        activeSearchMap.clear();
        delayMillis = DEFAULT_POLL_INTERVAL;
        forcePoll = false;
        polling = false;
        pollingTimer.cancel();
    }

    public void put(final DashboardQueryKey queryKey, final SearchModel searchModel) {
        activeSearchMap.put(queryKey, searchModel);
    }

    public void remove(final DashboardQueryKey queryKey) {
        activeSearchMap.remove(queryKey);
    }

    public void poll() {
        if (polling) {
            delayMillis = QUICK_POLL_INTERVAL;
            forcePoll = true;
        } else {
            poll(QUICK_POLL_INTERVAL);
        }
    }

    private void poll(final int delayMillis) {
        pollingTimer.schedule(delayMillis);
    }

    private void doPoll() {
        polling = true;
        final Set<DashboardSearchRequest> searchRequests = new HashSet<>();
        for (final Entry<DashboardQueryKey, SearchModel> entry : activeSearchMap.entrySet()) {
            final SearchModel searchModel = entry.getValue();
            final DashboardSearchRequest searchRequest = searchModel.getCurrentRequest();
            if (searchRequest != null) {
                searchRequests.add(searchRequest);
            }
        }

        final Rest<Set<DashboardSearchResponse>> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    try {
                        for (final DashboardSearchResponse searchResponse : result) {
                            final DashboardQueryKey queryKey = searchResponse.getQueryKey();
                            final SearchModel searchModel = activeSearchMap.get(queryKey);
                            if (searchModel != null) {
                                searchModel.update(searchResponse);
                            }
                        }

                        polling = false;

                        // Remember and reset delay.
                        final int delay = delayMillis;
                        delayMillis = DEFAULT_POLL_INTERVAL;

                        if (forcePoll || activeSearchMap.size() > 0) {
                            // Reset force.
                            forcePoll = false;
                            poll(delay);
                        }
                    } catch (final RuntimeException e) {
                        GWT.log(e.getMessage());
                    }
                })
                .call(DASHBOARD_RESOURCE)
                .poll(new SearchBusPollRequest(applicationInstanceIdProvider.get(), searchRequests));
    }
}
