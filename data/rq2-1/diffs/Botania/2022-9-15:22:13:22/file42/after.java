/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.subtile.generating;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.handler.ModSounds;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.List;

public class SubTileEntropinnyum extends GeneratingFlowerBlockEntity {
	private static final int RANGE = 12;
	private static final int EXPLODE_EFFECT_EVENT = 0;
	private static final int ANGRY_EFFECT_EVENT = 1;

	public SubTileEntropinnyum(BlockPos pos, BlockState state) {
		super(ModSubtiles.ENTROPINNYUM, pos, state);
	}

	public static boolean isUnethical(Entity e) {
		BlockPos center = e.blockPosition();
		if (!e.level.isLoaded(center)) {
			return false;
		}

		int x = center.getX();
		int y = center.getY();
		int z = center.getZ();
		int range = 3;

		// Should actually check for corals too, but it gets broken when the piston extends
		int movingPistons = 0;
		int rails = 0;
		int slimes = 0;
		for (BlockPos pos : BlockPos.betweenClosed(x - range, y - range, z - range, x + range + 1, y + range + 1, z + range + 1)) {
			BlockState state = e.level.getBlockState(pos);
			if (state.is(Blocks.MOVING_PISTON)) {
				movingPistons++;
				BlockEntity te = e.level.getBlockEntity(pos);
				if (te instanceof PistonMovingBlockEntity piston) {
					state = piston.getMovedState();
				}
			}

			if (state.getBlock() instanceof DetectorRailBlock) {
				rails++;
			} else if (state.getBlock() instanceof SlimeBlock || state.getBlock() instanceof HoneyBlock) {
				slimes++;
			}

			if (movingPistons > 0 && rails > 0 && slimes > 0) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void tickFlower() {
		super.tickFlower();

		if (!getLevel().isClientSide && getMana() == 0) {
			List<PrimedTnt> tnts = getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(getEffectivePos().offset(-RANGE, -RANGE, -RANGE), getEffectivePos().offset(RANGE + 1, RANGE + 1, RANGE + 1)));
			for (PrimedTnt tnt : tnts) {
				FluidState fluid = getLevel().getFluidState(tnt.blockPosition());
				if (tnt.getFuse() == 1 && tnt.isAlive() && fluid.isEmpty()) {
					boolean unethical = IXplatAbstractions.INSTANCE.ethicalComponent(tnt).isUnethical();
					tnt.playSound(unethical ? ModSounds.entropinnyumAngry : ModSounds.entropinnyumHappy, 1F, (1F + (getLevel().random.nextFloat() - getLevel().random.nextFloat()) * 0.2F) * 0.7F);
					tnt.discard();
					addMana(unethical ? 3 : getMaxMana());
					sync();

					getLevel().blockEvent(getBlockPos(), getBlockState().getBlock(), unethical ? ANGRY_EFFECT_EVENT : EXPLODE_EFFECT_EVENT, tnt.getId());
					break;
				}
			}
		}
	}

	@Override
	public boolean triggerEvent(int event, int param) {
		if (event == EXPLODE_EFFECT_EVENT) {
			if (getLevel().isClientSide && getLevel().getEntity(param) instanceof PrimedTnt) {
				Entity e = getLevel().getEntity(param);

				for (int i = 0; i < 50; i++) {
					SparkleParticleData data = SparkleParticleData.sparkle((float) (Math.random() * 0.65F + 1.25F), 1F, (float) Math.random() * 0.25F, (float) Math.random() * 0.25F, 12);
					level.addParticle(data, e.getX() + Math.random() * 4 - 2, e.getY() + Math.random() * 4 - 2, e.getZ() + Math.random() * 4 - 2, 0, 0, 0);
				}

				getLevel().addParticle(ParticleTypes.EXPLOSION_EMITTER, e.getX(), e.getY(), e.getZ(), 1D, 0D, 0D);
			}
			return true;
		} else if (event == ANGRY_EFFECT_EVENT) {
			if (getLevel().isClientSide && getLevel().getEntity(param) instanceof PrimedTnt) {
				Entity e = getLevel().getEntity(param);

				for (int i = 0; i < 50; i++) {
					level.addParticle(ParticleTypes.ANGRY_VILLAGER, e.getX() + Math.random() * 4 - 2, e.getY() + Math.random() * 4 - 2, e.getZ() + Math.random() * 4 - 2, 0, 0, 0);
				}
			}

			return true;
		} else {
			return super.triggerEvent(event, param);
		}
	}

	@Override
	public int getColor() {
		return 0xcb0000;
	}

	@Override
	public int getMaxMana() {
		return 6500;
	}

	@Override
	public RadiusDescriptor getRadius() {
		return RadiusDescriptor.Rectangle.square(getEffectivePos(), RANGE);
	}

}
