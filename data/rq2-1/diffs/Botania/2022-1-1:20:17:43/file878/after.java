/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import vazkii.botania.common.Botania;
import vazkii.botania.common.world.SkyblockWorldEvents;

@Mixin(ServerLevel.class)
public class MixinServerLevel {
	@Inject(at = @At("RETURN"), method = "addPlayer")
	private void onEntityAdd(ServerPlayer entity, CallbackInfo ci) {
		if (Botania.gardenOfGlassLoaded) {
			SkyblockWorldEvents.syncGogStatus(entity);
		}
	}
}
