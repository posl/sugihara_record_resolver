/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.fabric.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraSword;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class FabricMixinMinecraft {
	@Shadow
	public abstract boolean isPaused();

	@Shadow
	private float pausePartialTick;

	@Shadow
	public abstract float getFrameTime();

	@Shadow
	@Nullable
	public LocalPlayer player;

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"))
	private void onFrameStart(boolean tick, CallbackInfo ci) {
		ClientTickHandler.renderTick(isPaused() ? pausePartialTick : getFrameTime());

	}

	@Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"))
	private void leftClickEmpty(CallbackInfoReturnable<Boolean> ci) {
		ItemTerraSword.leftClick(player.getMainHandItem());
	}
}
