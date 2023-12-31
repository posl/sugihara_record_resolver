/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import vazkii.botania.api.recipe.IElvenTradeRecipe;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.helper.ItemNBTHelper;
import vazkii.botania.common.item.ItemLexicon;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LexiconElvenTradeRecipe implements IElvenTradeRecipe {
	private final ResourceLocation id;

	LexiconElvenTradeRecipe(ResourceLocation id) {
		this.id = id;
	}

	@Override
	public boolean containsItem(ItemStack stack) {
		return stack.is(ModItems.lexicon) && !ItemNBTHelper.getBoolean(stack, ItemLexicon.TAG_ELVEN_UNLOCK, false);
	}

	@Nonnull
	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.withSize(1, Ingredient.of(ModItems.lexicon));
	}

	@Nonnull
	@Override
	public ItemStack getToastSymbol() {
		return new ItemStack(ModBlocks.alfPortal);
	}

	@Nonnull
	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public List<ItemStack> getOutputs() {
		ItemStack stack = new ItemStack(ModItems.lexicon);
		stack.getOrCreateTag().putBoolean(ItemLexicon.TAG_ELVEN_UNLOCK, true);
		return Collections.singletonList(stack);
	}

	@Override
	public Optional<List<ItemStack>> match(List<ItemStack> stacks) {
		for (ItemStack stack : stacks) {
			if (containsItem(stack)) {
				return Optional.of(Collections.singletonList(stack));
			}
		}
		return Optional.empty();
	}

	@Override
	public List<ItemStack> getOutputs(List<ItemStack> inputs) {
		ItemStack stack = inputs.get(0).copy();
		stack.getOrCreateTag().putBoolean(ItemLexicon.TAG_ELVEN_UNLOCK, true);
		return Collections.singletonList(stack);
	}

	@Nonnull
	@Override
	public RecipeSerializer<LexiconElvenTradeRecipe> getSerializer() {
		return ModRecipeTypes.LEXICON_ELVEN_TRADE_SERIALIZER;
	}
}
