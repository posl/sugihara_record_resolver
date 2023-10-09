package stroom.test;

import stroom.config.app.AppConfig;
import stroom.config.app.AppConfigModule;
import stroom.config.app.Config;
import stroom.config.app.ConfigHolder;
import stroom.config.global.impl.ConfigMapper;
import stroom.test.common.util.db.DbTestUtil;
import stroom.util.config.AbstractConfigUtil;
import stroom.util.io.FileUtil;
import stroom.util.io.StroomPathConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AppConfigTestModule extends AppConfigModule {

    private final ConfigMapperSpy configMapperSpy;

    public AppConfigTestModule() {
        super(new ConfigHolderImpl());
        configMapperSpy = new ConfigMapperSpy(super.getConfigHolder());
    }

    public AppConfigTestModule(final ConfigHolder configHolder) {
        super(configHolder);
        configMapperSpy = new ConfigMapperSpy(configHolder);
    }

    @Override
    protected void configure() {
        super.configure();

        bind(ConfigMapper.class).toInstance(configMapperSpy);
        // Also bind instance to its superclass
        bind(ConfigMapperSpy.class).toInstance(configMapperSpy);
    }

//    @Provides
//    public Config getConfig() {
//        return ((ConfigHolderImpl) getConfigHolder()).getConfig();
//    }


    // --------------------------------------------------------------------------------


    private static class ConfigHolderImpl implements ConfigHolder {

        private final Config config;
        private final AppConfig appConfig;
        private final Path path;

        ConfigHolderImpl() {
            try {
                final String gradleWorker = DbTestUtil.getGradleWorker();
                final String prefix = "stroom_" + gradleWorker + "_";
                final Path dir = Files.createTempDirectory(prefix);
                this.path = dir.resolve("test.yml");

                final AppConfig vanillaAppConfig = new AppConfig();

                final StroomPathConfig modifiedPathConfig = vanillaAppConfig.getPathConfig()
                        .withHome(FileUtil.getCanonicalPath(dir))
                        .withTemp(FileUtil.getCanonicalPath(dir));
                final AppConfig modifiedAppConfig = AbstractConfigUtil.mutateTree(
                        vanillaAppConfig,
                        AppConfig.ROOT_PROPERTY_PATH,
                        Map.of(AppConfig.ROOT_PROPERTY_PATH.merge(AppConfig.PROP_NAME_PATH), modifiedPathConfig));

                this.appConfig = modifiedAppConfig;
                this.config = new Config();
                this.config.setYamlAppConfig(modifiedAppConfig);
            } catch (final IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }

        @Override
        public AppConfig getBootStrapConfig() {
            return appConfig;
        }

        @Override
        public Path getConfigFile() {
            return path;
        }

        public Config getConfig() {
            return config;
        }
    }
}
