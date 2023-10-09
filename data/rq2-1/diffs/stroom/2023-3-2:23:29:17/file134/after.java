package stroom.task.impl;

import stroom.cluster.task.api.NodeNotFoundException;
import stroom.cluster.task.api.NullClusterStateException;
import stroom.cluster.task.api.TargetNodeSetFactory;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValInteger;
import stroom.dashboard.expression.v1.ValLong;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValString;
import stroom.dashboard.expression.v1.Values;
import stroom.dashboard.expression.v1.ValuesConsumer;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.DataSource;
import stroom.datasource.api.v2.DateField;
import stroom.docref.DocRef;
import stroom.entity.shared.ExpressionCriteria;
import stroom.expression.matcher.ExpressionMatcher;
import stroom.expression.matcher.ExpressionMatcherFactory;
import stroom.searchable.api.Searchable;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.task.shared.TaskProgressResponse;
import stroom.task.shared.TaskResource;
import stroom.util.shared.ResultPage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Inject;

class SearchableTaskProgress implements Searchable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchableTaskProgress.class);
    private static final DocRef TASK_MANAGER_PSEUDO_DOC_REF = new DocRef("Searchable", "Task Manager", "Task Manager");

    private final Executor executor;
    private final TaskContextFactory taskContextFactory;
    private final TargetNodeSetFactory targetNodeSetFactory;
    private final TaskResource taskResource;
    private final SecurityContext securityContext;
    private final ExpressionMatcherFactory expressionMatcherFactory;

    @Inject
    SearchableTaskProgress(final Executor executor,
                           final TaskContextFactory taskContextFactory,
                           final TargetNodeSetFactory targetNodeSetFactory,
                           final TaskResource taskResource,
                           final SecurityContext securityContext,
                           final ExpressionMatcherFactory expressionMatcherFactory) {
        this.executor = executor;
        this.taskContextFactory = taskContextFactory;
        this.targetNodeSetFactory = targetNodeSetFactory;
        this.taskResource = taskResource;
        this.securityContext = securityContext;
        this.expressionMatcherFactory = expressionMatcherFactory;
    }

    @Override
    public DocRef getDocRef() {
        if (securityContext.hasAppPermission(PermissionNames.MANAGE_TASKS_PERMISSION)) {
            return TASK_MANAGER_PSEUDO_DOC_REF;
        }
        return null;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource
                .builder()
                .fields(TaskManagerFields.getFields())
                .build();
    }

    @Override
    public DateField getTimeField() {
        return TaskManagerFields.SUBMIT_TIME;
    }

    @Override
    public void search(final ExpressionCriteria criteria,
                       final AbstractField[] fields,
                       final ValuesConsumer consumer) {
        securityContext.secure(PermissionNames.MANAGE_TASKS_PERMISSION, () -> {
            final Map<String, TaskProgressResponse> nodeResponses = searchAllNodes();

            final ExpressionMatcher expressionMatcher = expressionMatcherFactory.create(
                    TaskManagerFields.getFieldMap());

            nodeResponses.values()
                    .stream()
                    .map(ResultPage::getValues)
                    .flatMap(List::stream)
                    .map(taskProgress -> {
                        final Map<String, Object> attributeMap = new HashMap<>();
                        attributeMap.put(TaskManagerFields.FIELD_NODE, taskProgress.getNodeName());
                        attributeMap.put(TaskManagerFields.FIELD_NAME, taskProgress.getTaskName());
                        attributeMap.put(TaskManagerFields.FIELD_USER, taskProgress.getUserName());
                        attributeMap.put(TaskManagerFields.FIELD_SUBMIT_TIME, taskProgress.getSubmitTimeMs());
                        attributeMap.put(TaskManagerFields.FIELD_AGE, taskProgress.getAgeMs());
                        attributeMap.put(TaskManagerFields.FIELD_INFO, taskProgress.getTaskInfo());
                        return attributeMap;
                    })
                    .filter(attributeMap -> expressionMatcher.match(attributeMap, criteria.getExpression()))
                    .forEach(attributeMap -> {
                        final Val[] arr = new Val[fields.length];
                        for (int i = 0; i < fields.length; i++) {
                            final AbstractField field = fields[i];
                            Val val = ValNull.INSTANCE;
                            if (field != null) {
                                final Object o = attributeMap.get(field.getName());
                                if (o != null) {
                                    if (o instanceof String) {
                                        val = ValString.create((String) o);
                                    } else if (o instanceof Long) {
                                        val = ValLong.create((long) o);
                                    } else if (o instanceof Integer) {
                                        val = ValInteger.create((int) o);
                                    }
                                }
                            }
                            arr[i] = val;
                        }
                        consumer.add(Values.of(arr));
                    });
        });
    }

    private Map<String, TaskProgressResponse> searchAllNodes() {
        final Function<TaskContext, Map<String, TaskProgressResponse>> function = taskContext -> {
            final Map<String, TaskProgressResponse> nodeResponses = new ConcurrentHashMap<>();

            try {
                // Get the nodes that we are going to send the entity event to.
                final Set<String> targetNodes = targetNodeSetFactory.getEnabledActiveTargetNodeSet();

                final CountDownLatch countDownLatch = new CountDownLatch(targetNodes.size());

                // Only send the event to remote nodes and not this one.
                // Send the entity event.
                targetNodes.forEach(nodeName -> {
                    final Supplier<TaskProgressResponse> supplier = taskContextFactory.childContextResult(taskContext,
                            "Getting progress from node '" + nodeName + "'",
                            tc ->
                                    taskResource.list(nodeName));
                    CompletableFuture
                            .supplyAsync(supplier, executor)
                            .whenComplete((r, t) -> {
                                if (r != null) {
                                    nodeResponses.putIfAbsent(nodeName, r);
                                }
                                countDownLatch.countDown();
                            });
                });

                // Wait for all requests to complete.
                countDownLatch.await();

            } catch (final NullClusterStateException | NodeNotFoundException | InterruptedException e) {
                LOGGER.warn(e.getMessage());
                LOGGER.debug(e.getMessage(), e);
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
            return nodeResponses;
        };
        return taskContextFactory.contextResult("Search Task Progress", function).get();
    }
}
