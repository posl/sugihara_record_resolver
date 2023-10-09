/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.mixin.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SkullBlock;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import vazkii.botania.client.model.ModelGaiaHead;
import vazkii.botania.client.render.tile.RenderTileGaiaHead;
import vazkii.botania.common.block.BlockGaiaHead;

import java.util.Map;

@Mixin(SkullBlockRenderer.class)
public abstract class MixinSkullBlockRenderer {
	@Shadow
	@Final
	private static Map<SkullBlock.Type, ResourceLocation> SKIN_BY_TYPE;

	@Inject(
		method = "createSkullRenderers",
		at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;", remap = false),
		locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private static void registerModel(EntityModelSet entityModelSet, CallbackInfoReturnable<Map<SkullBlock.Type, SkullModelBase>> cir,
			ImmutableMap.Builder<SkullBlock.Type, SkullModelBase> builder) {
		builder.put(BlockGaiaHead.GAIA_TYPE, new ModelGaiaHead());

		// placeholder to avoid crash
		SKIN_BY_TYPE.put(BlockGaiaHead.GAIA_TYPE, DefaultPlayerSkin.getDefaultSkin());
	}

	@Inject(at = @At("HEAD"), method = "getRenderType", cancellable = true)
	private static void hookGetRenderType(SkullBlock.Type type, GameProfile gameProfile, CallbackInfoReturnable<RenderType> cir) {
		if (type == BlockGaiaHead.GAIA_TYPE) {
			RenderTileGaiaHead.hookGetRenderType(cir);
		}
	}
}
