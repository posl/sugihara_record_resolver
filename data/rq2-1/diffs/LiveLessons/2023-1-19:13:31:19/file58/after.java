package folders.common;

import folders.client.RestTemplateResponseErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static folders.common.Constants.SERVER_BASE_URL;

/**
 * This class contains {@code Bean} annotated methods that can be
 * injected into client and server classes using the
 * {@code @Autowired} annotation.
 */
@Component
public class Components {
    /**
     * This factory method returns a new {@link RestTemplate}, which
     * enables a client to perform HTTP requests synchronously.
     *
     * @return A new {@link RestTemplate} configured to use the {@code
     *         SERVER_BASE_URL}
     */
    @Bean
    public RestTemplate getRestTemplate() {
        // Create a new RestTemplate.
        var restTemplate = new RestTemplate();

        restTemplate
                .setErrorHandler(new RestTemplateResponseErrorHandler());

        restTemplate
            // Set the base URL for the RestTemplate.
            .setUriTemplateHandler(new DefaultUriBuilderFactory(SERVER_BASE_URL));

        // Return restTemplate.
        return restTemplate;
    }
}
