package primechecker.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import primechecker.common.Options;
import reactor.core.publisher.Flux;

import java.net.http.HttpResponse;
import java.util.List;

import static primechecker.common.Constants.EndPoint.CHECK_IF_PRIME;
import static primechecker.common.Constants.EndPoint.CHECK_IF_PRIME_FLUX;

/**
 * This Spring controller demonstrates how WebFlux can be used to
 * handle HTTP GET requests asynchronously via the Project Reactor
 * {@link Flux} and {@link ParallelFlux} reactive types.  These
 * requests are mapped to endpoint handler methods that determine the
 * primality of large random {@link Integer} objects.  These methods
 * can be passed either individual prime candidates or a {@link List}
 * of prime candidates and process these candidates either
 * sequentially or in parallel in response to client directives.
 *
 * In Spring's approach to building RESTful web services, HTTP
 * requests are handled by a controller that defines the
 * endpoints/routes for each supported operation, i.e.,
 * {@code @GetMapping}, {@code @PostMapping}, {@code @PutMapping} and
 * {@code @DeleteMapping}, which correspond to the HTTP GET, POST,
 * PUT, and DELETE calls, respectively.  These components are
 * identified by the {@code @RestController} annotation below.
 *
 * WebFlux uses the {@code @GetMapping} annotation to map HTTP GET
 * requests onto methods in the {@link PCServerController}.  GET
 * requests invoked from any HTTP web client (e.g., a web browser or
 * client app) or command-line utility (e.g., Curl or Postman).
 *
 * The {@code @RestController} annotation also tells a controller that
 * the object returned is automatically serialized into JSON and
 * passed back within the body of an {@link HttpResponse} object.
 */
@RestController
public class PCServerController {
    /**
     * Application context is to return application in "/" endpoint.
     */
    @Autowired
    ApplicationContext applicationContext;

    /**
     * This auto-wired field connects the {@link PCServerController}
     * to the {@link PCServerService}.
     */
    @Autowired
    PCServerService mService;

    /**
     * A request for testing Eureka connection.
     *
     * @return The application name.
     */
    @GetMapping({"/", "/actuator/info"})
    ResponseEntity<String> info() {
        // Indicate the request succeeded.  and return the application
        // name.
        return ResponseEntity
            .ok(applicationContext.getId()
                + " is alive and running on "
                + Thread.currentThread());
    }

    /**
     * Checks the {@code primeCandidate} param for primality,
     * returning 0 if it's prime or the smallest factor if it's not.
     *
     * Spring WebFlux maps HTTP GET requests sent to the {@code
     * CHECK_IF_PRIME} endpoint to this method.
     *
     * @param strategy Which implementation strategy to forward the
     *                 request to
     * @param primeCandidate The {@link Integer} to check for
     *                       primality
     * @return An {@link Integer} that is 0 if the {@code
     *         primeCandidate} is prime and its smallest factor if
     *         it's not prime
     */
    @GetMapping(CHECK_IF_PRIME)
    public int checkIfPrime(@RequestParam Integer strategy,
                            @RequestParam Integer primeCandidate) {
        return mService
            // Forward to the service.
            .checkIfPrime(strategy,
                          primeCandidate);
    }

    /**
     * Checks all the elements in the {@code primeCandidates} {@link
     * Flux} param for primality and return a corresponding {@link
     * Flux} whose results indicate 0 if an element is prime or the
     * smallest factor if it's not.
     * 
     * Spring WebFlux maps HTTP GET requests sent to the {@code
     * CHECK_IF_PRIME_FLUX} endpoint to this method.
     *
     * @param strategy Which implementation strategy to forward the
     *                 request to
     * @param primeCandidates The {@link Flux} of {@link Integer}
     *                        objects to check for primality
     * @return An {@link Flux} emitting elements that are 0 if the
     *         corresponding element in {@code primeCandidate} is
     *         prime or its smallest factor if it's not prime
     */
    @PostMapping(value = CHECK_IF_PRIME_FLUX,
                 // Enables passing Flux as a param.
                 consumes = "application/stream+json")
    public Flux<Integer> checkIfPrimeFlux
        (@RequestParam Integer strategy,
         @RequestBody Flux<Integer> primeCandidates) {
        return mService
            // Forward to the service.
            .checkIfPrimeFlux(strategy,
                              primeCandidates);
    }
}
