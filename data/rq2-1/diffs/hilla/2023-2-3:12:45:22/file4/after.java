package dev.hilla.parser.core;

import java.util.Collection;
import java.util.Collections;

import jakarta.annotation.Nonnull;

public interface Plugin {

    void enter(NodePath<?> nodePath);

    void exit(NodePath<?> nodePath);

    PluginConfiguration getConfiguration();

    void setConfiguration(PluginConfiguration configuration);

    int getOrder();

    void setOrder(int order);

    default Collection<Class<? extends Plugin>> getRequiredPlugins() {
        return Collections.emptyList();
    }

    @Nonnull
    default Node<?, ?> resolve(@Nonnull Node<?, ?> node,
            @Nonnull NodePath<?> parentPath) {
        return node;
    }

    @Nonnull
    NodeDependencies scan(@Nonnull NodeDependencies nodeDependencies);

    void setStorage(SharedStorage storage);
}
