/*-
 * ========================LICENSE_START=================================
 * restheart-core
 * %%
 * Copyright (C) 2014 - 2022 SoftInstigate
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * =========================LICENSE_END==================================
 */
package org.restheart.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public class ProvidersChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvidersChecker.class);

    private static List<PluginDescriptor> enabledProviders(List<PluginDescriptor> providers) {
        return providers.stream()
            .filter(p -> p != null)
            .peek(p ->  { if (!p.enabled()) LOGGER.info("Provider {} disabled", p.name()); })
            .filter(p -> p.enabled())
            .collect(Collectors.toList());
    }

    private static void removeIfWrongDependency(MutableGraph<PluginDescriptor> providersGraph) {
        var toRemove = new ArrayList<PluginDescriptor>();
        providersGraph.nodes().forEach(thisProvider -> {
            thisProvider.injections().stream()
                .filter(i -> i instanceof FieldInjectionDescriptor a)
                .map(i -> (FieldInjectionDescriptor) i)
                .forEach(i -> {
                    var otherProviderName = (String )i.annotationParams().get(0).getValue();
                    var otherProvider = providerDescriptorFromName(otherProviderName);

                    if (otherProvider == null) {
                        LOGGER.error("Provider {} disabled: no provider found for @Inject(\"{}\")", thisProvider.name(), otherProviderName);
                        toRemove.add(thisProvider);
                    } else if (!otherProvider.enabled()) {
                        LOGGER.error("Provider {} disabled: the provider for @Inject(\"{}\") is disabled", thisProvider.name(), otherProvider.name());
                        toRemove.add(thisProvider);
                    } else {
                        // check provided class vs annotated class
                        var providedType = PluginsFactory.providersTypes().get(otherProviderName);
                        var fieldType = i.clazz();

                        if (!fieldType.isAssignableFrom(providedType)) {
                            LOGGER.error("Plugin {} disabled: the type of the provider for @Inject(\"{}\") is {} but the type of the annotated field {} is {}", thisProvider.name(), otherProviderName, providedType, i.field(), fieldType);
                            toRemove.add(thisProvider);
                        }
                }
            });
        });

        toRemove.stream().forEach(providersGraph::removeNode);
    }

    private static void removeIfCircularDependency(MutableGraph<PluginDescriptor> providersGraph) {
        var toRemove = new ArrayList<PluginDescriptor>();
        providersGraph.nodes().stream().forEach(provider -> {
            var reachableNodes = Graphs.reachableNodes(providersGraph, provider);
            var subGraph = Graphs.inducedSubgraph(providersGraph, reachableNodes);

            if (Graphs.hasCycle(subGraph)) {
                LOGGER.error("Provider {} disabled due to circular dependency", provider.name());
                toRemove.add(provider);
            }
        });
        toRemove.stream().forEach(providersGraph::removeNode);
    }

    /**
     * WIP
     * checks if there are cycles in the providers graph (mutual dependencies)
     */
    static Set<PluginDescriptor> validProviders(List<PluginDescriptor> providers) {
        MutableGraph<PluginDescriptor> providersGraph = GraphBuilder.directed().allowsSelfLoops(true).build();

        // add nodes
        enabledProviders(providers).stream().forEach(providersGraph::addNode);

        // add edges
        for (var thisProvider: providers) {
            thisProvider.injections().stream()
                .filter(i -> i instanceof FieldInjectionDescriptor a)
                .map(i -> (FieldInjectionDescriptor) i)
                .forEach(i -> {
                    var otherProviderName = (String) i.annotationParams().get(0).getValue();
                    var otherProvider = providerDescriptorFromName(otherProviderName);

                    if (otherProvider != null) {
                        providersGraph.putEdge(thisProvider, otherProvider);
                    }
                });
        }

        // remove nodes that have disabled dependencies
        // keep removing until it finds wrong providers
        var count = providersGraph.edges().size();
        removeIfWrongDependency(providersGraph);
        // remove nodes with circular dependencies
        removeIfCircularDependency(providersGraph);
        int newCount = providersGraph.edges().size();
        while(newCount < count) {
            count = providersGraph.edges().size();
            removeIfWrongDependency(providersGraph);
            // remove nodes that have circular dependencies
            removeIfCircularDependency(providersGraph);
            newCount = providersGraph.edges().size();
        };

        return providersGraph.nodes();
    }

    static PluginDescriptor providerDescriptorFromClass(String className) {
        return PluginsScanner.providers().stream().filter(p -> p.clazz().equals(className)).findFirst().orElse(null);
    }

    static  PluginDescriptor providerDescriptorFromName(String name) {
        return PluginsScanner.providers().stream().filter(p -> p.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * checks that a Provider exists and is enabled for each plugin @Inject field
     *
     * @param plugin
     * @return true if all the plugin dependencies can be resolved
     */
    static boolean checkDependencies(Set<PluginDescriptor> validProviders, PluginDescriptor plugin) {
        var ret = true;

        // check Field Injections that require Providers
        var injections = new ArrayList<FieldInjectionDescriptor>();

        plugin.injections().stream()
            .filter(i -> i instanceof FieldInjectionDescriptor fid)
            .map(i -> (FieldInjectionDescriptor) i)
            .forEach(injections::add);

        for (var injection : injections) {
            var providerName = injection.annotationParams().get(0).getValue();

            var _provider = validProviders.stream().filter(p -> p.name().equals(providerName)).findFirst();

            if (_provider.isEmpty()) {
                LOGGER.error("Plugin {} disabled: no provider found for @Inject(\"{}\")", plugin.name(), providerName);
                ret = false;
            } else if(_provider.get().clazz().equals(plugin.clazz())) {
                LOGGER.error("Provider {} disabled: it depends on itself via @Inject(\"{}\")", plugin.name(), providerName);
            } else {
                var provider = _provider.get();

                if (!provider.enabled()) {
                    LOGGER.error("Plugin {} disabled: the provider for @Inject(\"{}\") is disabled", plugin.name(), providerName);
                    return false;
                }
            }
        }

        return ret;
    }
}
