package com.simibubi.create.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedPress;
import com.simibubi.create.content.contraptions.processing.BasinRecipe;
import com.simibubi.create.content.contraptions.processing.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

@ParametersAreNonnullByDefault
public class PackingCategory extends BasinCategory {

	private final AnimatedPress press = new AnimatedPress(true);
	private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();
	private final PackingType type;

	enum PackingType {
		AUTO_SQUARE, COMPACTING
	}

	public static PackingCategory standard() {
		return new PackingCategory(PackingType.COMPACTING, AllBlocks.BASIN.get(), 103);
	}

	public static PackingCategory autoSquare() {
		return new PackingCategory(PackingType.AUTO_SQUARE, Blocks.CRAFTING_TABLE, 85);
	}

	protected PackingCategory(PackingType type, ItemLike icon, int height) {
		super(type != PackingType.AUTO_SQUARE, doubleItemIcon(AllBlocks.MECHANICAL_PRESS.get(), icon),
			emptyBackground(177, height));
		this.type = type;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, BasinRecipe recipe, IFocusGroup focuses) {
		if (type == PackingType.COMPACTING) {
			super.setRecipe(builder, recipe, focuses);
			return;
		}

		int i = 0;
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		int size = ingredients.size();
		int rows = size == 4 ? 2 : 3;
		while (i < size) {
			Ingredient ingredient = ingredients.get(i);
			builder
					.addSlot(RecipeIngredientRole.INPUT, (rows == 2 ? 27 : 18) + (i % rows) * 19, 51 - (i / rows) * 19)
					.setBackground(getRenderedSlot(), -1, -1)
					.addIngredients(ingredient);

			i++;
		}

		builder
				.addSlot(RecipeIngredientRole.OUTPUT, 142, 51)
				.setBackground(getRenderedSlot(), -1, -1)
				.addItemStack(recipe.getResultItem());
	}

	@Override
	public void draw(BasinRecipe recipe, IRecipeSlotsView iRecipeSlotsView, PoseStack matrixStack, double mouseX, double mouseY) {
		if (type == PackingType.COMPACTING) {
			super.draw(recipe, iRecipeSlotsView, matrixStack, mouseX, mouseY);
		} else {
			AllGuiTextures.JEI_DOWN_ARROW.render(matrixStack, 136, 32);
			AllGuiTextures.JEI_SHADOW.render(matrixStack, 81, 68);
		}


		HeatCondition requiredHeat = recipe.getRequiredHeat();
		if (requiredHeat != HeatCondition.NONE)
			heater.withHeat(requiredHeat.visualizeAsBlazeBurner())
				.draw(matrixStack, getBackground().getWidth() / 2 + 3, 55);
		press.draw(matrixStack, getBackground().getWidth() / 2 + 3, 34);


	}

}
