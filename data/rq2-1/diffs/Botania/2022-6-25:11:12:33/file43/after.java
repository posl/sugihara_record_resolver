package vazkii.botania.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import vazkii.botania.client.render.entity.EntityRenderers;
import vazkii.botania.client.render.tile.TEISR;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ForgeBlockEntityItemRendererHelper {
	// Nulls in ctor call are fine, we don't use those fields
	private static final BlockEntityWithoutLevelRenderer RENDERER = new BlockEntityWithoutLevelRenderer(null, null) {
		private final Map<Item, TEISR> renderers = new IdentityHashMap<>();

		@Override
		public void renderByItem(ItemStack stack, ItemTransforms.TransformType transform,
				PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
			var renderer = renderers.computeIfAbsent(stack.getItem(), i -> {
				var block = Block.byItem(i);
				return EntityRenderers.BE_ITEM_RENDERER_FACTORIES.get(block).apply(block);
			});
			renderer.render(stack, transform, ps, buffers, light, overlay);
		}
	};

	private static final IClientItemExtensions PROPS = new IClientItemExtensions() {
		@Override
		public BlockEntityWithoutLevelRenderer getCustomRenderer() {
			return ForgeBlockEntityItemRendererHelper.RENDERER;
		}
	};

	public static void initItem(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(PROPS);
	}
}
