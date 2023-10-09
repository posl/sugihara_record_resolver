package microservices.airlines.SWA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import utils.Options;

/**
 * This class provides the entry point into the Spring WebFlux-based
 * version of the Southwest Airlines (SWA) microservice.
 */
@SpringBootApplication
public class SWAPricesMicroservice {
    /**
     * A static main() entry point is needed to run the SWA
     * microservice.
     */
    public static void main(String[] argv) {
        // Parse the options.
        Options.instance().parseArgs(argv);

        SpringApplication
            // Launch this microservice within Spring WebFlux.
            .run(SWAPricesMicroservice.class, argv);
    }
}
