/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.ingest;

import org.elasticsearch.common.util.Maps;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.script.Metadata;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

class IngestDocMetadata extends Metadata {
    private static final FieldProperty<String> UPDATABLE_STRING = new FieldProperty<>(String.class, true, true, null);
    static final Map<String, FieldProperty<?>> PROPERTIES = Map.of(
        INDEX,
        UPDATABLE_STRING,
        ID,
        UPDATABLE_STRING,
        ROUTING,
        UPDATABLE_STRING,
        VERSION_TYPE,
        new FieldProperty<>(String.class, true, true, (k, v) -> {
            try {
                VersionType.fromString(v);
                return;
            } catch (IllegalArgumentException ignored) {}
            throw new IllegalArgumentException(
                k
                    + " must be a null or one of ["
                    + Arrays.stream(VersionType.values()).map(vt -> VersionType.toString(vt)).collect(Collectors.joining(", "))
                    + "] but was ["
                    + v
                    + "] with type ["
                    + v.getClass().getName()
                    + "]"
            );
        }),
        VERSION,
        new FieldProperty<>(Number.class, false, true, FieldProperty.LONGABLE_NUMBER),
        TYPE,
        new FieldProperty<>(String.class, true, false, null),
        IF_SEQ_NO,
        new FieldProperty<>(Number.class, true, true, FieldProperty.LONGABLE_NUMBER),
        IF_PRIMARY_TERM,
        new FieldProperty<>(Number.class, true, true, FieldProperty.LONGABLE_NUMBER),
        DYNAMIC_TEMPLATES,
        new FieldProperty<>(Map.class, true, true, null)
    );

    protected final ZonedDateTime timestamp;

    IngestDocMetadata(String index, String id, long version, String routing, VersionType versionType, ZonedDateTime timestamp) {
        this(metadataMap(index, id, version, routing, versionType), timestamp);
    }

    IngestDocMetadata(Map<String, Object> metadata, ZonedDateTime timestamp) {
        super(metadata, PROPERTIES);
        this.timestamp = timestamp;
    }

    /**
     * Create the backing metadata map with the standard contents assuming default validators.
     */
    protected static Map<String, Object> metadataMap(String index, String id, long version, String routing, VersionType versionType) {
        Map<String, Object> metadata = Maps.newHashMapWithExpectedSize(IngestDocument.Metadata.values().length);
        metadata.put(IngestDocument.Metadata.INDEX.getFieldName(), index);
        metadata.put(IngestDocument.Metadata.ID.getFieldName(), id);
        metadata.put(IngestDocument.Metadata.VERSION.getFieldName(), version);
        if (routing != null) {
            metadata.put(IngestDocument.Metadata.ROUTING.getFieldName(), routing);
        }
        if (versionType != null) {
            metadata.put(IngestDocument.Metadata.VERSION_TYPE.getFieldName(), VersionType.toString(versionType));
        }
        return metadata;
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
}
