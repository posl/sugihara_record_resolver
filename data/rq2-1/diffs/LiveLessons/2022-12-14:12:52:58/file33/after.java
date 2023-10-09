package mathservices.server.primality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import mathservices.common.Options;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * This class provides the entry point into the Spring WebMVC-based
 * version of the Primality microservice.
 * 
 * The {@code @SpringBootApplication} annotation enables apps to use
 * autoconfiguration, component scan, and to define extra
 * configurations on their "application" class.
 * 
 * The {@code @ComponentScan} annotation configures component scanning
 * directives for use with {@code @Configuration} classes.
 */
@SpringBootApplication
//@ComponentScan("mathservices")
@PropertySources({
        @PropertySource("classpath:/primality/primalitymicroservice.properties")
})
public class PrimalityApplication {
    /**
     * A static main() entry point is needed to run the {@link
     * PrimalityApplication} server app.
     */
    public static void main(String[] argv) {
        // Parse the options.
        Options.instance().parseArgs(argv);

        SpringApplication
            // Launch the ServerApplication within Spring WebMVC.
            .run(PrimalityApplication.class, argv);
    }
}
