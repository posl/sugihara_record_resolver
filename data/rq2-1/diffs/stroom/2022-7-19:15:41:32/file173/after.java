/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 */

package stroom.dashboard.shared;

import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.Param;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonPropertyOrder({
        "dataSourceRef",
        "expression",
        "componentSettingsMap",
        "params",
        "incremental",
        "storeHistory",
        "queryInfo"})
@JsonInclude(Include.NON_NULL)
public class Search {

    @JsonProperty
    private final DocRef dataSourceRef;
    @JsonProperty
    private final ExpressionOperator expression;
    @JsonProperty
    private final Map<String, ComponentSettings> componentSettingsMap;
    @JsonProperty
    private final List<Param> params;
    @JsonProperty
    private final boolean incremental;
    @JsonProperty
    private final String queryInfo;

    @JsonCreator
    public Search(@JsonProperty("dataSourceRef") final DocRef dataSourceRef,
                  @JsonProperty("expression") final ExpressionOperator expression,
                  @JsonProperty("componentSettingsMap") final Map<String, ComponentSettings> componentSettingsMap,
                  @JsonProperty("params") final List<Param> params,
                  @JsonProperty("incremental") final boolean incremental,
                  @JsonProperty("queryInfo") final String queryInfo) {
        this.dataSourceRef = dataSourceRef;
        this.expression = expression;
        this.componentSettingsMap = componentSettingsMap;
        this.params = params;
        this.incremental = incremental;
        this.queryInfo = queryInfo;
    }

    public DocRef getDataSourceRef() {
        return dataSourceRef;
    }

    public ExpressionOperator getExpression() {
        return expression;
    }

    public Map<String, ComponentSettings> getComponentSettingsMap() {
        return componentSettingsMap;
    }

    public List<Param> getParams() {
        return params;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public String getQueryInfo() {
        return queryInfo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Search search = (Search) o;
        return incremental == search.incremental &&
                Objects.equals(dataSourceRef, search.dataSourceRef) &&
                Objects.equals(expression, search.expression) &&
                Objects.equals(componentSettingsMap, search.componentSettingsMap) &&
                Objects.equals(params, search.params) &&
                Objects.equals(queryInfo, search.queryInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                dataSourceRef,
                expression,
                componentSettingsMap,
                params,
                incremental,
                queryInfo);
    }

    @Override
    public String toString() {
        return "Search{" +
                "dataSourceRef=" + dataSourceRef +
                ", expression=" + expression +
                ", componentSettingsMap=" + componentSettingsMap +
                ", params=" + params +
                ", incremental=" + incremental +
                ", queryInfo='" + queryInfo + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder {

        private DocRef dataSourceRef;
        private ExpressionOperator expression;
        private Map<String, ComponentSettings> componentSettingsMap;
        private List<Param> params;
        private boolean incremental;
        private String queryInfo;

        private Builder() {
        }

        private Builder(final Search search) {
            this.dataSourceRef = search.dataSourceRef;
            this.expression = search.expression;
            this.componentSettingsMap = search.componentSettingsMap;
            this.params = search.params;
            this.incremental = search.incremental;
            this.queryInfo = search.queryInfo;
        }

        public Builder dataSourceRef(final DocRef dataSourceRef) {
            this.dataSourceRef = dataSourceRef;
            return this;
        }

        public Builder expression(final ExpressionOperator expression) {
            this.expression = expression;
            return this;
        }

        public Builder componentSettingsMap(final Map<String, ComponentSettings> componentSettingsMap) {
            this.componentSettingsMap = componentSettingsMap;
            return this;
        }

        public Builder params(final List<Param> params) {
            this.params = params;
            return this;
        }

        public Builder incremental(final boolean incremental) {
            this.incremental = incremental;
            return this;
        }

        public Builder queryInfo(final String queryInfo) {
            this.queryInfo = queryInfo;
            return this;
        }

        public Search build() {
            return new Search(
                    dataSourceRef,
                    expression,
                    componentSettingsMap,
                    params,
                    incremental,
                    queryInfo);
        }
    }
}
