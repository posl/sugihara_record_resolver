/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.core.handler;

import net.minecraft.resources.ResourceLocation;

import vazkii.botania.client.lib.LibResources;
import vazkii.botania.common.entity.EntityDoppleganger;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class BossBarHandler {

	private BossBarHandler() {}

	// Only access on the client thread!
	public static final Set<EntityDoppleganger> bosses = Collections.newSetFromMap(new WeakHashMap<>());
	public static final ResourceLocation defaultBossBar = new ResourceLocation(LibResources.GUI_BOSS_BAR);

	/* todo 1.16-fabric
	public static void onBarRender(RenderGameOverlayEvent.BossInfo evt) {
		UUID infoUuid = evt.getBossInfo().getUuid();
		for (EntityDoppleganger currentBoss : bosses) {
			if (currentBoss.getBossInfoUuid().equals(infoUuid)) {
				MatrixStack ms = evt.getMatrixStack();
				evt.setCanceled(true);
	
				MinecraftClient mc = MinecraftClient.getInstance();
				Rect2i bgRect = currentBoss.getBossBarTextureRect();
				Rect2i fgRect = currentBoss.getBossBarHPTextureRect();
				Text name = evt.getBossInfo().getName();
				int c = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
				int x = evt.getX();
				int y = evt.getY();
				int xf = x + (bgRect.getWidth() - fgRect.getWidth()) / 2;
				int yf = y + (bgRect.getHeight() - fgRect.getHeight()) / 2;
				int fw = (int) ((double) fgRect.getWidth() * evt.getBossInfo().getPercent());
				int tx = c - mc.textRenderer.getWidth(name) / 2;
	
				RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
				int auxHeight = currentBoss.bossBarRenderCallback(ms, x, y);
				RenderSystem.enableBlend();
				RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				mc.getTextureManager().bindTexture(currentBoss.getBossBarTexture());
				drawBar(ms, currentBoss, x, y, bgRect.getX(), bgRect.getY(), bgRect.getWidth(), bgRect.getHeight(), true);
				drawBar(ms, currentBoss, xf, yf, fgRect.getX(), fgRect.getY(), fw, fgRect.getHeight(), false);
				mc.textRenderer.drawWithShadow(ms, name, tx, y - 10, 0xA2018C);
				RenderSystem.enableBlend();
				evt.setIncrement(Math.max(bgRect.getHeight(), fgRect.getHeight()) + auxHeight + mc.textRenderer.fontHeight);
			}
		}
	}
	
	private static void drawBar(PoseStack ms, EntityDoppleganger currentBoss, int x, int y, int u, int v, int w, int h, boolean bg) {
		ShaderHelper.BotaniaShader program = currentBoss.getBossBarShaderProgram(bg);
	
		if (program != null) {
			ShaderCallback callback = currentBoss.getBossBarShaderCallback(bg);
			barUniformCallback.set(u, v, callback);
			ShaderHelper.useShader(program, barUniformCallback);
		}
	
		RenderHelper.drawTexturedModalRect(ms, x, y, u, v, w, h);
	
		if (program != null) {
			ShaderHelper.releaseShader();
		}
	}
	
	private static class BarCallback implements ShaderCallback {
		int x, y;
		ShaderCallback callback;
	
		@Override
		public void call(int shader) {
			int startXUniform = GlStateManager._glGetUniformLocation(shader, "startX");
			int startYUniform = GlStateManager._glGetUniformLocation(shader, "startY");
	
			GlStateManager._glUniform1i(startXUniform, x);
			GlStateManager._glUniform1i(startYUniform, y);
	
			if (callback != null) {
				callback.call(shader);
			}
		}
	
		void set(int x, int y, ShaderCallback callback) {
			this.x = x;
			this.y = y;
			this.callback = callback;
		}
	}
	*/

}
