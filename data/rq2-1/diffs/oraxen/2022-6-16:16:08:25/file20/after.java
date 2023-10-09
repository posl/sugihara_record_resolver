package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ItemParser {

    private static final Map<String, ModelData> MODEL_DATAS_BY_ID = new HashMap<>();

    private final OraxenMeta oraxenMeta;
    private final ConfigurationSection section;
    private Material type;
    private WrappedMMOItem mmoItem;
    private boolean configUpdated = false;

    public ItemParser(ConfigurationSection section) {
        this.section = section;

        if (section.isConfigurationSection("mmoitem"))
            mmoItem = new WrappedMMOItem(section.getConfigurationSection("mmoitem"));
        else
            type = Material.getMaterial(section.getString("material"));

        oraxenMeta = new OraxenMeta();
        if (section.isConfigurationSection("Pack")) {
            ConfigurationSection packSection = section.getConfigurationSection("Pack");
            oraxenMeta.setPackInfos(packSection);
            if (packSection.isInt("custom_model_data"))
                MODEL_DATAS_BY_ID
                        .put(section.getName(),
                                new ModelData(type, oraxenMeta.getModelName(),
                                        packSection.getInt("custom_model_data")));
        }
    }

    public boolean usesMMOItems() {
        return type == null;
    }

    private String parseComponentString(String miniString) {
        return Utils.LEGACY_COMPONENT_SERIALIZER.serialize(Utils.MINI_MESSAGE
                .parse(miniString));
    }

    public ItemBuilder buildItem() {
        return buildItem(section.contains("displayname") ? parseComponentString(section.getString("displayname")) : null);
    }

    public ItemBuilder buildItem(String name) {
        ItemBuilder item = usesMMOItems() ? new ItemBuilder(mmoItem) : new ItemBuilder(type);
        if (name != null)
            item.setDisplayName(name);
        return applyConfig(item);
    }

    private ItemBuilder applyConfig(ItemBuilder item) {

        if (section.contains("durability"))
            item.setDurability((short) section.getInt("durability"));

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            for (int i = 0; i < lore.size(); i++)
                lore.set(i, parseComponentString(lore.get(i)));
            item.setLore(lore);
        }

        if (section.contains("unbreakable"))
            item.setUnbreakable(section.getBoolean("unbreakable"));

        if (section.contains("color")) {
            String[] colors = section.getString("color").split(", ");
            item
                    .setColor(org.bukkit.Color
                            .fromRGB(Integer.parseInt(colors[0]),
                                    Integer.parseInt(colors[1]),
                                    Integer.parseInt(colors[2])));
        }

        parseMiscOptions(item);
        parseVanillaSections(item);
        parseOraxenSections(item);
        item.setOraxenMeta(oraxenMeta);
        return item;
    }

    private void parseMiscOptions(ItemBuilder item) {
        if (section.contains("no_auto_update"))
            oraxenMeta.setNoUpdate(section.getBoolean("no_auto_update"));

        if (section.contains("excludeFromInventory") && section.getBoolean("excludeFromInventory"))
            oraxenMeta.setExcludedFromInventory();

        if (!section.contains("injectID") || section.getBoolean("injectId"))
            item
                    .setCustomTag(new NamespacedKey(OraxenPlugin.get(), "id"), PersistentDataType.STRING,
                            section.getName());
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private void parseVanillaSections(ItemBuilder item) {

        if (section.contains("ItemFlags")) {
            List<String> itemFlags = section.getStringList("ItemFlags");
            for (String itemFlag : itemFlags)
                item.addItemFlags(ItemFlag.valueOf(itemFlag));
        }

        if (section.contains("PotionEffects")) {
            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
            List<LinkedHashMap<String, Object>> potionEffects = (List<LinkedHashMap<String, Object>>) section
                    .getList("PotionEffects");
            for (Map<String, Object> serializedPotionEffect : potionEffects) {
                PotionEffectType effect = PotionEffectType.getByName((String) serializedPotionEffect.get("type"));
                int duration = (int) serializedPotionEffect.get("duration");
                int amplifier = (int) serializedPotionEffect.get("amplifier");
                boolean ambient = (boolean) serializedPotionEffect.get("ambient");
                boolean particles = (boolean) serializedPotionEffect.get("particles");
                boolean icon = (boolean) serializedPotionEffect.get("icon");
                item.addPotionEffect(new PotionEffect(effect, duration, amplifier, ambient, particles, icon));
            }
        }

        if (section.contains("PersistentData")) {
            try {
                List<LinkedHashMap<String, Object>> dataHolder = (List<LinkedHashMap<String, Object>>) section
                        .getList("PersistentData");
                for (LinkedHashMap<String, Object> attributeJson : dataHolder) {
                    String[] keyContent = ((String) attributeJson.get("key")).split(":");
                    final Object persistentDataType = PersistentDataType.class
                            .getDeclaredField((String) attributeJson.get("type")).get(null);
                    item.addCustomTag(new NamespacedKey(keyContent[0], keyContent[1]),
                            (PersistentDataType) persistentDataType,
                            attributeJson.get("value"));
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        if (section.contains("AttributeModifiers")) {
            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
            List<LinkedHashMap<String, Object>> attributes = (List<LinkedHashMap<String, Object>>) section
                    .getList("AttributeModifiers");
            for (LinkedHashMap<String, Object> attributeJson : attributes) {
                AttributeModifier attributeModifier = AttributeModifier.deserialize(attributeJson);
                Attribute attribute = Attribute.valueOf((String) attributeJson.get("attribute"));
                item.addAttributeModifiers(attribute, attributeModifier);
            }
        }

        if (section.contains("Enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("Enchantments");
            for (String enchant : enchantSection.getKeys(false))
                item
                        .addEnchant(EnchantmentWrapper.getByKey(NamespacedKey.minecraft(enchant)),
                                enchantSection.getInt(enchant));
        }
    }

    private void parseOraxenSections(ItemBuilder item) {

        if (section.isConfigurationSection("Mechanics")) {
            ConfigurationSection mechanicsSection = section.getConfigurationSection("Mechanics");
            for (String mechanicID : mechanicsSection.getKeys(false)) {
                MechanicFactory factory = MechanicsManager.getMechanicFactory(mechanicID);
                if (factory != null) {
                    Mechanic mechanic = factory.parse(mechanicsSection.getConfigurationSection(mechanicID));
                    // Apply item modifiers
                    for (Function<ItemBuilder, ItemBuilder> itemModifier : mechanic.getItemModifiers())
                        item = itemModifier.apply(item);
                }
            }
        }

        if (oraxenMeta.hasPackInfos()) {
            int customModelData;
            if (MODEL_DATAS_BY_ID.containsKey(section.getName()))
                customModelData = MODEL_DATAS_BY_ID.get(section.getName()).getDurability();
            else {
                customModelData = ModelData.generateId(oraxenMeta.getModelName(), type);
                if (Settings.AUTOMATICALLY_SET_MODEL_DATA.toBool()) {
                    configUpdated = true;
                    section.getConfigurationSection("Pack").set("custom_model_data", customModelData);
                }
            }
            item.setCustomModelData(customModelData);
            oraxenMeta.setCustomModelData(customModelData);
        }
    }

    public boolean isConfigUpdated() {
        return configUpdated;
    }

}
