/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.impl;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.block.IFloatingFlower;
import vazkii.botania.client.gui.HUDHandler;

import java.util.Collections;
import java.util.Map;

public class BotaniaAPIClientImpl implements BotaniaAPIClient {
	private final Map<IFloatingFlower.IslandType, ResourceLocation> islandTypeModels = Maps.newHashMap();

	@Override
	public void registerIslandTypeModel(IFloatingFlower.IslandType islandType, ResourceLocation model) {
		islandTypeModels.put(islandType, model);
	}

	@Override
	public Map<IFloatingFlower.IslandType, ResourceLocation> getRegisteredIslandTypeModels() {
		return Collections.unmodifiableMap(islandTypeModels);
	}

	@Override
	public void drawSimpleManaHUD(PoseStack ms, int color, int mana, int maxMana, String name) {
		HUDHandler.drawSimpleManaHUD(ms, color, mana, maxMana, name);
	}

	@Override
	public void drawComplexManaHUD(PoseStack ms, int color, int mana, int maxMana, String name, ItemStack bindDisplay, boolean properlyBound) {
		HUDHandler.drawComplexManaHUD(color, ms, mana, maxMana, name, bindDisplay, properlyBound);
	}
}
