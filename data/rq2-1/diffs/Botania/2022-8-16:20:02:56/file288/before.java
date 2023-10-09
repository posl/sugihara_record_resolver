/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.crafting.recipe;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import vazkii.botania.common.crafting.RecipeSerializerBase;
import vazkii.botania.xplat.IXplatAbstractions;

import javax.annotation.Nonnull;

public class ManaUpgradeRecipe extends ShapedRecipe {
	public ManaUpgradeRecipe(ShapedRecipe compose) {
		super(compose.getId(), compose.getGroup(), compose.getWidth(), compose.getHeight(), compose.getIngredients(), compose.getResultItem());
	}

	public static ItemStack output(ItemStack output, Container inv) {
		ItemStack out = output.copy();
		var outItem = IXplatAbstractions.INSTANCE.findManaItem(out);
		if (outItem == null) {
			return out;
		}
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			var item = IXplatAbstractions.INSTANCE.findManaItem(stack);
			if (!stack.isEmpty() && item != null) {
				outItem.addMana(item.getMana());
			}
		}
		return out;
	}

	@Nonnull
	@Override
	public ItemStack assemble(@Nonnull CraftingContainer inv) {
		return output(super.assemble(inv), inv);
	}

	@Nonnull
	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	public static final RecipeSerializer<ManaUpgradeRecipe> SERIALIZER = new Serializer();

	private static class Serializer extends RecipeSerializerBase<ManaUpgradeRecipe> {
		@Override
		public ManaUpgradeRecipe fromJson(@Nonnull ResourceLocation recipeId, @Nonnull JsonObject json) {
			return new ManaUpgradeRecipe(SHAPED_RECIPE.fromJson(recipeId, json));
		}

		@Override
		public ManaUpgradeRecipe fromNetwork(@Nonnull ResourceLocation recipeId, @Nonnull FriendlyByteBuf buffer) {
			return new ManaUpgradeRecipe(SHAPED_RECIPE.fromNetwork(recipeId, buffer));
		}

		@Override
		public void toNetwork(@Nonnull FriendlyByteBuf buffer, @Nonnull ManaUpgradeRecipe recipe) {
			SHAPED_RECIPE.toNetwork(buffer, recipe);
		}
	}
}
