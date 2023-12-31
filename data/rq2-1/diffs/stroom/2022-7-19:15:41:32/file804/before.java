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

package stroom.query.api.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Object describing the response to a {@link SearchRequest searchRequest} which may or may not contains results
 */
@JsonPropertyOrder({"highlights", "results", "errors", "complete"})
@JsonInclude(Include.NON_NULL)
@XmlRootElement(name = "searchResponse")
@XmlType(name = "SearchResponse", propOrder = {"highlights", "results", "errors", "complete"})
@XmlAccessorType(XmlAccessType.FIELD)
@Schema(description = "The response to a search request, that may or may not contain results. The results " +
        "may only be a partial set if an iterative screech was requested")
public final class SearchResponse {

    public static final String TIMEOUT_MESSAGE = "The search timed out after ";

    @XmlElementWrapper(name = "highlights")
    @XmlElement(name = "highlight")
    @Schema(description = "A list of strings to highlight in the UI that should correlate with the search query.",
            required = true)
    @JsonProperty
    private final List<String> highlights;

    @XmlElementWrapper(name = "results")
    @XmlElements({
            @XmlElement(name = "table", type = TableResult.class),
            @XmlElement(name = "vis", type = FlatResult.class)
    })
    @JsonProperty
    private final List<Result> results;

    @XmlElementWrapper(name = "errors")
    @XmlElement(name = "error")
    @JsonPropertyDescription("A list of errors that occurred in running the query")
    @JsonProperty
    private final List<String> errors;

    @XmlElement
    @JsonPropertyDescription("True if the query has returned all known results")
    @JsonProperty
    private final Boolean complete;

    /**
     * @param highlights A list of strings to highlight in the UI that should correlate with the search query.
     * @param results    A list of {@link Result result} objects that each correspond to a
     *                   {@link ResultRequest resultRequest} in the {@link SearchRequest searchRequest}
     * @param errors     Any errors that have been generated during searching.
     * @param complete   Complete means that the search has finished and there are no more results to come.
     */
    @JsonCreator
    public SearchResponse(@JsonProperty("highlights") final List<String> highlights,
                          @JsonProperty("results") final List<Result> results,
                          @JsonProperty("errors") final List<String> errors,
                          @JsonProperty("complete") final Boolean complete) {
        this.highlights = highlights;
        this.results = results;
        this.errors = errors;
        this.complete = complete;
    }

    /**
     * @return A list of strings to highlight in the UI that should correlate with the search query.
     */
    public List<String> getHighlights() {
        return highlights;
    }

    /**
     * @return A list of {@link Result result} objects, corresponding to the {@link ResultRequest resultRequests} in
     * the {@link SearchRequest searchRequest}
     */
    public List<Result> getResults() {
        return results;
    }

    /**
     * @return A list of errors found when performing the search
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * @return The completed status of the search.  A value of false or null indicates the search has not yet
     * found all results.
     */
    public Boolean getComplete() {
        return complete;
    }

    /**
     * @return The completed status of the search.  A value of false indicates the search has not yet found all results
     */
    public boolean complete() {
        return complete != null && complete;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchResponse that = (SearchResponse) o;
        return Objects.equals(highlights, that.highlights) &&
                Objects.equals(results, that.results) &&
                Objects.equals(errors, that.errors) &&
                Objects.equals(complete, that.complete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(highlights, results, errors, complete);
    }

    @Override
    public String toString() {
        return "SearchResponse{" +
                "highlights=" + highlights +
                ", results=" + results +
                ", errors=" + errors +
                ", complete=" + complete +
                '}';
    }

    public static class TableResultBuilder extends Builder<TableResult, TableResultBuilder> {

        @Override
        public TableResultBuilder self() {
            return this;
        }
    }

    public static class FlatResultBuilder extends Builder<FlatResult, FlatResultBuilder> {

        @Override
        public FlatResultBuilder self() {
            return this;
        }
    }

    /**
     * Builder for constructing a {@link SearchResponse}
     *
     * @param <T_RESULT_CLASS> The class of the popToWhenComplete builder, allows nested building
     */
    private abstract static class Builder<
            T_RESULT_CLASS extends Result,
            T_CHILD_CLASS extends Builder<T_RESULT_CLASS, ?>> {

        // Mandatory parameters
        Boolean complete;

        // Optional parameters
        List<String> highlights = new ArrayList<>();
        List<Result> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Builder() {
        }

        Builder(final SearchResponse searchResponse) {
            complete = searchResponse.complete;
            highlights = searchResponse.highlights;
            results = searchResponse.results;
            errors = searchResponse.errors;
        }

        /**
         * @param value are the results considered complete
         * @return The {@link Builder}, enabling method chaining
         */
        public final T_CHILD_CLASS complete(final Boolean value) {
            this.complete = value;
            return self();
        }

//        /**
//         * Adds zero-many highlights to the search response
//         *
//         * @param highlights A set of strings to highlight in the UI that should correlate with the search query.
//         * @return this builder instance
//         */
//        public final CHILD_CLASS addHighlights(String... highlights) {
//            this.highlights.addAll(Arrays.asList(highlights));
//            return self();
//        }
//
//        /**
//         * Adds zero-many
//         *
//         * @param results The results that where found
//         * @return this builder instance
//         */
//        @SafeVarargs
//        public final CHILD_CLASS addResults(ResultClass... results) {
//            this.results.addAll(Arrays.asList(results));
//            return self();
//        }
//
//        /**
//         * Adds zero-many
//         *
//         * @param errors The errors that have occurred
//         * @return this builder instance
//         */
//        public final CHILD_CLASS addErrors(String... errors) {
//            this.errors.addAll(Arrays.asList(errors));
//            return self();
//        }

        public T_CHILD_CLASS highlights(final List<String> highlights) {
            this.highlights = highlights;
            return self();
        }

        public T_CHILD_CLASS results(final List<Result> results) {
            this.results = results;
            return self();
        }

        public T_CHILD_CLASS errors(final List<String> errors) {
            this.errors = errors;
            return self();
        }

        /**
         * Builds the {@link SearchResponse searchResponse} object
         *
         * @return A populated {@link SearchResponse searchResponse} object
         */
        public SearchResponse build() {
            return new SearchResponse(highlights, results, errors, complete);
        }

        protected abstract T_CHILD_CLASS self();
    }

}
