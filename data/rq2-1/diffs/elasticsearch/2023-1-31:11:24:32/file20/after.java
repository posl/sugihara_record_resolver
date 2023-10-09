/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.action.admin.indices.rollover;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.core.RestApiVersion;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request class to swap index under an alias or increment data stream generation upon satisfying conditions
 * <p>
 * Note: there is a new class with the same name for the Java HLRC that uses a typeless format.
 * Any changes done to this class should also go to that client class.
 */
public class RolloverRequest extends AcknowledgedRequest<RolloverRequest> implements IndicesRequest {

    private static final ObjectParser<RolloverRequest, Boolean> PARSER = new ObjectParser<>("rollover");
    private static final ObjectParser<Map<String, Condition<?>>, Void> CONDITION_PARSER = new ObjectParser<>("conditions");

    private static final ParseField CONDITIONS = new ParseField("conditions");
    private static final ParseField MAX_AGE_CONDITION = new ParseField(MaxAgeCondition.NAME);
    private static final ParseField MAX_DOCS_CONDITION = new ParseField(MaxDocsCondition.NAME);
    private static final ParseField MAX_SIZE_CONDITION = new ParseField(MaxSizeCondition.NAME);
    private static final ParseField MAX_PRIMARY_SHARD_SIZE_CONDITION = new ParseField(MaxPrimaryShardSizeCondition.NAME);
    private static final ParseField MAX_PRIMARY_SHARD_DOCS_CONDITION = new ParseField(MaxPrimaryShardDocsCondition.NAME);
    private static final ParseField MIN_AGE_CONDITION = new ParseField(MinAgeCondition.NAME);
    private static final ParseField MIN_DOCS_CONDITION = new ParseField(MinDocsCondition.NAME);
    private static final ParseField MIN_SIZE_CONDITION = new ParseField(MinSizeCondition.NAME);
    private static final ParseField MIN_PRIMARY_SHARD_SIZE_CONDITION = new ParseField(MinPrimaryShardSizeCondition.NAME);
    private static final ParseField MIN_PRIMARY_SHARD_DOCS_CONDITION = new ParseField(MinPrimaryShardDocsCondition.NAME);

    static {
        CONDITION_PARSER.declareString(
            (conditions, s) -> conditions.put(MaxAgeCondition.NAME, new MaxAgeCondition(TimeValue.parseTimeValue(s, MaxAgeCondition.NAME))),
            MAX_AGE_CONDITION
        );
        CONDITION_PARSER.declareLong(
            (conditions, value) -> conditions.put(MaxDocsCondition.NAME, new MaxDocsCondition(value)),
            MAX_DOCS_CONDITION
        );
        CONDITION_PARSER.declareString(
            (conditions, s) -> conditions.put(
                MaxSizeCondition.NAME,
                new MaxSizeCondition(ByteSizeValue.parseBytesSizeValue(s, MaxSizeCondition.NAME))
            ),
            MAX_SIZE_CONDITION
        );
        CONDITION_PARSER.declareString(
            (conditions, s) -> conditions.put(
                MaxPrimaryShardSizeCondition.NAME,
                new MaxPrimaryShardSizeCondition(ByteSizeValue.parseBytesSizeValue(s, MaxPrimaryShardSizeCondition.NAME))
            ),
            MAX_PRIMARY_SHARD_SIZE_CONDITION
        );
        CONDITION_PARSER.declareLong(
            (conditions, value) -> conditions.put(MaxPrimaryShardDocsCondition.NAME, new MaxPrimaryShardDocsCondition(value)),
            MAX_PRIMARY_SHARD_DOCS_CONDITION
        );
        CONDITION_PARSER.declareString(
            (conditions, s) -> conditions.put(MinAgeCondition.NAME, new MinAgeCondition(TimeValue.parseTimeValue(s, MinAgeCondition.NAME))),
            MIN_AGE_CONDITION
        );
        CONDITION_PARSER.declareLong(
            (conditions, value) -> conditions.put(MinDocsCondition.NAME, new MinDocsCondition(value)),
            MIN_DOCS_CONDITION
        );
        CONDITION_PARSER.declareString(
            (conditions, s) -> conditions.put(
                MinSizeCondition.NAME,
                new MinSizeCondition(ByteSizeValue.parseBytesSizeValue(s, MinSizeCondition.NAME))
            ),
            MIN_SIZE_CONDITION
        );
        CONDITION_PARSER.declareString(
            (conditions, s) -> conditions.put(
                MinPrimaryShardSizeCondition.NAME,
                new MinPrimaryShardSizeCondition(ByteSizeValue.parseBytesSizeValue(s, MinPrimaryShardSizeCondition.NAME))
            ),
            MIN_PRIMARY_SHARD_SIZE_CONDITION
        );
        CONDITION_PARSER.declareLong(
            (conditions, value) -> conditions.put(MinPrimaryShardDocsCondition.NAME, new MinPrimaryShardDocsCondition(value)),
            MIN_PRIMARY_SHARD_DOCS_CONDITION
        );

        PARSER.declareField(
            (parser, request, context) -> CONDITION_PARSER.parse(parser, request.conditions, null),
            CONDITIONS,
            ObjectParser.ValueType.OBJECT
        );
        PARSER.declareField(
            (parser, request, context) -> request.createIndexRequest.settings(parser.map()),
            CreateIndexRequest.SETTINGS,
            ObjectParser.ValueType.OBJECT
        );
        PARSER.declareField((parser, request, includeTypeName) -> {
            if (includeTypeName) {
                // expecting one type only
                for (Map.Entry<String, Object> mappingsEntry : parser.map().entrySet()) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> value = (Map<String, Object>) mappingsEntry.getValue();
                    request.createIndexRequest.mapping(value);
                }
            } else {
                // a type is not included, add a dummy _doc type
                Map<String, Object> mappings = parser.map();
                if (MapperService.isMappingSourceTyped(MapperService.SINGLE_MAPPING_NAME, mappings)) {
                    throw new IllegalArgumentException(
                        "The mapping definition cannot be nested under a type "
                            + "["
                            + MapperService.SINGLE_MAPPING_NAME
                            + "] unless include_type_name is set to true."
                    );
                }
                request.createIndexRequest.mapping(mappings);
            }
        }, CreateIndexRequest.MAPPINGS.forRestApiVersion(RestApiVersion.equalTo(RestApiVersion.V_7)), ObjectParser.ValueType.OBJECT);
        PARSER.declareField((parser, request, context) -> {
            // a type is not included, add a dummy _doc type
            Map<String, Object> mappings = parser.map();
            if (MapperService.isMappingSourceTyped(MapperService.SINGLE_MAPPING_NAME, mappings)) {

                throw new IllegalArgumentException("The mapping definition cannot be nested under a type");
            }
            request.createIndexRequest.mapping(mappings);
        }, CreateIndexRequest.MAPPINGS.forRestApiVersion(RestApiVersion.onOrAfter(RestApiVersion.V_8)), ObjectParser.ValueType.OBJECT);

        PARSER.declareField(
            (parser, request, context) -> request.createIndexRequest.aliases(parser.map()),
            CreateIndexRequest.ALIASES,
            ObjectParser.ValueType.OBJECT
        );
    }

    private String rolloverTarget;
    private String newIndexName;
    private boolean dryRun;
    private final Map<String, Condition<?>> conditions = Maps.newMapWithExpectedSize(2);
    // the index name "_na_" is never read back, what matters are settings, mappings and aliases
    private CreateIndexRequest createIndexRequest = new CreateIndexRequest("_na_");

    public RolloverRequest(StreamInput in) throws IOException {
        super(in);
        rolloverTarget = in.readString();
        newIndexName = in.readOptionalString();
        dryRun = in.readBoolean();
        int size = in.readVInt();
        for (int i = 0; i < size; i++) {
            Condition<?> condition = in.readNamedWriteable(Condition.class);
            this.conditions.put(condition.name, condition);
        }
        createIndexRequest = new CreateIndexRequest(in);
    }

    RolloverRequest() {}

    public RolloverRequest(String rolloverTarget, String newIndexName) {
        this.rolloverTarget = rolloverTarget;
        this.newIndexName = newIndexName;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = createIndexRequest.validate();
        if (rolloverTarget == null) {
            validationException = addValidationError("rollover target is missing", validationException);
        }

        // if the request has any conditions, then at least one condition must be a max_* condition
        boolean noMaxConditions = conditions.values().stream().noneMatch(c -> Condition.Type.MAX == c.type());
        if (conditions.size() > 0 && noMaxConditions) {
            validationException = addValidationError(
                "at least one max_* rollover condition must be set when using min_* conditions",
                validationException
            );
        }

        return validationException;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(rolloverTarget);
        out.writeOptionalString(newIndexName);
        out.writeBoolean(dryRun);
        out.writeCollection(
            conditions.values().stream().filter(c -> c.includedInVersion(out.getTransportVersion())).toList(),
            StreamOutput::writeNamedWriteable
        );
        createIndexRequest.writeTo(out);
    }

    @Override
    public String[] indices() {
        return new String[] { rolloverTarget };
    }

    @Override
    public IndicesOptions indicesOptions() {
        return IndicesOptions.strictSingleIndexNoExpandForbidClosed();
    }

    @Override
    public boolean includeDataStreams() {
        return true;
    }

    /**
     * Sets the rollover target to rollover to another index
     */
    public void setRolloverTarget(String rolloverTarget) {
        this.rolloverTarget = rolloverTarget;
    }

    /**
     * Sets the alias to rollover to another index
     */
    public void setNewIndexName(String newIndexName) {
        this.newIndexName = newIndexName;
    }

    /**
     * Sets if the rollover should not be executed when conditions are met
     */
    public void dryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Sets the wait for active shards configuration for the rolled index that gets created.
     */
    public void setWaitForActiveShards(ActiveShardCount waitForActiveShards) {
        createIndexRequest.waitForActiveShards(waitForActiveShards);
    }

    /**
     * Adds condition to check if the index is at least <code>age</code> old
     */
    public void addMaxIndexAgeCondition(TimeValue age) {
        MaxAgeCondition maxAgeCondition = new MaxAgeCondition(age);
        if (this.conditions.containsKey(maxAgeCondition.name)) {
            throw new IllegalArgumentException(maxAgeCondition.name + " condition is already set");
        }
        this.conditions.put(maxAgeCondition.name, maxAgeCondition);
    }

    /**
     * Adds condition to check if the index has at least <code>numDocs</code>
     */
    public void addMaxIndexDocsCondition(long numDocs) {
        MaxDocsCondition maxDocsCondition = new MaxDocsCondition(numDocs);
        if (this.conditions.containsKey(maxDocsCondition.name)) {
            throw new IllegalArgumentException(maxDocsCondition.name + " condition is already set");
        }
        this.conditions.put(maxDocsCondition.name, maxDocsCondition);
    }

    /**
     * Adds a size-based condition to check if the index size is at least <code>size</code>.
     */
    public void addMaxIndexSizeCondition(ByteSizeValue size) {
        MaxSizeCondition maxSizeCondition = new MaxSizeCondition(size);
        if (this.conditions.containsKey(maxSizeCondition.name)) {
            throw new IllegalArgumentException(maxSizeCondition + " condition is already set");
        }
        this.conditions.put(maxSizeCondition.name, maxSizeCondition);
    }

    /**
     * Adds a size-based condition to check if the size of the largest primary shard is at least <code>size</code>.
     */
    public void addMaxPrimaryShardSizeCondition(ByteSizeValue size) {
        MaxPrimaryShardSizeCondition maxPrimaryShardSizeCondition = new MaxPrimaryShardSizeCondition(size);
        if (this.conditions.containsKey(maxPrimaryShardSizeCondition.name)) {
            throw new IllegalArgumentException(maxPrimaryShardSizeCondition + " condition is already set");
        }
        this.conditions.put(maxPrimaryShardSizeCondition.name, maxPrimaryShardSizeCondition);
    }

    /**
     * Adds a size-based condition to check if the docs of the largest primary shard has at least <code>numDocs</code>
     */
    public void addMaxPrimaryShardDocsCondition(long numDocs) {
        MaxPrimaryShardDocsCondition maxPrimaryShardDocsCondition = new MaxPrimaryShardDocsCondition(numDocs);
        if (this.conditions.containsKey(maxPrimaryShardDocsCondition.name)) {
            throw new IllegalArgumentException(maxPrimaryShardDocsCondition.name + " condition is already set");
        }
        this.conditions.put(maxPrimaryShardDocsCondition.name, maxPrimaryShardDocsCondition);
    }

    /**
     * Adds required condition to check if the index is at least <code>age</code> old
     */
    public void addMinIndexAgeCondition(TimeValue age) {
        MinAgeCondition minAgeCondition = new MinAgeCondition(age);
        if (this.conditions.containsKey(minAgeCondition.name)) {
            throw new IllegalArgumentException(minAgeCondition.name + " condition is already set");
        }
        this.conditions.put(minAgeCondition.name, minAgeCondition);
    }

    /**
     * Adds required condition to check if the index has at least <code>numDocs</code>
     */
    public void addMinIndexDocsCondition(long numDocs) {
        MinDocsCondition minDocsCondition = new MinDocsCondition(numDocs);
        if (this.conditions.containsKey(minDocsCondition.name)) {
            throw new IllegalArgumentException(minDocsCondition.name + " condition is already set");
        }
        this.conditions.put(minDocsCondition.name, minDocsCondition);
    }

    /**
     * Adds a size-based required condition to check if the index size is at least <code>size</code>.
     */
    public void addMinIndexSizeCondition(ByteSizeValue size) {
        MinSizeCondition minSizeCondition = new MinSizeCondition(size);
        if (this.conditions.containsKey(minSizeCondition.name)) {
            throw new IllegalArgumentException(minSizeCondition + " condition is already set");
        }
        this.conditions.put(minSizeCondition.name, minSizeCondition);
    }

    /**
     * Adds a size-based required condition to check if the size of the largest primary shard is at least <code>size</code>.
     */
    public void addMinPrimaryShardSizeCondition(ByteSizeValue size) {
        MinPrimaryShardSizeCondition minPrimaryShardSizeCondition = new MinPrimaryShardSizeCondition(size);
        if (this.conditions.containsKey(minPrimaryShardSizeCondition.name)) {
            throw new IllegalArgumentException(minPrimaryShardSizeCondition + " condition is already set");
        }
        this.conditions.put(minPrimaryShardSizeCondition.name, minPrimaryShardSizeCondition);
    }

    /**
     * Adds a size-based required condition to check if the docs of the largest primary shard has at least <code>numDocs</code>
     */
    public void addMinPrimaryShardDocsCondition(long numDocs) {
        MinPrimaryShardDocsCondition minPrimaryShardDocsCondition = new MinPrimaryShardDocsCondition(numDocs);
        if (this.conditions.containsKey(minPrimaryShardDocsCondition.name)) {
            throw new IllegalArgumentException(minPrimaryShardDocsCondition.name + " condition is already set");
        }
        this.conditions.put(minPrimaryShardDocsCondition.name, minPrimaryShardDocsCondition);
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public Map<String, Condition<?>> getConditions() {
        return Collections.unmodifiableMap(conditions);
    }

    public String getRolloverTarget() {
        return rolloverTarget;
    }

    public String getNewIndexName() {
        return newIndexName;
    }

    /**
     * Given the results of evaluating each individual condition, determine whether the rollover request should proceed -- that is,
     * whether the conditions are met.
     *
     * If there are no conditions at all, then the request is unconditional (i.e. a command), and the conditions are met.
     *
     * If the request has conditions, then all min_* conditions and at least one max_* condition must have a true result.
     *
     * @param conditionResults a map of individual conditions and their associated evaluation results
     *
     * @return where the conditions for rollover are satisfied or not
     */
    public boolean areConditionsMet(Map<String, Boolean> conditionResults) {
        boolean allMinConditionsMet = conditions.values()
            .stream()
            .filter(c -> Condition.Type.MIN == c.type())
            .allMatch(c -> conditionResults.getOrDefault(c.toString(), false));

        boolean anyMaxConditionsMet = conditions.values()
            .stream()
            .filter(c -> Condition.Type.MAX == c.type())
            .anyMatch(c -> conditionResults.getOrDefault(c.toString(), false));

        return conditionResults.size() == 0 || (allMinConditionsMet && anyMaxConditionsMet);
    }

    /**
     * Returns the inner {@link CreateIndexRequest}. Allows to configure mappings, settings and aliases for the new index.
     */
    public CreateIndexRequest getCreateIndexRequest() {
        return createIndexRequest;
    }

    // param isTypeIncluded decides how mappings should be parsed from XContent
    public void fromXContent(boolean isTypeIncluded, XContentParser parser) throws IOException {
        PARSER.parse(parser, this, isTypeIncluded);
    }

    @Override
    public Task createTask(long id, String type, String action, TaskId parentTaskId, Map<String, String> headers) {
        return new CancellableTask(id, type, action, "", parentTaskId, headers);
    }
}