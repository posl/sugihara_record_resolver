/*
 * Copyright 2021 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.storage;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.whispersystems.textsecuregcm.entities.PreKey;
import org.whispersystems.textsecuregcm.util.AttributeValues;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import static org.whispersystems.textsecuregcm.metrics.MetricsUtil.name;

public class Keys extends AbstractDynamoDbStore {

  private final String tableName;

  static final String KEY_ACCOUNT_UUID = "U";
  static final String KEY_DEVICE_ID_KEY_ID = "DK";
  static final String KEY_PUBLIC_KEY = "P";

  private static final Timer STORE_KEYS_TIMER = Metrics.timer(name(Keys.class, "storeKeys"));
  private static final Timer TAKE_KEY_FOR_DEVICE_TIMER = Metrics.timer(name(Keys.class, "takeKeyForDevice"));
  private static final Timer GET_KEY_COUNT_TIMER = Metrics.timer(name(Keys.class, "getKeyCount"));
  private static final Timer DELETE_KEYS_FOR_DEVICE_TIMER = Metrics.timer(name(Keys.class, "deleteKeysForDevice"));
  private static final Timer DELETE_KEYS_FOR_ACCOUNT_TIMER = Metrics.timer(name(Keys.class, "deleteKeysForAccount"));
  private static final DistributionSummary CONTESTED_KEY_DISTRIBUTION = Metrics.summary(name(Keys.class, "contestedKeys"));
  private static final DistributionSummary KEY_COUNT_DISTRIBUTION = Metrics.summary(name(Keys.class, "keyCount"));
  private static final Counter KEYS_EMPTY_TAKE_COUNTER = Metrics.counter(name(Keys.class, "takeKeyEmpty"));

  public Keys(final DynamoDbClient dynamoDB, final String tableName) {
    super(dynamoDB);
    this.tableName = tableName;
  }

  public void store(final UUID identifier, final long deviceId, final List<PreKey> keys) {
    STORE_KEYS_TIMER.record(() -> {
      delete(identifier, deviceId);

      writeInBatches(keys, batch -> {
        List<WriteRequest> items = new ArrayList<>();
        for (final PreKey preKey : batch) {
          items.add(WriteRequest.builder()
              .putRequest(PutRequest.builder()
                  .item(getItemFromPreKey(identifier, deviceId, preKey))
                  .build())
              .build());
        }
        executeTableWriteItemsUntilComplete(Map.of(tableName, items));
      });
    });
  }

  public Optional<PreKey> take(final UUID identifier, final long deviceId) {
    return TAKE_KEY_FOR_DEVICE_TIMER.record(() -> {
      final AttributeValue partitionKey = getPartitionKey(identifier);
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression("#uuid = :uuid AND begins_with (#sort, :sortprefix)")
          .expressionAttributeNames(Map.of("#uuid", KEY_ACCOUNT_UUID, "#sort", KEY_DEVICE_ID_KEY_ID))
          .expressionAttributeValues(Map.of(
              ":uuid", partitionKey,
              ":sortprefix", getSortKeyPrefix(deviceId)))
          .projectionExpression(KEY_DEVICE_ID_KEY_ID)
          .consistentRead(false)
          .build();

      int contestedKeys = 0;

      try {
        QueryResponse response = db().query(queryRequest);
        for (Map<String, AttributeValue> candidate : response.items()) {
          DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
              .tableName(tableName)
              .key(Map.of(
                  KEY_ACCOUNT_UUID, partitionKey,
                  KEY_DEVICE_ID_KEY_ID, candidate.get(KEY_DEVICE_ID_KEY_ID)))
              .returnValues(ReturnValue.ALL_OLD)
              .build();
          DeleteItemResponse deleteItemResponse = db().deleteItem(deleteItemRequest);
          if (deleteItemResponse.hasAttributes()) {
            return Optional.of(getPreKeyFromItem(deleteItemResponse.attributes()));
          }

          contestedKeys++;
        }

        KEYS_EMPTY_TAKE_COUNTER.increment();
        return Optional.empty();
      } finally {
        CONTESTED_KEY_DISTRIBUTION.record(contestedKeys);
      }
    });
  }

  public int getCount(final UUID identifier, final long deviceId) {
    return GET_KEY_COUNT_TIMER.record(() -> {
      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression("#uuid = :uuid AND begins_with (#sort, :sortprefix)")
          .expressionAttributeNames(Map.of("#uuid", KEY_ACCOUNT_UUID, "#sort", KEY_DEVICE_ID_KEY_ID))
          .expressionAttributeValues(Map.of(
              ":uuid", getPartitionKey(identifier),
              ":sortprefix", getSortKeyPrefix(deviceId)))
          .select(Select.COUNT)
          .consistentRead(false)
          .build();

      int keyCount = 0;
      // This is very confusing, but does appear to be the intended behavior. See:
      //
      // - https://github.com/aws/aws-sdk-java/issues/693
      // - https://github.com/aws/aws-sdk-java/issues/915
      // - https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#Query.Count
      for (final QueryResponse page : db().queryPaginator(queryRequest)) {
        keyCount += page.count();
      }
      KEY_COUNT_DISTRIBUTION.record(keyCount);
      return keyCount;
    });
  }

  public void delete(final UUID accountUuid) {
    DELETE_KEYS_FOR_ACCOUNT_TIMER.record(() -> {
      final QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression("#uuid = :uuid")
          .expressionAttributeNames(Map.of("#uuid", KEY_ACCOUNT_UUID))
          .expressionAttributeValues(Map.of(
              ":uuid", getPartitionKey(accountUuid)))
          .projectionExpression(KEY_DEVICE_ID_KEY_ID)
          .consistentRead(true)
          .build();

      deleteItemsForAccountMatchingQuery(accountUuid, queryRequest);
    });
  }

  public void delete(final UUID accountUuid, final long deviceId) {
    DELETE_KEYS_FOR_DEVICE_TIMER.record(() -> {
      final QueryRequest queryRequest = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression("#uuid = :uuid AND begins_with (#sort, :sortprefix)")
          .expressionAttributeNames(Map.of("#uuid", KEY_ACCOUNT_UUID, "#sort", KEY_DEVICE_ID_KEY_ID))
          .expressionAttributeValues(Map.of(
              ":uuid", getPartitionKey(accountUuid),
              ":sortprefix", getSortKeyPrefix(deviceId)))
          .projectionExpression(KEY_DEVICE_ID_KEY_ID)
          .consistentRead(true)
          .build();

      deleteItemsForAccountMatchingQuery(accountUuid, queryRequest);
    });
  }

  private void deleteItemsForAccountMatchingQuery(final UUID accountUuid, final QueryRequest querySpec) {
    final AttributeValue partitionKey = getPartitionKey(accountUuid);

    writeInBatches(db().query(querySpec).items(), batch -> {
      List<WriteRequest> deletes = new ArrayList<>();
      for (final Map<String, AttributeValue> item : batch) {
        deletes.add(WriteRequest.builder()
            .deleteRequest(DeleteRequest.builder()
                .key(Map.of(
                    KEY_ACCOUNT_UUID, partitionKey,
                    KEY_DEVICE_ID_KEY_ID, item.get(KEY_DEVICE_ID_KEY_ID)))
                .build())
            .build());
      }
      executeTableWriteItemsUntilComplete(Map.of(tableName, deletes));
    });
  }

  private static AttributeValue getPartitionKey(final UUID accountUuid) {
    return AttributeValues.fromUUID(accountUuid);
  }

  private static AttributeValue getSortKey(final long deviceId, final long keyId) {
    final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
    byteBuffer.putLong(deviceId);
    byteBuffer.putLong(keyId);
    return AttributeValues.fromByteBuffer(byteBuffer.flip());
  }

  @VisibleForTesting
  static AttributeValue getSortKeyPrefix(final long deviceId) {
    final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]);
    byteBuffer.putLong(deviceId);
    return AttributeValues.fromByteBuffer(byteBuffer.flip());
  }

  private Map<String, AttributeValue> getItemFromPreKey(final UUID accountUuid, final long deviceId, final PreKey preKey) {
    return Map.of(
        KEY_ACCOUNT_UUID, getPartitionKey(accountUuid),
        KEY_DEVICE_ID_KEY_ID, getSortKey(deviceId, preKey.getKeyId()),
        KEY_PUBLIC_KEY, AttributeValues.fromString(preKey.getPublicKey()));
  }

  private PreKey getPreKeyFromItem(Map<String, AttributeValue> item) {
    final long keyId = item.get(KEY_DEVICE_ID_KEY_ID).b().asByteBuffer().getLong(8);
    return new PreKey(keyId, item.get(KEY_PUBLIC_KEY).s());
  }
}
