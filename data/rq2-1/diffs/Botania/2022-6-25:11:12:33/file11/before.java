/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.fabric.integration.rei;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import me.shedaniel.math.Point;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.DefaultStrippingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomDisplay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.item.IAncientWillContainer;
import vazkii.botania.api.recipe.IOrechidRecipe;
import vazkii.botania.client.core.handler.CorporeaInputHandler;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.crafting.*;
import vazkii.botania.common.item.ItemAncientWill;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraPick;
import vazkii.botania.common.item.lens.ItemLens;
import vazkii.botania.common.lib.ModTags;
import vazkii.botania.fabric.xplat.FabricXplatImpl;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class BotaniaREIPlugin implements REIClientPlugin {
	public BotaniaREIPlugin() {
		CorporeaInputHandler.hoveredStackGetter = BotaniaREIPlugin::getHoveredREIStack;
		CorporeaInputHandler.supportedGuiFilter = CorporeaInputHandler.supportedGuiFilter.or(s -> s instanceof DisplayScreen);
	}

	@Override
	public void registerCategories(CategoryRegistry helper) {
		helper.add(List.of(
				new PetalApothecaryREICategory(),
				new PureDaisyREICategory(),
				new ManaPoolREICategory(),
				new RunicAltarREICategory(),
				new ElvenTradeREICategory(),
				new BreweryREICategory(),
				new TerraPlateREICategory(),
				new OrechidREICategory(BotaniaREICategoryIdentifiers.ORECHID, ModSubtiles.orechid),
				new OrechidREICategory(BotaniaREICategoryIdentifiers.ORECHID_IGNEM, ModSubtiles.orechidIgnem),
				new OrechidREICategory(BotaniaREICategoryIdentifiers.MARIMORPHOSIS, ModSubtiles.marimorphosis)
		));

		helper.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(ModItems.craftingHalo), EntryStacks.of(ModItems.autocraftingHalo));
		Set<ItemLike> apothecaries = ImmutableSet.of(
				ModBlocks.defaultAltar,
				ModBlocks.desertAltar,
				ModBlocks.forestAltar,
				ModBlocks.fungalAltar,
				ModBlocks.mesaAltar,
				ModBlocks.mossyAltar,
				ModBlocks.mountainAltar,
				ModBlocks.plainsAltar,
				ModBlocks.swampAltar,
				ModBlocks.taigaAltar);
		for (ItemLike altar : apothecaries) {
			helper.addWorkstations(BotaniaREICategoryIdentifiers.PETAL_APOTHECARY, EntryStacks.of(altar));
		}
		helper.addWorkstations(BotaniaREICategoryIdentifiers.BREWERY, EntryStacks.of(ModBlocks.brewery));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.ELVEN_TRADE, EntryStacks.of(ModBlocks.alfPortal));
		Set<ItemLike> manaPools = ImmutableSet.of(
				ModBlocks.manaPool,
				ModBlocks.dilutedPool,
				ModBlocks.fabulousPool
		);
		for (ItemLike pool : manaPools) {
			helper.addWorkstations(BotaniaREICategoryIdentifiers.MANA_INFUSION, EntryStacks.of(pool));
		}
		helper.addWorkstations(BotaniaREICategoryIdentifiers.ORECHID, EntryStacks.of(ModSubtiles.orechid), EntryStacks.of(ModSubtiles.orechidFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.ORECHID_IGNEM, EntryStacks.of(ModSubtiles.orechidIgnem), EntryStacks.of(ModSubtiles.orechidIgnemFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.MARIMORPHOSIS, EntryStacks.of(ModSubtiles.marimorphosis), EntryStacks.of(ModSubtiles.marimorphosisFloating),
				EntryStacks.of(ModSubtiles.marimorphosisChibi), EntryStacks.of(ModSubtiles.marimorphosisChibiFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.PURE_DAISY, EntryStacks.of(ModSubtiles.pureDaisy), EntryStacks.of(ModSubtiles.pureDaisyFloating));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.RUNE_ALTAR, EntryStacks.of(ModBlocks.runeAltar));
		helper.addWorkstations(BotaniaREICategoryIdentifiers.TERRA_PLATE, EntryStacks.of(ModBlocks.terraPlate));

		helper.removePlusButton(BotaniaREICategoryIdentifiers.PETAL_APOTHECARY);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.BREWERY);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.ELVEN_TRADE);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.MANA_INFUSION);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.ORECHID);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.ORECHID_IGNEM);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.MARIMORPHOSIS);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.PURE_DAISY);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.RUNE_ALTAR);
		helper.removePlusButton(BotaniaREICategoryIdentifiers.TERRA_PLATE);
	}

	@Override
	public void registerDisplays(DisplayRegistry helper) {
		registerAncientWillRecipeWrapper(helper);
		registerCompositeLensRecipeWrapper(helper);
		registerTerraPickTippingRecipeWrapper(helper);

		helper.registerFiller(RecipePetals.class, PetalApothecaryREIDisplay::new);
		helper.registerFiller(RecipeBrew.class, BreweryREIDisplay::new);
		Predicate<? extends RecipeElvenTrade> pred = recipe -> !recipe.isReturnRecipe();
		helper.registerFiller(RecipeElvenTrade.class, pred, ElvenTradeREIDisplay::new);
		helper.registerFiller(LexiconElvenTradeRecipe.class, ElvenTradeREIDisplay::new);
		helper.registerFiller(RecipeManaInfusion.class, ManaPoolREIDisplay::new);
		helper.registerFiller(RecipePureDaisy.class, PureDaisyREIDisplay::new);
		helper.registerFiller(RecipeRuneAltar.class, RunicAltarREIDisplay::new);
		helper.registerFiller(RecipeTerraPlate.class, TerraPlateREIDisplay::new);

		try {
			for (var entry : FabricXplatImpl.CUSTOM_STRIPPING.entrySet()) {
				helper.add(new DefaultStrippingDisplay(EntryStacks.of(entry.getKey()), EntryStacks.of(entry.getValue())));
			}
		} catch (Exception e) {
			BotaniaAPI.LOGGER.error("Error adding strippable entry to REI", e);
		}

		Object2IntMap<Block> weights = getWeights(ModRecipeTypes.ORECHID_TYPE, helper.getRecipeManager());
		helper.registerRecipeFiller(RecipeOrechid.class, ModRecipeTypes.ORECHID_TYPE,
				r -> new OrechidREIDisplay(r, weights.getInt(r.getInput())));

		Object2IntMap<Block> weightsIgnem = getWeights(ModRecipeTypes.ORECHID_IGNEM_TYPE, helper.getRecipeManager());
		helper.registerRecipeFiller(RecipeOrechidIgnem.class, ModRecipeTypes.ORECHID_IGNEM_TYPE,
				r -> new OrechidIgnemREIDisplay(r, weightsIgnem.getInt(r.getInput())));

		Object2IntMap<Block> weightsMarim = getWeights(ModRecipeTypes.MARIMORPHOSIS_TYPE, helper.getRecipeManager());
		helper.registerRecipeFiller(RecipeMarimorphosis.class, ModRecipeTypes.MARIMORPHOSIS_TYPE,
				r -> new MarimorphosisREIDisplay(r, weightsMarim.getInt(r.getInput())));
	}

	public static Object2IntMap<Block> getWeights(RecipeType<IOrechidRecipe> type, RecipeManager manager) {
		Object2IntOpenHashMap<Block> map = new Object2IntOpenHashMap<>();
		for (IOrechidRecipe recipe : manager.getAllRecipesFor(type)) {
			map.addTo(recipe.getInput(), recipe.getWeight());
		}
		return map;
	}

	void registerAncientWillRecipeWrapper(DisplayRegistry helper) {
		ImmutableList.Builder<EntryIngredient> input = ImmutableList.builder();
		ImmutableList.Builder<EntryStack<ItemStack>> output = ImmutableList.builder();
		Set<ItemStack> wills = ImmutableSet.of(new ItemStack(ModItems.ancientWillAhrim), new ItemStack(ModItems.ancientWillDharok), new ItemStack(ModItems.ancientWillGuthan), new ItemStack(ModItems.ancientWillKaril), new ItemStack(ModItems.ancientWillTorag), new ItemStack(ModItems.ancientWillVerac));
		IAncientWillContainer container = (IAncientWillContainer) ModItems.terrasteelHelm;

		ItemStack helmet = new ItemStack(ModItems.terrasteelHelm);
		input.add(EntryIngredients.of(helmet));
		input.add(EntryIngredients.ofItemStacks(wills));
		for (ItemStack will : wills) {
			ItemStack copy = helmet.copy();
			container.addAncientWill(copy, ((ItemAncientWill) will.getItem()).type);
			output.add(EntryStacks.of(copy));
		}
		helper.add(new DefaultCustomDisplay(null, input.build(), Collections.singletonList(EntryIngredient.of(output.build()))));
	}

	void registerCompositeLensRecipeWrapper(DisplayRegistry helper) {
		List<ItemStack> lensStacks =
				StreamSupport.stream(Registry.ITEM.getTagOrEmpty(ModTags.Items.LENS).spliterator(), false)
						.map(ItemStack::new)
						.filter(s -> !((ItemLens) s.getItem()).isControlLens(s))
						.filter(s -> ((ItemLens) s.getItem()).isCombinable(s))
						.toList();
		List<Item> lenses = lensStacks.stream().map(ItemStack::getItem).toList();
		List<EntryIngredient> inputs = Arrays.asList(EntryIngredients.ofItemStacks(lensStacks), EntryIngredients.of(new ItemStack(Items.SLIME_BALL)), EntryIngredients.ofItemStacks(lensStacks));
		int end = lenses.size() - 1;

		List<ItemStack> firstInput = new ArrayList<>();
		List<ItemStack> secondInput = new ArrayList<>();
		List<ItemStack> outputs = new ArrayList<>();
		for (int i = 1; i <= end; i++) {
			ItemStack firstLens = new ItemStack(lenses.get(i));
			for (Item secondLens : lenses) {
				if (secondLens == firstLens.getItem()) {
					continue;
				}

				ItemStack secondLensStack = new ItemStack(secondLens);
				if (((ItemLens) firstLens.getItem()).canCombineLenses(firstLens, secondLensStack)) {
					firstInput.add(firstLens);
					secondInput.add(secondLensStack);
					outputs.add(((ItemLens) firstLens.getItem()).setCompositeLens(firstLens.copy(), secondLensStack));
				}
			}
		}
		inputs.set(0, EntryIngredients.ofItemStacks(firstInput));
		inputs.set(2, EntryIngredients.ofItemStacks(secondInput));

		helper.add(new DefaultCustomDisplay(null, inputs, Collections.singletonList(EntryIngredients.ofItemStacks(outputs))));
	}

	void registerTerraPickTippingRecipeWrapper(DisplayRegistry helper) {
		List<EntryIngredient> inputs = ImmutableList.of(EntryIngredients.of(ModItems.terraPick), EntryIngredients.of(ModItems.elementiumPick));
		ItemStack output = new ItemStack(ModItems.terraPick);
		ItemTerraPick.setTipped(output);

		helper.add(new DefaultCustomDisplay(null, inputs, Collections.singletonList(EntryIngredients.of(output))));
	}

	private static ItemStack getHoveredREIStack() {
		return REIRuntime.getInstance().getOverlay().map(o -> {
			ItemStack stack;
			if (REIRuntime.getInstance().isOverlayVisible()) {
				stack = unwrapEntry(o.getEntryList().getFocusedStack());
				if (!stack.isEmpty()) {
					return stack;
				}
				stack = o.getFavoritesList()
						.map(OverlayListWidget::getFocusedStack)
						.map(BotaniaREIPlugin::unwrapEntry)
						.orElse(ItemStack.EMPTY);
				if (!stack.isEmpty()) {
					return stack;
				}
			}
			if (Minecraft.getInstance().screen instanceof DisplayScreen) {
				Point point = PointHelper.ofMouse();
				for (var child : Minecraft.getInstance().screen.children()) {
					if (child.isMouseOver(point.x, point.y) && child instanceof Slot slot) {
						stack = unwrapEntry(slot.getCurrentEntry());
						if (!stack.isEmpty()) {
							return stack;
						}
					}
				}
			}
			return ItemStack.EMPTY;
		}).orElse(ItemStack.EMPTY);
	}

	private static ItemStack unwrapEntry(@Nullable EntryStack<?> stack) {
		if (stack != null && !stack.isEmpty() && stack.getType() == VanillaEntryTypes.ITEM) {
			return (ItemStack) stack.getValue();
		}
		return ItemStack.EMPTY;
	}
}
