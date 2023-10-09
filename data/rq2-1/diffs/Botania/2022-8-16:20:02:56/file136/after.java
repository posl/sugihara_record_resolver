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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.NotNull;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class AlfPortalBreadTrigger extends SimpleCriterionTrigger<AlfPortalBreadTrigger.Instance> {
	public static final ResourceLocation ID = prefix("alf_portal_bread");
	public static final AlfPortalBreadTrigger INSTANCE = new AlfPortalBreadTrigger();

	private AlfPortalBreadTrigger() {}

	@NotNull
	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@NotNull
	@Override
	public AlfPortalBreadTrigger.Instance createInstance(@NotNull JsonObject json, EntityPredicate.Composite playerPredicate, DeserializationContext conditions) {
		return new AlfPortalBreadTrigger.Instance(playerPredicate, LocationPredicate.fromJson(json.get("portal_location")));
	}

	public void trigger(ServerPlayer player, BlockPos portal) {
		this.trigger(player, instance -> instance.test(player.getLevel(), portal));
	}

	public static class Instance extends AbstractCriterionTriggerInstance {
		private final LocationPredicate portal;

		public Instance(EntityPredicate.Composite playerPredicate, LocationPredicate portal) {
			super(ID, playerPredicate);
			this.portal = portal;
		}

		@NotNull
		@Override
		public ResourceLocation getCriterion() {
			return ID;
		}

		boolean test(ServerLevel world, BlockPos portal) {
			return this.portal.matches(world, portal.getX(), portal.getY(), portal.getZ());
		}

		public LocationPredicate getPortal() {
			return this.portal;
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			if (portal != LocationPredicate.ANY) {
				json.add("portal_location", portal.serializeToJson());
			}
			return json;
		}
	}
}
