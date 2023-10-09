package com.simibubi.create.compat.jei.category;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.simibubi.create.AllFluids;
import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.content.contraptions.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.contraptions.processing.ProcessingOutput;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

public abstract class CreateRecipeCategory<T extends Recipe<?>> implements IRecipeCategory<T> {

	public final List<Supplier<List<? extends Recipe<?>>>> recipes = new ArrayList<>();
	public final List<Supplier<? extends ItemStack>> recipeCatalysts = new ArrayList<>();

	protected ResourceLocation uid;
	protected String name;
	private IDrawable background;
	private IDrawable icon;

	public CreateRecipeCategory(IDrawable icon, IDrawable background) {
		this.background = background;
		this.icon = icon;
	}

	public void setCategoryId(String name) {
		this.uid = Create.asResource(name);
		this.name = name;
	}

	@Override
	public ResourceLocation getUid() {
		return uid;
	}

	@Override
	public Component getTitle() {
		return Lang.translate("recipe." + name);
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	public static AllGuiTextures getRenderedSlot(Recipe<?> recipe, int index) {
		AllGuiTextures jeiSlot = AllGuiTextures.JEI_SLOT;
		if (!(recipe instanceof ProcessingRecipe))
			return jeiSlot;
		ProcessingRecipe<?> processingRecipe = (ProcessingRecipe<?>) recipe;
		List<ProcessingOutput> rollableResults = processingRecipe.getRollableResults();
		if (rollableResults.size() <= index)
			return jeiSlot;
		if (processingRecipe.getRollableResults()
			.get(index)
			.getChance() == 1)
			return jeiSlot;
		return AllGuiTextures.JEI_CHANCE_SLOT;
	}

	public static IDrawable emptyBackground(int width, int height) {
		return new EmptyBackground(width, height);
	}

	public static IDrawable doubleItemIcon(ItemLike item1, ItemLike item2) {
		return new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2));
	}

	public static IDrawable itemIcon(ItemLike item) {
		return new DoubleItemIcon(() -> new ItemStack(item), () -> ItemStack.EMPTY);
	}

	public static void addStochasticTooltip(IGuiItemStackGroup itemStacks, List<ProcessingOutput> results) {
		addStochasticTooltip(itemStacks, results, 1);
	}

	public static void addStochasticTooltip(IGuiItemStackGroup itemStacks, List<ProcessingOutput> results,
		int startIndex) {
		itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
			if (input)
				return;
			if (slotIndex < startIndex)
				return;
			ProcessingOutput output = results.get(slotIndex - startIndex);
			float chance = output.getChance();
			if (chance != 1)
				tooltip.add(1, Lang.translate("recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
					.withStyle(ChatFormatting.GOLD));
		});
	}

	public static IRecipeSlotTooltipCallback addStochasticTooltip(ProcessingOutput output) {
		return (view, tooltip) -> {
			float chance = output.getChance();
			if (chance != 1)
				tooltip.add(1, Lang.translate("recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
					.withStyle(ChatFormatting.GOLD));
		};
	}

	public static List<FluidStack> withImprovedVisibility(List<FluidStack> stacks) {
		return stacks.stream()
			.map(CreateRecipeCategory::withImprovedVisibility)
			.collect(Collectors.toList());
	}

	public static FluidStack withImprovedVisibility(FluidStack stack) {
		FluidStack display = stack.copy();
		int displayedAmount = (int) (stack.getAmount() * .75f) + 250;
		display.setAmount(displayedAmount);
		return display;
	}

	public static void addFluidTooltip(IGuiFluidStackGroup fluidStacks, List<FluidIngredient> inputs,
		List<FluidStack> outputs) {
		addFluidTooltip(fluidStacks, inputs, outputs, -1);
	}

	public static void addFluidTooltip(IGuiFluidStackGroup fluidStacks, List<FluidIngredient> inputs,
		List<FluidStack> outputs, int index) {
		List<Integer> amounts = new ArrayList<>();
		inputs.forEach(f -> amounts.add(f.getRequiredAmount()));
		outputs.forEach(f -> amounts.add(f.getAmount()));

		fluidStacks.addTooltipCallback((slotIndex, input, fluid, tooltip) -> {
			if (index != -1 && slotIndex != index)
				return;

			if (fluid.getFluid()
				.isSame(AllFluids.POTION.get())) {
				Component name = fluid.getDisplayName();
				if (tooltip.isEmpty())
					tooltip.add(0, name);
				else
					tooltip.set(0, name);

				ArrayList<Component> potionTooltip = new ArrayList<>();
				PotionFluidHandler.addPotionTooltip(fluid, potionTooltip, 1);
				tooltip.addAll(1, potionTooltip.stream()
					.collect(Collectors.toList()));
			}

			int amount = amounts.get(index != -1 ? 0 : slotIndex);
			Component text = new TextComponent(String.valueOf(amount)).append(Lang.translate("generic.unit.millibuckets")).withStyle(ChatFormatting.GOLD);
			if (tooltip.isEmpty())
				tooltip.add(0, text);
			else {
				List<Component> siblings = tooltip.get(0)
					.getSiblings();
				siblings.add(new TextComponent(" "));
				siblings.add(text);
			}
		});
	}

}
