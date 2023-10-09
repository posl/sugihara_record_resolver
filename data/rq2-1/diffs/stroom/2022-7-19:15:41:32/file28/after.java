package stroom.app.guice;

import stroom.config.app.AppConfigModule;
import stroom.config.app.Config;
import stroom.config.app.ConfigHolder;
import stroom.config.app.ConfigHolderImpl;
import stroom.config.global.impl.GlobalConfigBootstrapModule;
import stroom.config.global.impl.db.GlobalConfigDaoModule;
import stroom.db.util.DbModule;
import stroom.util.io.DirProvidersModule;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import io.dropwizard.setup.Environment;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class BootStrapModule extends AbstractModule {

    private final Config configuration;
    private final Environment environment;
    private final ConfigHolder configHolder;
    private final Supplier<AbstractModule> dbModuleSupplier;
    private final Function<ConfigHolder, AppConfigModule> appConfigModuleFunc;

    public BootStrapModule(final Config configuration,
                           final Environment environment,
                           final ConfigHolder configHolder,
                           final Supplier<AbstractModule> dbModuleSupplier,
                           final Function<ConfigHolder, AppConfigModule> appConfigModuleFunc) {
        this.configuration = configuration;
        this.environment = environment;
        this.configHolder = configHolder;
        this.dbModuleSupplier = dbModuleSupplier;
        this.appConfigModuleFunc = appConfigModuleFunc;
    }

    public BootStrapModule(final Config configuration,
                           final Environment environment,
                           final Path configFile,
                           final Supplier<AbstractModule> dbModuleSupplier,
                           final Function<ConfigHolder, AppConfigModule> appConfigModuleFunc) {
        this(
                configuration,
                environment,
                new ConfigHolderImpl(configuration.getYamlAppConfig(), configFile),
                dbModuleSupplier,
                appConfigModuleFunc);
    }

    public BootStrapModule(final Config configuration,
                           final Environment environment,
                           final Path configFile) {
        this(configuration,
                environment,
                configFile,
                DbModule::new,
                AppConfigModule::new);
    }

    public BootStrapModule(final Config configuration,
                           final Path configFile) {
        this(configuration,
                null,
                configFile,
                DbModule::new,
                AppConfigModule::new);
    }

    @Override
    protected void configure() {
        super.configure();

        // The binds in here need to be the absolute bare minimum to get the DB
        // datasources connected and read all the DB based config props.

        bind(Config.class).toInstance(configuration);

        final AppConfigModule appConfigModule = appConfigModuleFunc.apply(configHolder);
        install(appConfigModule);

        // These are needed so the Hikari pools can register metrics/health checks
        bindMetricsAndHealthChecksRegistries();

        install(dbModuleSupplier.get());

        install(new DbConnectionsModule());

        // Any DAO/Service modules that we must have
        install(new GlobalConfigBootstrapModule());
        install(new GlobalConfigDaoModule());
        install(new DirProvidersModule());
    }

    protected Config getConfiguration() {
        return configuration;
    }

    protected ConfigHolder getConfigHolder() {
        return configHolder;
    }

    protected Environment getEnvironment() {
        return environment;
    }

    private void bindMetricsAndHealthChecksRegistries() {
        final HealthCheckRegistry healthCheckRegistry;
        final MetricRegistry metricRegistry;
        if (environment != null) {
            // Make the various DW objects available, bind them individually so
            // modules don't need to pull in all of DW just for metrics.
            bind(Environment.class).toInstance(environment);
            metricRegistry = environment.metrics();
            healthCheckRegistry = environment.healthChecks();
        } else {
            // Allows us to load up the app in the absence of a the DW jersey environment
            // e.g. for migrations
            // Just use brand new registries so code works. We don't care what gets written to
            // those registries.
            metricRegistry = new MetricRegistry();
            healthCheckRegistry = new HealthCheckRegistry();
        }
        bind(MetricRegistry.class).toInstance(metricRegistry);
        bind(HealthCheckRegistry.class).toInstance(healthCheckRegistry);
    }
}
