/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.fielddata;

import org.elasticsearch.search.lookup.SearchLookup;

import java.util.function.Supplier;

/**
 * Holds context information for the construction of FieldData
 *
 * @param fullyQualifiedIndexName the index name with any remote index information added
 * @param lookupSupplier a supplier for a SearchLookup to be used by runtime scripts
 */
public record FieldDataContext(String fullyQualifiedIndexName, Supplier<SearchLookup> lookupSupplier) {

    /**
     * A context to use when runtime fields are not available
     *
     * Used for validating index sorts, eager global ordinal loading, etc
     *
     * @param reason the reason that runtime fields are not supported
     */
    public static FieldDataContext noRuntimeFields(String reason) {
        return new FieldDataContext(
            "",
            () -> { throw new UnsupportedOperationException("Runtime fields not supported for [" + reason + "]"); }
        );
    }
}
