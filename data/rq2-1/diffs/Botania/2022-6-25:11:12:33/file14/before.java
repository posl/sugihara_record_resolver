/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.fabric.integration.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import vazkii.botania.api.recipe.StateIngredient;
import vazkii.botania.client.gui.HUDHandler;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.mana.TilePool;
import vazkii.botania.common.helper.ItemNBTHelper;
import vazkii.botania.common.lib.ResourceLocationHelper;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ManaPoolREICategory implements DisplayCategory<ManaPoolREIDisplay> {
	private final EntryStack<ItemStack> manaPool = EntryStacks.of(new ItemStack(ModBlocks.manaPool));
	private final ResourceLocation OVERLAY = ResourceLocationHelper.prefix("textures/gui/pure_daisy_overlay.png");

	@Override
	public @Nonnull CategoryIdentifier<ManaPoolREIDisplay> getCategoryIdentifier() {
		return BotaniaREICategoryIdentifiers.MANA_INFUSION;
	}

	@Override
	public @Nonnull Renderer getIcon() {
		return manaPool;
	}

	@Override
	public @Nonnull Component getTitle() {
		return new TranslatableComponent("botania.nei.manaPool");
	}

	@Override
	public @Nonnull List<Widget> setupDisplay(ManaPoolREIDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		ItemStack pool = manaPool.getValue().copy();
		ItemNBTHelper.setBoolean(pool, "RenderFull", true);
		EntryStack<ItemStack> renderPool = EntryStacks.of(pool);
		Point center = new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 14);

		widgets.add(Widgets.createRecipeBase(bounds));
		widgets.add(Widgets.createDrawableWidget(((helper, matrices, mouseX, mouseY, delta) -> {
			CategoryUtils.drawOverlay(helper, matrices, OVERLAY, center.x - 23, center.y - 13, 0, 0, 65, 44);
			HUDHandler.renderManaBar(matrices, center.x - 43, center.y + 37, 0x0000FF, 0.75F, display.getManaCost(), TilePool.MAX_MANA / 10);
		})));

		widgets.add(Widgets.createSlot(center).entry(renderPool).disableBackground());
		StateIngredient catalyst = display.getCatalyst();
		if (catalyst != null) {
			List<EntryStack<ItemStack>> entries = catalyst.getDisplayed()
					.stream().map(state -> EntryStacks.of(state.getBlock()))
					.collect(Collectors.toList());
			widgets.add(Widgets.createSlot(new Point(center.x - 50, center.y)).entries(entries).disableBackground());
		}
		widgets.add(Widgets.createSlot(new Point(center.x - 30, center.y)).entries(display.getInputEntries().get(0)).disableBackground());
		widgets.add(Widgets.createSlot(new Point(center.x + 29, center.y)).entries(display.getOutputEntries().get(0)).disableBackground());
		return widgets;
	}

	@Override
	public int getDisplayHeight() {
		return 65;
	}
}
