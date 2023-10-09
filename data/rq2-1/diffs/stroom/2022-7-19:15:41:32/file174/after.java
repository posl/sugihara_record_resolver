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

package stroom.dashboard.shared;

import stroom.query.api.v2.QueryKey;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonInclude(Include.NON_NULL)
public class SearchKeepAliveRequest {

    @JsonProperty
    private final Set<QueryKey> activeKeys;
    @JsonProperty
    private final Set<QueryKey> deadKeys;

    @JsonCreator
    public SearchKeepAliveRequest(@JsonProperty("activeKeys") final Set<QueryKey> activeKeys,
                                  @JsonProperty("deadKeys") final Set<QueryKey> deadKeys) {
        this.activeKeys = activeKeys;
        this.deadKeys = deadKeys;
    }

    public Set<QueryKey> getActiveKeys() {
        return activeKeys;
    }

    public Set<QueryKey> getDeadKeys() {
        return deadKeys;
    }

    @Override
    public String toString() {
        return "SearchKeepAliveRequest{" +
                "activeKeys=" + activeKeys +
                ", deadKeys=" + deadKeys +
                '}';
    }
}
