/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import vazkii.botania.common.handler.ModSounds;
import vazkii.botania.common.helper.ItemNBTHelper;

import javax.annotation.Nonnull;

import java.util.List;

public class ItemTemperanceStone extends Item {
	public static final String TAG_ACTIVE = "active";

	public ItemTemperanceStone(Properties builder) {
		super(builder);
	}

	@Nonnull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @Nonnull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ItemNBTHelper.setBoolean(stack, TAG_ACTIVE, !ItemNBTHelper.getBoolean(stack, TAG_ACTIVE, false));
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.temperanceStoneConfigure, SoundSource.NEUTRAL, 1F, 1F);
		return InteractionResultHolder.success(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> stacks, TooltipFlag flags) {
		if (ItemNBTHelper.getBoolean(stack, TAG_ACTIVE, false)) {
			stacks.add(Component.translatable("botaniamisc.active"));
		} else {
			stacks.add(Component.translatable("botaniamisc.inactive"));
		}
	}

	public static boolean hasTemperanceActive(Player player) {
		Container inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.is(ModItems.temperanceStone) && ItemNBTHelper.getBoolean(stack, TAG_ACTIVE, false)) {
				return true;
			}
		}

		return false;
	}

}
