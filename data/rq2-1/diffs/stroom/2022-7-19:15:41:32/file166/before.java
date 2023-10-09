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

package stroom.dashboard.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class DashboardQueryKey {

    @JsonProperty
    private final String uuid;
    @JsonProperty
    private final String dashboardUuid;
    @JsonProperty
    private final String componentId;

    @JsonCreator
    public DashboardQueryKey(@JsonProperty("uuid") final String uuid,
                             @JsonProperty("dashboardUuid") final String dashboardUuid,
                             @JsonProperty("componentId") final String componentId) {
        this.uuid = uuid;
        this.dashboardUuid = dashboardUuid;
        this.componentId = componentId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDashboardUuid() {
        return dashboardUuid;
    }

    public String getComponentId() {
        return componentId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DashboardQueryKey that = (DashboardQueryKey) o;
        return Objects.equals(uuid, that.uuid) &&
                Objects.equals(dashboardUuid, that.dashboardUuid) &&
                Objects.equals(componentId, that.componentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, dashboardUuid, componentId);
    }

    @Override
    public String toString() {
        return "DashboardQueryKey{" +
                "uuid='" + uuid + '\'' +
                ", dashboardUuid=" + dashboardUuid +
                ", componentId='" + componentId + '\'' +
                '}';
    }
}
