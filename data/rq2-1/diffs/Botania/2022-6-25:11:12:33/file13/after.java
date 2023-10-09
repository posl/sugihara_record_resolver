/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.fabric.integration.rei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.lib.ResourceLocationHelper;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ElvenTradeREICategory implements DisplayCategory<ElvenTradeREIDisplay> {
	private final EntryStack<ItemStack> gateway = EntryStacks.of(new ItemStack(ModBlocks.alfPortal));
	private final ResourceLocation TRADE_OVERLAY = ResourceLocationHelper.prefix("textures/gui/elven_trade_overlay.png");

	@Override
	public @Nonnull CategoryIdentifier<ElvenTradeREIDisplay> getCategoryIdentifier() {
		return BotaniaREICategoryIdentifiers.ELVEN_TRADE;
	}

	@Override
	public @Nonnull Renderer getIcon() {
		return gateway;
	}

	@Override
	public @Nonnull Component getTitle() {
		return Component.translatable("botania.nei.elvenTrade");
	}

	@Override
	public @Nonnull List<Widget> setupDisplay(ElvenTradeREIDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		Point center = new Point(bounds.getCenterX() - 8, bounds.getCenterY() - 4);
		widgets.add(Widgets.createRecipeBase(bounds));
		widgets.add(Widgets.createDrawableWidget((helper, matrices, mouseX, mouseY, delta) -> {
			CategoryUtils.drawOverlay(helper, matrices, TRADE_OVERLAY, center.x - 45, center.y - 34, 20, 19, 71, 75);
			drawPortal(matrices, center);
		}));
		int x = center.x - 20;
		for (EntryIngredient o : display.getInputEntries()) {
			widgets.add(Widgets.createSlot(new Point(x, center.y - 40)).entries(o).disableBackground());
			x += 18;
		}
		x = center.x + 28;
		for (EntryIngredient o : display.getOutputEntries()) {
			widgets.add(Widgets.createSlot(new Point(x, center.y)).entries(o).disableBackground());
			x += 18;
		}
		return widgets;
	}

	@Override
	public int getDisplayHeight() {
		return 100;
	}

	void drawPortal(PoseStack matrices, Point point) {
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocationHelper.prefix("block/alfheim_portal_swirl"));
		MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
		VertexConsumer v = immediate.getBuffer(RenderType.solid());
		int startX = point.x - 43;
		int startY = point.y - 17;
		int stopX = startX + 48;
		int stopY = startY + 48;
		Matrix4f mat = matrices.last().pose();
		Matrix3f n = matrices.last().normal();
		v.vertex(mat, startX, startY, 0).color(1f, 1f, 1f, 1f).uv(sprite.getU0(), sprite.getV0()).uv2(0xF000F0).normal(n, 1, 0, 0).endVertex();
		v.vertex(mat, startX, stopY, 0).color(1f, 1f, 1f, 1f).uv(sprite.getU0(), sprite.getV1()).uv2(0xF000F0).normal(n, 1, 0, 0).endVertex();
		v.vertex(mat, stopX, stopY, 0).color(1f, 1f, 1f, 1f).uv(sprite.getU1(), sprite.getV1()).uv2(0xF000F0).normal(n, 1, 0, 0).endVertex();
		v.vertex(mat, stopX, startY, 0).color(1f, 1f, 1f, 1f).uv(sprite.getU1(), sprite.getV0()).uv2(0xF000F0).normal(n, 1, 0, 0).endVertex();
		immediate.endBatch();
	}
}
