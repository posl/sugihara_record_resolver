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
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.client.render.AccessoryRenderRegistry;
import vazkii.botania.client.render.AccessoryRenderer;
import vazkii.botania.common.handler.EquipmentHandler;
import vazkii.botania.common.item.BotaniaItems;
import vazkii.botania.common.proxy.IProxy;

import java.util.List;

public class BenevolentGoddessCharmItem extends BaubleItem {

	public static final int COST = 1000;

	public BenevolentGoddessCharmItem(Properties props) {
		super(props);
		IProxy.INSTANCE.runOnClient(() -> () -> AccessoryRenderRegistry.register(this, new Renderer()));
	}

	public static boolean shouldProtectExplosion(Level world, Vec3 vec) {
		List<Player> players = world.getEntitiesOfClass(Player.class, new AABB(vec.x, vec.y, vec.z, vec.x, vec.y, vec.z).inflate(8));

		for (Player player : players) {
			ItemStack charm = EquipmentHandler.findOrEmpty(BotaniaItems.goddessCharm, player);
			if (!charm.isEmpty() && ManaItemHandler.instance().requestManaExact(charm, player, COST, true)) {
				return true;
			}
		}
		return false;
	}

	public static class Renderer implements AccessoryRenderer {
		@Override
		public void doRender(HumanoidModel<?> bipedModel, ItemStack stack, LivingEntity living, PoseStack ms, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
			bipedModel.head.translateAndRotate(ms);
			ms.translate(0.275, -0.4, 0);
			ms.mulPose(Vector3f.YP.rotationDegrees(-90F));
			ms.scale(0.55F, -0.55F, -0.55F);
			Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.NONE,
					light, OverlayTexture.NO_OVERLAY, ms, buffers, living.getId());
		}
	}

}
