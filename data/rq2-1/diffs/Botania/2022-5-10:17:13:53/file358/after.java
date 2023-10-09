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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.AABB;

import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.TileEntityGeneratingFlower;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.handler.ModSounds;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.List;

public class SubTileNarslimmus extends TileEntityGeneratingFlower {

	private static final int RANGE = 2;
	private static final int MAX_MANA = manaForSize(4);

	public SubTileNarslimmus(BlockPos pos, BlockState state) {
		super(ModSubtiles.NARSLIMMUS, pos, state);
	}

	@Override
	public void tickFlower() {
		super.tickFlower();

		if (ticksExisted % 5 == 0) {
			List<Slime> slimes = getLevel().getEntitiesOfClass(Slime.class, new AABB(getEffectivePos().offset(-RANGE, -RANGE, -RANGE), getEffectivePos().offset(RANGE + 1, RANGE + 1, RANGE + 1)));
			for (Slime slime : slimes) {
				if (slime.isAlive() && IXplatAbstractions.INSTANCE.narslimmusComponent(slime).isNaturalSpawned()) {
					int size = slime.getSize();
					if (!slime.level.isClientSide) {
						slime.discard();
						slime.playSound(size > 1 ? ModSounds.narslimmusEatBig : ModSounds.narslimmusEatSmall, 1F, 1F);
						addMana(manaForSize(size));
						sync();
					}

					int times = 8 * (int) Math.pow(2, size);
					for (int j = 0; j < times; ++j) {
						float f = slime.level.random.nextFloat() * (float) Math.PI * 2.0F;
						float f1 = slime.level.random.nextFloat() * 0.5F + 0.5F;
						float f2 = Mth.sin(f) * size * 0.5F * f1;
						float f3 = Mth.cos(f) * size * 0.5F * f1;
						float f4 = slime.level.random.nextFloat() * size * 0.5F * f1;
						slime.level.addParticle(ParticleTypes.ITEM_SLIME, slime.getX() + f2, slime.getBoundingBox().minY + f4, slime.getZ() + f3, 0.0D, 0.0D, 0.0D);
					}
					break;
				}
			}
		}
	}

	private static int manaForSize(int size) {
		size = Math.min(size, 4);
		return 1200 * (int) Math.pow(2, size);
	}

	@Override
	public RadiusDescriptor getRadius() {
		return RadiusDescriptor.Rectangle.square(getEffectivePos(), RANGE);
	}

	@Override
	public int getMaxMana() {
		return MAX_MANA;
	}

	@Override
	public int getColor() {
		return 0x71C373;
	}

	public static void onSpawn(Entity entity) {
		boolean slimeChunk = isSlimeChunk(entity.level, entity.getX(), entity.getZ());
		if (slimeChunk) {
			entity.getSelfAndPassengers().forEach(e -> {
				if (e instanceof Slime slime) {
					IXplatAbstractions.INSTANCE.narslimmusComponent(slime).setNaturalSpawn(true);
				}
			});
		}
	}

	private static boolean isSlimeChunk(Level world, double x, double z) {
		return isSlimeChunk(world, new BlockPos(x, 0, z));
	}

	public static boolean isSlimeChunk(Level world, BlockPos pos) {
		ChunkPos chunkpos = new ChunkPos(pos);
		return WorldgenRandom.seedSlimeChunk(chunkpos.x, chunkpos.z, ((ServerLevel) world).getSeed(), 987234911L).nextInt(10) == 0;
	}

}
