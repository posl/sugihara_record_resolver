package edu.vandy.mathservices.microservices.primality;

import edu.vandy.mathservices.common.Options;
import edu.vandy.mathservices.common.PrimeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.net.http.HttpResponse;
import java.util.List;

import static edu.vandy.mathservices.common.Constants.EndPoint.CHECK_PRIMALITY_LIST;

/**
 * This Spring controller demonstrates how WebMVC can be used to
 * handle HTTP GET requests via the Java structured concurrency
 * framework.  These requests are mapped to endpoint handler methods
 * that determine the primality of large random {@link Integer}
 * objects.
 *
 * In Spring's approach to building RESTful web services, HTTP
 * requests are handled by a controller that defines the
 * endpoints/routes for each supported operation, i.e.,
 * {@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping} and
 * {@code @DeleteMapping}, which correspond to the HTTP GET, POST,
 * PUT, and DELETE calls, respectively.  These components are
 * identified by the {@code @RestController} annotation below.
 *
 * WebMVC uses the {@code @GetMapping} annotation to map HTTP GET
 * requests onto methods in the {@link PrimalityController}.  GET
 * requests invoked from any HTTP web client (e.g., a web browser or
 * client app) or command-line utility (e.g., Curl or Postman).
 *
 * The {@code @RestController} annotation also tells a controller that
 * the object returned is automatically serialized into JSON and passed
 * back within the body of an {@link HttpResponse} object.
 */
@RestController
public class PrimalityController {
    /**
     * This auto-wired field connects the {@link PrimalityController}
     * to the {@link PrimalityService}.
     */
    @Autowired
    PrimalityService mService;

    /**
     * Checks all the elements in the {@code primeCandidates} {@link
     * List} param for primality and return a corresponding {@link
     * List} whose {@link PrimeResult} elements indicate 0 if an
     * element is prime or the smallest factor if it's not.
     *
     * Spring WebMVC maps HTTP GET requests sent to the {@code
     * CHECK_PRIMALITIES} endpoint to this method.
     *
     * @param primeCandidates The {@link List} of {@link Integer}
     *                        objects to check for primality
     * @return An {@link List} of {@link PrimeResult} objects
     */
    @GetMapping(CHECK_PRIMALITY_LIST)
    public List<PrimeResult> checkPrimalities
    (@RequestParam List<Integer> primeCandidates) {
        return mService
            // Forward to the service.
            .checkPrimalities(primeCandidates);
    }
}
