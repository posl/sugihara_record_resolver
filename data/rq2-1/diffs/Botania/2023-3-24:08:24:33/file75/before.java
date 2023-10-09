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
import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import vazkii.botania.client.core.handler.ClientTickHandler;
import vazkii.botania.client.core.proxy.ClientProxy;
import vazkii.botania.client.lib.ResourcesLib;
import vazkii.botania.client.model.BotaniaModelLayers;
import vazkii.botania.client.model.TeruTeruBozuModel;
import vazkii.botania.common.block.block_entity.TeruTeruBozuBlockEntity;

import java.util.Random;

public class TeruTeruBozuBlockEntityRenderer implements BlockEntityRenderer<TeruTeruBozuBlockEntity> {

	private static final ResourceLocation texture = new ResourceLocation(ResourcesLib.MODEL_TERU_TERU_BOZU);
	private static final ResourceLocation textureHalloween = new ResourceLocation(ResourcesLib.MODEL_TERU_TERU_BOZU_HALLOWEEN);
	private final TeruTeruBozuModel model;

	public TeruTeruBozuBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
		model = new TeruTeruBozuModel(ctx.bakeLayer(BotaniaModelLayers.TERU_TERU_BOZU));
	}

	@Override
	public void render(@Nullable TeruTeruBozuBlockEntity tileentity, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		ms.pushPose();
		ms.mulPose(Vector3f.XP.rotationDegrees(180));
		double time = ClientTickHandler.ticksInGame + partialTicks;
		boolean hasWorld = tileentity != null && tileentity.getLevel() != null;
		if (hasWorld) {
			time += new Random(tileentity.getBlockPos().hashCode()).nextInt(1000);
		}

		ms.translate(0.5F, -1.25F + (hasWorld ? (float) Math.sin(time * 0.01F) * 0.05F : 0F), -0.5F);
		if (hasWorld) {
			ms.mulPose(Vector3f.YP.rotationDegrees((float) (time * 0.3)));
			ms.mulPose(Vector3f.ZP.rotationDegrees(4F * (float) Math.sin(time * 0.05F)));
			float s = 0.75F;
			ms.scale(s, s, s);
		}

		VertexConsumer buffer = buffers.getBuffer(model.renderType(ClientProxy.dootDoot ? textureHalloween : texture));
		model.renderToBuffer(ms, buffer, light, overlay, 1, 1, 1, 1);
		ms.popPose();
	}

}
