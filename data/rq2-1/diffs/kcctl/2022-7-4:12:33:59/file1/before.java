/*
 *  Copyright 2021 The original authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.kcctl.command;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.kcctl.completion.ConnectorNameCompletions;
import org.kcctl.service.ConnectorInfo;
import org.kcctl.service.ConnectorStatusInfo;
import org.kcctl.service.KafkaConnectApi;
import org.kcctl.service.TaskState;
import org.kcctl.service.TopicsInfo;
import org.kcctl.util.ConfigurationContext;
import org.kcctl.util.Connectors;
import org.kcctl.util.Tuple;
import org.kcctl.util.Version;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.kcctl.util.Colors.ANSI_GREEN;
import static org.kcctl.util.Colors.ANSI_RED;
import static org.kcctl.util.Colors.ANSI_RESET;
import static org.kcctl.util.Colors.ANSI_WHITE_BOLD;
import static org.kcctl.util.Colors.ANSI_YELLOW;

@Command(name = "connector", aliases = "connectors", description = "Displays information about given connectors")
public class DescribeConnectorCommand implements Callable<Integer> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Parameters(paramLabel = "CONNECTOR NAME", description = "Name of the connector", completionCandidates = ConnectorNameCompletions.class)
    Set<String> names = Set.of();

    @CommandLine.Option(names = { "-e", "--reg-exp" }, description = "use CONNECTOR NAME(s) as regexp pattern(s) to use on all connectors")
    boolean regexpMode = false;

    @Option(names = { "--tasks-config" }, description = "Displays tasks configuration")
    boolean includeTasksConfig;

    private final ConfigurationContext context;

    @Inject
    public DescribeConnectorCommand(ConfigurationContext context) {
        this.context = context;
    }

    // Hack : Picocli currently require an empty constructor to generate the completion file
    public DescribeConnectorCommand() {
        context = new ConfigurationContext();
    }

    private final Version requiredVersionForTasksConfig = new Version(2, 8);
    private final Version requiredVersionForTopicsApi = new Version(2, 5);

    @Override
    public Integer call() {

        KafkaConnectApi kafkaConnectApi = RestClientBuilder.newBuilder()
                .baseUri(context.getCurrentContext().getCluster())
                .build(KafkaConnectApi.class);
        Version currentVersion = new Version(kafkaConnectApi.getWorkerInfo().version);

        if (includeTasksConfig) {
            if (!currentVersion.greaterOrEquals(requiredVersionForTasksConfig)) {
                System.out.println("--tasks-config requires at least Kafka Connect 2.8. Current version: " + currentVersion);
                return 1;
            }
        }

        Set<String> selectedConnector = Connectors.getSelectedConnectors(kafkaConnectApi, names, regexpMode);
        for (String connectorToDescribe : selectedConnector) {
            int returnCode = describe(kafkaConnectApi, connectorToDescribe, currentVersion);
            if (returnCode > 0)
                return returnCode;
        }

        return 0;
    }

    private int describe(KafkaConnectApi kafkaConnectApi, String connectorToDescribe, Version currentVersion) {
        try {
            ConnectorInfo connector = kafkaConnectApi.getConnector(connectorToDescribe);
            ConnectorStatusInfo connectorStatus = kafkaConnectApi.getConnectorStatus(connectorToDescribe);
            Map<String, String> connectorConfig = kafkaConnectApi.getConnectorConfig(connectorToDescribe);

            List<Tuple> connectorInfo = Arrays.asList(
                    new Tuple("Name", connector.name),
                    new Tuple("Type", connectorStatus.type),
                    new Tuple("State", colorizeState(connectorStatus.connector.state)),
                    new Tuple("Worker ID", connectorStatus.connector.worker_id));

            Tuple.print(connectorInfo);

            // Config
            List<Tuple> config = new ArrayList<>();

            // Sort connector config
            Map<String, String> sortedConnectorConfig = new TreeMap<>(Comparator.comparing(x -> x));
            sortedConnectorConfig.putAll(connectorConfig);

            for (Entry<String, String> configEntry : sortedConnectorConfig.entrySet()) {
                config.add(new Tuple("  " + configEntry.getKey(), configEntry.getValue()));
            }

            Tuple.print(Arrays.asList(new Tuple(ANSI_WHITE_BOLD + "Config" + ANSI_RESET, "")));
            Tuple.print(config);

            Tuple.print(Arrays.asList(new Tuple(ANSI_WHITE_BOLD + "Tasks" + ANSI_RESET, "")));

            Map<String, Map<String, String>> tasksConfigs;
            if (includeTasksConfig) {
                tasksConfigs = kafkaConnectApi.getConnectorTasksConfig(connectorToDescribe);
            }
            else {
                tasksConfigs = Collections.emptyMap();
            }

            // Tasks
            for (TaskState task : connectorStatus.tasks) {
                Tuple.print(Arrays.asList(new Tuple("  " + task.id, "")));
                List<Tuple> tuples = new ArrayList<>();
                tuples.add(new Tuple("    State", colorizeState(task.state)));
                tuples.add(new Tuple("    Worker ID", task.worker_id));

                if (includeTasksConfig) {
                    tuples.add(new Tuple("    Config", ""));

                    for (Entry<String, String> taskConfig : tasksConfigs.get(connectorToDescribe + "-" + task.id).entrySet()) {
                        tuples.add(new Tuple("      " + taskConfig.getKey(), taskConfig.getValue()));
                    }
                }

                if (task.state.equals("FAILED")) {
                    tuples.add(new Tuple("    Trace", task.trace.replaceAll("Caused by", "      Caused by")));
                }
                Tuple.print(tuples);
            }

            if (currentVersion.greaterOrEquals(requiredVersionForTopicsApi)) {

                Map<String, TopicsInfo> connectorTopics = kafkaConnectApi.getConnectorTopics(connectorToDescribe);

                Tuple.print(Arrays.asList(new Tuple(ANSI_WHITE_BOLD + "Topics" + ANSI_RESET, "")));

                List<Tuple> topics = new ArrayList<>();

                for (String topic : connectorTopics.entrySet().iterator().next().getValue().topics) {
                    topics.add(new Tuple("", "  " + topic));
                }
                Tuple.print(topics);
            }
        }
        catch (Exception e) {
            if (!e.getMessage().contains("not found")) {
                throw e;
            }

            spec.commandLine().getOut().println("Connector " + connectorToDescribe + " not found. The following connector(s) are available:");

            GetConnectorsCommand getConnectors = new GetConnectorsCommand(context, spec);
            getConnectors.run();

            return 1;
        }

        return 0;
    }

    private String colorizeState(String state) {
        if (state.equals("RUNNING")) {
            return ANSI_GREEN + "RUNNING" + ANSI_RESET;
        }
        else if (state.equals("PAUSED")) {
            return ANSI_YELLOW + "PAUSED" + ANSI_RESET;
        }
        else if (state.equals("FAILED")) {
            return ANSI_RED + "FAILED" + ANSI_RESET;
        }
        else if (state.equals("UNASSIGNED")) {
            return ANSI_YELLOW + "UNASSIGNED" + ANSI_RESET;
        }
        else {
            return state;
        }
    }
}
