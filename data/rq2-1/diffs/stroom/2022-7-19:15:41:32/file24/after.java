package stroom.app.commands;

import stroom.app.BootstrapUtil;
import stroom.app.guice.AppModule;
import stroom.config.app.Config;
import stroom.util.guice.GuiceUtil;

import com.google.inject.Injector;
import com.google.inject.Key;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import javax.sql.DataSource;

/**
 * Additional DW Command so instead of
 * ... server ../local.yml
 * you can do
 * ... migrate ../local.yml
 * and it will run all the DB migrations without running up the app
 */
public abstract class AbstractStroomAccountConfiguredCommand extends ConfiguredCommand<Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStroomAccountConfiguredCommand.class);

    private final Path configFile;

    public AbstractStroomAccountConfiguredCommand(final Path configFile,
                                                  final String commandName,
                                                  final String description) {
        super(commandName, description);
        this.configFile = configFile;
    }

    @Override
    protected void run(final Bootstrap<Config> bootstrap,
                       final Namespace namespace,
                       final Config config) throws Exception {

        LOGGER.info("Using application configuration file {}",
                configFile.toAbsolutePath().normalize());

        try {
            LOGGER.debug("Creating bootstrap injector");
            final Injector bootstrapInjector = BootstrapUtil.bootstrapApplication(
                    config, configFile);

            LOGGER.debug("Creating app injector");
            final Injector appInjector = bootstrapInjector.createChildInjector(
                    new AppModule());

            // Force guice to get all datasource instances from the multibinder
            // so the migration will be run for each stroom module
            // Relies on all db modules adding an entry to the multibinder
            appInjector.getInstance(Key.get(GuiceUtil.setOf(DataSource.class)));

            LOGGER.info("DB migration complete");

            runCommand(bootstrap, namespace, config, appInjector);
        } catch (Exception e) {
            LOGGER.error("Error initialising application", e);
            System.exit(1);
        }
    }

    protected abstract void runCommand(final Bootstrap<Config> bootstrap,
                                       final Namespace namespace,
                                       final Config config,
                                       final Injector injector);
}
