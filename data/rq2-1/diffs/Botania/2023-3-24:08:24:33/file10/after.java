package vazkii.botania.fabric.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.lib.BotaniaTags;
import vazkii.botania.data.recipes.BotaniaRecipeProvider;

import java.util.function.Consumer;

import static vazkii.botania.data.recipes.CraftingRecipeProvider.*;

public class FabricRecipeProvider extends BotaniaRecipeProvider {
	public FabricRecipeProvider(PackOutput packOutput) {
		super(packOutput);
	}

	@Override
	protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
		// TODO is it possible to move the tags that differ by platform into an XPlatAbstraction?

		// TODO 1.19.3 find proper categories for all these recipes
		// Quartz tag
		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, BotaniaBlocks.azulejo0)
				.requires(Items.BLUE_DYE)
				.requires(FabricItemTagProvider.QUARTZ_BLOCKS)
				.unlockedBy("has_item", conditionsFromItem(Items.BLUE_DYE))
				.save(consumer);

		// Chest tag
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BotaniaItems.baubleBox)
				.define('C', FabricItemTagProvider.WOODEN_CHESTS)
				.define('G', Items.GOLD_INGOT)
				.define('M', BotaniaTags.Items.INGOTS_MANASTEEL)
				.pattern(" M ")
				.pattern("MCG")
				.pattern(" M ")
				.unlockedBy("has_item", conditionsFromTag(BotaniaTags.Items.INGOTS_MANASTEEL))
				.save(consumer);

		registerRedStringBlock(consumer, BotaniaBlocks.redStringContainer, Ingredient.of(FabricItemTagProvider.WOODEN_CHESTS), conditionsFromTag(FabricItemTagProvider.WOODEN_CHESTS));
		ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, BotaniaBlocks.corporeaRetainer)
				.requires(FabricItemTagProvider.WOODEN_CHESTS)
				.requires(BotaniaItems.corporeaSpark)
				.unlockedBy("has_item", conditionsFromItem(BotaniaItems.corporeaSpark))
				.save(consumer);
	}

	@Override
	public String getName() {
		return "Botania recipes (Fabric-specific)";
	}
}
