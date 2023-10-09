/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.rod;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.api.item.IBlockProvider;
import vazkii.botania.api.mana.ManaItemHandler;

public class ItemCobbleRod extends Item {

	static final int COST = 150;

	public ItemCobbleRod(Properties props) {
		super(props);
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		return ItemDirtRod.place(ctx, Blocks.COBBLESTONE, COST, 0.3F, 0.3F, 0.3F);
	}

	public static class BlockProvider implements IBlockProvider {
		@Override
		public boolean provideBlock(Player player, ItemStack requestor, Block block, boolean doit) {
			if (block == Blocks.COBBLESTONE) {
				return (doit && ManaItemHandler.instance().requestManaExactForTool(requestor, player, COST, true)) ||
						(!doit && ManaItemHandler.instance().requestManaExactForTool(requestor, player, COST, false));
			}
			return false;
		}

		@Override
		public int getBlockCount(Player player, ItemStack requestor, Block block) {
			if (block == Blocks.COBBLESTONE) {
				return ManaItemHandler.instance().getInvocationCountForTool(requestor, player, COST);
			}
			return 0;
		}
	}

}
