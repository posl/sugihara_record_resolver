/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.patchouli.processor;

import com.google.common.collect.ImmutableList;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import vazkii.botania.api.recipe.IManaInfusionRecipe;
import vazkii.botania.client.patchouli.PatchouliUtils;
import vazkii.botania.common.Botania;
import vazkii.botania.common.crafting.ModRecipeTypes;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManaInfusionProcessor implements IComponentProcessor {
	private List<IManaInfusionRecipe> recipes;
	private boolean hasCustomHeading;

	@Override
	public void setup(IVariableProvider variables) {
		if (variables.has("recipes") && variables.has("group")) {
			Botania.LOGGER.warn("Mana infusion template has both 'recipes' and 'group', ignoring 'recipes'");
		}

		ImmutableList.Builder<IManaInfusionRecipe> builder = ImmutableList.builder();
		if (variables.has("group")) {
			String group = variables.get("group").asString();
			builder.addAll(PatchouliUtils.getRecipeGroup(ModRecipeTypes.MANA_INFUSION_TYPE, group));
		} else {
			for (IVariable s : variables.get("recipes").asListOrSingleton()) {
				IManaInfusionRecipe recipe = PatchouliUtils.getRecipe(ModRecipeTypes.MANA_INFUSION_TYPE, new ResourceLocation(s.asString()));
				if (recipe != null) {
					builder.add(recipe);
				}
			}
		}

		this.recipes = builder.build();
		this.hasCustomHeading = variables.has("heading");
	}

	@Override
	public IVariable process(String key) {
		if (recipes.isEmpty()) {
			return null;
		}
		switch (key) {
			case "heading":
				if (!hasCustomHeading) {
					return IVariable.from(recipes.get(0).getResultItem().getHoverName());
				}
				return null;
			case "input":
				return PatchouliUtils.interweaveIngredients(recipes.stream().map(r -> r.getIngredients().get(0)).collect(Collectors.toList()));
			case "output":
				return IVariable.wrapList(recipes.stream().map(IManaInfusionRecipe::getResultItem).map(IVariable::from).collect(Collectors.toList()));
			case "catalyst":
				return IVariable.wrapList(recipes.stream().map(IManaInfusionRecipe::getRecipeCatalyst)
						.flatMap(ingr -> {
							if (ingr == null) {
								return Stream.of(ItemStack.EMPTY);
							}
							return ingr.getDisplayedStacks().stream();
						})
						.map(IVariable::from)
						.collect(Collectors.toList()));
			case "mana":
				return IVariable.wrapList(recipes.stream().mapToInt(IManaInfusionRecipe::getManaToConsume).mapToObj(IVariable::wrap).collect(Collectors.toList()));
			case "drop":
				Component q = new TextComponent("(?)").withStyle(ChatFormatting.BOLD);
				return IVariable.from(new TranslatableComponent("botaniamisc.drop").append(" ").append(q));
			case "dropTip0":
			case "dropTip1":
				Component drop = new KeybindComponent("key.drop").withStyle(ChatFormatting.GREEN);
				return IVariable.from(new TranslatableComponent("botaniamisc." + key, drop));
		}
		return null;
	}
}
