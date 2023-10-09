/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import vazkii.botania.common.helper.ItemNBTHelper;
import vazkii.botania.common.internal_caps.KeptItemsComponent;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.ArrayList;
import java.util.List;

public class ItemKeepIvy extends Item {

	public static final String TAG_KEEP = "Botania_keepIvy";

	public static final String TAG_PLAYER_KEPT_DROPS = "Botania_playerKeptDrops";
	private static final String TAG_DROP_COUNT = "dropCount";
	private static final String TAG_DROP_PREFIX = "dropPrefix";

	public ItemKeepIvy(Properties props) {
		super(props);
	}

	public static boolean hasIvy(ItemStack stack) {
		return !stack.isEmpty() && stack.hasTag() && ItemNBTHelper.getBoolean(stack, TAG_KEEP, false);
	}

	// Accessories are handled in the integration code
	public static void keepDropsOnDeath(Player player) {
		List<ItemStack> keeps = new ArrayList<>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (!stack.isEmpty() && stack.hasTag() && ItemNBTHelper.getBoolean(stack, TAG_KEEP, false)) {
				keeps.add(stack);
				player.getInventory().setItem(i, ItemStack.EMPTY);
			}
		}

		// The capabilities are not yet invalidated at this point, no need to do reviveCaps
		KeptItemsComponent data = IXplatAbstractions.INSTANCE.keptItemsComponent(player, false);
		data.addAll(keeps);
	}

	public static void onPlayerRespawn(Player oldPlayer, Player newPlayer, boolean alive) {
		if (!alive) {
			// At this point, the Forge capabilities have been invalidated and are no longer
			// accessible unless we do a hacky reviveCaps() call, see ForgeXplatImpl for details.
			KeptItemsComponent keeps = IXplatAbstractions.INSTANCE.keptItemsComponent(oldPlayer, true);

			for (ItemStack stack : keeps.getStacks()) {
				ItemStack copy = stack.copy();
				copy.removeTagKey(TAG_KEEP);
				if (!newPlayer.getInventory().add(copy)) {
					newPlayer.spawnAtLocation(copy);
				}
			}
		}
	}

}
