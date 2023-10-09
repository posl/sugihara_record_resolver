package stroom.config.global.impl.db;

import stroom.config.global.impl.ConfigPropertyDao;
import stroom.config.global.shared.ConfigProperty;
import stroom.config.impl.db.jooq.tables.records.ConfigRecord;
import stroom.db.util.GenericDao;
import stroom.db.util.JooqUtil;
import stroom.util.logging.LogUtil;
import stroom.util.shared.PropertyPath;

import org.jooq.Record;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.inject.Inject;

import static stroom.config.impl.db.jooq.tables.Config.CONFIG;

class ConfigPropertyDaoImpl implements ConfigPropertyDao {

    private static final Function<Record, ConfigProperty> RECORD_TO_CONFIG_PROPERTY_MAPPER = record -> {
        final ConfigProperty configProperty = new ConfigProperty(PropertyPath.fromPathString(record.get(CONFIG.NAME)));
        configProperty.setId(record.get(CONFIG.ID));
        configProperty.setVersion(record.get(CONFIG.VERSION));
        configProperty.setCreateTimeMs(record.get(CONFIG.CREATE_TIME_MS));
        configProperty.setCreateUser(record.get(CONFIG.CREATE_USER));
        configProperty.setUpdateTimeMs(record.get(CONFIG.UPDATE_TIME_MS));
        configProperty.setUpdateUser(record.get(CONFIG.UPDATE_USER));
        String value = record.get(CONFIG.VAL);
        // value col is not-null
        if (value.isEmpty()) {
            value = null;
        }
        configProperty.setDatabaseOverrideValue(value);
        return configProperty;
    };

    private static final BiFunction<ConfigProperty, ConfigRecord, ConfigRecord> CONFIG_PROPERTY_TO_RECORD_MAPPER =
            (configProperty, record) -> {
                record.from(configProperty);
                record.set(CONFIG.ID, configProperty.getId());
                record.set(CONFIG.VERSION, configProperty.getVersion());
                record.set(CONFIG.CREATE_TIME_MS, configProperty.getCreateTimeMs());
                record.set(CONFIG.CREATE_USER, configProperty.getCreateUser());
                record.set(CONFIG.UPDATE_TIME_MS, configProperty.getUpdateTimeMs());
                record.set(CONFIG.UPDATE_USER, configProperty.getUpdateUser());
                record.set(CONFIG.NAME, configProperty.getNameAsString());
                // DB doesn't allow null values so use empty string
                if (!configProperty.hasDatabaseOverride()) {
                    // If there is no value override then we don't want it in the DB
                    // Code further up the chain should have dealt with this
                    throw new RuntimeException(LogUtil.message(
                            "Trying to save a config record when there is no databaseValue {}",
                            configProperty));
                }
                record.set(CONFIG.VAL, configProperty.getDatabaseOverrideValue().getValueOrElse(""));
                return record;
            };

    private final GlobalConfigDbConnProvider globalConfigDbConnProvider;
    private final GenericDao<ConfigRecord, ConfigProperty, Integer> genericDao;

    @Inject
    ConfigPropertyDaoImpl(final GlobalConfigDbConnProvider globalConfigDbConnProvider) {
        this.globalConfigDbConnProvider = globalConfigDbConnProvider;
        this.genericDao = new GenericDao<>(
                globalConfigDbConnProvider,
                CONFIG,
                CONFIG.ID,
                CONFIG_PROPERTY_TO_RECORD_MAPPER,
                RECORD_TO_CONFIG_PROPERTY_MAPPER);
    }

    @Override
    public ConfigProperty create(final ConfigProperty configProperty) {
        return genericDao.create(configProperty);
    }

    @Override
    public Optional<ConfigProperty> fetch(final int id) {
        return genericDao.fetch(id);
    }

    @Override
    public Optional<ConfigProperty> fetch(final String propertyName) {
        Objects.requireNonNull(propertyName);
        return JooqUtil.contextResult(globalConfigDbConnProvider, context -> context
                        .selectFrom(CONFIG)
                        .where(CONFIG.NAME.eq(propertyName))
                        .fetchOptional())
                .map(RECORD_TO_CONFIG_PROPERTY_MAPPER);
    }

    @Override
    public ConfigProperty update(final ConfigProperty configProperty) {
        return genericDao.update(configProperty);
    }

    @Override
    public boolean delete(final int id) {
        return genericDao.delete(id);
    }

    @Override
    public boolean delete(final String name) {
        return JooqUtil.contextResult(globalConfigDbConnProvider, context -> context
                .deleteFrom(CONFIG)
                .where(CONFIG.NAME.eq(name))
                .execute()) > 0;
    }

    @Override
    public boolean delete(final PropertyPath propertyPath) {
        return delete(propertyPath.toString());
    }

    @Override
    public List<ConfigProperty> list() {
        return JooqUtil.contextResult(globalConfigDbConnProvider, context -> context
                        .fetch(CONFIG))
                .map(RECORD_TO_CONFIG_PROPERTY_MAPPER::apply);
    }
}
