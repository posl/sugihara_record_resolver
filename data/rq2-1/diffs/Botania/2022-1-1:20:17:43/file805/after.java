/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AccessorAbstractFurnaceBlockEntity {
	@Invoker("canBurn")
	static boolean botania_canAcceptRecipeOutput(@Nullable Recipe<?> recipe, NonNullList<ItemStack> items, int maxStackSize) {
		throw new IllegalStateException();
	}

	@Accessor("items")
	NonNullList<ItemStack> getItems();

	@Accessor
	RecipeType<? extends AbstractCookingRecipe> getRecipeType();

	@Accessor
	int getLitTime();

	@Accessor
	void setLitTime(int burnTime);

	@Accessor
	int getCookingProgress();

	@Accessor
	int getCookingTotalTime();

	@Accessor
	void setCookingProgress(int cookTime);
}
