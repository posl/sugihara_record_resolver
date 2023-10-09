package org.apereo.cas.configuration;

import org.apereo.cas.configuration.support.RelaxedPropertyNames;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * This is {@link CasConfigurationPropertiesEnvironmentManager}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */

@Slf4j
@RequiredArgsConstructor
@Getter
public class CasConfigurationPropertiesEnvironmentManager {

    /**
     * Property name passed to the environment that indicates the path to the standalone configuration file.
     */
    public static final String PROPERTY_CAS_STANDALONE_CONFIGURATION_FILE = "cas.standalone.configuration-file";

    /**
     * Property name passed to the environment that indicates the path to the standalone configuration directory.
     */
    public static final String PROPERTY_CAS_STANDALONE_CONFIGURATION_DIRECTORY = "cas.standalone.configuration-directory";

    /**
     * Configuration directories for CAS, listed in order.
     */
    private static final File[] DEFAULT_CAS_CONFIG_DIRECTORIES = {
        new File("/etc/cas/config"),
        new File("/opt/cas/config"),
        new File("/var/cas/config")
    };

    private final ConfigurationPropertiesBindingPostProcessor binder;

    /**
     * Rebind cas configuration properties.
     *
     * @param binder             the binder
     * @param applicationContext the application context
     * @return the application context
     */
    public static ApplicationContext rebindCasConfigurationProperties(final ConfigurationPropertiesBindingPostProcessor binder,
                                                                      final ApplicationContext applicationContext) {
        val config = applicationContext.getBean(CasConfigurationProperties.class);
        val name = String.format("%s-%s", CasConfigurationProperties.PREFIX, config.getClass().getName());
        binder.postProcessBeforeInitialization(config, name);
        val bean = applicationContext.getAutowireCapableBeanFactory().initializeBean(config, name);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
        LOGGER.debug("Reloaded CAS configuration [{}]", name);
        return applicationContext;
    }

    /**
     * Rebind cas configuration properties.
     *
     * @param applicationContext the application context
     * @return the application context
     */
    public ApplicationContext rebindCasConfigurationProperties(final ApplicationContext applicationContext) {
        return rebindCasConfigurationProperties(this.binder, applicationContext);
    }

    /**
     * Gets standalone profile configuration directory.
     *
     * @param environment the environment
     * @return the standalone profile configuration directory
     */
    public File getStandaloneProfileConfigurationDirectory(final Environment environment) {
        val values = new LinkedHashSet<>(RelaxedPropertyNames.forCamelCase(PROPERTY_CAS_STANDALONE_CONFIGURATION_DIRECTORY).getValues());
        values.add(PROPERTY_CAS_STANDALONE_CONFIGURATION_DIRECTORY);

        val file = values
            .stream()
            .map(key -> environment.getProperty(key, File.class))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (file != null && file.exists()) {
            LOGGER.trace("Received standalone configuration directory [{}]", file);
            return file;
        }

        return Arrays.stream(DEFAULT_CAS_CONFIG_DIRECTORIES)
            .filter(File::exists)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets standalone profile configuration file.
     *
     * @param environment the environment
     * @return the standalone profile configuration file
     */
    public File getStandaloneProfileConfigurationFile(final Environment environment) {
        val values = new LinkedHashSet<>(RelaxedPropertyNames.forCamelCase(PROPERTY_CAS_STANDALONE_CONFIGURATION_FILE).getValues());
        values.add(PROPERTY_CAS_STANDALONE_CONFIGURATION_FILE);

        return values
            .stream()
            .map(key -> environment.getProperty(key, File.class))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets application name.
     *
     * @param environment the environment
     * @return the application name
     */
    public String getApplicationName(final Environment environment) {
        return environment.getProperty("spring.application.name", "cas");
    }

    /**
     * Gets configuration name.
     *
     * @param environment the environment
     * @return the configuration name
     */
    public String getConfigurationName(final Environment environment) {
        return environment.getProperty("spring.config.name", "cas");
    }
}
