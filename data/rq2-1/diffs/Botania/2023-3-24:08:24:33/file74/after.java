/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.render.block_entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.core.handler.MiscellaneousModels;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity;
import vazkii.botania.common.helper.VecHelper;

public class TerrestrialAgglomerationPlateBlockEntityRenderer implements BlockEntityRenderer<TerrestrialAgglomerationPlateBlockEntity> {

	public TerrestrialAgglomerationPlateBlockEntityRenderer(BlockEntityRendererProvider.Context manager) {}

	@Override
	public void render(@NotNull TerrestrialAgglomerationPlateBlockEntity plate, float f, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		float alphaMod = Math.min(1.0F, plate.getCompletion() / 0.1F);

		ms.pushPose();
		ms.translate(0F, 3F / 16F + 0.001F, 0F);
		ms.mulPose(VecHelper.rotateX(90F));

		float alpha = (float) ((Math.sin((ClientTickHandler.ticksInGame + f) / 8D) + 1D) / 5D + 0.6D) * alphaMod;

		VertexConsumer buffer = buffers.getBuffer(RenderHelper.TERRA_PLATE);
		RenderHelper.renderIconFullBright(ms, buffer, MiscellaneousModels.INSTANCE.terraPlateOverlay.sprite(), alpha);

		ms.popPose();
	}

}
