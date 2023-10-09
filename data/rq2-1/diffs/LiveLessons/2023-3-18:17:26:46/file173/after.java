package edu.vandy.quoteservices.microservice;

import edu.vandy.quoteservices.common.BaseApplication;
import edu.vandy.quoteservices.common.Quote;
import edu.vandy.quoteservices.repository.JPAQuoteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

/**
 * This class provides the entry point into the Spring WebFlux-based
 * version of the Handey quote microservice.
 *
 * The {@code @SpringBootApplication} annotation enables apps to use
 * autoconfiguration, component scan, and to define extra
 * configurations on their "application" class.
 *
 * The {@code @ComponentScan} annotation configures component scanning
 * directives for use with {@code @Configuration} classes.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = {Quote.class})
@EnableJpaRepositories(basePackageClasses = {JPAQuoteRepository.class})
public class ZippyApplication 
       extends BaseApplication {
    /**
     * The static main() entry point runs this Spring application.
     */
    public static void main(String[] args) {
        // Call the BaseApplication.run() method to build and run this
        // application.
        run(ZippyApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(JPAQuoteRepository repository) {
        return args -> {
            repository.findAllById(List.of(1, 2))
                      .stream()
                      .forEach(it -> System.out.println(it.quote));
        };
    }
}
