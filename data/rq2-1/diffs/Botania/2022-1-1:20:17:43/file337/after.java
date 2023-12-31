/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.subtile.functional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.TileEntityFunctionalFlower;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.core.handler.ConfigHandler;

public class SubTileJadedAmaranthus extends TileEntityFunctionalFlower {
	private static final int COST = 100;
	final int RANGE = 4;

	public SubTileJadedAmaranthus(BlockPos pos, BlockState state) {
		super(ModSubtiles.JADED_AMARANTHUS, pos, state);
	}

	@Override
	public void tickFlower() {
		super.tickFlower();

		if (getLevel().isClientSide || redstoneSignal > 0) {
			return;
		}

		if (ticksExisted % 30 == 0 && getMana() >= COST) {
			BlockPos pos = new BlockPos(
					getEffectivePos().getX() - RANGE + getLevel().random.nextInt(RANGE * 2 + 1),
					getEffectivePos().getY() + RANGE,
					getEffectivePos().getZ() - RANGE + getLevel().random.nextInt(RANGE * 2 + 1)
			);

			BlockPos up = pos.above();

			for (int i = 0; i < RANGE * 2; i++) {
				DyeColor color = DyeColor.byId(getLevel().random.nextInt(16));
				BlockState flower = ModBlocks.getFlower(color).defaultBlockState();

				if (getLevel().isEmptyBlock(up) && flower.canSurvive(getLevel(), up)) {
					if (ConfigHandler.COMMON.blockBreakParticles.getValue()) {
						getLevel().levelEvent(2001, up, Block.getId(flower));
					}
					getLevel().setBlockAndUpdate(up, flower);
					addMana(-COST);
					sync();

					break;
				}

				up = pos;
				pos = pos.below();
			}
		}
	}

	@Override
	public boolean acceptsRedstone() {
		return true;
	}

	@Override
	public int getColor() {
		return 0x961283;
	}

	@Override
	public RadiusDescriptor getRadius() {
		return new RadiusDescriptor.Square(getEffectivePos(), RANGE);
	}

	@Override
	public int getMaxMana() {
		return COST;
	}

}
