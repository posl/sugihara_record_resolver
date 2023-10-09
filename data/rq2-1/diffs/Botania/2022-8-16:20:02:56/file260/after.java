/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.crafting;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.botania.api.recipe.IManaInfusionRecipe;
import vazkii.botania.api.recipe.StateIngredient;
import vazkii.botania.common.block.ModBlocks;

import java.util.Objects;

public class RecipeManaInfusion implements IManaInfusionRecipe {
	private final ResourceLocation id;
	private final ItemStack output;
	private final Ingredient input;
	private final int mana;
	@Nullable
	private final StateIngredient catalyst;
	private final String group;

	public RecipeManaInfusion(ResourceLocation id, ItemStack output, Ingredient input, int mana,
			@Nullable String group, @Nullable StateIngredient catalyst) {
		Preconditions.checkArgument(mana > 0, "Mana cost must be positive");
		Preconditions.checkArgument(mana <= 1_000_001, "Mana cost must be at most a pool"); // Leaving wiggle room for a certain modpack having creative-pool-only recipes
		this.id = id;
		this.output = output;
		this.input = input;
		this.mana = mana;
		this.group = group == null ? "" : group;
		this.catalyst = catalyst;
	}

	@Deprecated
	public RecipeManaInfusion(ResourceLocation id, ItemStack output, Ingredient input, int mana,
			@Nullable String group, @Nullable BlockState catalystState) {
		this(id, output, input, mana, group, StateIngredientHelper.of(catalystState));
	}

	@NotNull
	@Override
	public final ResourceLocation getId() {
		return id;
	}

	@NotNull
	@Override
	public RecipeSerializer<RecipeManaInfusion> getSerializer() {
		return ModRecipeTypes.MANA_INFUSION_SERIALIZER;
	}

	@Override
	public boolean matches(ItemStack stack) {
		return input.test(stack);
	}

	@Override
	public StateIngredient getRecipeCatalyst() {
		return catalyst;
	}

	@Override
	public int getManaToConsume() {
		return mana;
	}

	@NotNull
	@Override
	public ItemStack getResultItem() {
		return output;
	}

	@NotNull
	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, input);
	}

	@NotNull
	@Override
	public String getGroup() {
		return group;
	}

	@NotNull
	@Override
	public ItemStack getToastSymbol() {
		return new ItemStack(ModBlocks.manaPool);
	}

	public static class Serializer extends RecipeSerializerBase<RecipeManaInfusion> {

		@NotNull
		@Override
		public RecipeManaInfusion fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
			JsonElement input = Objects.requireNonNull(json.get("input"));
			Ingredient ing = Ingredient.fromJson(input);
			ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
			int mana = GsonHelper.getAsInt(json, "mana");
			String group = GsonHelper.getAsString(json, "group", "");
			StateIngredient catalyst = null;
			if (json.has("catalyst")) {
				JsonElement element = json.get("catalyst");
				if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) {
					throw new JsonParseException("Legacy mana infusion catalyst syntax used");
				}
				catalyst = StateIngredientHelper.deserialize(element.getAsJsonObject());
			}

			return new RecipeManaInfusion(id, output, ing, mana, group, catalyst);
		}

		@Nullable
		@Override
		public RecipeManaInfusion fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
			Ingredient input = Ingredient.fromNetwork(buf);
			ItemStack output = buf.readItem();
			int mana = buf.readVarInt();
			StateIngredient catalyst = null;
			if (buf.readBoolean()) {
				catalyst = StateIngredientHelper.read(buf);
			}
			String group = buf.readUtf();
			return new RecipeManaInfusion(id, output, input, mana, group, catalyst);
		}

		@Override
		public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull RecipeManaInfusion recipe) {
			recipe.getIngredients().get(0).toNetwork(buf);
			buf.writeItem(recipe.getResultItem());
			buf.writeVarInt(recipe.getManaToConsume());
			boolean hasCatalyst = recipe.getRecipeCatalyst() != null;
			buf.writeBoolean(hasCatalyst);
			if (hasCatalyst) {
				recipe.getRecipeCatalyst().write(buf);
			}
			buf.writeUtf(recipe.getGroup());
		}
	}
}
