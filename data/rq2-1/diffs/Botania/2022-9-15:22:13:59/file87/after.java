/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.crafting.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.common.item.BotaniaItems;

public class SpellbindingClothRecipe extends CustomRecipe {
	public static final SimpleRecipeSerializer<SpellbindingClothRecipe> SERIALIZER = new SimpleRecipeSerializer<>(SpellbindingClothRecipe::new);

	public SpellbindingClothRecipe(ResourceLocation id) {
		super(id);
	}

	@Override
	public boolean matches(@NotNull CraftingContainer inv, @NotNull Level world) {
		boolean foundCloth = false;
		boolean foundEnchanted = false;

		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) {
				if (stack.isEnchanted() && !foundEnchanted && !stack.is(BotaniaItems.spellCloth)) {
					foundEnchanted = true;
				} else if (stack.is(BotaniaItems.spellCloth) && !foundCloth) {
					foundCloth = true;
				} else {
					return false; // Found an invalid item, breaking the recipe
				}
			}
		}

		return foundCloth && foundEnchanted;
	}

	@NotNull
	@Override
	public ItemStack assemble(@NotNull CraftingContainer inv) {
		ItemStack stackToDisenchant = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.isEnchanted() && !stack.is(BotaniaItems.spellCloth)) {
				stackToDisenchant = stack.copy();
				stackToDisenchant.setCount(1);
				break;
			}
		}

		if (stackToDisenchant.isEmpty()) {
			return ItemStack.EMPTY;
		}

		stackToDisenchant.removeTagKey("Enchantments"); // Remove enchantments
		stackToDisenchant.removeTagKey("RepairCost");
		return stackToDisenchant;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@NotNull
	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@NotNull
	@Override
	public NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer inv) {
		return RecipeUtils.getRemainingItemsSub(inv, s -> {
			if (s.is(BotaniaItems.spellCloth)) {
				ItemStack copy = s.copy();
				copy.setCount(1);
				copy.setDamageValue(copy.getDamageValue() + 1);
				return copy;
			}
			return null;
		});
	}
}
