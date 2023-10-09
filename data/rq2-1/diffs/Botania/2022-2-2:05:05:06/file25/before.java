/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.fabric;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogicRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.material.Fluids;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.BotaniaFabricCapabilities;
import vazkii.botania.api.block.IHornHarvestable;
import vazkii.botania.api.mana.ManaNetworkCallback;
import vazkii.botania.client.fx.ModParticles;
import vazkii.botania.common.ModStats;
import vazkii.botania.common.advancements.*;
import vazkii.botania.common.block.*;
import vazkii.botania.common.block.string.BlockRedStringInterceptor;
import vazkii.botania.common.block.subtile.functional.SubTileDaffomill;
import vazkii.botania.common.block.subtile.functional.SubTileTigerseye;
import vazkii.botania.common.block.tile.*;
import vazkii.botania.common.block.tile.corporea.TileCorporeaIndex;
import vazkii.botania.common.brew.ModBrews;
import vazkii.botania.common.brew.ModPotions;
import vazkii.botania.common.command.SkyblockCommand;
import vazkii.botania.common.crafting.ModRecipeTypes;
import vazkii.botania.common.entity.EntityDoppleganger;
import vazkii.botania.common.entity.ModEntities;
import vazkii.botania.common.handler.*;
import vazkii.botania.common.impl.BotaniaAPIImpl;
import vazkii.botania.common.impl.DefaultHornHarvestable;
import vazkii.botania.common.impl.corporea.DefaultCorporeaMatchers;
import vazkii.botania.common.item.*;
import vazkii.botania.common.item.equipment.bauble.ItemFlightTiara;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraAxe;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraSword;
import vazkii.botania.common.item.material.ItemEnderAir;
import vazkii.botania.common.item.relic.ItemFlugelEye;
import vazkii.botania.common.item.relic.ItemLokiRing;
import vazkii.botania.common.item.rod.*;
import vazkii.botania.common.loot.LootHandler;
import vazkii.botania.common.loot.ModLootModifiers;
import vazkii.botania.common.world.ModFeatures;
import vazkii.botania.common.world.SkyblockChunkGenerator;
import vazkii.botania.common.world.SkyblockWorldEvents;
import vazkii.botania.fabric.network.FabricPacketHandler;
import vazkii.botania.xplat.BotaniaConfig;
import vazkii.botania.xplat.IXplatAbstractions;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.Arrays;
import java.util.function.BiConsumer;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class FabricCommonInitializer implements ModInitializer {
	@Override
	public void onInitialize() {
		coreInit();
		registryInit();

		PaintableData.init();
		DefaultCorporeaMatchers.init();

		PatchouliAPI.get().registerMultiblock(Registry.BLOCK.getKey(ModBlocks.alfPortal), TileAlfPortal.MULTIBLOCK.get());
		PatchouliAPI.get().registerMultiblock(Registry.BLOCK.getKey(ModBlocks.terraPlate), TileTerraPlate.MULTIBLOCK.get());
		PatchouliAPI.get().registerMultiblock(Registry.BLOCK.getKey(ModBlocks.enchanter), TileEnchanter.MULTIBLOCK.get());
		PatchouliAPI.get().registerMultiblock(prefix("gaia_ritual"), EntityDoppleganger.ARENA_MULTIBLOCK.get());

		OrechidManager.registerListener();
		TileCraftCrate.registerListener();

		registerCapabilities();
		registerEvents();
	}

	private void coreInit() {
		FiberBotaniaConfig.setup();
		EquipmentHandler.init();
		FabricPacketHandler.init();
	}

	private void registryInit() {
		// Core item/block/BE
		ModSounds.init(bind(Registry.SOUND_EVENT));
		ModBlocks.registerBlocks(bind(Registry.BLOCK));
		ModBlocks.registerItemBlocks(bind(Registry.ITEM));
		ModFluffBlocks.registerBlocks(bind(Registry.BLOCK));
		ModFluffBlocks.registerItemBlocks(bind(Registry.ITEM));
		ModTiles.registerTiles(bind(Registry.BLOCK_ENTITY_TYPE));
		ModItems.registerItems(bind(Registry.ITEM));
		ModSubtiles.registerBlocks(bind(Registry.BLOCK));
		ModSubtiles.registerItemBlocks(bind(Registry.ITEM));
		ModSubtiles.registerTEs(bind(Registry.BLOCK_ENTITY_TYPE));
		ModBlocks.addDispenserBehaviours();

		int blazeTime = 2400;
		FuelRegistry.INSTANCE.add(ModBlocks.blazeBlock.asItem(), blazeTime * (IXplatAbstractions.INSTANCE.gogLoaded() ? 5 : 10));

		// GUI and Recipe
		ModItems.registerMenuTypes(bind(Registry.MENU));
		ModItems.registerRecipeSerializers(bind(Registry.RECIPE_SERIALIZER));
		ModRecipeTypes.registerRecipeTypes(bind(Registry.RECIPE_SERIALIZER));

		// Entities
		ModEntities.registerEntities(bind(Registry.ENTITY_TYPE));
		ModEntities.registerAttributes(FabricDefaultAttributeRegistry::register);
		PixieHandler.registerAttribute(bind(Registry.ATTRIBUTE));
		MinecartComparatorLogicRegistry.register(ModEntities.POOL_MINECART, (minecart, state, pos) -> minecart.getComparatorLevel());

		// Potions
		ModPotions.registerPotions(bind(Registry.MOB_EFFECT));
		ModBrews.registerBrews();

		// Worldgen
		ModFeatures.registerFeatures(bind(Registry.FEATURE));
		SkyblockChunkGenerator.init();
		if (BotaniaConfig.common().worldgenEnabled()) {
			BiomeModifications.addFeature(ctx -> {
				Biome.BiomeCategory category = ctx.getBiome().getBiomeCategory();
				return !ModFeatures.TYPE_BLACKLIST.contains(category);
			},
					GenerationStep.Decoration.VEGETAL_DECORATION,
					BuiltinRegistries.PLACED_FEATURE.getResourceKey(ModFeatures.MYSTICAL_FLOWERS_PLACED).orElseThrow());
			BiomeModifications.addFeature(
					ctx -> ctx.getBiome().getBiomeCategory() != Biome.BiomeCategory.THEEND,
					GenerationStep.Decoration.VEGETAL_DECORATION,
					BuiltinRegistries.PLACED_FEATURE.getResourceKey(ModFeatures.MYSTICAL_MUSHROOMS_PLACED).orElseThrow());
		}

		// Rest
		ModCriteriaTriggers.init();
		ModLootModifiers.init();
		ModParticles.registerParticles(bind(Registry.PARTICLE_TYPE));
		ModStats.init();
	}

	private void registerEvents() {
		if (IXplatAbstractions.INSTANCE.gogLoaded()) {
			UseBlockCallback.EVENT.register(SkyblockWorldEvents::onPlayerInteract);
		}
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> ((ItemExchangeRod) ModItems.exchangeRod).onLeftClick(player, world, hand, pos, direction));
		AttackEntityCallback.EVENT.register(ItemGravityRod::onAttack);
		AttackEntityCallback.EVENT.register(ItemTerraSword::attackEntity);
		CommandRegistrationCallback.EVENT.register(this::registerCommands);
		EntitySleepEvents.ALLOW_SLEEPING.register(SleepingHandler::trySleep);
		EntityTrackingEvents.START_TRACKING.register(SubTileDaffomill::onItemTrack);
		LootTableLoadingCallback.EVENT.register((resourceManager, manager, id, supplier, setter) -> LootHandler.lootLoad(id, supplier::withPool));
		ManaNetworkCallback.EVENT.register(ManaNetworkHandler.instance::onNetworkEvent);
		ServerEntityEvents.ENTITY_LOAD.register(SubTileTigerseye::pacifyAfterLoad);
		ServerLifecycleEvents.SERVER_STARTED.register(this::serverAboutToStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
		ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> ItemFlightTiara.playerLoggedOut(handler.player)));
		ServerPlayerEvents.AFTER_RESPAWN.register(ItemKeepIvy::onPlayerRespawn);
		ServerTickEvents.END_WORLD_TICK.register(CommonTickHandler::onTick);
		ServerTickEvents.END_WORLD_TICK.register(ItemGrassSeeds::onTickEnd);
		ServerTickEvents.END_WORLD_TICK.register(ItemTerraAxe::onTickEnd);
		UseBlockCallback.EVENT.register(BlockRedStringInterceptor::onInteract);
		UseBlockCallback.EVENT.register(ItemLokiRing::onPlayerInteract);
		UseItemCallback.EVENT.register(ItemEnderAir::onPlayerInteract);
	}

	private static <T> BiConsumer<T, ResourceLocation> bind(Registry<? super T> registry) {
		return (t, id) -> Registry.register(registry, id, t);
	}

	private void registerCapabilities() {
		FluidStorage.ITEM.registerForItems((stack, context) -> new FullItemFluidStorage(context, Items.BOWL,
				FluidVariant.of(Fluids.WATER), FluidConstants.BLOCK),
				ModItems.waterBowl);

		BotaniaFabricCapabilities.AVATAR_WIELDABLE.registerForItems((stack, c) -> new ItemDirtRod.AvatarBehavior(), ModItems.dirtRod);
		BotaniaFabricCapabilities.AVATAR_WIELDABLE.registerForItems((stack, c) -> new ItemDiviningRod.AvatarBehavior(), ModItems.diviningRod);
		BotaniaFabricCapabilities.AVATAR_WIELDABLE.registerForItems((stack, c) -> new ItemFireRod.AvatarBehavior(), ModItems.fireRod);
		BotaniaFabricCapabilities.AVATAR_WIELDABLE.registerForItems((stack, c) -> new ItemMissileRod.AvatarBehavior(), ModItems.missileRod);
		BotaniaFabricCapabilities.AVATAR_WIELDABLE.registerForItems((stack, c) -> new ItemRainbowRod.AvatarBehavior(), ModItems.rainbowRod);
		BotaniaFabricCapabilities.AVATAR_WIELDABLE.registerForItems((stack, c) -> new ItemTornadoRod.AvatarBehavior(), ModItems.tornadoRod);
		BotaniaFabricCapabilities.BLOCK_PROVIDER.registerForItems((stack, c) -> new ItemDirtRod.BlockProvider(stack), ModItems.dirtRod);
		BotaniaFabricCapabilities.BLOCK_PROVIDER.registerForItems((stack, c) -> new ItemBlackHoleTalisman.BlockProvider(stack), ModItems.blackHoleTalisman);
		BotaniaFabricCapabilities.BLOCK_PROVIDER.registerForItems((stack, c) -> new ItemCobbleRod.BlockProvider(), ModItems.cobbleRod);
		BotaniaFabricCapabilities.BLOCK_PROVIDER.registerForItems((stack, c) -> new ItemEnderHand.BlockProvider(stack), ModItems.enderHand);
		BotaniaFabricCapabilities.BLOCK_PROVIDER.registerForItems((stack, c) -> new ItemTerraformRod.BlockProvider(), ModItems.terraformRod);
		BotaniaFabricCapabilities.COORD_BOUND_ITEM.registerForItems((st, c) -> new ItemFlugelEye.CoordBoundItem(st), ModItems.flugelEye);
		BotaniaFabricCapabilities.COORD_BOUND_ITEM.registerForItems((st, c) -> new ItemManaMirror.CoordBoundItem(st), ModItems.manaMirror);
		BotaniaFabricCapabilities.COORD_BOUND_ITEM.registerForItems((st, c) -> new ItemTwigWand.CoordBoundItem(st), ModItems.twigWand);

		BotaniaFabricCapabilities.EXOFLAME_HEATABLE.registerFallback((world, pos, state, blockEntity, context) -> {
			if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
				return new ExoflameFurnaceHandler.FurnaceExoflameHeatable(furnace);
			}
			return null;
		});
		BotaniaFabricCapabilities.HORN_HARVEST.registerForBlocks((w, p, s, be, c) -> (world, pos, stack, hornType) -> hornType == IHornHarvestable.EnumHornType.CANOPY,
				Blocks.VINE, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT, Blocks.TWISTING_VINES,
				Blocks.TWISTING_VINES_PLANT, Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT);
		BotaniaFabricCapabilities.HORN_HARVEST.registerForBlocks((w, p, s, be, c) -> DefaultHornHarvestable.INSTANCE,
				Arrays.stream(DyeColor.values()).map(ModBlocks::getMushroom).toArray(Block[]::new));
		BotaniaFabricCapabilities.HORN_HARVEST.registerForBlocks((w, p, s, be, c) -> DefaultHornHarvestable.INSTANCE,
				Arrays.stream(DyeColor.values()).map(ModBlocks::getShinyFlower).toArray(Block[]::new));
		BotaniaFabricCapabilities.HOURGLASS_TRIGGER.registerForBlockEntities((be, c) -> {
			var torch = (TileAnimatedTorch) be;
			return hourglass -> torch.toggle();
		}, ModTiles.ANIMATED_TORCH);
		BotaniaFabricCapabilities.WANDABLE.registerForBlocks(
				(world, pos, state, blockEntity, context) -> (player, stack, side) -> ((BlockPistonRelay) state.getBlock()).onUsedByWand(player, stack, world, pos),
				ModBlocks.pistonRelay
		);
		BotaniaFabricCapabilities.WANDABLE.registerSelf(
				ModTiles.ALF_PORTAL, ModTiles.ANIMATED_TORCH, ModTiles.CORPOREA_CRYSTAL_CUBE, ModTiles.CORPOREA_RETAINER,
				ModTiles.CRAFT_CRATE, ModTiles.ENCHANTER, ModTiles.HOURGLASS, ModTiles.PLATFORM, ModTiles.POOL,
				ModTiles.RUNE_ALTAR, ModTiles.SPREADER, ModTiles.TURNTABLE,
				ModSubtiles.DAFFOMILL, ModSubtiles.HOPPERHOCK, ModSubtiles.HOPPERHOCK_CHIBI,
				ModSubtiles.RANNUNCARPUS, ModSubtiles.RANNUNCARPUS_CHIBI
		);
	}

	private void serverAboutToStart(MinecraftServer server) {
		if (BotaniaAPI.instance().getClass() != BotaniaAPIImpl.class) {
			String clname = BotaniaAPI.instance().getClass().getName();
			throw new IllegalAccessError("The Botania API has been overriden. "
					+ "This will cause crashes and compatibility issues, and that's why it's marked as"
					+ " \"Do not Override\". Whoever had the brilliant idea of overriding it needs to go"
					+ " back to elementary school and learn to read. (Actual classname: " + clname + ")");
		}

		if (server.isDedicatedServer()) {
			ContributorList.firstStart();
		}
	}

	private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
		if (IXplatAbstractions.INSTANCE.gogLoaded()) {
			SkyblockCommand.register(dispatcher);
		}
	}

	private void serverStopping(MinecraftServer server) {
		ManaNetworkHandler.instance.clear();
		TileCorporeaIndex.clearIndexCache();
	}

}
