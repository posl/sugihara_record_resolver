package stroom.proxy.app;

import stroom.proxy.app.forwarder.ForwardHttpPostConfig;
import stroom.util.io.FileUtil;
import stroom.util.logging.LogUtil;

import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(DropwizardExtensionsSupport.class)
public abstract class AbstractApplicationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationTest.class);

    private static final Config config;

    static {
        config = loadYamlFile("proxy-dev.yml");

        // The key/trust store paths will not be available in travis so null them out
        config.getProxyConfig()
                .getForwardDestinations()
                .forEach(forwardDestinationConfig -> {
                    if (forwardDestinationConfig instanceof ForwardHttpPostConfig) {
                        ((ForwardHttpPostConfig) forwardDestinationConfig).setSslConfig(null);
                    }
                });

        config.getProxyConfig()
                .getRestClientConfig()
                .setTlsConfiguration(null);

        // If the home/temp paths don't exist then startup will exit, killing the rest of the tests
        final ProxyPathConfig proxyPathConfig = config.getProxyConfig().getPathConfig();
        try {
            final Path temp = Files.createTempDirectory("stroom-proxy");
            final Path homeDir = temp.resolve("home");
            final Path tempDir = temp.resolve("temp");
            Files.createDirectories(homeDir);
            Files.createDirectories(tempDir);

            proxyPathConfig.setHome(FileUtil.getCanonicalPath(homeDir));
            proxyPathConfig.setTemp(FileUtil.getCanonicalPath(tempDir));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Path proxyHomeDir = Paths.get(proxyPathConfig.getHome());
        final Path proxyTempDir = Paths.get(proxyPathConfig.getTemp());

        try {
            Files.createDirectories(proxyHomeDir);
        } catch (IOException e) {
            LOGGER.error("Error creating home directory {}", proxyHomeDir.toAbsolutePath(), e);
        }

        try {
            Files.createDirectories(proxyTempDir);
        } catch (IOException e) {
            LOGGER.error("Error creating temp directory {}", proxyTempDir.toAbsolutePath(), e);
        }
    }

    static Config getConfig() {
        return config;
    }

    private static Config readConfig(final Path configFile) {
        final ConfigurationSourceProvider configurationSourceProvider = ProxyYamlUtil.createConfigurationSourceProvider(
                new FileConfigurationSourceProvider(),
                true);

        final ConfigurationFactoryFactory<Config> configurationFactoryFactory =
                new DefaultConfigurationFactoryFactory<>();

        final ConfigurationFactory<Config> configurationFactory = configurationFactoryFactory
                .create(
                        Config.class,
                        io.dropwizard.jersey.validation.Validators.newValidator(),
                        Jackson.newObjectMapper(),
                        "dw");

        Config config = null;
        try {
            config = configurationFactory.build(configurationSourceProvider, configFile.toAbsolutePath().toString());
        } catch (ConfigurationException | IOException e) {
            throw new RuntimeException(LogUtil.message("Error parsing configuration from file {}",
                    configFile.toAbsolutePath()), e);
        }

        return config;
    }

    private static Config loadYamlFile(final String filename) {
        Path path = getStroomProxyAppFile(filename);

        return readConfig(path);
    }

    private static Path getStroomProxyAppFile(final String filename) {
        final String codeSourceLocation = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        LOGGER.info(codeSourceLocation);

        Path path = Paths.get(codeSourceLocation);
        while (path != null && !path.getFileName().toString().equals("stroom-proxy-app")) {
            path = path.getParent();
        }
        if (path != null) {
            path = path.resolve(filename);
        }

        if (path == null) {
            throw new RuntimeException("Unable to find " + filename);
        }
        return path;
    }

}
