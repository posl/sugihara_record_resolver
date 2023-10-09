/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.world;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileManaFlame;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.tool.ToolCommons;
import vazkii.botania.mixin.AccessorSoundType;
import vazkii.botania.network.clientbound.PacketGogWorld;
import vazkii.botania.xplat.BotaniaConfig;
import vazkii.botania.xplat.IXplatAbstractions;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public final class SkyblockWorldEvents {

	private SkyblockWorldEvents() {}

	private static final TagKey<Block> PEBBLE_SOURCES = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("gardenofglass:pebble_sources"));

	public static void syncGogStatus(ServerPlayer e) {
		boolean isGog = SkyblockChunkGenerator.isWorldSkyblock(e.level);
		if (isGog) {
			IXplatAbstractions.INSTANCE.sendToPlayer(e, PacketGogWorld.INSTANCE);
		}
	}

	public static void onPlayerJoin(ServerPlayer player) {
		ServerLevel world = player.getLevel();
		if (SkyblockChunkGenerator.isWorldSkyblock(world)) {
			SkyblockSavedData data = SkyblockSavedData.get(world);
			if (!data.skyblocks.containsValue(Util.NIL_UUID)) {
				IslandPos islandPos = data.getSpawn();
				world.setDefaultSpawnPos(islandPos.getCenter(), 0);
				spawnPlayer(player, islandPos);
				BotaniaAPI.LOGGER.info("Created the spawn GoG island");
			}
		}
	}

	public static InteractionResult onPlayerInteract(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
		if (IXplatAbstractions.INSTANCE.gogLoaded()) {
			ItemStack equipped = player.getItemInHand(hand);

			if (equipped.isEmpty() && player.isShiftKeyDown()) {
				BlockState state = world.getBlockState(hit.getBlockPos());
				Block block = state.getBlock();

				if (state.is(PEBBLE_SOURCES)) {
					SoundType st = state.getSoundType();
					SoundEvent sound = ((AccessorSoundType) st).botania_getBreakSound();
					player.playSound(sound, st.getVolume() * 0.4F, st.getPitch() + (float) (Math.random() * 0.2 - 0.1));

					if (world.isClientSide) {
						player.swing(hand);
					} else if (Math.random() < 0.8) {
						player.drop(new ItemStack(ModItems.pebble), false);
					}

					return InteractionResult.SUCCESS;
				}
			} else if (!equipped.isEmpty() && equipped.is(Items.BOWL)) {
				BlockHitResult rtr = ToolCommons.raytraceFromEntity(player, 4.5F, true);
				if (rtr.getType() == HitResult.Type.BLOCK) {
					BlockPos pos = rtr.getBlockPos();
					if (world.getBlockState(pos).getMaterial() == Material.WATER) {
						if (!world.isClientSide) {
							equipped.shrink(1);

							if (equipped.isEmpty()) {
								player.setItemInHand(hand, new ItemStack(ModItems.waterBowl));
							} else {
								player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.waterBowl));
							}
						}

						return InteractionResult.SUCCESS;
					}
				}
			}
		}
		return InteractionResult.PASS;
	}

	public static void spawnPlayer(Player player, IslandPos islandPos) {
		BlockPos pos = islandPos.getCenter();

		if (player instanceof ServerPlayer pmp) {
			createSkyblock(pmp.getLevel(), pos);
			pmp.teleportTo(pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5);
			pmp.setRespawnPosition(pmp.level.dimension(), pos, 0, true, false);
			if (BotaniaConfig.common().gogSpawnWithLexicon()) {
				player.getInventory().add(new ItemStack(ModItems.lexicon));
			}
		}
	}

	public static void createSkyblock(ServerLevel level, BlockPos pos) {
		var manager = level.getStructureManager();
		var template = manager.get(prefix("gog_island")).orElseThrow();
		var structureBlockInfos = template.filterBlocks(pos, new StructurePlaceSettings(), Blocks.STRUCTURE_BLOCK, false);
		structureBlockInfos.removeIf(info -> info.nbt == null);

		BlockPos offset;
		var infoOptional = structureBlockInfos.stream()
				.filter(info -> "spawn_point".equals(info.nbt.getString("metadata")))
				.findFirst();
		if (infoOptional.isPresent()) {
			offset = infoOptional.get().pos;
		} else {
			BotaniaAPI.LOGGER.error("Structure botania:gog_island has no spawn_point data marker block, trying to offset it somewhat in the center");
			Vec3i size = template.getSize();
			offset = new BlockPos(size.getX() / 2, size.getY(), size.getZ() / 2);
		}
		BlockPos startPoint = pos.subtract(offset);

		template.placeInWorld(level,
				startPoint,
				startPoint,
				new StructurePlaceSettings().addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK),
				level.random,
				Block.UPDATE_ALL);
		for (var info : structureBlockInfos) {
			if ("light".equals(info.nbt.getString("metadata"))) {
				BlockPos lightPos = startPoint.offset(info.pos);
				if (level.setBlockAndUpdate(lightPos, ModBlocks.manaFlame.defaultBlockState())) {
					int r = 70 + level.random.nextInt(185);
					int g = 70 + level.random.nextInt(185);
					int b = 70 + level.random.nextInt(185);
					int color = r << 16 | g << 8 | b;
					((TileManaFlame) level.getBlockEntity(lightPos)).setColor(color);
				}
			}
		}
	}
}
