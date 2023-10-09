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

package stroom.dashboard.impl;

import stroom.dashboard.impl.download.TypeConverter;
import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.DashboardSearchResponse;
import stroom.query.api.v2.FlatResult;
import stroom.query.api.v2.Format.Type;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.SearchResponse;
import stroom.query.api.v2.VisResult;
import stroom.query.api.v2.VisResult.Store;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.string.ExceptionStringUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchResponseMapper {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SearchResponseMapper.class);

    public DashboardSearchResponse mapResponse(final DashboardQueryKey queryKey,
                                               final stroom.query.api.v2.SearchResponse searchResponse) {
        if (searchResponse == null) {
            return null;
        }

        Set<String> highlights = null;
        if (searchResponse.getHighlights() != null) {
            highlights = new HashSet<>(searchResponse.getHighlights());
        }

        List<Result> results = null;
        if (searchResponse.getResults() != null) {
            results = new ArrayList<>();
            for (final Result result : searchResponse.getResults()) {
                results.add(mapResult(result));
            }
        }

        List<String> errors = searchResponse.getErrors();
        if (errors != null) {
            // Remove timeout message as this is expected to occur for non-incremental dashboard searches.
            errors = errors
                    .stream()
                    .filter(error -> !error.startsWith(SearchResponse.TIMEOUT_MESSAGE))
                    .collect(Collectors.toList());
        }

        return new DashboardSearchResponse(queryKey,
                highlights,
                errors,
                searchResponse.complete(),
                results);
    }

    private Result mapResult(final Result result) {
        if (result == null) {
            return null;
        }

        if (result instanceof stroom.query.api.v2.TableResult) {
            return result;

//            final stroom.query.api.v2.TableResult tableResult = (stroom.query.api.v2.TableResult) result;
//
//            final List<Field> fields = tableResult.getFields();
//            final List<Row> rows = tableResult.getRows();
//            stroom.query.api.v2.OffsetRange resultRange = tableResult.getResultRange();
//
//            return new TableResult(
//            tableResult.getComponentId(), fields, rows,
//            resultRange, tableResult.getTotalResults(), tableResult.getError());
        } else if (result instanceof FlatResult) {
            final FlatResult visResult = (FlatResult) result;
            return mapVisResult(visResult);
        }

        return null;
    }
//
//    private List<Field> mapFields(final List<stroom.query.api.v2.Field> fields) {
//        final List<Field> copy = new ArrayList<>();
//        if (fields != null) {
//            for (final stroom.query.api.v2.Field field : fields) {
//                final Field item = Field.builder()
//                        .id(field.getId())
//                        .name(field.getName())
//                        .expression(field.getExpression())
//                        .group(field.getGroup())
//                        .width(-1)
//                        .visible(true)
//                        .special(false)
//                        .build();
//                copy.add(item);
//            }
//        }
//        return copy;
//    }
//
//    private List<Row> mapRows(final List<stroom.query.api.v2.Row> rows) {
//        final List<Row> copy = new ArrayList<>();
//        if (rows != null) {
//            for (final stroom.query.api.v2.Row row : rows) {
//                final Row item = new Row(row.getGroupKey(), row.getValues(), row.getDepth());
//                copy.add(item);
//            }
//        }
//        return copy;
//    }

    private VisResult mapVisResult(final FlatResult result) {
        String json = null;
        List<String> errors = result.getErrors();
        if (errors == null || errors.size() == 0) {
            try {
                final List<stroom.query.api.v2.Field> fields = result.getStructure();
                if (fields != null && result.getValues() != null) {
                    int valueOffset = 0;

                    final Map<Integer, List<String>> typeMap = new HashMap<>();
                    final Map<Integer, List<String>> sortDirectionMap = new HashMap<>();
                    int maxDepth = 0;
                    for (final stroom.query.api.v2.Field field : fields) {
                        // Ignore key and depth fields.
                        if (field.getName() != null && field.getName().startsWith(":")) {
                            valueOffset++;

                        } else {
                            String type = Type.GENERAL.name();
                            if (field.getFormat() != null && field.getFormat().getType() != null) {
                                type = field.getFormat().getType().name();
                            }
                            typeMap.computeIfAbsent(field.getGroup(), k -> new ArrayList<>()).add(type);

                            // The vizes need to know what the sort direction is for the various fields/keys
                            String sortDirection = null;
                            if (field.getSort() != null && field.getSort().getDirection() != null) {
                                sortDirection = field.getSort().getDirection().getDisplayValue();
                            }
                            sortDirectionMap.computeIfAbsent(field.getGroup(),
                                    k -> new ArrayList<>()).add(sortDirection);

                            if (field.getGroup() != null) {
                                maxDepth = Math.max(maxDepth, field.getGroup() + 1);
                                valueOffset++;
                            }
                        }
                    }

                    // Create an array of types.
                    String[][] types = new String[maxDepth + 1][];
                    for (final Entry<Integer, List<String>> entry : typeMap.entrySet()) {
                        int group = maxDepth;
                        if (entry.getKey() != null) {
                            group = entry.getKey();
                        }

                        String[] row = new String[entry.getValue().size()];
                        row = entry.getValue().toArray(row);
                        types[group] = row;
                    }

                    // Create an array of sortDirections
                    String[][] sortDirections = new String[maxDepth + 1][];
                    for (final Entry<Integer, List<String>> entry : sortDirectionMap.entrySet()) {
                        int group = maxDepth;
                        if (entry.getKey() != null) {
                            group = entry.getKey();
                        }

                        String[] row = new String[entry.getValue().size()];
                        row = entry.getValue().toArray(row);
                        sortDirections[group] = row;
                    }

                    final int valueCount = fields.size() - valueOffset;

                    final Map<Object, List<List<Object>>> map = new HashMap<>();
                    for (final List<Object> row : result.getValues()) {
                        map.computeIfAbsent(row.get(0), k -> new ArrayList<>()).add(row);
                    }

                    final Store store = getStore(null, map, types, sortDirections, valueCount, maxDepth, 0);
                    json = getMapper(false).writeValueAsString(store);
                }
            } catch (final JsonProcessingException | RuntimeException e) {
                LOGGER.debug(e::getMessage, e);
                errors = Collections.singletonList(ExceptionStringUtil.getMessage(e));
            }
        }

        return new VisResult(result.getComponentId(), json, result.getSize(), errors);
    }

    private ObjectMapper getMapper(final boolean indent) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Enabling default typing adds type information where it would otherwise be ambiguous,
        // i.e. for abstract classes
//        mapper.enableDefaultTyping();
        return mapper;
    }

    private Store getStore(final Object key,
                           final Map<Object, List<List<Object>>> map,
                           final String[][] types,
                           final String[][] sortDirections,
                           final int valueCount,
                           final int maxDepth,
                           final int depth) {
        Store store = null;

        final List<List<Object>> rows = map.get(key);
        if (rows != null) {
            final List<Object> values = new ArrayList<>();
            final Double[] min = new Double[valueCount];
            final Double[] max = new Double[valueCount];
            final Double[] sum = new Double[valueCount];

            for (final List<Object> row : rows) {
                if (depth < maxDepth) {
                    final Store childStore = getStore(
                            row.get(1), map, types, sortDirections, valueCount, maxDepth, depth + 1);
                    if (childStore != null) {
                        values.add(childStore);

                        for (int i = 0; i < valueCount; i++) {
                            min[i] = min(min[i], childStore.min[i]);
                            max[i] = max(max[i], childStore.max[i]);
                            sum[i] = sum(sum[i], childStore.sum[i]);
                        }
                    }
                } else {
                    final List<Object> vals = row.subList(row.size() - valueCount, row.size());
                    values.add(vals);
                    for (int i = 0; i < valueCount; i++) {
                        final Double dbl = TypeConverter.getDouble(vals.get(i));
                        if (dbl != null) {
                            min[i] = min(min[i], dbl);
                            max[i] = max(max[i], dbl);
                            sum[i] = sum(sum[i], dbl);
                        }
                    }
                }
            }

            store = new Store();
            if (key instanceof List) {
                List list = (List) key;
                store.key = list.get(list.size() - 1);
            }
            // The type/sortDirection for all the keys in the level below (i.e in the values[])
            store.keyType = types[depth][0];
            store.keySortDirection = sortDirections[depth][0];

            // The child values (that may be branches or leaves)
            store.values = values.toArray(new Object[0]);

            // The types/sortDirections of the leaf values, where array position == field position
            store.types = types[types.length - 1];
            store.sortDirections = sortDirections[sortDirections.length - 1];

            store.min = min;
            store.max = max;
            store.sum = sum;
        }

        return store;
    }

    private Double min(final Double m1, final Double m2) {
        if (m1 == null) {
            return m2;
        } else if (m2 == null) {
            return m1;
        }
        return Math.min(m1, m2);
    }

    private Double max(final Double m1, final Double m2) {
        if (m1 == null) {
            return m2;
        } else if (m2 == null) {
            return m1;
        }
        return Math.max(m1, m2);
    }

    private Double sum(final Double m1, final Double m2) {
        if (m1 == null) {
            return m2;
        } else if (m2 == null) {
            return m1;
        }
        return m1 + m2;
    }

//    private Map<Object, Store> mapNodes(
//    final Node[] nodes, final Field[][] structure, final int depth, final String[] types) {
//        if (nodes == null) {
//            return null;
//        }
//
//        String keyType = Type.GENERAL.name();
//        Field[] fields = structure[depth];
//        if (fields.length == 1) {
//            keyType = fields[0].getFormat().getType().name();
//        }
//
//        final Map<Object, Store> map = new HashMap<>();
//        for (final Node node : nodes) {
//
//            // Turn node key into basic single key value that visualisations expect.
//            Object keyValue = null;
//            if (node.getKey() != null) {
//                final Object[] keyValues = node.getKey()[depth];
//                if (keyValues != null && keyValues.length > 0) {
//                    if (keyValues.length == 1) {
//                        keyValue = keyValues[0];
//                    } else {
//                        final StringBuilder sb = new StringBuilder();
//                        for (final Object v : keyValues) {
//                            sb.append(v);
//                            sb.append("|");
//                        }
//                        sb.setLength(sb.length() - 1);
//                        keyValue = sb.toString();
//                    }
//                }
//            }
//
//            final Store store = new Store(keyValue, keyType, types);
//            store.map = mapNodes(node.getNodes(), structure, depth + 1, types);
//            store.values = mapValues(node.getValues(), structure, de);
//            store.min = node.getMin();
//            store.max = node.getMax();
//            store.sum = node.getSum();
//
//            map.put(keyValue, store);
//        }
//
//        return map;
//    }
//
//    private Object mapNode(
//    final Map<String, Object> node, String keyType, final Field[] structure, final int depth, final String[] types) {
//        Object key = node.get("key");
//        Object values = node.get("values");
//        Object min = node.get("min");
//        Object max = node.get("max");
//        Object sum = node.get("sum");
//
//        // Turn node key into basic single key value that visualisations expect.
//        Object keyValue = null;
//        if (key != null) {
//            final Object[] keyValues = (Object[])((Object[]) key)[depth];
//            if (keyValues != null && keyValues.length > 0) {
//                if (keyValues.length == 1) {
//                    keyValue = keyValues[0];
//                } else {
//                    final StringBuilder sb = new StringBuilder();
//                    for (final Object v : keyValues) {
//                        sb.append(v);
//                        sb.append("|");
//                    }
//                    sb.setLength(sb.length() - 1);
//                    keyValue = sb.toString();
//                }
//            }
//        }
//
//        final Store store = new Store(keyValue, keyType, types);
//        store.values = mapValues(node.getValues(), structure, depth + 1, types);
//        store.min = node.getMin();
//        store.max = node.getMax();
//        store.sum = node.getSum();
//
//        return store;
//    }
//
//    private Object[] mapValues(
//    final Object[][] values, final Field[] structure, final int depth, final String[] types) {
//        if (values == null) {
//            return null;
//        }
//
//        String keyType = Type.GENERAL.name();
//        Field[] fields = structure[depth];
//        if (fields.length == 1) {
//            keyType = fields[0].getFormat().getType().name();
//        }
//
//        final Object[] mapped = new Object[values.length];
//        for (int i = 0; i < values.length; i++) {
//            final Object value = values[i];
//            if (value instanceof Node) {
//                mapped[i] = mapNode((Node) value, keyType, structure, depth, types);
//            } else {
//                mapped[i] = value;
//            }
//        }
//
//        return mapped;
//    }
}
