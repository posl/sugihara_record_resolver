package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanicFactory;
import net.kyori.adventure.text.minimessage.Template;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RepairCommand {

    public CommandAPICommand getRepairCommand() {
        return new CommandAPICommand("repair")
                .withPermission("oraxen.command.repair")
                .withArguments(new TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("hand", "all")))
                .executes((sender, args) -> {

                    if (sender instanceof Player player) if ((args[0]).equals("hand")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item == null || item.getType() == Material.AIR) {
                            Message.CANNOT_BE_REPAIRED_INVALID.send(sender);
                            return;
                        }
                        if (repairPlayerItem(item))
                            Message.CANNOT_BE_REPAIRED.send(sender);

                    } else if (player.hasPermission("oraxen.command.repair.all")) {
                        ItemStack[] items = (ItemStack[]) ArrayUtils.addAll(player.getInventory().getStorageContents(),
                                player.getInventory().getArmorContents());
                        int failed = 0;
                        for (ItemStack item : items) {
                            if (item == null || item.getType() == Material.AIR)
                                continue;
                            if (repairPlayerItem(item)) {
                                Message.CANNOT_BE_REPAIRED.send(sender);
                                failed++;
                            }
                        }
                        Message.REPAIRED_ITEMS.send(sender,
                                Template.template("amount", String.valueOf(items.length - failed)));
                    } else
                        Message.NO_PERMISSION.send(sender,
                                Template.template("permission", "oraxen.command.repair.all"));
                    else
                        Message.NOT_PLAYER.send(sender);
                });
    }


    private static boolean repairPlayerItem(ItemStack itemStack) {
        String itemId = OraxenItems.getIdByItem(itemStack);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable))
            return true;
        DurabilityMechanicFactory durabilityFactory = DurabilityMechanicFactory.get();
        if (durabilityFactory.isNotImplementedIn(itemId)) {
            if ((boolean) Settings.REPAIR_COMMAND_ORAXEN_DURABILITY.getValue()) // not oraxen item
                return true;
            if (damageable.getDamage() == 0) // full durability
                return true;
        } else {
            DurabilityMechanic durabilityMechanic = (DurabilityMechanic) durabilityFactory.getMechanic(itemId);
            PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
            int realMaxDurability = durabilityMechanic.getItemMaxDurability();
            int damage = realMaxDurability
                    - persistentDataContainer.get(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER);
            if (damage == 0) // full durability
                return true;
            persistentDataContainer
                    .set(DurabilityMechanic.NAMESPACED_KEY, PersistentDataType.INTEGER, realMaxDurability);
        }
        damageable.setDamage(0);
        itemStack.setItemMeta(damageable);
        return false;
    }

}
