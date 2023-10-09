/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.core.helper;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class CoreShaders {
	private static ShaderInstance starfieldShaderInstance;
	private static ShaderInstance doppleganger;
	private static ShaderInstance manaPool;
	private static ShaderInstance terraPlate;
	private static ShaderInstance enchanter;
	private static ShaderInstance pylon;
	private static ShaderInstance halo;
	private static ShaderInstance filmGrainParticle;

	public static void init(ResourceManager resourceManager,
			List<Pair<ShaderInstance, Consumer<ShaderInstance>>> registrations) throws IOException {
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__starfield", DefaultVertexFormat.POSITION),
				inst -> starfieldShaderInstance = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__doppleganger", DefaultVertexFormat.NEW_ENTITY),
				inst -> doppleganger = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__mana_pool", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
				inst -> manaPool = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__terra_plate_rune", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
				inst -> terraPlate = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__enchanter_rune", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
				inst -> enchanter = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__pylon", DefaultVertexFormat.NEW_ENTITY),
				inst -> pylon = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__halo", DefaultVertexFormat.POSITION_COLOR_TEX),
				inst -> halo = inst)
		);
		registrations.add(Pair.of(
				new ShaderInstance(resourceManager, "botania__film_grain_particle", DefaultVertexFormat.PARTICLE),
				inst -> filmGrainParticle = inst)
		);
	}

	public static ShaderInstance starfield() {
		return starfieldShaderInstance;
	}

	public static ShaderInstance doppleganger() {
		return doppleganger;
	}

	public static ShaderInstance manaPool() {
		return manaPool;
	}

	public static ShaderInstance terraPlate() {
		return terraPlate;
	}

	public static ShaderInstance enchanter() {
		return enchanter;
	}

	public static ShaderInstance pylon() {
		return pylon;
	}

	public static ShaderInstance halo() {
		return halo;
	}

	public static ShaderInstance filmGrainParticle() {
		return filmGrainParticle;
	}
}
