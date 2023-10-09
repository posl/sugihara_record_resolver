package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsView {

    private final YamlConfiguration settings = new ResourcesManager(OraxenPlugin.get()).getSettings();
    private final FontManager fontManager = OraxenPlugin.get().getFontManager();
    private final String baseMenuTexture = ChatColor.WHITE +
            String.valueOf(fontManager.getGlyphFromName("menu_items_shift").character()) +
            fontManager.getGlyphFromName("menu_items").character() +
            fontManager.getGlyphFromName("menu_items_overlay_shift").character();
    ChestGui mainGui;

    public ChestGui create() {
        final Map<File, ChestGui> files = new HashMap<>();
        for (final File file : OraxenItems.getMap().keySet()) {
            final List<ItemBuilder> unexcludedItems = OraxenItems.getUnexcludedItems(file);
            if (unexcludedItems.size() > 0)
                files.put(file, createSubGUI(file.getName(), unexcludedItems));
        }
        final int rows = (files.size() - 1) / 9 + 1;
        mainGui = new ChestGui(6, getMenuTexture("<#84CBFF>"));
        final StaticPane filesPane = new StaticPane(0, 0, 9, rows);
        int i = 0;
        for (final var entry : files.entrySet()) {
            final GuiItem item = new GuiItem(getItemStack(entry.getKey()), event ->
                    entry.getValue().show(event.getWhoClicked()));
            filesPane.addItem(item, i % 9, i / 9);
            i++;
        }

        mainGui.addPane(filesPane);
        mainGui.setOnTopClick(event -> event.setCancelled(true));
        return mainGui;
    }

    private ChestGui createSubGUI(final String fileName, final List<ItemBuilder> items) {
        final int rows = Math.min((items.size() - 1) / 9 + 2, 6);
        final ChestGui gui = new ChestGui(6,
                getMenuTexture(settings.getString(String.format("gui_inventory.%s.color",
                        Utils.removeExtension(fileName)))));
        final PaginatedPane pane = new PaginatedPane(9, rows);

        for (int i = 0; i < (items.size() - 1) / 45 + 1; i++) {
            final List<ItemStack> itemStackList = extractPageItems(items, i);
            final StaticPane staticPane = new StaticPane(9, Math.min((itemStackList.size() - 1) / 9 + 1, 5));
            for (int itemIndex = 0; itemIndex < itemStackList.size(); itemIndex++) {
                final ItemStack oraxenItem = itemStackList.get(itemIndex);
                staticPane.addItem(new GuiItem(oraxenItem,
                                event -> event.getWhoClicked().getInventory().addItem(oraxenItem)),
                        itemIndex % 9, itemIndex / 9);
            }
            pane.addPane(i, staticPane);
        }


        //page selection
        final StaticPane back = new StaticPane(2, 5, 1, 1);
        final StaticPane forward = new StaticPane(6, 5, 1, 1);
        final StaticPane exit = new StaticPane(4, 5, 9, 1);

        back.addItem(new GuiItem((OraxenItems.getItemById("arrow_previous_icon") == null
                ? new ItemBuilder(Material.ARROW)
                : OraxenItems.getItemById("arrow_previous_icon")).build(), event -> {
            pane.setPage(pane.getPage() - 1);

            if (pane.getPage() == 0) back.setVisible(false);

            forward.setVisible(true);
            gui.update();
        }), 0, 0);

        back.setVisible(false);

        forward.addItem(new GuiItem((OraxenItems.getItemById("arrow_next_icon") == null
                ? new ItemBuilder(Material.ARROW)
                : OraxenItems.getItemById("arrow_next_icon")).build(), event -> {
            pane.setPage(pane.getPage() + 1);
            if (pane.getPage() == pane.getPages() - 1) forward.setVisible(false);

            back.setVisible(true);
            gui.update();
        }), 0, 0);
        if (pane.getPages() <= 1)
            forward.setVisible(false);

        exit.addItem(new GuiItem((OraxenItems.getItemById("exit_icon") == null
                ? new ItemBuilder(Material.BARRIER)
                : OraxenItems.getItemById("exit_icon"))
                .build(), event ->
                mainGui.show(event.getWhoClicked())
        ), 0, 0);

        gui.addPane(back);
        gui.addPane(forward);
        gui.addPane(exit);
        gui.addPane(pane);
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        return gui;
    }

    private List<ItemStack> extractPageItems(final List<ItemBuilder> items, final int page) {
        final List<ItemStack> output = new ArrayList<>();
        for (int i = page * 45; i < (page + 1) * 45 && i < items.size(); i++) output.add(items.get(i).build());
        return output;
    }

    private ItemStack getItemStack(final File file) {
        ItemStack itemStack;
        String material = settings
                .getString(String.format("gui_inventory.%s.icon", Utils.removeExtension(file.getName())),
                        "PAPER");
        if (material == null)
            material = "PAPER";

        try {
            itemStack = new ItemBuilder(OraxenItems.getItemById(material).build())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setDisplayName(ChatColor.GREEN + file.getName())
                    .setLore(new ArrayList<>())
                    .build();
        } catch (final Exception e) {
            try {
                itemStack = new ItemBuilder(Material.getMaterial(material.toUpperCase()))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setDisplayName(ChatColor.GREEN + file.getName())
                        .build();
            } catch (final Exception ignored) {
                itemStack = new ItemBuilder(Material.PAPER)
                        .setDisplayName(ChatColor.GREEN + file.getName())
                        .build();
            }
        }

        if (itemStack == null)
            // avoid possible bug if isOraxenItems is available but can't be an itemstack
            itemStack = new ItemBuilder(Material.PAPER).setDisplayName(ChatColor.GREEN + file.getName()).build();

        return itemStack;
    }

    private String getMenuTexture(final String color) {
        return baseMenuTexture + Utils.LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.get()
                .parse(color + fontManager.getGlyphFromName("menu_items_overlay").character()));
    }
}
