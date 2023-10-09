/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import vazkii.botania.common.block.BlockMod;
import vazkii.botania.common.block.BlockOpenCrate;
import vazkii.botania.common.block.tile.ModTiles;
import vazkii.botania.common.block.tile.mana.TileTurntable;

import javax.annotation.Nonnull;

import java.util.Random;

public class BlockTurntable extends BlockMod implements EntityBlock {

	public BlockTurntable(Properties builder) {
		super(builder);
	}

	@Nonnull
	@Override
	public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
		return new TileTurntable(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModTiles.TURNTABLE, TileTurntable::commonTick);
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, Random rand) {
		if (world.hasNeighborSignal(pos) && rand.nextDouble() < 0.2) {
			BlockOpenCrate.redstoneParticlesOnFullBlock(world, pos, rand);
		}
	}
}
