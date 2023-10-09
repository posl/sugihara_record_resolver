/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.string;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import vazkii.botania.common.block.tile.ModTiles;
import vazkii.botania.common.block.tile.string.TileRedString;
import vazkii.botania.common.block.tile.string.TileRedStringFertilizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockRedStringFertilizer extends BlockRedString implements BonemealableBlock {

	public BlockRedStringFertilizer(BlockBehaviour.Properties builder) {
		super(builder);
		registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
	}

	@Override
	public boolean isValidBonemealTarget(@Nonnull BlockGetter world, @Nonnull BlockPos pos, @Nonnull BlockState state, boolean isClient) {
		return ((TileRedStringFertilizer) world.getBlockEntity(pos)).canGrow(world, isClient);
	}

	@Override
	public boolean isBonemealSuccess(@Nonnull Level world, @Nonnull RandomSource rand, @Nonnull BlockPos pos, @Nonnull BlockState state) {
		return ((TileRedStringFertilizer) world.getBlockEntity(pos)).canUseBonemeal(world, rand);
	}

	@Override
	public void performBonemeal(@Nonnull ServerLevel world, @Nonnull RandomSource rand, @Nonnull BlockPos pos, @Nonnull BlockState state) {
		((TileRedStringFertilizer) world.getBlockEntity(pos)).grow(world, rand);
	}

	@Nonnull
	@Override
	public TileRedString newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
		return new TileRedStringFertilizer(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModTiles.RED_STRING_FERTILIZER, TileRedStringFertilizer::commonTick);
	}
}
