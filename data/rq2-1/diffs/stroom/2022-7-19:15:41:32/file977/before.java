package stroom.statistics.impl.sql.search;

import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.query.api.v2.OffsetRange;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.query.api.v2.TableResult;
import stroom.query.common.v2.SearchResponseCreator;
import stroom.query.common.v2.SearchResponseCreatorCache;
import stroom.query.common.v2.SearchResponseCreatorManager;
import stroom.security.api.SecurityContext;
import stroom.statistics.impl.sql.SQLStatisticCacheImpl;
import stroom.statistics.impl.sql.StatisticsQueryService;
import stroom.statistics.impl.sql.entity.StatisticStoreCache;
import stroom.statistics.impl.sql.entity.StatisticsDataSourceProvider;
import stroom.statistics.impl.sql.shared.StatisticStoreDoc;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@SuppressWarnings("unused") //used by DI
public class StatisticsQueryServiceImpl implements StatisticsQueryService {

    public static final long PROCESS_PAYLOAD_INTERVAL_SECS = 1L;
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(SQLStatisticCacheImpl.class);
    private final StatisticsDataSourceProvider statisticsDataSourceProvider;
    private final StatisticStoreCache statisticStoreCache;
    private final SearchResponseCreatorManager searchResponseCreatorManager;
    private final SecurityContext securityContext;

    @Inject
    public StatisticsQueryServiceImpl(final StatisticsDataSourceProvider statisticsDataSourceProvider,
                                      final StatisticStoreCache statisticStoreCache,
                                      final SqlStatisticsSearchResponseCreatorManager searchResponseCreatorManager,
                                      final SecurityContext securityContext) {
        this.statisticsDataSourceProvider = statisticsDataSourceProvider;
        this.statisticStoreCache = statisticStoreCache;
        this.searchResponseCreatorManager = searchResponseCreatorManager;
        this.securityContext = securityContext;
    }


    @Override
    public DataSource getDataSource(final DocRef docRef) {
        return securityContext.useAsReadResult(() -> {
            LOGGER.debug(() -> "getDataSource called for docRef " + docRef);
            return statisticsDataSourceProvider.getDataSource(docRef);
        });
    }

    @Override
    public SearchResponse search(final SearchRequest searchRequest) {
        return securityContext.useAsReadResult(() -> {
            LOGGER.debug(() -> "search called for searchRequest " + searchRequest);

            final DocRef docRef = Preconditions.checkNotNull(
                    Preconditions.checkNotNull(
                                    Preconditions.checkNotNull(searchRequest)
                                            .getQuery())
                            .getDataSource());
            Preconditions.checkNotNull(searchRequest.getResultRequests(),
                    "searchRequest must have at least one resultRequest");
            Preconditions.checkArgument(!searchRequest.getResultRequests().isEmpty(),
                    "searchRequest must have at least one resultRequest");

            final StatisticStoreDoc statisticStoreEntity = statisticStoreCache.getStatisticsDataSource(docRef);

            if (statisticStoreEntity == null) {
                return buildEmptyResponse(
                        searchRequest,
                        Collections.singletonList("Statistic configuration could not be found for uuid " +
                                docRef.getUuid()));
            } else {
                return buildResponse(searchRequest, statisticStoreEntity);
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
                                         final StatisticStoreDoc statisticStoreEntity) {

        Preconditions.checkNotNull(searchRequest);
        Preconditions.checkNotNull(statisticStoreEntity);


        // This will create/get a searchResponseCreator for this query key
        final SearchResponseCreator searchResponseCreator = searchResponseCreatorManager.get(
                new SearchResponseCreatorCache.Key(searchRequest));

        // This will build a response from the search whether it is still running or has finished
        return searchResponseCreator.create(searchRequest);
    }

    private SearchResponse buildEmptyResponse(final SearchRequest searchRequest, final List<String> errorMessages) {

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
                errorMessages,
                true);
    }

    @Override
    public String getType() {
        return StatisticStoreDoc.DOCUMENT_TYPE;
    }
}
