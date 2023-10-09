/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.render.tile;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import vazkii.botania.client.core.handler.ClientTickHandler;

import java.util.function.Supplier;

public class TEISR implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	private final Block block;
	private final Supplier<BlockEntity> dummy;

	public TEISR(Block block) {
		this.block = Preconditions.checkNotNull(block);
		this.dummy = Suppliers.memoize(() -> {
			BlockEntityType<?> type = Registry.BLOCK_ENTITY_TYPE.getOptional(Registry.BLOCK.getKey(block)).get();
			return type.create(new BlockPos(0, Integer.MIN_VALUE, 0), block.defaultBlockState());
		});
	}

	@Override
	public void render(ItemStack stack, ItemTransforms.TransformType mode, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		if (stack.is(block.asItem())) {
			BlockEntityRenderer<?> r = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(dummy.get());
			if (r != null) {
				r.render(null, ClientTickHandler.partialTicks, ms, buffers, light, overlay);
			}
		}
	}
}
