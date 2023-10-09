/*
 * Copyright 2016 Crown Copyright
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

package stroom.pipeline.shared;

import stroom.docref.HasDisplayValue;
import stroom.docstore.shared.Doc;
import stroom.util.shared.HasData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

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
        "data",
        "converterType"})
@JsonInclude(Include.NON_NULL)
public class TextConverterDoc extends Doc implements HasData {

    public static final String DOCUMENT_TYPE = "TextConverter";

    @JsonProperty
    private String description;
    @JsonProperty
    private String data;
    @JsonProperty
    private TextConverterType converterType;

    public TextConverterDoc() {
        converterType = TextConverterType.NONE;
    }

    @JsonCreator
    public TextConverterDoc(@JsonProperty("type") final String type,
                            @JsonProperty("uuid") final String uuid,
                            @JsonProperty("name") final String name,
                            @JsonProperty("version") final String version,
                            @JsonProperty("createTime") final Long createTime,
                            @JsonProperty("updateTime") final Long updateTime,
                            @JsonProperty("createUser") final String createUser,
                            @JsonProperty("updateUser") final String updateUser,
                            @JsonProperty("description") final String description,
                            @JsonProperty("data") final String data,
                            @JsonProperty("converterType") final TextConverterType converterType) {
        super(type, uuid, name, version, createTime, updateTime, createUser, updateUser);
        this.description = description;
        this.data = data;
        this.converterType = converterType;

        if (converterType == null) {
            this.converterType = TextConverterType.NONE;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(final String data) {
        this.data = data;
    }

    public TextConverterType getConverterType() {
        return converterType;
    }

    public void setConverterType(final TextConverterType converterType) {
        this.converterType = converterType;
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
        final TextConverterDoc that = (TextConverterDoc) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(data, that.data) &&
                Objects.equals(converterType, that.converterType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, data, converterType);
    }

    public enum TextConverterType implements HasDisplayValue {
        NONE("None"),
        DATA_SPLITTER("Data Splitter"),
        XML_FRAGMENT("XML Fragment");

        private final String displayValue;

        TextConverterType(final String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String getDisplayValue() {
            return displayValue;
        }
    }
}
