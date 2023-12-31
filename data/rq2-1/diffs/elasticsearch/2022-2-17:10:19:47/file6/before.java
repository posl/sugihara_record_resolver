/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.fieldcaps;

import org.elasticsearch.common.Strings;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.MapperServiceTestCase;
import org.elasticsearch.index.query.SearchExecutionContext;

import java.io.IOException;
import java.util.function.Predicate;

public class FieldCapabilitiesFilterTests extends MapperServiceTestCase {

    public void testExcludeNestedFields() throws IOException {
        MapperService mapperService = createMapperService("""
            { "_doc" : {
              "properties" : {
                "field1" : { "type" : "keyword" },
                "field2" : {
                  "type" : "nested",
                  "properties" : {
                    "field3" : { "type" : "keyword" }
                  }
                },
                "field4" : { "type" : "keyword" }
              }
            } }
            """);
        SearchExecutionContext sec = createSearchExecutionContext(mapperService);

        FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
            "index",
            sec,
            new String[] { "*" },
            new String[] { "-nested" },
            Strings.EMPTY_ARRAY,
            f -> true
        );

        assertNotNull(response.getField("field1"));
        assertNotNull(response.getField("field4"));
        assertNull(response.getField("field2"));
        assertNull(response.getField("field2.field3"));
    }

    public void testMetadataFilters() throws IOException {
        MapperService mapperService = createMapperService("""
            { "_doc" : {
              "properties" : {
                "field1" : { "type" : "keyword" },
                "field2" : { "type" : "keyword" }
              }
            } }
            """);
        SearchExecutionContext sec = createSearchExecutionContext(mapperService);

        {
            FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
                "index",
                sec,
                new String[] { "*" },
                new String[] { "+metadata" },
                Strings.EMPTY_ARRAY,
                f -> true
            );
            assertNotNull(response.getField("_index"));
            assertNull(response.getField("field1"));
        }
        {
            FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
                "index",
                sec,
                new String[] { "*" },
                new String[] { "-metadata" },
                Strings.EMPTY_ARRAY,
                f -> true
            );
            assertNull(response.getField("_index"));
            assertNotNull(response.getField("field1"));
        }
    }

    public void testExcludeMultifields() throws IOException {
        MapperService mapperService = createMapperService("""
            { "_doc" : {
              "properties" : {
                "field1" : {
                  "type" : "text",
                  "fields" : {
                    "keyword" : { "type" : "keyword" }
                  }
                },
                "field2" : { "type" : "keyword" }
              },
              "runtime" : {
                "field2.keyword" : { "type" : "keyword" }
              }
            } }
            """);
        SearchExecutionContext sec = createSearchExecutionContext(mapperService);

        FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
            "index",
            sec,
            new String[] { "*" },
            new String[] { "-multifield" },
            Strings.EMPTY_ARRAY,
            f -> true
        );
        assertNotNull(response.getField("field1"));
        assertNull(response.getField("field1.keyword"));
        assertNotNull(response.getField("field2"));
        assertNotNull(response.getField("field2.keyword"));
        assertNotNull(response.getField("_index"));
    }

    public void testDontIncludeParentInfo() throws IOException {
        MapperService mapperService = createMapperService("""
            { "_doc" : {
              "properties" : {
                "parent" : {
                  "properties" : {
                    "field1" : { "type" : "keyword" },
                    "field2" : { "type" : "keyword" }
                  }
                }
              }
            } }
            """);
        SearchExecutionContext sec = createSearchExecutionContext(mapperService);

        FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
            "index",
            sec,
            new String[] { "*" },
            new String[] { "-parent" },
            Strings.EMPTY_ARRAY,
            f -> true
        );
        assertNotNull(response.getField("parent.field1"));
        assertNotNull(response.getField("parent.field2"));
        assertNull(response.getField("parent"));
    }

    public void testSecurityFilter() throws IOException {
        MapperService mapperService = createMapperService("""
            { "_doc" : {
              "properties" : {
                "permitted1" : { "type" : "keyword" },
                "permitted2" : { "type" : "keyword" },
                "forbidden" : { "type" : "keyword" }
              }
            } }
            """);
        SearchExecutionContext sec = createSearchExecutionContext(mapperService);
        Predicate<String> securityFilter = f -> f.startsWith("permitted");

        {
            FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
                "index",
                sec,
                new String[] { "*" },
                Strings.EMPTY_ARRAY,
                Strings.EMPTY_ARRAY,
                securityFilter
            );

            assertNotNull(response.getField("permitted1"));
            assertNull(response.getField("forbidden"));
            assertNotNull(response.getField("_index"));     // security filter doesn't apply to metadata
        }

        {
            FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
                "index",
                sec,
                new String[] { "*" },
                new String[] { "-metadata" },
                Strings.EMPTY_ARRAY,
                securityFilter
            );

            assertNotNull(response.getField("permitted1"));
            assertNull(response.getField("forbidden"));
            assertNull(response.getField("_index"));     // -metadata filter applies on top
        }
    }

    public void testFieldTypeFiltering() throws IOException {
        MapperService mapperService = createMapperService("""
            { "_doc" : {
              "properties" : {
                "field1" : { "type" : "keyword" },
                "field2" : { "type" : "long" },
                "field3" : { "type" : "text" }
              }
            } }
            """);
        SearchExecutionContext sec = createSearchExecutionContext(mapperService);

        FieldCapabilitiesIndexResponse response = FieldCapabilitiesFetcher.retrieveFieldCaps(
            "index",
            sec,
            new String[] { "*" },
            Strings.EMPTY_ARRAY,
            new String[] { "text", "keyword" },
            f -> true
        );
        assertNotNull(response.getField("field1"));
        assertNull(response.getField("field2"));
        assertNotNull(response.getField("field3"));
        assertNull(response.getField("_index"));
    }
}
