/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.core.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;

import org.lwjgl.glfw.GLFW;

import vazkii.botania.common.handler.ModSounds;
import vazkii.botania.common.item.ItemLexicon;

public class KonamiHandler {
	private static final int[] KONAMI_CODE = {
			GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_UP,
			GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_DOWN,
			GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
			GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
			GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A,
	};
	private static int nextLetter = 0;
	private static int konamiTime = 0;

	public static void clientTick(Minecraft client) {
		if (konamiTime > 0) {
			konamiTime--;
		}

		if (!ItemLexicon.isOpen()) {
			nextLetter = 0;
		}
	}

	public static void handleInput(int key, int action, int modifiers) {
		Minecraft mc = Minecraft.getInstance();
		if (modifiers == 0 && action == GLFW.GLFW_PRESS && ItemLexicon.isOpen()) {
			if (konamiTime == 0 && key == KONAMI_CODE[nextLetter]) {
				nextLetter++;
				if (nextLetter >= KONAMI_CODE.length) {
					mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.way, 1.0F));
					nextLetter = 0;
					konamiTime = 240;
				}
			} else {
				nextLetter = 0;
			}
		}
	}

	public static void renderBook(ResourceLocation book, Screen gui, int mouseX, int mouseY, float partialTicks, PoseStack ms) {
		if (konamiTime > 0) {
			String meme = I18n.get("botania.subtitle.way");
			RenderSystem.disableDepthTest();
			ms.pushPose();
			int fullWidth = Minecraft.getInstance().font.width(meme);
			int left = gui.width;
			double widthPerTick = (fullWidth + gui.width) / 240;
			double currWidth = left - widthPerTick * (240 - (konamiTime - partialTicks)) * 3.2;

			ms.translate(currWidth, gui.height / 2 - 10, 0);
			ms.scale(4, 4, 4);
			Minecraft.getInstance().font.drawShadow(ms, meme, 0, 0, 0xFFFFFF);
			ms.popPose();
			RenderSystem.enableDepthTest();
		}
	}
}
