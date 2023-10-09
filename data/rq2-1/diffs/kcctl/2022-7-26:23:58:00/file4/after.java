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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.kcctl.service.ConnectorPlugin;
import org.kcctl.service.KafkaConnectApi;
import org.kcctl.util.ConfigurationContext;
import org.kcctl.util.Version;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "plugins", description = "Displays information about available connector plug-ins")
public class GetPluginsCommand implements Callable<Integer> {

    private final Version requiredVersionForAllPlugins = new Version(3, 2);
    private final Set<String> defaultPlugins = Set.of("source", "sink");

    private final ConfigurationContext context;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = { "-t", "--types" }, description = "Types of plugins", split = ",")
    Set<String> pluginTypes;

    @Inject
    public GetPluginsCommand(ConfigurationContext context) {
        this.context = context;
    }

    // Hack : Picocli currently require an empty constructor to generate the completion file
    public GetPluginsCommand() {
        context = new ConfigurationContext();
    }

    @Override
    public Integer call() {
        KafkaConnectApi kafkaConnectApi = RestClientBuilder.newBuilder()
                .baseUri(context.getCurrentContext().getCluster())
                .build(KafkaConnectApi.class);

        Version currentVersion = new Version(kafkaConnectApi.getWorkerInfo().version());

        if (pluginTypes != null) {
            if (!currentVersion.greaterOrEquals(requiredVersionForAllPlugins)) {
                spec.commandLine().getErr().println("--types requires at least Kafka Connect 3.2. Current version: " + currentVersion);
                return 1;
            }
        }
        else {
            pluginTypes = defaultPlugins;
        }

        List<ConnectorPlugin> connectorPlugins = kafkaConnectApi.getConnectorPlugins(false);
        if (!pluginTypes.contains("all")) {
            connectorPlugins.removeIf(p -> !pluginTypes.contains(p.type()));
        }
        connectorPlugins.sort(Comparator.comparing(c -> c.type()));

        spec.commandLine().getOut().println();
        spec.commandLine().getOut().println(AsciiTable.getTable(AsciiTable.NO_BORDERS, connectorPlugins, Arrays.asList(
                new Column().header("TYPE").dataAlign(HorizontalAlign.LEFT).with(plugin -> plugin.type()),
                new Column().header(" CLASS").dataAlign(HorizontalAlign.LEFT).with(plugin -> " " + plugin.clazz()),
                new Column().header(" VERSION").dataAlign(HorizontalAlign.LEFT).with(plugin -> " " + (plugin.version() == null ? "n/a" : plugin.version())))));
        spec.commandLine().getOut().println();
        return 0;
    }
}
