/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.gui;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.lwjgl.opengl.GL11;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.api.recipe.ManaInfusionRecipe;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.lib.ResourcesLib;
import vazkii.botania.common.block.BotaniaBlocks;
import vazkii.botania.common.block.block_entity.PetalApothecaryBlockEntity;
import vazkii.botania.common.block.block_entity.RunicAltarBlockEntity;
import vazkii.botania.common.block.block_entity.corporea.CorporeaCrystalCubeBlockEntity;
import vazkii.botania.common.block.block_entity.corporea.CorporeaIndexBlockEntity;
import vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity;
import vazkii.botania.common.handler.EquipmentHandler;
import vazkii.botania.common.helper.PlayerHelper;
import vazkii.botania.common.item.AssemblyHaloItem;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.item.WandOfTheForestItem;
import vazkii.botania.common.item.WorldshaperssSextantItem;
import vazkii.botania.common.item.equipment.bauble.FlugelTiaraItem;
import vazkii.botania.common.item.equipment.bauble.ManaseerMonocleItem;
import vazkii.botania.common.item.equipment.bauble.RingOfDexterousMotionItem;
import vazkii.botania.common.lib.ModTags;
import vazkii.botania.xplat.BotaniaConfig;
import vazkii.botania.xplat.IClientXplatAbstractions;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.List;

public final class HUDHandler {

	private HUDHandler() {}

	public static final ResourceLocation manaBar = new ResourceLocation(ResourcesLib.GUI_MANA_HUD);

	public static void onDrawScreenPost(PoseStack ms, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) {
			return;
		}
		ProfilerFiller profiler = mc.getProfiler();
		ItemStack main = mc.player.getMainHandItem();
		ItemStack offhand = mc.player.getOffhandItem();

		profiler.push("botania-hud");

		if (Minecraft.getInstance().gameMode.canHurtPlayer()) {
			ItemStack tiara = EquipmentHandler.findOrEmpty(BotaniaItems.flightTiara, mc.player);
			if (!tiara.isEmpty()) {
				profiler.push("flugelTiara");
				FlugelTiaraItem.ClientLogic.renderHUD(ms, mc.player, tiara);
				profiler.pop();
			}

			ItemStack dodgeRing = EquipmentHandler.findOrEmpty(BotaniaItems.dodgeRing, mc.player);
			if (!dodgeRing.isEmpty()) {
				profiler.push("dodgeRing");
				RingOfDexterousMotionItem.ClientLogic.renderHUD(ms, mc.player, dodgeRing, partialTicks);
				profiler.pop();
			}
		}

		HitResult pos = mc.hitResult;

		if (pos instanceof BlockHitResult result) {
			BlockPos bpos = result.getBlockPos();

			BlockState state = mc.level.getBlockState(bpos);
			BlockEntity tile = mc.level.getBlockEntity(bpos);

			if (PlayerHelper.hasAnyHeldItem(mc.player)) {
				if (PlayerHelper.hasHeldItemClass(mc.player, WandOfTheForestItem.class)) {
					var hud = IClientXplatAbstractions.INSTANCE.findWandHud(mc.level, bpos, state, tile);
					if (hud != null) {
						profiler.push("wandItem");
						hud.renderHUD(ms, mc);
						profiler.pop();
					}
				}
				if (tile instanceof ManaPoolBlockEntity pool && !mc.player.getMainHandItem().isEmpty()) {
					renderPoolRecipeHUD(ms, pool, mc.player.getMainHandItem());
				}
			}
			if (!PlayerHelper.hasHeldItem(mc.player, BotaniaItems.lexicon)) {
				if (tile instanceof PetalApothecaryBlockEntity altar) {
					PetalApothecaryBlockEntity.Hud.render(altar, ms, mc);
				} else if (tile instanceof RunicAltarBlockEntity runeAltar) {
					RunicAltarBlockEntity.Hud.render(runeAltar, ms, mc);
				} else if (tile instanceof CorporeaCrystalCubeBlockEntity cube) {
					renderCrystalCubeHUD(ms, cube);
				}
			}
		}

		if (!CorporeaIndexBlockEntity.getNearbyValidIndexes(mc.player).isEmpty() && mc.screen instanceof ChatScreen) {
			profiler.push("nearIndex");
			renderNearIndexDisplay(ms);
			profiler.pop();
		}

		if (!main.isEmpty() && main.getItem() instanceof AssemblyHaloItem) {
			profiler.push("craftingHalo_main");
			AssemblyHaloItem.Rendering.renderHUD(ms, mc.player, main);
			profiler.pop();
		} else if (!offhand.isEmpty() && offhand.getItem() instanceof AssemblyHaloItem) {
			profiler.push("craftingHalo_off");
			AssemblyHaloItem.Rendering.renderHUD(ms, mc.player, offhand);
			profiler.pop();
		}

		if (!main.isEmpty() && main.getItem() instanceof WorldshaperssSextantItem) {
			profiler.push("sextant");
			WorldshaperssSextantItem.Hud.render(ms, mc.player, main);
			profiler.pop();
		}

		/*if(equippedStack != null && equippedStack.is(BotaniaItems.flugelEye)) {
			profiler.startSection("flugelEye");
			EyeOfTheFlugelItem.renderHUD(event.getResolution(), mc.player, equippedStack);
			profiler.endSection();
		}*/

		if (ManaseerMonocleItem.hasMonocle(mc.player)) {
			profiler.push("monocle");
			ManaseerMonocleItem.Hud.render(ms, mc.player);
			profiler.pop();
		}

		profiler.push("manaBar");

		Player player = mc.player;
		if (!player.isSpectator()) {
			int totalMana = 0;
			int totalMaxMana = 0;
			boolean anyRequest = false;

			Container mainInv = player.getInventory();
			Container accInv = BotaniaAPI.instance().getAccessoriesInventory(player);

			int invSize = mainInv.getContainerSize();
			int size = invSize + accInv.getContainerSize();

			for (int i = 0; i < size; i++) {
				boolean useAccessories = i >= invSize;
				Container inv = useAccessories ? accInv : mainInv;
				ItemStack stack = inv.getItem(i - (useAccessories ? invSize : 0));

				if (!stack.isEmpty()) {
					anyRequest = anyRequest || stack.is(ModTags.Items.MANA_USING_ITEMS);
				}
			}

			List<ItemStack> items = ManaItemHandler.instance().getManaItems(player);
			List<ItemStack> acc = ManaItemHandler.instance().getManaAccesories(player);
			for (ItemStack stack : Iterables.concat(items, acc)) {
				var manaItem = IXplatAbstractions.INSTANCE.findManaItem(stack);
				if (!manaItem.isNoExport()) {
					totalMana += manaItem.getMana();
					totalMaxMana += manaItem.getMaxMana();
				}
			}

			if (anyRequest) {
				renderManaInvBar(ms, totalMana, totalMaxMana);
			}
		}

		profiler.popPush("itemsRemaining");
		ItemsRemainingRenderHandler.render(ms, partialTicks);
		profiler.pop();
		profiler.pop();

		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
	}

	private static void renderManaInvBar(PoseStack ms, int totalMana, int totalMaxMana) {
		Minecraft mc = Minecraft.getInstance();
		int width = 182;
		int x = mc.getWindow().getGuiScaledWidth() / 2 - width / 2;
		int y = mc.getWindow().getGuiScaledHeight() - BotaniaConfig.client().manaBarHeight();

		if (totalMaxMana == 0) {
			width = 0;
		} else {
			width *= (double) totalMana / (double) totalMaxMana;
		}

		if (width == 0) {
			if (totalMana > 0) {
				width = 1;
			} else {
				return;
			}
		}

		int color = Mth.hsvToRgb(0.55F, (float) Math.min(1F, Math.sin(Util.getMillis() / 200D) * 0.5 + 1F), 1F);
		int r = (color >> 16 & 0xFF);
		int g = (color >> 8 & 0xFF);
		int b = color & 0xFF;
		RenderSystem.setShaderColor(r / 255F, g / 255F, b / 255F, 1 - (r / 255F));
		RenderSystem.setShaderTexture(0, manaBar);

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		RenderHelper.drawTexturedModalRect(ms, x, y, 0, 251, width, 5);
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static void renderPoolRecipeHUD(PoseStack ms, ManaPoolBlockEntity tile, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();

		profiler.push("poolRecipe");
		ManaInfusionRecipe recipe = tile.getMatchingRecipe(stack, tile.getLevel().getBlockState(tile.getBlockPos().below()));
		if (recipe != null) {
			int x = mc.getWindow().getGuiScaledWidth() / 2 - 11;
			int y = mc.getWindow().getGuiScaledHeight() / 2 + 10;

			int u = tile.getCurrentMana() >= recipe.getManaToConsume() ? 0 : 22;
			int v = mc.player.getName().getString().equals("haighyorkie") && mc.player.isShiftKeyDown() ? 23 : 8;

			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			RenderSystem.setShaderTexture(0, manaBar);
			RenderHelper.drawTexturedModalRect(ms, x, y, u, v, 22, 15);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

			mc.getItemRenderer().renderAndDecorateItem(stack, x - 20, y);
			mc.getItemRenderer().renderAndDecorateItem(recipe.getResultItem(), x + 26, y);
			mc.getItemRenderer().renderGuiItemDecorations(mc.font, recipe.getResultItem(), x + 26, y);

			RenderSystem.disableBlend();
		}
		profiler.pop();
	}

	private static void renderCrystalCubeHUD(PoseStack ms, CorporeaCrystalCubeBlockEntity tile) {
		Minecraft mc = Minecraft.getInstance();
		ProfilerFiller profiler = mc.getProfiler();

		profiler.push("crystalCube");
		ItemStack target = tile.getRequestTarget();
		if (!target.isEmpty()) {
			String s1 = target.getHoverName().getString();
			String s2 = tile.getItemCount() + "x";
			int strlen = Math.max(mc.font.width(s1), mc.font.width(s2));
			int w = mc.getWindow().getGuiScaledWidth();
			int h = mc.getWindow().getGuiScaledHeight();
			int boxH = h / 2 + (tile.locked ? 20 : 10);
			GuiComponent.fill(ms, w / 2 + 8, h / 2 - 12, w / 2 + strlen + 32, boxH, 0x44000000);
			GuiComponent.fill(ms, w / 2 + 6, h / 2 - 14, w / 2 + strlen + 34, boxH + 2, 0x44000000);

			mc.font.drawShadow(ms, s1, w / 2 + 30, h / 2 - 10, 0x6666FF);
			mc.font.drawShadow(ms, tile.getItemCount() + "x", w / 2 + 30, h / 2, 0xFFFFFF);
			if (tile.locked) {
				mc.font.drawShadow(ms, I18n.get("botaniamisc.locked"), w / 2 + 30, h / 2 + 10, 0xFFAA00);
			}
			mc.getItemRenderer().renderAndDecorateItem(target, w / 2 + 10, h / 2 - 10);
		}

		profiler.pop();
	}

	private static void renderNearIndexDisplay(PoseStack ms) {
		Minecraft mc = Minecraft.getInstance();
		String txt0 = I18n.get("botaniamisc.nearIndex0");
		String txt1 = ChatFormatting.GRAY + I18n.get("botaniamisc.nearIndex1");
		String txt2 = ChatFormatting.GRAY + I18n.get("botaniamisc.nearIndex2");

		int l = Math.max(mc.font.width(txt0), Math.max(mc.font.width(txt1), mc.font.width(txt2))) + 20;
		int x = mc.getWindow().getGuiScaledWidth() - l - 20;
		int y = mc.getWindow().getGuiScaledHeight() - 60;

		GuiComponent.fill(ms, x - 6, y - 6, x + l + 6, y + 37, 0x44000000);
		GuiComponent.fill(ms, x - 4, y - 4, x + l + 4, y + 35, 0x44000000);
		mc.getItemRenderer().renderAndDecorateItem(new ItemStack(BotaniaBlocks.corporeaIndex), x, y + 10);

		mc.font.drawShadow(ms, txt0, x + 20, y, 0xFFFFFF);
		mc.font.drawShadow(ms, txt1, x + 20, y + 14, 0xFFFFFF);
		mc.font.drawShadow(ms, txt2, x + 20, y + 24, 0xFFFFFF);
	}

	public static void drawSimpleManaHUD(PoseStack ms, int color, int mana, int maxMana, String name) {
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		Minecraft mc = Minecraft.getInstance();
		int x = mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(name) / 2;
		int y = mc.getWindow().getGuiScaledHeight() / 2 + 10;

		mc.font.drawShadow(ms, name, x, y, color);

		x = mc.getWindow().getGuiScaledWidth() / 2 - 51;
		y += 10;

		renderManaBar(ms, x, y, color, 1F, mana, maxMana);

		RenderSystem.disableBlend();
	}

	public static void drawComplexManaHUD(int color, PoseStack ms, int mana, int maxMana, String name, ItemStack bindDisplay, boolean properlyBound) {
		drawSimpleManaHUD(ms, color, mana, maxMana, name);

		Minecraft mc = Minecraft.getInstance();

		int x = mc.getWindow().getGuiScaledWidth() / 2 + 55;
		int y = mc.getWindow().getGuiScaledHeight() / 2 + 12;

		mc.getItemRenderer().renderAndDecorateItem(bindDisplay, x, y);

		RenderSystem.disableDepthTest();
		ms.pushPose();
		// renderAndDecorateItem draws at blitOffset + 50, + 200 (further down the call stack).
		// We want the checkmark on top of that. yeah these numbers are pretty arbitrary and dumb
		ms.translate(0, 0, mc.getItemRenderer().blitOffset + 50 + 200 + 1);
		if (properlyBound) {
			mc.font.drawShadow(ms, "\u2714", x + 10, y + 9, 0x004C00);
			mc.font.drawShadow(ms, "\u2714", x + 10, y + 8, 0x0BD20D);
		} else {
			mc.font.drawShadow(ms, "\u2718", x + 10, y + 9, 0x4C0000);
			mc.font.drawShadow(ms, "\u2718", x + 10, y + 8, 0xD2080D);
		}
		ms.popPose();
		RenderSystem.enableDepthTest();
	}

	public static void renderManaBar(PoseStack ms, int x, int y, int color, float alpha, int mana, int maxMana) {
		Minecraft mc = Minecraft.getInstance();

		RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
		RenderSystem.setShaderTexture(0, manaBar);
		RenderHelper.drawTexturedModalRect(ms, x, y, 0, 0, 102, 5);

		int manaPercentage = Math.max(0, (int) ((double) mana / (double) maxMana * 100));

		if (manaPercentage == 0 && mana > 0) {
			manaPercentage = 1;
		}

		RenderHelper.drawTexturedModalRect(ms, x + 1, y + 1, 0, 5, 100, 3);

		float red = (color >> 16 & 0xFF) / 255F;
		float green = (color >> 8 & 0xFF) / 255F;
		float blue = (color & 0xFF) / 255F;
		RenderSystem.setShaderColor(red, green, blue, alpha);
		RenderHelper.drawTexturedModalRect(ms, x + 1, y + 1, 0, 5, Math.min(100, manaPercentage), 3);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
