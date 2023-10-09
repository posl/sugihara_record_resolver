/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.equipment.tool.manasteel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.common.item.equipment.tool.ToolCommons;

import java.util.function.Consumer;

public class ItemManasteelSword extends SwordItem {

	public static final int MANA_PER_DAMAGE = 60;

	public ItemManasteelSword(Properties props) {
		this(BotaniaAPI.instance().getManasteelItemTier(), props);
	}

	public ItemManasteelSword(Tier mat, Properties props) {
		this(mat, 3, -2.4F, props);
	}

	public ItemManasteelSword(Tier mat, int attackDamage, float attackSpeed, Properties props) {
		super(mat, attackDamage, attackSpeed, props);
	}

	public static <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
		int manaPerDamage = ((ItemManasteelSword) stack.getItem()).getManaPerDamage();
		return ToolCommons.damageItemIfPossible(stack, amount, entity, manaPerDamage);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity player, int slot, boolean selected) {
		if (!world.isClientSide && player instanceof Player && stack.getDamageValue() > 0 && ManaItemHandler.instance().requestManaExactForTool(stack, (Player) player, getManaPerDamage() * 2, true)) {
			stack.setDamageValue(stack.getDamageValue() - 1);
		}
	}

	public int getManaPerDamage() {
		return MANA_PER_DAMAGE;
	}
}
