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

package stroom.importexport.shared;

import stroom.util.shared.ResourceKey;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class ImportConfigResponse {

    @JsonProperty
    private final ResourceKey resourceKey;
    @JsonProperty
    private final List<ImportState> confirmList;

    @JsonCreator
    public ImportConfigResponse(@JsonProperty("resourceKey") final ResourceKey resourceKey,
                                @JsonProperty("confirmList") final List<ImportState> confirmList) {
        this.resourceKey = resourceKey;
        this.confirmList = confirmList;
    }

    public ResourceKey getResourceKey() {
        return resourceKey;
    }

    public List<ImportState> getConfirmList() {
        return confirmList;
    }
}
