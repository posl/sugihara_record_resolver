/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.health;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.health.node.DiskHealthInfo;
import org.elasticsearch.health.node.FetchHealthInfoCacheAction;
import org.elasticsearch.health.node.HealthInfo;
import org.elasticsearch.health.node.selection.HealthNode;
import org.elasticsearch.test.ESTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.elasticsearch.health.HealthStatus.GREEN;
import static org.elasticsearch.health.HealthStatus.RED;
import static org.elasticsearch.health.HealthStatus.UNKNOWN;
import static org.elasticsearch.health.HealthStatus.YELLOW;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class HealthServiceTests extends ESTestCase {

    public void testShouldReturnGroupedIndicators() throws Exception {

        var networkLatency = new HealthIndicatorResult("network_latency", GREEN, null, null, null, null);
        var slowTasks = new HealthIndicatorResult("slow_task_assignment", YELLOW, null, null, null, null);
        var shardsAvailable = new HealthIndicatorResult("shards_availability", GREEN, null, null, null, null);

        var service = new HealthService(
            Collections.emptyList(),
            List.of(
                createMockHealthIndicatorService(networkLatency),
                createMockHealthIndicatorService(slowTasks),
                createMockHealthIndicatorService(shardsAvailable)
            )
        );

        NodeClient client = getTestClient(HealthInfo.EMPTY_HEALTH_INFO);

        assertExpectedHealthIndicatorResults(service, client, null, slowTasks, networkLatency, shardsAvailable);
        assertExpectedHealthIndicatorResults(service, client, "slow_task_assignment", slowTasks);
    }

    private void assertExpectedHealthIndicatorResults(
        HealthService service,
        NodeClient client,
        String indicatorName,
        HealthIndicatorResult... expectedHealthIndicatorResults
    ) throws Exception {
        AtomicBoolean onResponseCalled = new AtomicBoolean(false);
        service.getHealth(
            client,
            indicatorName,
            false,
            getExpectedHealthIndicatorResultsActionListener(onResponseCalled, expectedHealthIndicatorResults)
        );
        assertBusy(() -> assertThat(onResponseCalled.get(), equalTo(true)));
    }

    private ActionListener<List<HealthIndicatorResult>> getExpectedHealthIndicatorResultsActionListener(
        AtomicBoolean onResponseCalled,
        HealthIndicatorResult... healthIndicatorResults
    ) {
        return new ActionListener<>() {
            @Override
            public void onResponse(List<HealthIndicatorResult> results) {
                assertThat(results.size(), equalTo(healthIndicatorResults.length));
                assertThat(results, hasItems(healthIndicatorResults));
                onResponseCalled.set(true);
            }

            @Override
            public void onFailure(Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public void testDuplicateIndicatorNames() throws Exception {
        // Same indicator name, should throw exception:
        var networkLatency = new HealthIndicatorResult(
            "network_latency",
            GREEN,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        var slowTasks = new HealthIndicatorResult("network_latency", YELLOW, null, null, Collections.emptyList(), Collections.emptyList());
        var service = new HealthService(
            Collections.emptyList(),
            List.of(
                createMockHealthIndicatorService(networkLatency),
                createMockHealthIndicatorService(slowTasks),
                createMockHealthIndicatorService(networkLatency)
            )
        );
        NodeClient client = getTestClient(HealthInfo.EMPTY_HEALTH_INFO);
        // This is testing an assertion, so we expect it to blow up in place rather than calling onFailure:
        assertGetHealthThrowsException(service, client, null, true, AssertionError.class, null, false);
    }

    public void testMissingIndicator() throws Exception {
        var networkLatency = new HealthIndicatorResult("network_latency", GREEN, null, null, null, null);
        var slowTasks = new HealthIndicatorResult("slow_task_assignment", YELLOW, null, null, null, null);
        var shardsAvailable = new HealthIndicatorResult("shards_availability", GREEN, null, null, null, null);

        var service = new HealthService(
            Collections.emptyList(),
            List.of(
                createMockHealthIndicatorService(networkLatency),
                createMockHealthIndicatorService(slowTasks),
                createMockHealthIndicatorService(shardsAvailable)
            )
        );
        NodeClient client = getTestClient(HealthInfo.EMPTY_HEALTH_INFO);
        assertGetHealthThrowsException(
            service,
            client,
            "indicator99",
            false,
            ResourceNotFoundException.class,
            "Did not find indicator indicator99",
            true
        );
    }

    private <T extends Throwable> void assertGetHealthThrowsException(
        HealthService service,
        NodeClient client,
        String indicatorName,
        boolean explain,
        Class<T> expectedType,
        String expectedMessage,
        boolean expectOnFailCalled
    ) throws Exception {
        AtomicBoolean onFailureCalled = new AtomicBoolean(false);
        ActionListener<List<HealthIndicatorResult>> listener = getExpectThrowsActionListener(
            onFailureCalled,
            expectedType,
            expectedMessage
        );
        try {
            service.getHealth(client, indicatorName, explain, listener);
        } catch (Throwable t) {
            if (expectOnFailCalled || (expectedType.isInstance(t) == false)) {
                throw new RuntimeException("Unexpected throwable", t);
            } else {
                // Expected
                if (expectedMessage != null) {
                    assertThat(t.getMessage(), equalTo(expectedMessage));
                }
            }
        }
        if (expectOnFailCalled) {
            assertBusy(() -> assertThat(onFailureCalled.get(), equalTo(true)));
        }
    }

    private <T extends Throwable> ActionListener<List<HealthIndicatorResult>> getExpectThrowsActionListener(
        AtomicBoolean onFailureCalled,
        Class<T> expectedType,
        String expectedMessage
    ) {
        return new ActionListener<>() {
            @Override
            public void onResponse(List<HealthIndicatorResult> healthIndicatorResults) {
                fail("Expected failure");
            }

            @Override
            public void onFailure(Exception e) {
                if (expectedType.isInstance(e)) {
                    if (expectedMessage == null) {
                        onFailureCalled.set(true);
                    } else {
                        assertThat(e.getMessage(), equalTo(expectedMessage));
                        onFailureCalled.set(true);
                    }
                } else {
                    throw new RuntimeException("Unexpected exception", e);
                }
            }
        };
    }

    public void testPreflightIndicatorResultsPresent() throws Exception {
        // Preflight check
        var hasMaster = new HealthIndicatorResult("has_master", GREEN, null, null, null, null);
        // Other indicators
        var networkLatency = new HealthIndicatorResult("network_latency", GREEN, null, null, null, null);
        var slowTasks = new HealthIndicatorResult("slow_task_assignment", YELLOW, null, null, null, null);
        var shardsAvailable = new HealthIndicatorResult("shards_availability", GREEN, null, null, null, null);

        var service = new HealthService(
            List.of(createMockHealthIndicatorService(hasMaster)),
            List.of(
                createMockHealthIndicatorService(networkLatency),
                createMockHealthIndicatorService(slowTasks),
                createMockHealthIndicatorService(shardsAvailable)
            )
        );
        NodeClient client = getTestClient(HealthInfo.EMPTY_HEALTH_INFO);

        // Get all indicators returns preflight result mixed in with the other indicators
        assertExpectedHealthIndicatorResults(service, client, null, hasMaster, networkLatency, slowTasks, shardsAvailable);

        // Getting single indicator returns correct indicator still
        assertExpectedHealthIndicatorResults(service, client, "slow_task_assignment", slowTasks);

        // Getting single preflight indicator returns preflight indicator correctly
        assertExpectedHealthIndicatorResults(service, client, "has_master", hasMaster);
    }

    public void testThatIndicatorsGetHealthInfoData() throws Exception {
        /*
         * This test makes sure that HealthService is passing the data returned by the FetchHealthInfoCacheAction to all of the
         * HealthIndicatorServices except for the preflight ones.
         */
        // Preflight check
        var hasMaster = new HealthIndicatorResult("has_master", GREEN, null, null, null, null);
        // Other indicators
        var networkLatency = new HealthIndicatorResult("network_latency", GREEN, null, null, null, null);
        var slowTasks = new HealthIndicatorResult("slow_task_assignment", YELLOW, null, null, null, null);
        var shardsAvailable = new HealthIndicatorResult("shards_availability", GREEN, null, null, null, null);
        Map<String, DiskHealthInfo> diskHealthInfoMap = new HashMap<>();
        diskHealthInfoMap.put(
            randomAlphaOfLength(30),
            new DiskHealthInfo(randomFrom(HealthStatus.values()), randomFrom(DiskHealthInfo.Cause.values()))
        );
        HealthInfo healthInfo = new HealthInfo(diskHealthInfoMap);

        var service = new HealthService(
            // The preflight indicator does not get data because the data is not fetched until after the preflight check
            List.of(createMockHealthIndicatorService(hasMaster, HealthInfo.EMPTY_HEALTH_INFO)),
            List.of(
                createMockHealthIndicatorService(networkLatency, healthInfo),
                createMockHealthIndicatorService(slowTasks, healthInfo),
                createMockHealthIndicatorService(shardsAvailable, healthInfo)
            )
        );
        NodeClient client = getTestClient(healthInfo);

        // Get all indicators returns preflight result mixed in with the other indicators
        assertExpectedHealthIndicatorResults(service, client, null, hasMaster, networkLatency, slowTasks, shardsAvailable);
    }

    private void assertIndicatorIsUnknownStatus(HealthIndicatorResult result) {
        assertThat(result.status(), is(equalTo(UNKNOWN)));
        assertThat(result.symptom(), is(HealthService.UNKNOWN_RESULT_SUMMARY_PREFLIGHT_FAILED));
    }

    public void testPreflightIndicatorFailureTriggersUnknownResults() throws Exception {
        // Preflight checks
        var hasMaster = new HealthIndicatorResult("has_master", RED, null, null, null, null);
        var hasStorage = new HealthIndicatorResult("has_storage", GREEN, null, null, null, null);
        // Other indicators
        var networkLatency = new HealthIndicatorResult("network_latency", GREEN, null, null, null, null);
        var slowTasks = new HealthIndicatorResult("slow_task_assignment", YELLOW, null, null, null, null);
        var shardsAvailable = new HealthIndicatorResult("shards_availability", GREEN, null, null, null, null);

        var service = new HealthService(
            List.of(createMockHealthIndicatorService(hasMaster), createMockHealthIndicatorService(hasStorage)),
            List.of(
                createMockHealthIndicatorService(networkLatency),
                createMockHealthIndicatorService(slowTasks),
                createMockHealthIndicatorService(shardsAvailable)
            )
        );
        NodeClient client = getTestClient(HealthInfo.EMPTY_HEALTH_INFO);
        {
            List<HealthIndicatorResult> health = getHealthIndicatorResults(service, client, null);
            assertThat(health.size(), is(equalTo(5)));
            // Preflight indicators unchanged; posflight all say
            List<String> nonPreflightNames = Stream.of(networkLatency, slowTasks, shardsAvailable)
                .map(HealthIndicatorResult::name)
                .toList();
            health.stream()
                .filter(healthIndicatorResult -> nonPreflightNames.contains(healthIndicatorResult.name()))
                .forEach(this::assertIndicatorIsUnknownStatus);
            List<HealthIndicatorResult> preflightResults = List.of(hasMaster, hasStorage);
            preflightResults.forEach(healthIndicatorResult -> assertTrue(health.contains(healthIndicatorResult)));
        }

        {
            List<HealthIndicatorResult> health = getHealthIndicatorResults(service, client, "slow_task_assignment");
            assertThat(health.size(), is(equalTo(1)));
            assertIndicatorIsUnknownStatus(health.get(0));
        }

        {
            List<HealthIndicatorResult> health = getHealthIndicatorResults(service, client, "has_master");
            assertThat(health.size(), is(equalTo(1)));
            assertThat(health.get(0), is(equalTo(hasMaster)));
        }
    }

    private List<HealthIndicatorResult> getHealthIndicatorResults(HealthService service, NodeClient client, String indicatorName)
        throws Exception {
        AtomicReference<List<HealthIndicatorResult>> resultReference = new AtomicReference<>();
        ActionListener<List<HealthIndicatorResult>> listener = new ActionListener<>() {
            @Override
            public void onResponse(List<HealthIndicatorResult> healthIndicatorResults) {
                resultReference.set(healthIndicatorResults);
            }

            @Override
            public void onFailure(Exception e) {
                throw new RuntimeException(e);
            }
        };
        service.getHealth(client, indicatorName, false, listener);
        assertBusy(() -> assertNotNull(resultReference.get()));
        return resultReference.get();
    }

    /**
     * This returns a mocked NodeClient that will return the given HealthInfo if the FetchHealthInfoCacheAction is called.
     * @param healthInfo The HealthInfo that will be returned if this client calls the FetchHealthInfoCacheAction
     * @return A mocked NodeClient
     */
    @SuppressWarnings("unchecked")
    private NodeClient getTestClient(HealthInfo healthInfo) {
        NodeClient client = mock(NodeClient.class);
        doAnswer(invocation -> {
            ActionListener<FetchHealthInfoCacheAction.Response> actionListener = invocation.getArgument(2, ActionListener.class);
            actionListener.onResponse(new FetchHealthInfoCacheAction.Response(healthInfo));
            return null;
        }).when(client).doExecute(any(ActionType.class), any(), any(ActionListener.class));
        return client;
    }

    private static HealthIndicatorService createMockHealthIndicatorService(HealthIndicatorResult result) {
        return createMockHealthIndicatorService(result, null);
    }

    /**
     * This returns a test HealthIndicatorService
     * @param result The HealthIndicatorResult that will be returned by the calculate method when the HealthIndicatorService returned by
     *               this method is called
     * @param expectedHealthInfo If this HealthInfo is not null then the returned HealthIndicatorService's calculate method will assert
     *                           that the HealthInfo it is passed is equal to this when it is called
     * @return A test HealthIndicatorService
     */
    private static HealthIndicatorService createMockHealthIndicatorService(HealthIndicatorResult result, HealthInfo expectedHealthInfo) {
        return new HealthIndicatorService() {
            @Override
            public String name() {
                return result.name();
            }

            @Override
            public HealthIndicatorResult calculate(boolean explain, HealthInfo healthInfo) {
                if (expectedHealthInfo != null && HealthNode.isEnabled()) {
                    assertThat(healthInfo, equalTo(expectedHealthInfo));
                }
                return result;
            }
        };
    }
}
