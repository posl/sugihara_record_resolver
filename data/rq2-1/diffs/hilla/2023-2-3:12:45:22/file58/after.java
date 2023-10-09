package dev.hilla.parser.plugins.nonnull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import dev.hilla.parser.core.AbstractPlugin;
import dev.hilla.parser.core.NodeDependencies;
import dev.hilla.parser.core.NodePath;
import dev.hilla.parser.core.Plugin;
import dev.hilla.parser.core.PluginConfiguration;
import dev.hilla.parser.models.AnnotatedModel;
import dev.hilla.parser.models.AnnotationInfoModel;
import dev.hilla.parser.models.ClassInfoModel;
import dev.hilla.parser.models.PackageInfoModel;
import dev.hilla.parser.models.jackson.JacksonPropertyModel;
import dev.hilla.parser.plugins.backbone.BackbonePlugin;
import dev.hilla.parser.plugins.backbone.nodes.MethodNode;
import dev.hilla.parser.plugins.backbone.nodes.MethodParameterNode;
import dev.hilla.parser.plugins.backbone.nodes.PropertyNode;
import dev.hilla.parser.plugins.backbone.nodes.TypeSignatureNode;

import io.swagger.v3.oas.models.media.Schema;

public final class NonnullPlugin extends AbstractPlugin<NonnullPluginConfig> {
    private Map<String, AnnotationMatcher> annotationsMap = mapByName(
            NonnullPluginConfig.Processor.defaults);

    public NonnullPlugin() {
        super();
        setOrder(100);
    }

    static private Map<String, AnnotationMatcher> mapByName(
            Collection<AnnotationMatcher> annotations) {
        return annotations.stream().collect(Collectors
                .toMap(AnnotationMatcher::getName, Function.identity()));
    }

    @Override
    public void enter(NodePath<?> nodePath) {
    }

    @Override
    public void exit(NodePath<?> nodePath) {
        if (!(nodePath.getNode().getTarget() instanceof Schema)) {
            return;
        }

        var schema = (Schema<?>) nodePath.getNode().getTarget();

        // Apply annotations from package (NonNullApi)
        var annotations = getPackageAnnotationsStream(nodePath);

        // Apply from current node, if source is annotated
        if (nodePath.getNode().getSource() instanceof AnnotatedModel) {
            annotations = Stream.concat(annotations,
                    ((AnnotatedModel) nodePath.getNode().getSource())
                            .getAnnotationsStream());
        }

        annotations = considerAscendantAnnotations(annotations, nodePath);

        annotations.map(annotation -> annotationsMap.get(annotation.getName()))
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(AnnotationMatcher::getScore))
                .map(AnnotationMatcher::doesMakeNullable).ifPresent(
                        nullable -> schema.setNullable(nullable ? true : null));
    }

    @Override
    public Collection<Class<? extends Plugin>> getRequiredPlugins() {
        return List.of(BackbonePlugin.class);
    }

    @Nonnull
    @Override
    public NodeDependencies scan(@Nonnull NodeDependencies nodeDependencies) {
        return nodeDependencies;
    }

    @Override
    public void setConfiguration(@Nonnull PluginConfiguration configuration) {
        super.setConfiguration(configuration);
        this.annotationsMap = mapByName(
                new NonnullPluginConfig.Processor(getConfiguration())
                        .process());
    }

    private Optional<PackageInfoModel> findClosestPackage(
            NodePath<?> nodePath) {
        return nodePath.stream().map(NodePath::getNode)
                .filter(node -> node.getSource() instanceof ClassInfoModel)
                .map(node -> (ClassInfoModel) node.getSource()).findFirst()
                .map(ClassInfoModel::getPackage);
    }

    private Stream<AnnotationInfoModel> getPackageAnnotationsStream(
            NodePath<?> nodePath) {
        return findClosestPackage(nodePath).stream()
                .flatMap(PackageInfoModel::getAnnotationsStream);
    }

    /**
     * Adds ascendant annotations for check in case the type is annotated on
     * method/parameter/property level.
     *
     * @param annotations
     *            initial type annotations
     * @param nodePath
     *            the node path
     * @return stream of all annotations to check
     */
    private Stream<AnnotationInfoModel> considerAscendantAnnotations(
            Stream<AnnotationInfoModel> annotations, NodePath<?> nodePath) {
        var current = nodePath.getNode();
        var parent = nodePath.getParentPath().getNode();

        if (current instanceof TypeSignatureNode) {
            if (parent instanceof PropertyNode) {
                annotations = Stream.concat(annotations,
                        ((JacksonPropertyModel) parent.getSource()).getType()
                                .getAnnotationsStream());
            }

            if (parent instanceof MethodNode
                    || parent instanceof MethodParameterNode
                    || parent instanceof PropertyNode) {
                annotations = Stream.concat(annotations,
                        ((AnnotatedModel) parent.getSource())
                                .getAnnotationsStream());
            }
        }

        return annotations;
    }
}
