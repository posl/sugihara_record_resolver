package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigsManager {

    private final JavaPlugin plugin;
    private final YamlConfiguration defaultSettings;
    private final YamlConfiguration defaultFont;
    private final YamlConfiguration defaultLanguage;
    private YamlConfiguration settings;
    private YamlConfiguration font;
    private YamlConfiguration language;
    private File itemsFolder;

    public ConfigsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        defaultSettings = extractDefault("settings.yml");
        defaultFont = extractDefault("font.yml");
        defaultLanguage = extractDefault("languages/english.yml");
    }

    public YamlConfiguration getSettings() {
        return settings != null ? settings : defaultSettings;
    }

    public YamlConfiguration getLanguage() {
        return language != null ? language : defaultLanguage;
    }

    public YamlConfiguration getFont() {
        return font != null ? font : defaultFont;
    }

    private YamlConfiguration extractDefault(String source) {
        InputStreamReader inputStreamReader = new InputStreamReader(plugin.getResource(source));
        try {
            return YamlConfiguration.loadConfiguration(inputStreamReader);
        } finally {
            try {
                inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean validatesConfig() {
        ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
        settings = validate(resourcesManager, "settings.yml", defaultSettings);
        font = validate(resourcesManager, "font.yml", defaultFont);
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        languagesFolder.mkdir();
        String languageFile = "languages/" + settings.getString(Settings.PLUGIN_LANGUAGE.getPath()) + ".yml";
        language = validate(resourcesManager, languageFile, defaultLanguage);

        // check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("items", "yml");
        }

        return true; // todo : return false when an error is detected + prints a detailed error
    }

    private YamlConfiguration validate(ResourcesManager resourcesManager, String configName, YamlConfiguration defaultConfiguration) {
        File configurationFile = resourcesManager.extractConfiguration(configName);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile);
        boolean updated = false;
        for (String key : defaultConfiguration.getKeys(true))
            if (configuration.get(key) == null) {
                updated = true;
                Message.UPDATING_CONFIG.log();
                configuration.set(key, defaultConfiguration.get(key));
            }
        if (updated)
            try {
                configuration.save(configurationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        return configuration;
    }


    public Map<File, Map<String, ItemBuilder>> parsesConfigs() {
        Map<File, Map<String, ItemBuilder>> parseMap = new LinkedHashMap<>();
        List<File> configs = Arrays
                .stream(getItemsFiles())
                .filter(file -> file.getName().endsWith(".yml"))
                .collect(Collectors.toList());
        for (File file : configs) {
            parseMap.put(file, parsesConfig(YamlConfiguration.loadConfiguration(file), file));
        }
        return parseMap;
    }

    public Map<String, ItemBuilder> parsesConfig(YamlConfiguration config, File itemFile) {
        Map<String, ItemParser> parseMap = new LinkedHashMap<>();
        ItemParser errorItem = new ItemParser(Settings.ERROR_ITEM.toConfigSection());
        for (String itemSectionName : config.getKeys(false)) {
            if (!config.isConfigurationSection(itemSectionName))
                continue;
            ConfigurationSection itemSection = config.getConfigurationSection(itemSectionName);
            parseMap.put(itemSectionName, new ItemParser(itemSection));
        }
        boolean configUpdated = false;
        // because we must have parse all the items before building them to be able to
        // use available models
        Map<String, ItemBuilder> map = new LinkedHashMap<>();
        for (Map.Entry<String, ItemParser> entry : parseMap.entrySet()) {
            ItemParser itemParser = entry.getValue();
            try {
                map.put(entry.getKey(), itemParser.buildItem());
            } catch (Exception e) {
                map
                        .put(entry.getKey(),
                                errorItem
                                        .buildItem(String.valueOf(ChatColor.DARK_RED) + ChatColor.BOLD
                                                + e.getClass().getSimpleName() + ": " + ChatColor.RED + entry.getKey()));
                Logs.logError("ERROR BUILDING ITEM \"" + entry.getKey() + "\"");
                e.printStackTrace();
            }
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }
        if (configUpdated)
            try {
                config.save(itemFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        return map;
    }

    private File[] getItemsFiles() {
        File[] itemsConfig = itemsFolder.listFiles();
        Arrays.sort(itemsConfig);
        return itemsConfig;
    }

}
