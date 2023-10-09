/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.common.proxy.IProxy;

public class StarfieldCreatorBlockEntity extends BotaniaBlockEntity {
	public StarfieldCreatorBlockEntity(BlockPos pos, BlockState state) {
		super(BotaniaBlockEntities.STARFIELD, pos, state);
	}

	public static void clientTick(Level level, BlockPos worldPosition, BlockState state, StarfieldCreatorBlockEntity self) {
		level.updateSkyBrightness(); // this isn't called often on clients, but we need so that isDay is accurate.
		if (level.isDay()) {
			return;
		}

		double radius = 512;
		int iter = 2;
		for (int i = 0; i < iter; i++) {
			double x = worldPosition.getX() + 0.5 + (Math.random() - 0.5) * radius;
			double y = Math.min(256, worldPosition.getY() + IProxy.INSTANCE.getClientRenderDistance() * 16);
			double z = worldPosition.getZ() + 0.5 + (Math.random() - 0.5) * radius;

			float w = 0.6F;
			float c = 1F - w;

			float r = w + (float) Math.random() * c;
			float g = w + (float) Math.random() * c;
			float b = w + (float) Math.random() * c;

			float s = 20F + (float) Math.random() * 20F;
			int m = 50;

			SparkleParticleData data = SparkleParticleData.sparkle(s, r, g, b, m);
			level.addParticle(data, true, x, y, z, 0, 0, 0);
		}
	}

}
