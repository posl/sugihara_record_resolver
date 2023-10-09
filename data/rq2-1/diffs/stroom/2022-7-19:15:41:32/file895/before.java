package stroom.searchable.impl;

import stroom.datasource.api.v2.DataSource;
import stroom.datasource.api.v2.DataSourceProvider;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionUtil;
import stroom.query.api.v2.OffsetRange;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.query.api.v2.TableResult;
import stroom.query.common.v2.SearchResponseCreator;
import stroom.query.common.v2.SearchResponseCreatorCache;
import stroom.query.common.v2.SearchResponseCreatorManager;
import stroom.searchable.api.Searchable;
import stroom.searchable.api.SearchableProvider;
import stroom.security.api.SecurityContext;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@SuppressWarnings("unused")
// Used by DI
class SearchableService implements DataSourceProvider {

    public static final long PROCESS_PAYLOAD_INTERVAL_SECS = 1L;
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SearchableService.class);
    private final SearchableProvider searchableProvider;
    private final SearchResponseCreatorManager searchResponseCreatorManager;
    private final SecurityContext securityContext;

    @Inject
    SearchableService(final SearchableProvider searchableProvider,
                      final SearchableSearchResponseCreatorManager searchResponseCreatorManager,
                      final SecurityContext securityContext) {
        this.searchableProvider = searchableProvider;
        this.searchResponseCreatorManager = searchResponseCreatorManager;
        this.securityContext = securityContext;
    }

    @Override
    public DataSource getDataSource(final DocRef docRef) {
        return securityContext.useAsReadResult(() -> {
            LOGGER.debug(() -> "getDataSource called for docRef " + docRef);
            final Searchable searchable = searchableProvider.get(docRef);
            if (searchable == null) {
                return null;
            }
            return searchable.getDataSource();
        });
    }

    @Override
    public SearchResponse search(final SearchRequest searchRequest) {
        return securityContext.useAsReadResult(() -> {
            LOGGER.debug(() -> "search called for searchRequest " + searchRequest);

            // Replace expression parameters.
            final SearchRequest modifiedSearchRequest = ExpressionUtil.replaceExpressionParameters(searchRequest);

            final DocRef docRef = Preconditions.checkNotNull(
                    Preconditions.checkNotNull(
                                    Preconditions.checkNotNull(modifiedSearchRequest)
                                            .getQuery())
                            .getDataSource());
            Preconditions.checkNotNull(modifiedSearchRequest.getResultRequests(),
                    "searchRequest must have at least one resultRequest");
            Preconditions.checkArgument(!modifiedSearchRequest.getResultRequests().isEmpty(),
                    "searchRequest must have at least one resultRequest");

            final Searchable searchable = searchableProvider.get(docRef);
            if (searchable == null) {
                return buildEmptyResponse(
                        modifiedSearchRequest,
                        Collections.singletonList("Searchable could not be found for uuid " + docRef.getUuid()));
            } else {
                return buildResponse(modifiedSearchRequest, searchable);
            }
        });
    }

    @Override
    public Boolean keepAlive(final QueryKey queryKey) {
        LOGGER.trace(() -> "keepAlive() " + queryKey);
        return searchResponseCreatorManager
                .getOptional(new SearchResponseCreatorCache.Key(queryKey))
                .map(SearchResponseCreator::keepAlive)
                .orElse(Boolean.FALSE);
    }

    @Override
    public Boolean destroy(final QueryKey queryKey) {
        LOGGER.debug(() -> "destroy called for queryKey " + queryKey);
        // remove the creator from the cache which will trigger the onRemove listener
        // which will call destroy on the store
        searchResponseCreatorManager.remove(new SearchResponseCreatorCache.Key(queryKey));
        return Boolean.TRUE;
    }

    private SearchResponse buildResponse(final SearchRequest searchRequest,
                                         final Searchable searchable) {

        Preconditions.checkNotNull(searchRequest);
        Preconditions.checkNotNull(searchable);

        // This will create/get a searchResponseCreator for this query key
        final SearchResponseCreator searchResponseCreator = searchResponseCreatorManager.get(
                new SearchResponseCreatorCache.Key(searchRequest));

        // This will build a response from the search whether it is still running or has finished
        return searchResponseCreator.create(searchRequest);
    }

    private SearchResponse buildEmptyResponse(final SearchRequest searchRequest, final List<String> errors) {

        List<Result> results;
        if (searchRequest.getResultRequests() != null) {
            results = searchRequest.getResultRequests().stream()
                    .map(resultRequest -> new TableResult(
                            resultRequest.getComponentId(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            new OffsetRange(0, 0),
                            0,
                            null))
                    .collect(Collectors.toList());
        } else {
            results = Collections.emptyList();
        }

        return new SearchResponse(
                Collections.emptyList(),
                results,
                errors,
                true);
    }

    @Override
    public String getType() {
        return "Searchable";
    }
}
