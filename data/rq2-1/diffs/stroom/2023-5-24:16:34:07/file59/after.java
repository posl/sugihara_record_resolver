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

package stroom.analytics.shared;

import stroom.util.shared.BaseCriteria;
import stroom.util.shared.CriteriaFieldSort;
import stroom.util.shared.PageRequest;
import stroom.util.shared.filter.FilterFieldDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Criteria class.
 */
@JsonInclude(Include.NON_NULL)
public class FindAnalyticNotificationCriteria extends BaseCriteria {

    public static final String FIELD_NAME = "Name";
    public static final FilterFieldDefinition FIELD_DEF_NAME = FilterFieldDefinition.defaultField(FIELD_NAME);

    @JsonProperty
    private String analyticDocUuid;
    @JsonProperty
    private String quickFilterInput;

    public FindAnalyticNotificationCriteria() {
    }

    @JsonCreator
    public FindAnalyticNotificationCriteria(@JsonProperty("pageRequest") final PageRequest pageRequest,
                                            @JsonProperty("sortList") final List<CriteriaFieldSort> sortList,
                                            @JsonProperty("analyticDocUuid") final String analyticDocUuid,
                                            @JsonProperty("quickFilterInput") final String quickFilterInput) {
        super(pageRequest, sortList);
        this.quickFilterInput = quickFilterInput;
        this.analyticDocUuid = analyticDocUuid;
    }

    public String getAnalyticDocUuid() {
        return analyticDocUuid;
    }

    public void setAnalyticDocUuid(final String analyticDocUuid) {
        this.analyticDocUuid = analyticDocUuid;
    }

    public String getQuickFilterInput() {
        return quickFilterInput;
    }

    public void setQuickFilterInput(final String quickFilterInput) {
        this.quickFilterInput = quickFilterInput;
    }
}
