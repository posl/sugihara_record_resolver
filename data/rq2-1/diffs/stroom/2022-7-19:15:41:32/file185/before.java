/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.shared;

import stroom.docref.DocRef;
import stroom.docstore.shared.Doc;
import stroom.pipeline.shared.data.PipelineData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * This entity is used to persist pipeline configuration.
 */
@JsonPropertyOrder({
        "type",
        "uuid",
        "name",
        "version",
        "createTime",
        "updateTime",
        "createUser",
        "updateUser",
        "description",
        "parentPipeline",
        "pipelineData"})
@JsonInclude(Include.NON_NULL)
public class PipelineDoc extends Doc {

    public static final String DOCUMENT_TYPE = "Pipeline";

    @JsonProperty
    private String description;
    @JsonProperty
    private DocRef parentPipeline;
    @JsonProperty
    private PipelineData pipelineData;

    public PipelineDoc() {
    }

    @JsonCreator
    public PipelineDoc(@JsonProperty("type") final String type,
                       @JsonProperty("uuid") final String uuid,
                       @JsonProperty("name") final String name,
                       @JsonProperty("version") final String version,
                       @JsonProperty("createTime") final Long createTime,
                       @JsonProperty("updateTime") final Long updateTime,
                       @JsonProperty("createUser") final String createUser,
                       @JsonProperty("updateUser") final String updateUser,
                       @JsonProperty("description") final String description,
                       @JsonProperty("parentPipeline") final DocRef parentPipeline,
                       @JsonProperty("pipelineData") final PipelineData pipelineData) {
        super(type, uuid, name, version, createTime, updateTime, createUser, updateUser);
        this.description = description;
        this.parentPipeline = parentPipeline;
        this.pipelineData = pipelineData;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public DocRef getParentPipeline() {
        return parentPipeline;
    }

    public void setParentPipeline(final DocRef parentPipeline) {
        this.parentPipeline = parentPipeline;
    }

    public PipelineData getPipelineData() {
        return pipelineData;
    }

    public void setPipelineData(final PipelineData pipelineData) {
        this.pipelineData = pipelineData;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final PipelineDoc that = (PipelineDoc) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(parentPipeline, that.parentPipeline) &&
                Objects.equals(pipelineData, that.pipelineData);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), description, parentPipeline, pipelineData);
    }
}
