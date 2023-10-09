package stroom.feed.impl;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;
import stroom.util.shared.validation.ValidRegex;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder(alphabetic = true)
public class FeedConfig extends AbstractConfig implements IsStroomConfig {

    public static final String PROP_NAME_FEED_NAME_PATTERN = "feedNamePattern";
    public static final String PROP_NAME_FEED_DOC_CACHE = "feedDocCache";

    private final String unknownClassification;
    private final String feedNamePattern;
    private final CacheConfig feedDocCache;

    public FeedConfig() {
        unknownClassification = "UNKNOWN CLASSIFICATION";
        feedNamePattern = "^[A-Z0-9_-]{3,}$";

        feedDocCache = CacheConfig.builder()
                .maximumSize(1000L)
                .expireAfterWrite(StroomDuration.ofMinutes(1))
                .build();
    }

    @SuppressWarnings("unused")
    @JsonCreator
    public FeedConfig(@JsonProperty("unknownClassification") final String unknownClassification,
                      @JsonProperty("feedNamePattern") final String feedNamePattern,
                      @JsonProperty("feedDocCache") final CacheConfig feedDocCache) {
        this.unknownClassification = unknownClassification;
        this.feedNamePattern = feedNamePattern;
        this.feedDocCache = feedDocCache;
    }

    @JsonPropertyDescription("The classification banner to display for data if one is not defined")
    public String getUnknownClassification() {
        return unknownClassification;
    }

    @JsonPropertyDescription("The regex pattern for feed names")
    @JsonProperty(PROP_NAME_FEED_NAME_PATTERN)
    @ValidRegex
    public String getFeedNamePattern() {
        return feedNamePattern;
    }

    @JsonProperty(PROP_NAME_FEED_DOC_CACHE)
    public CacheConfig getFeedDocCache() {
        return feedDocCache;
    }

    @Override
    public String toString() {
        return "FeedConfig{" +
                "unknownClassification='" + unknownClassification + '\'' +
                ", feedNamePattern='" + feedNamePattern + '\'' +
                ", feedDocCache=" + feedDocCache +
                '}';
    }
}
