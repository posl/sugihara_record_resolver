/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.equipment.bauble;

import com.google.common.base.Predicates;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import org.apache.commons.lang3.mutable.MutableFloat;

import vazkii.botania.client.fx.WispParticleData;
import vazkii.botania.client.lib.ResourcesLib;
import vazkii.botania.common.handler.BotaniaSounds;

import java.util.List;

public class CloakOfSinItem extends CloakOfVirtueItem {

	private static final ResourceLocation texture = new ResourceLocation(ResourcesLib.MODEL_UNHOLY_CLOAK);
	private static final ResourceLocation textureGlow = new ResourceLocation(ResourcesLib.MODEL_UNHOLY_CLOAK_GLOW);

	public CloakOfSinItem(Properties props) {
		super(props);
	}

	@Override
	protected boolean effectOnDamage(DamageSource src, MutableFloat amount, Player player, ItemStack stack) {
		if (!src.isBypassArmor()) {
			int range = 6;
			@SuppressWarnings("unchecked")
			List<Enemy> mobs = (List<Enemy>) (List<?>) player.getLevel().getEntitiesOfClass(Entity.class, new AABB(player.getX() - range, player.getY() - range, player.getZ() - range, player.getX() + range, player.getY() + range, player.getZ() + range), Predicates.instanceOf(Enemy.class));
			for (Enemy mob : mobs) {
				if (mob instanceof LivingEntity entity) {
					entity.hurt(DamageSource.playerAttack(player), amount.getValue());
				}
			}

			player.getLevel().playSound(null, player.getX(), player.getY(), player.getZ(), BotaniaSounds.unholyCloak, SoundSource.PLAYERS, 1F, 1F);
			for (int i = 0; i < 90; i++) {
				float rad = i * 4F * (float) Math.PI / 180F;
				float xMotion = (float) Math.cos(rad) * 0.2F;
				float zMotion = (float) Math.sin(rad) * 0.2F;
				WispParticleData data = WispParticleData.wisp(0.6F + (float) Math.random() * 0.2F, 0.4F + (float) Math.random() + 0.25F, 0F, 0F);
				player.getLevel().addParticle(data, player.getX(), player.getY() + 0.5, player.getZ(), xMotion, 0F, zMotion);
			}

			return true;
		}

		return false;
	}

	@Override
	ResourceLocation getCloakTexture() {
		return texture;
	}

	@Override
	ResourceLocation getCloakGlowTexture() {
		return textureGlow;
	}

}
