/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An item that implements this can break multiple blocks at once
 * with a Ring of Loki. Usage of this interface requires an implementation
 * (see ItemTerraPick).
 */
public interface SequentialBreaker {

	void breakOtherBlock(Player player, ItemStack stack, BlockPos pos, BlockPos originPos, Direction side);

}
