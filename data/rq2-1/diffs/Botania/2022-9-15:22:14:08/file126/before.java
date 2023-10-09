/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.data.recipes;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import vazkii.botania.mixin.RecipeProviderAccessor;
import vazkii.botania.xplat.IXplatAbstractions;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public abstract class BotaniaRecipeProvider implements DataProvider {
	private final DataGenerator generator;

	protected BotaniaRecipeProvider(DataGenerator generator) {
		this.generator = generator;
	}

	// [VanillaCopy] RecipeProvider
	@Override
	public void run(CachedOutput cache) {
		Path path = this.generator.getOutputFolder();
		Set<ResourceLocation> set = Sets.newHashSet();
		registerRecipes((recipeJsonProvider) -> {
			if (!set.add(recipeJsonProvider.getId())) {
				throw new IllegalStateException("Duplicate recipe " + recipeJsonProvider.getId());
			} else {
				RecipeProviderAccessor.callSaveRecipe(cache, recipeJsonProvider.serializeRecipe(), path.resolve("data/" + recipeJsonProvider.getId().getNamespace() + "/recipes/" + recipeJsonProvider.getId().getPath() + ".json"));
				JsonObject jsonObject = recipeJsonProvider.serializeAdvancement();
				if (jsonObject != null) {
					IXplatAbstractions.INSTANCE.saveRecipeAdvancement(this.generator, cache, jsonObject, path.resolve("data/" + recipeJsonProvider.getId().getNamespace() + "/advancements/" + recipeJsonProvider.getAdvancementId().getPath() + ".json"));
				}
			}
		});
	}

	protected abstract void registerRecipes(Consumer<FinishedRecipe> consumer);
}
