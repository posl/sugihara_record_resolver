/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.equipment.bauble;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import vazkii.botania.api.item.ICosmeticAttachable;
import vazkii.botania.api.item.ICosmeticBauble;
import vazkii.botania.common.core.handler.EquipmentHandler;
import vazkii.botania.common.lib.ModTags;

public class ItemMonocle extends ItemBauble implements ICosmeticBauble {

	public ItemMonocle(Properties props) {
		super(props);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void doRender(HumanoidModel<?> bipedModel, ItemStack stack, LivingEntity player, PoseStack ms, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		bipedModel.head.translateAndRotate(ms);
		ms.translate(0.15, -0.2, -0.25);
		ms.scale(0.3F, -0.3F, -0.3F);
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.NONE,
				light, OverlayTexture.NO_OVERLAY, ms, buffers, player.getId());
	}

	@Environment(EnvType.CLIENT)
	public static void renderHUD(PoseStack ms, Player player) {
		Minecraft mc = Minecraft.getInstance();
		HitResult ray = mc.hitResult;
		if (ray == null || ray.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos pos = ((BlockHitResult) ray).getBlockPos();
		BlockState state = player.level.getBlockState(pos);
		player.level.getBlockEntity(pos);

		ItemStack dispStack = ItemStack.EMPTY;
		String text = "";

		if (state.is(Blocks.REDSTONE_WIRE)) {
			dispStack = new ItemStack(Items.REDSTONE);
			text = ChatFormatting.RED + "" + state.getValue(RedStoneWireBlock.POWER);
		} else if (state.is(Blocks.REPEATER)) {
			dispStack = new ItemStack(Blocks.REPEATER);
			text = "" + state.getValue(RepeaterBlock.DELAY);
		} else if (state.is(Blocks.COMPARATOR)) {
			dispStack = new ItemStack(Blocks.COMPARATOR);
			text = state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT ? "-" : "+";
		}

		if (dispStack.isEmpty()) {
			return;
		}

		int x = mc.getWindow().getGuiScaledWidth() / 2 + 15;
		int y = mc.getWindow().getGuiScaledHeight() / 2 - 8;

		mc.getItemRenderer().renderAndDecorateItem(dispStack, x, y);

		mc.font.drawShadow(ms, text, x + 20, y + 4, 0xFFFFFF);
	}

	public static boolean hasMonocle(LivingEntity living) {
		return !EquipmentHandler.findOrEmpty(stack -> {
			if (!stack.isEmpty()) {
				Item item = stack.getItem();
				if (stack.is(ModTags.Items.BURST_VIEWERS)) {
					return true;
				}
				if (item instanceof ICosmeticAttachable attach) {
					ItemStack cosmetic = attach.getCosmeticItem(stack);
					return !cosmetic.isEmpty() && cosmetic.is(ModTags.Items.BURST_VIEWERS);
				}
			}
			return false;
		}, living).isEmpty();
	}

}
