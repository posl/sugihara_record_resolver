/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import vazkii.botania.common.entity.ModEntities;
import vazkii.botania.common.lib.ModTags;
import vazkii.botania.xplat.IXplatAbstractions;

public class EntityTagProvider extends EntityTypeTagsProvider {
	public EntityTagProvider(DataGenerator generator) {
		super(generator);
	}

	@Override
	protected void addTags() {
		tag(ModTags.Entities.COCOON_COMMON).add(
				EntityType.PIG, EntityType.COW, EntityType.CHICKEN, EntityType.RABBIT, EntityType.SHEEP
		);
		tag(ModTags.Entities.COCOON_RARE)
				.add(EntityType.HORSE, EntityType.DONKEY, EntityType.WOLF, EntityType.OCELOT,
						EntityType.CAT, EntityType.PARROT, EntityType.LLAMA, EntityType.FOX,
						EntityType.PANDA, EntityType.TURTLE, EntityType.GOAT)
				.addOptional(new ResourceLocation("quark", "frog"));

		tag(ModTags.Entities.COCOON_COMMON_AQUATIC)
				.add(EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SQUID)
				.addOptional(new ResourceLocation("quark", "crab"));
		tag(ModTags.Entities.COCOON_RARE_AQUATIC).add(EntityType.DOLPHIN, EntityType.GLOW_SQUID, EntityType.AXOLOTL);

		tag(ModTags.Entities.SHADED_MESA_BLACKLIST).add(EntityType.ENDER_DRAGON, EntityType.WITHER,
				EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.END_CRYSTAL, EntityType.PAINTING,
				EntityType.COMMAND_BLOCK_MINECART, EntityType.MARKER, EntityType.AREA_EFFECT_CLOUD,
				EntityType.EVOKER_FANGS, EntityType.LEASH_KNOT,
				ModEntities.CORPOREA_SPARK, ModEntities.DOPPLEGANGER, ModEntities.FLAME_RING, ModEntities.MAGIC_LANDMINE,
				ModEntities.MAGIC_MISSILE, ModEntities.MANA_BURST, ModEntities.PINK_WITHER, ModEntities.SPARK, ModEntities.PLAYER_MOVER);

		tag(IXplatAbstractions.INSTANCE.entityTag(new ResourceLocation("c", "bosses")))
				.add(ModEntities.DOPPLEGANGER);
	}
}
