/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.integration.jei.crafting;

import com.google.common.collect.ImmutableList;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.botania.common.crafting.recipe.TerraShattererTippingRecipe;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraPick;

import java.util.Collections;
import java.util.List;

public class TerraShattererTippingRecipeWrapper implements ICraftingCategoryExtension {
	private final List<List<ItemStack>> inputs;
	private final ItemStack output;
	private final ResourceLocation name;

	public TerraShattererTippingRecipeWrapper(TerraShattererTippingRecipe recipe) {
		inputs = ImmutableList.of(ImmutableList.of(new ItemStack(ModItems.terraPick)), ImmutableList.of(new ItemStack(ModItems.elementiumPick)));
		output = new ItemStack(ModItems.terraPick);
		ItemTerraPick.setTipped(output);

		this.name = recipe.getId();
	}

	@Override
	public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull ICraftingGridHelper helper, @NotNull IFocusGroup focuses) {
		helper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, 0, 0);
		helper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK, Collections.singletonList(output));
	}

	@Nullable
	@Override
	public ResourceLocation getRegistryName() {
		return name;
	}
}
