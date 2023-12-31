package com.simibubi.create.content.curiosities.armor;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.advancement.AllAdvancements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class DivingHelmetItem extends BaseArmorItem {
	public static final EquipmentSlot SLOT = EquipmentSlot.HEAD;

	public DivingHelmetItem(ArmorMaterial material, Properties properties, ResourceLocation textureLoc) {
		super(material, SLOT, properties, textureLoc);
	}

	public static boolean isWornBy(Entity entity) {
		ItemStack stack = getWornItem(entity);
		if (stack == null) {
			return false;
		}
		return stack.getItem() instanceof DivingHelmetItem;
	}

	@Nullable
	public static ItemStack getWornItem(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return null;
		}
		return livingEntity.getItemBySlot(SLOT);
	}

	@SubscribeEvent
	public static void breatheUnderwater(LivingUpdateEvent event) {
		LivingEntity entity = event.getEntityLiving();
		Level world = entity.level;
		boolean second = world.getGameTime() % 20 == 0;
		boolean drowning = entity.getAirSupply() == 0;

		if (world.isClientSide)
			entity.getPersistentData()
				.remove("VisualBacktankAir");

		if (!isWornBy(entity))
			return;

		boolean lavaDiving = entity.isEyeInFluid(FluidTags.LAVA);
		if (!entity.isEyeInFluid(FluidTags.WATER) && !lavaDiving)
			return;
		if (entity instanceof Player && ((Player) entity).isCreative())
			return;

		ItemStack backtank = BacktankUtil.get(entity);
		if (backtank.isEmpty())
			return;
		if (!BacktankUtil.hasAirRemaining(backtank))
			return;

		if (lavaDiving) {
			if (entity instanceof ServerPlayer sp)
				AllAdvancements.DIVING_SUIT_LAVA.awardTo(sp);
			return;
		}

		if (drowning)
			entity.setAirSupply(10);

		if (world.isClientSide)
			entity.getPersistentData()
				.putInt("VisualBacktankAir", (int) BacktankUtil.getAir(backtank));

		if (!second)
			return;

		if (entity instanceof ServerPlayer sp)
			AllAdvancements.DIVING_SUIT.awardTo(sp);

		entity.setAirSupply(Math.min(entity.getMaxAirSupply(), entity.getAirSupply() + 10));
		entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 30, 0, true, false, true));
		BacktankUtil.consumeAir(entity, backtank, 1);
	}
}
