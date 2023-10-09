/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item.equipment.tool.bow;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.common.item.equipment.tool.ToolCommons;

import java.util.function.Consumer;

public class CrystalBowItem extends LivingwoodBowItem {

	private static final int ARROW_COST = 200;

	public CrystalBowItem(Properties builder) {
		super(builder);
	}

	// [VanillaCopy] super
	@NotNull
	@Override
	public InteractionResultHolder<ItemStack> use(@NotNull Level worldIn, Player playerIn, @NotNull InteractionHand handIn) {
		ItemStack itemstack = playerIn.getItemInHand(handIn);
		boolean flag = canFire(itemstack, playerIn); // Botania - custom check

		if (!playerIn.getAbilities().instabuild && !flag) {
			return InteractionResultHolder.fail(itemstack);
		} else {
			playerIn.startUsingItem(handIn);
			return InteractionResultHolder.consume(itemstack);
		}
	}

	// [VanillaCopy] super
	@Override
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level worldIn, LivingEntity entityLiving, int timeLeft) {
		if (entityLiving instanceof Player playerentity) {
			boolean flag = canFire(stack, playerentity); // Botania - custom check
			ItemStack itemstack = playerentity.getProjectile(stack);

			int i = (int) ((getUseDuration(stack) - timeLeft) * chargeVelocityMultiplier()); // Botania - velocity multiplier
			if (i < 0) {
				return;
			}

			if (!itemstack.isEmpty() || flag) {
				if (itemstack.isEmpty()) {
					itemstack = new ItemStack(Items.ARROW);
				}

				float f = getPowerForTime(i);
				if (!((double) f < 0.1D)) {
					boolean flag1 = playerentity.getAbilities().instabuild || itemstack.is(Items.ARROW);
					if (!worldIn.isClientSide) {
						ArrowItem arrowitem = (ArrowItem) (itemstack.getItem() instanceof ArrowItem ? itemstack.getItem() : Items.ARROW);
						AbstractArrow abstractarrowentity = arrowitem.createArrow(worldIn, itemstack, playerentity);
						abstractarrowentity.shootFromRotation(playerentity, playerentity.getXRot(), playerentity.getYRot(), 0.0F, f * 3.0F, 1.0F);
						if (f == 1.0F) {
							abstractarrowentity.setCritArrow(true);
						}

						int j = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
						if (j > 0) {
							abstractarrowentity.setBaseDamage(abstractarrowentity.getBaseDamage() + (double) j * 0.5D + 0.5D);
						}

						int k = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
						if (k > 0) {
							abstractarrowentity.setKnockback(k);
						}

						if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
							abstractarrowentity.setSecondsOnFire(100);
						}

						// Botania - onFire
						onFire(stack, playerentity, flag1, abstractarrowentity);
						stack.hurtAndBreak(1, playerentity, (p_220009_1_) -> {
							p_220009_1_.broadcastBreakEvent(playerentity.getUsedItemHand());
						});
						if (flag1 || playerentity.getAbilities().instabuild && (itemstack.is(Items.SPECTRAL_ARROW) || itemstack.is(Items.TIPPED_ARROW))) {
							abstractarrowentity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
						}

						worldIn.addFreshEntity(abstractarrowentity);
					}

					worldIn.playSound((Player) null, playerentity.getX(), playerentity.getY(), playerentity.getZ(),
							SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
							1.0F, 1.0F / (playerentity.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
					if (!flag1 && !playerentity.getAbilities().instabuild) {
						itemstack.shrink(1);
						if (itemstack.isEmpty()) {
							playerentity.getInventory().removeItem(itemstack);
						}
					}

					playerentity.awardStat(Stats.ITEM_USED.get(this));
				}
			}
		}
	}

	@Override
	public float chargeVelocityMultiplier() {
		return 2F;
	}

	private boolean canFire(ItemStack stack, Player player) {
		boolean infinity = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
		return player.getAbilities().instabuild || ManaItemHandler.instance().requestManaExactForTool(stack, player, ARROW_COST / (infinity ? 2 : 1), false);
	}

	private void onFire(ItemStack stack, LivingEntity living, boolean infinity, AbstractArrow arrow) {
		arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
		boolean infinity = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
		return ToolCommons.damageItemIfPossible(stack, amount, entity, ARROW_COST / (infinity ? 2 : 1));
	}
}
