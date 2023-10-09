/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.data.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.crafting.ModRecipeTypes;
import vazkii.botania.common.helper.ItemNBTHelper;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class PetalProvider extends BotaniaRecipeProvider {
	public PetalProvider(DataGenerator gen) {
		super(gen);
	}

	@Override
	public String getName() {
		return "Botania petal apothecary recipes";
	}

	@Override
	public void registerRecipes(Consumer<net.minecraft.data.recipes.FinishedRecipe> consumer) {
		Ingredient white = tagIngr("petals/white");
		Ingredient orange = tagIngr("petals/orange");
		Ingredient magenta = tagIngr("petals/magenta");
		Ingredient lightBlue = tagIngr("petals/light_blue");
		Ingredient yellow = tagIngr("petals/yellow");
		Ingredient lime = tagIngr("petals/lime");
		Ingredient pink = tagIngr("petals/pink");
		Ingredient gray = tagIngr("petals/gray");
		Ingredient lightGray = tagIngr("petals/light_gray");
		Ingredient cyan = tagIngr("petals/cyan");
		Ingredient purple = tagIngr("petals/purple");
		Ingredient blue = tagIngr("petals/blue");
		Ingredient brown = tagIngr("petals/brown");
		Ingredient green = tagIngr("petals/green");
		Ingredient red = tagIngr("petals/red");
		Ingredient black = tagIngr("petals/black");
		Ingredient runeWater = Ingredient.of(ModItems.runeWater);
		Ingredient runeFire = Ingredient.of(ModItems.runeFire);
		Ingredient runeEarth = Ingredient.of(ModItems.runeEarth);
		Ingredient runeAir = Ingredient.of(ModItems.runeAir);
		Ingredient runeSpring = Ingredient.of(ModItems.runeSpring);
		Ingredient runeSummer = Ingredient.of(ModItems.runeSummer);
		Ingredient runeAutumn = Ingredient.of(ModItems.runeAutumn);
		Ingredient runeWinter = Ingredient.of(ModItems.runeWinter);
		Ingredient runeMana = Ingredient.of(ModItems.runeMana);
		Ingredient runeLust = Ingredient.of(ModItems.runeLust);
		Ingredient runeGluttony = Ingredient.of(ModItems.runeGluttony);
		Ingredient runeGreed = Ingredient.of(ModItems.runeGreed);
		Ingredient runeSloth = Ingredient.of(ModItems.runeSloth);
		Ingredient runeWrath = Ingredient.of(ModItems.runeWrath);
		Ingredient runeEnvy = Ingredient.of(ModItems.runeEnvy);
		Ingredient runePride = Ingredient.of(ModItems.runePride);

		Ingredient redstoneRoot = Ingredient.of(ModItems.redstoneRoot);
		Ingredient pixieDust = Ingredient.of(ModItems.pixieDust);
		Ingredient gaiaSpirit = Ingredient.of(ModItems.lifeEssence);

		consumer.accept(make(ModSubtiles.pureDaisy, white, white, white, white));
		consumer.accept(make(ModSubtiles.manastar, lightBlue, green, red, cyan));

		consumer.accept(make(ModSubtiles.endoflame, brown, brown, red, lightGray));
		consumer.accept(make(ModSubtiles.hydroangeas, blue, blue, cyan, cyan));
		consumer.accept(make(ModSubtiles.thermalily, red, orange, orange, runeEarth, runeFire));
		consumer.accept(make(ModSubtiles.rosaArcana, pink, pink, purple, purple, lime, runeMana));
		consumer.accept(make(ModSubtiles.munchdew, lime, lime, red, red, green, runeGluttony));
		consumer.accept(make(ModSubtiles.entropinnyum, red, red, gray, gray, white, white, runeWrath, runeFire));
		consumer.accept(make(ModSubtiles.kekimurus, white, white, orange, orange, brown, brown, runeGluttony, pixieDust));
		consumer.accept(make(ModSubtiles.gourmaryllis, lightGray, lightGray, yellow, yellow, red, runeFire, runeSummer));
		consumer.accept(make(ModSubtiles.narslimmus, lime, lime, green, green, black, runeSummer, runeWater));
		consumer.accept(make(ModSubtiles.spectrolus, red, red, green, green, blue, blue, white, white, runeWinter, runeAir, pixieDust));
		consumer.accept(make(ModSubtiles.rafflowsia, purple, purple, green, green, black, runeEarth, runePride, pixieDust));
		consumer.accept(make(ModSubtiles.shulkMeNot, purple, purple, magenta, magenta, lightGray, gaiaSpirit, runeEnvy, runeWrath));
		consumer.accept(make(ModSubtiles.dandelifeon, purple, purple, lime, green, runeWater, runeFire, runeEarth, runeAir, gaiaSpirit));

		consumer.accept(make(ModSubtiles.jadedAmaranthus, purple, lime, green, runeSpring, redstoneRoot));
		consumer.accept(make(ModSubtiles.bellethorn, red, red, red, cyan, cyan, redstoneRoot));
		consumer.accept(make(ModSubtiles.dreadthorn, black, black, black, cyan, cyan, redstoneRoot));
		consumer.accept(make(ModSubtiles.heiseiDream, magenta, magenta, purple, pink, runeWrath, pixieDust));
		consumer.accept(make(ModSubtiles.tigerseye, yellow, brown, orange, lime, runeAutumn));

		net.minecraft.data.recipes.FinishedRecipe base = make(ModSubtiles.orechid, gray, gray, yellow, green, red, runePride, runeGreed, redstoneRoot, pixieDust);
		net.minecraft.data.recipes.FinishedRecipe gog = make(ModSubtiles.orechid, gray, gray, yellow, yellow, green, green, red, red);
		consumer.accept(new GogAlternationResult(gog, base));

		consumer.accept(make(ModSubtiles.orechidIgnem, red, red, white, white, pink, runePride, runeGreed, redstoneRoot, pixieDust));
		consumer.accept(make(ModSubtiles.fallenKanade, white, white, yellow, yellow, orange, runeSpring));
		consumer.accept(make(ModSubtiles.exoflame, red, red, gray, lightGray, runeFire, runeSummer));
		consumer.accept(make(ModSubtiles.agricarnation, lime, lime, green, yellow, runeSpring, redstoneRoot));
		consumer.accept(make(ModSubtiles.hopperhock, gray, gray, lightGray, lightGray, runeAir, redstoneRoot));
		consumer.accept(make(ModSubtiles.tangleberrie, cyan, cyan, gray, lightGray, runeAir, runeEarth));
		consumer.accept(make(ModSubtiles.jiyuulia, pink, pink, purple, lightGray, runeWater, runeAir));
		consumer.accept(make(ModSubtiles.rannuncarpus, orange, orange, yellow, runeEarth, redstoneRoot));
		consumer.accept(make(ModSubtiles.hyacidus, purple, purple, magenta, magenta, green, runeWater, runeAutumn, redstoneRoot));
		consumer.accept(make(ModSubtiles.pollidisiac, red, red, pink, pink, orange, runeLust, runeFire));
		consumer.accept(make(ModSubtiles.clayconia, lightGray, lightGray, gray, cyan, runeEarth));
		consumer.accept(make(ModSubtiles.loonium, green, green, green, green, gray, runeSloth, runeGluttony, runeEnvy, redstoneRoot, pixieDust));
		consumer.accept(make(ModSubtiles.daffomill, white, white, brown, yellow, runeAir, redstoneRoot));
		consumer.accept(make(ModSubtiles.vinculotus, black, black, purple, purple, green, runeWater, runeSloth, runeLust, redstoneRoot));
		consumer.accept(make(ModSubtiles.spectranthemum, white, white, lightGray, lightGray, cyan, runeEnvy, runeWater, redstoneRoot, pixieDust));
		consumer.accept(make(ModSubtiles.medumone, brown, brown, gray, gray, runeEarth, redstoneRoot));
		consumer.accept(make(ModSubtiles.marimorphosis, gray, yellow, green, red, runeEarth, runeFire, redstoneRoot));
		consumer.accept(make(ModSubtiles.bubbell, cyan, cyan, lightBlue, lightBlue, blue, blue, runeWater, runeSummer, pixieDust));
		consumer.accept(make(ModSubtiles.solegnolia, brown, brown, red, blue, redstoneRoot));
		consumer.accept(make(ModSubtiles.bergamute, orange, green, green, redstoneRoot));
		consumer.accept(make(ModSubtiles.labellia, yellow, yellow, blue, white, black, runeAutumn, redstoneRoot, pixieDust));

		consumer.accept(make(ModBlocks.motifDaybloom, yellow, yellow, orange, lightBlue));
		consumer.accept(make(ModBlocks.motifNightshade, black, black, purple, gray));

		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		ItemNBTHelper.setString(stack, "SkullOwner", "Vazkii");
		Ingredient[] inputs = new Ingredient[16];
		Arrays.fill(inputs, pink);
		consumer.accept(new NbtOutputResult(new FinishedRecipe(idFor(prefix("vazkii_head")), stack, inputs), stack.getTag()));
	}

	protected static Ingredient tagIngr(String tag) {
		return Ingredient.of(TagKey.create(Registry.ITEM_REGISTRY, prefix(tag)));
	}

	protected static FinishedRecipe make(ItemLike item, Ingredient... ingredients) {
		return new FinishedRecipe(idFor(Registry.ITEM.getKey(item.asItem())), new ItemStack(item), ingredients);
	}

	protected static ResourceLocation idFor(ResourceLocation name) {
		return new ResourceLocation(name.getNamespace(), "petal_apothecary/" + name.getPath());
	}

	protected static class FinishedRecipe implements net.minecraft.data.recipes.FinishedRecipe {
		private final ResourceLocation id;
		private final ItemStack output;
		private final Ingredient[] inputs;

		private FinishedRecipe(ResourceLocation id, ItemStack output, Ingredient... inputs) {
			this.id = id;
			this.output = output;
			this.inputs = inputs;
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			json.add("output", ItemNBTHelper.serializeStack(output));
			JsonArray ingredients = new JsonArray();
			for (Ingredient ingr : inputs) {
				ingredients.add(ingr.toJson());
			}
			json.add("ingredients", ingredients);
		}

		@Override
		public ResourceLocation getId() {
			return id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return ModRecipeTypes.PETAL_SERIALIZER;
		}

		@Nullable
		@Override
		public JsonObject serializeAdvancement() {
			return null;
		}

		@Nullable
		@Override
		public ResourceLocation getAdvancementId() {
			return null;
		}
	}
}
