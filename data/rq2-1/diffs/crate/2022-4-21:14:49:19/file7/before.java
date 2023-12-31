/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.metadata.blob;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNotFoundException;

import io.crate.analyze.NumberOfReplicas;
import io.crate.blob.v2.BlobIndex;
import io.crate.blob.v2.BlobIndicesService;
import io.crate.exceptions.RelationUnknown;
import io.crate.metadata.RelationName;

public class InternalBlobTableInfoFactory implements BlobTableInfoFactory {

    private static final Logger LOGGER = LogManager.getLogger(InternalBlobTableInfoFactory.class);
    private final Path[] dataFiles;
    private final Path globalBlobPath;

    @Inject
    public InternalBlobTableInfoFactory(Settings settings,
                                        Environment environment) {
        this(settings, environment.dataFiles());
    }

    public InternalBlobTableInfoFactory(Settings settings,
                                        Path[] dataFiles) {
        this.dataFiles = dataFiles;
        this.globalBlobPath = BlobIndicesService.getGlobalBlobPath(settings);;
    }

    private IndexMetadata resolveIndexMetadata(String tableName, ClusterState state) {
        String indexName = BlobIndex.fullIndexName(tableName);
        Index index;
        try {
            index = IndexNameExpressionResolver.concreteIndices(state, IndicesOptions.strictExpandOpen(), indexName)[0];
        } catch (IndexNotFoundException ex) {
            throw new RelationUnknown(indexName, ex);
        }
        return state.metadata().index(index);
    }

    @Override
    public BlobTableInfo create(RelationName ident, ClusterState clusterState) {
        IndexMetadata indexMetadata = resolveIndexMetadata(ident.name(), clusterState);
        Settings settings = indexMetadata.getSettings();
        return new BlobTableInfo(
            ident,
            indexMetadata.getIndex().getName(),
            indexMetadata.getNumberOfShards(),
            NumberOfReplicas.fromSettings(settings),
            settings,
            blobsPath(settings),
            IndexMetadata.SETTING_INDEX_VERSION_CREATED.get(settings),
            settings.getAsVersion(IndexMetadata.SETTING_VERSION_UPGRADED, null),
            indexMetadata.getState() == IndexMetadata.State.CLOSE);
    }

    private String blobsPath(Settings indexMetadataSettings) {
        String blobsPath;
        String blobsPathStr = BlobIndicesService.SETTING_INDEX_BLOBS_PATH.get(indexMetadataSettings);
        if (!Strings.isNullOrEmpty(blobsPathStr)) {
            blobsPath = blobsPathStr;
        } else {
            Path path = globalBlobPath;
            if (path != null) {
                blobsPath = path.toString();
            } else {
                // TODO: should we set this to null because there is no special blobPath?
                blobsPath = dataFiles[0].toString();
            }
        }
        return blobsPath;
    }
}
