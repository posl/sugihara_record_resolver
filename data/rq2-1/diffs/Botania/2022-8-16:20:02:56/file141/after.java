/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.advancements;

import com.google.gson.JsonObject;

import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.botania.common.item.ItemManaGun;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class ManaGunTrigger extends SimpleCriterionTrigger<ManaGunTrigger.Instance> {
	private static final ResourceLocation ID = prefix("fire_mana_blaster");
	public static final ManaGunTrigger INSTANCE = new ManaGunTrigger();

	private ManaGunTrigger() {}

	@NotNull
	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@NotNull
	@Override
	public ManaGunTrigger.Instance createInstance(@NotNull JsonObject json, EntityPredicate.Composite playerPred, DeserializationContext conditions) {
		Boolean desu = json.get("desu") == null ? null : json.get("desu").getAsBoolean();
		return new ManaGunTrigger.Instance(playerPred, ItemPredicate.fromJson(json.get("item")),
				EntityPredicate.fromJson(json.get("user")), desu);
	}

	public void trigger(ServerPlayer player, ItemStack stack) {
		trigger(player, instance -> instance.test(stack, player));
	}

	public static class Instance extends AbstractCriterionTriggerInstance {
		private final ItemPredicate item;
		private final EntityPredicate user;
		@Nullable
		private final Boolean desu;

		public Instance(EntityPredicate.Composite entityPred, ItemPredicate count, EntityPredicate user, Boolean desu) {
			super(ID, entityPred);
			this.item = count;
			this.user = user;
			this.desu = desu;
		}

		@NotNull
		@Override
		public ResourceLocation getCriterion() {
			return ID;
		}

		boolean test(ItemStack stack, ServerPlayer entity) {
			return this.item.matches(stack) && this.user.matches(entity, entity)
					&& (desu == null || desu == ItemManaGun.isSugoiKawaiiDesuNe(stack));
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			if (item != ItemPredicate.ANY) {
				json.add("item", item.serializeToJson());
			}
			if (user != EntityPredicate.ANY) {
				json.add("user", user.serializeToJson());
			}
			if (desu != null) {
				json.addProperty("desu", desu);
			}
			return json;
		}

		public ItemPredicate getItem() {
			return this.item;
		}

		public EntityPredicate getUser() {
			return this.user;
		}

		@Nullable
		public Boolean getDesu() {
			return this.desu;
		}
	}
}
