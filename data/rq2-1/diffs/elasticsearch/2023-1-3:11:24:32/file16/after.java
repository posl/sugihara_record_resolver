/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.admin.indices.settings.get;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.RandomCreateIndexGenerator;
import org.elasticsearch.test.AbstractChunkedSerializingTestCase;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class GetSettingsResponseTests extends AbstractChunkedSerializingTestCase<GetSettingsResponse> {

    @Override
    protected GetSettingsResponse createTestInstance() {
        HashMap<String, Settings> indexToSettings = new HashMap<>();
        HashMap<String, Settings> indexToDefaultSettings = new HashMap<>();

        IndexScopedSettings indexScopedSettings = IndexScopedSettings.DEFAULT_SCOPED_SETTINGS;

        Set<String> indexNames = new HashSet<String>();
        int numIndices = randomIntBetween(1, 5);
        for (int x = 0; x < numIndices; x++) {
            String indexName = randomAlphaOfLength(5);
            indexNames.add(indexName);
        }

        for (String indexName : indexNames) {
            Settings.Builder builder = Settings.builder();
            builder.put(RandomCreateIndexGenerator.randomIndexSettings());
            /*
            We must ensure that *something* is in the settings response as we optimize away empty settings
            blocks in x content responses
             */
            builder.put("index.refresh_interval", "1s");
            indexToSettings.put(indexName, builder.build());
        }

        if (randomBoolean()) {
            for (String indexName : indexToSettings.keySet()) {
                Settings defaultSettings = indexScopedSettings.diff(indexToSettings.get(indexName), Settings.EMPTY);
                indexToDefaultSettings.put(indexName, defaultSettings);
            }
        }

        return new GetSettingsResponse(indexToSettings, indexToDefaultSettings);
    }

    @Override
    protected Writeable.Reader<GetSettingsResponse> instanceReader() {
        return GetSettingsResponse::new;
    }

    @Override
    protected GetSettingsResponse doParseInstance(XContentParser parser) throws IOException {
        return GetSettingsResponse.fromXContent(parser);
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        // we do not want to add new fields at the root (index-level), or inside settings blocks
        return f -> f.equals("") || f.contains(".settings") || f.contains(".defaults");
    }

    public void testChunking() {
        AbstractChunkedSerializingTestCase.assertChunkCount(createTestInstance(), response -> 2 + response.getIndexToSettings().size());
    }

}
