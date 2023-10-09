package edu.vandy.quoteservices.common;

import java.util.List;

/**
 * This interface defines the methods that are called by the
 * {@link BaseController}, which serves as the main "front-end" app
 * gateway entry point for remote clients that want to receive quotes.
 *
 * This interface is used to collate common functionality of all
 * Service implementations and to enable a common interface for the
 * {@link BaseController} to delegate to.
 *
 * Any class that implements this interface should be annotated as a
 * Spring {@code @Service}, which enables the automatic detection and
 * wiring of dependent implementation classes via classpath scanning.
 */
public interface BaseService<T> {
    /**
     * @return A {@link List} of all {@link Quote} objects
     */
    T getAllQuotes();

    /**
     * Get a {@link Quote} corresponding to the given id.
     *
     * @param quoteId An {@link Integer} containing the given
     *                 {@code quoteId}
     * @return A {@link Quote} containing the requested {@code quoteId}
     */
    Quote getQuote(Integer quoteId);

    /**
     * Get a {@link List} that contains the requested quotes.
     *
     * @param quoteIds A {@link List} containing the given random
     *                 {@code quoteIds}
     * @param parallel Run the queries in parallel if true, else run
     *                 sequentially
     * @return A {@code T} of all requested {@link Quote} objects
     */
    T postQuotes(List<Integer> quoteIds,
                 Boolean parallel);

    /**
     * Search for quotes containing the given {@link String} queries
     * and return a {@link T} of matches.
     *
     * @param queries The search queries
     * @param parallel Run the queries in parallel if true, else run
     *                 sequentially
     * @return A {@code T} of quotes containing the given {@code
     *         queries}
     */
     T search(List<String> queries,
              Boolean parallel);

    /**
     * Search for quotes containing all the given {@link String} and
     * return a {@link List} that emits the matching {@link Quote}
     * objects.
     *
     * @param queries The search queries
     * @return A {@code Flux} that emits {@link Quote} objects
     *         containing the given {@code queries}
     */
     T searchEx(List<String> queries,
                Boolean parallel);
}
