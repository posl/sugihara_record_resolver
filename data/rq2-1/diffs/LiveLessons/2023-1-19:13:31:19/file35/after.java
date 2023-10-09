package berraquotes.client;

import berraquotes.common.Quote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This client uses Spring WebMVC features to perform synchronous
 * remote method invocations on the {@code BerraQuoteController}
 * microservice to request Yogi Berra quotes.
 *
 * The {@code @Component} annotation allows Spring to automatically
 * detect custom beans, i.e., Spring will scan the application for
 * classes annotated with {@code @Component}, instantiate them, and
 * inject the specified dependencies into them without having to write
 * any explicit code.
 */
@Component
public class BerraQuotesClient {
    /**
     * This auto-wired field connects the {@link BerraQuotesClient} to
     * the {@link BerraQuotesProxy} that performs HTTP requests
     * synchronously.
     */
    @Autowired
    private BerraQuotesProxy mQuoteProxy;

    /**
     * @return An {@link List} containing all {@link
     *         Quote} objects
     */
    public List<Quote> getAllQuotes() {
        return mQuoteProxy
            // Forward to the proxy.
            .getAllQuotes();
    }

    /**
     * Get a {@link List} that contains the requested quotes.
     *
     * @param quoteIds A {@link List} containing the given
     *                 {@code quoteIds}
     * @return An {@link List} containing the requested {@link
     *         Quote} objects
     */
    public List<Quote> getQuotes(List<Integer> quoteIds) {
        return mQuoteProxy
            // Forward to the proxy.
            .getQuotes(quoteIds);
    }

    /**
     * Get a {@link List} that contains quotes that match the
     * {@code query}.
     *
     * @param query A {@link String} to search for
     * @return An {@link List} containing matching {@link
     *         Quote} objects
     */
    public List<Quote> searchQuotes(String query) {
        return mQuoteProxy
            // Forward to the proxy.
            .searchQuotes(query);
    }
}
