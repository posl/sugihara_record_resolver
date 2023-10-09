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

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.lib.ResourceLocationHelper;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class OrechidREICategory implements DisplayCategory<OrechidBaseREIDisplay<?>> {
	private final EntryStack<ItemStack> orechid;
	private final CategoryIdentifier<? extends OrechidBaseREIDisplay<?>> categoryId;
	private final String langKey;
	private final ResourceLocation OVERLAY = ResourceLocationHelper.prefix("textures/gui/pure_daisy_overlay.png");

	public OrechidREICategory(CategoryIdentifier<? extends OrechidBaseREIDisplay<?>> categoryId, Block orechid) {
		this.categoryId = categoryId;
		this.orechid = EntryStacks.of(orechid);
		this.langKey = "botania.nei." + (orechid == ModSubtiles.orechidIgnem ? "orechidIgnem" : Registry.BLOCK.getKey(orechid).getPath());
	}

	@Override
	public @Nonnull CategoryIdentifier<? extends OrechidBaseREIDisplay<?>> getCategoryIdentifier() {
		return categoryId;
	}

	@Override
	public @Nonnull Renderer getIcon() {
		return orechid;
	}

	@Override
	public @Nonnull Component getTitle() {
		return Component.translatable(langKey);
	}

	@Override
	public @Nonnull List<Widget> setupDisplay(OrechidBaseREIDisplay<?> display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		Point center = new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 9);

		widgets.add(Widgets.createRecipeBase(bounds));
		widgets.add(Widgets.createDrawableWidget(((helper, matrices, mouseX, mouseY, delta) -> CategoryUtils.drawOverlay(helper, matrices, OVERLAY, center.x - 23, center.y - 13, 0, 0, 65, 44))));
		widgets.add(Widgets.createSlot(center).entry(orechid).disableBackground());
		widgets.add(Widgets.createSlot(new Point(center.x - 30, center.y)).entries(display.getInputEntries().get(0)).disableBackground());
		widgets.add(Widgets.createSlot(new Point(center.x + 29, center.y)).entries(display.getOutputEntries().get(0)).disableBackground());
		return widgets;
	}

	@Override
	public int getDisplayHeight() {
		return 54;
	}
}
