/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.MinecartComparatorLogicRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.BotaniaCapabilities;
import vazkii.botania.api.block.IHornHarvestable;
import vazkii.botania.api.corporea.CorporeaHelper;
import vazkii.botania.api.mana.ManaNetworkCallback;
import vazkii.botania.client.fx.ModParticles;
import vazkii.botania.common.advancements.*;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModFluffBlocks;
import vazkii.botania.common.block.ModSubtiles;
import vazkii.botania.common.block.string.BlockRedStringInterceptor;
import vazkii.botania.common.block.subtile.functional.SubTileTigerseye;
import vazkii.botania.common.block.tile.ModTiles;
import vazkii.botania.common.block.tile.TileAlfPortal;
import vazkii.botania.common.block.tile.TileCraftCrate;
import vazkii.botania.common.block.tile.TileEnchanter;
import vazkii.botania.common.block.tile.TileTerraPlate;
import vazkii.botania.common.block.tile.corporea.TileCorporeaIndex;
import vazkii.botania.common.brew.ModBrews;
import vazkii.botania.common.brew.ModPotions;
import vazkii.botania.common.core.ModStats;
import vazkii.botania.common.core.command.SkyblockCommand;
import vazkii.botania.common.core.handler.*;
import vazkii.botania.common.core.helper.ColorHelper;
import vazkii.botania.common.core.loot.LootHandler;
import vazkii.botania.common.core.loot.ModLootModifiers;
import vazkii.botania.common.core.proxy.IProxy;
import vazkii.botania.common.crafting.ModRecipeTypes;
import vazkii.botania.common.entity.ModEntities;
import vazkii.botania.common.impl.BotaniaAPIImpl;
import vazkii.botania.common.impl.corporea.CorporeaItemStackMatcher;
import vazkii.botania.common.impl.corporea.CorporeaStringMatcher;
import vazkii.botania.common.item.ItemGrassSeeds;
import vazkii.botania.common.item.ItemKeepIvy;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.bauble.ItemFlightTiara;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraAxe;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraSword;
import vazkii.botania.common.item.material.ItemEnderAir;
import vazkii.botania.common.item.relic.ItemLokiRing;
import vazkii.botania.common.item.rod.ItemExchangeRod;
import vazkii.botania.common.item.rod.ItemGravityRod;
import vazkii.botania.common.lib.LibMisc;
import vazkii.botania.common.network.PacketHandler;
import vazkii.botania.common.world.ModFeatures;
import vazkii.botania.common.world.SkyblockChunkGenerator;
import vazkii.botania.common.world.SkyblockWorldEvents;
import vazkii.botania.data.DataGenerators;
import vazkii.botania.fabric.FiberBotaniaConfig;
import vazkii.botania.xplat.IXplatAbstractions;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.IStateMatcher;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class Botania implements ModInitializer {

	public static boolean gardenOfGlassLoaded = false;

	public static IProxy proxy = new IProxy() {};
	public static Consumer<Supplier<Runnable>> runOnClient = s -> {};
	public static volatile boolean configLoaded = false;

	@Override
	public void onInitialize() {
		gardenOfGlassLoaded = IXplatAbstractions.INSTANCE.isModLoaded(LibMisc.GOG_MOD_ID);
		if (IXplatAbstractions.INSTANCE.isPhysicalClient()) {
			runOnClient = t -> t.get().run();
		}
		FiberBotaniaConfig.setup();

		EquipmentHandler.init();
		ModFeatures.registerFeatures(bind(Registry.FEATURE));
		ModBlocks.registerBlocks(bind(Registry.BLOCK));
		ModItems.registerItems(bind(Registry.ITEM));
		ModItems.registerRecipeSerializers(bind(Registry.RECIPE_SERIALIZER));
		ModEntities.registerEntities(bind(Registry.ENTITY_TYPE));
		MinecartComparatorLogicRegistry.register(ModEntities.POOL_MINECART, (minecart, state, pos) -> minecart.getComparatorLevel());
		ModRecipeTypes.registerRecipeTypes(bind(Registry.RECIPE_SERIALIZER));
		ModSounds.init(bind(Registry.SOUND_EVENT));
		ModBrews.registerBrews(bind(BotaniaAPI.instance().getBrewRegistry()));
		ModPotions.registerPotions(bind(Registry.MOB_EFFECT));
		int blazeTime = 2400;
		FuelRegistry.INSTANCE.add(ModBlocks.blazeBlock.asItem(), blazeTime * (Botania.gardenOfGlassLoaded ? 5 : 10));
		ModBlocks.registerItemBlocks(bind(Registry.ITEM));
		ModTiles.registerTiles(bind(Registry.BLOCK_ENTITY_TYPE));
		ModFluffBlocks.registerBlocks(bind(Registry.BLOCK));
		ModFluffBlocks.registerItemBlocks(bind(Registry.ITEM));
		ModParticles.registerParticles(bind(Registry.PARTICLE_TYPE));
		ModSubtiles.registerBlocks(bind(Registry.BLOCK));
		ModSubtiles.registerItemBlocks(bind(Registry.ITEM));
		ModSubtiles.registerTEs(bind(Registry.BLOCK_ENTITY_TYPE));
		PixieHandler.registerAttribute(bind(Registry.ATTRIBUTE));

		commonSetup();
		ServerLifecycleEvents.SERVER_STARTED.register(this::serverAboutToStart);
		CommandRegistrationCallback.EVENT.register(this::registerCommands);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
		UseBlockCallback.EVENT.register(ItemLokiRing::onPlayerInteract);
		UseItemCallback.EVENT.register(ItemEnderAir::onPlayerInteract);
		ServerTickEvents.END_WORLD_TICK.register(ItemGrassSeeds::onTickEnd);
		ServerPlayerEvents.AFTER_RESPAWN.register(ItemKeepIvy::onPlayerRespawn);
		ServerTickEvents.END_WORLD_TICK.register(CommonTickHandler::onTick);
		UseBlockCallback.EVENT.register(BlockRedStringInterceptor::onInteract);
		ManaNetworkCallback.EVENT.register(ManaNetworkHandler.instance::onNetworkEvent);
		LootTableLoadingCallback.EVENT.register((resourceManager, manager, id, supplier, setter) -> LootHandler.lootLoad(id, supplier::withPool));
		ServerPlayConnectionEvents.DISCONNECT.register(ItemFlightTiara::playerLoggedOut);
		ServerEntityEvents.ENTITY_LOAD.register(SubTileTigerseye::pacifyAfterLoad);
		AttackEntityCallback.EVENT.register(ItemGravityRod::onAttack);
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> ((ItemExchangeRod) ModItems.exchangeRod).onLeftClick(player, world, hand, pos, direction));
		ServerTickEvents.END_WORLD_TICK.register(ItemTerraAxe::onTickEnd);
		AttackEntityCallback.EVENT.register(ItemTerraSword::attackEntity);
		EntitySleepEvents.ALLOW_SLEEPING.register(SleepingHandler::trySleep);
		OrechidManager.registerListener();
		TileCraftCrate.registerListener();

		ModLootModifiers.init();
		ModCriteriaTriggers.init();
	}

	private static <T> BiConsumer<T, ResourceLocation> bind(Registry<? super T> registry) {
		return (t, id) -> Registry.register(registry, id, t);
	}

	private void commonSetup() {
		PacketHandler.init();
		PaintableData.init();

		CorporeaHelper.instance().registerRequestMatcher(prefix("string"), CorporeaStringMatcher.class, CorporeaStringMatcher::createFromNBT);
		CorporeaHelper.instance().registerRequestMatcher(prefix("item_stack"), CorporeaItemStackMatcher.class, CorporeaItemStackMatcher::createFromNBT);

		if (Botania.gardenOfGlassLoaded) {
			UseBlockCallback.EVENT.register(SkyblockWorldEvents::onPlayerInteract);
		}

		SkyblockChunkGenerator.init();

		ModEntities.registerAttributes(FabricDefaultAttributeRegistry::register);

		PatchouliAPI.get().registerMultiblock(Registry.BLOCK.getKey(ModBlocks.alfPortal), TileAlfPortal.MULTIBLOCK.get());
		PatchouliAPI.get().registerMultiblock(Registry.BLOCK.getKey(ModBlocks.terraPlate), TileTerraPlate.MULTIBLOCK.get());
		PatchouliAPI.get().registerMultiblock(Registry.BLOCK.getKey(ModBlocks.enchanter), TileEnchanter.MULTIBLOCK.get());

		String[][] pat = new String[][] {
				{
						"P_______P",
						"_________",
						"_________",
						"_________",
						"_________",
						"_________",
						"_________",
						"_________",
						"P_______P",
				},
				{
						"_________",
						"_________",
						"_________",
						"_________",
						"____B____",
						"_________",
						"_________",
						"_________",
						"_________",
				},
				{
						"_________",
						"_________",
						"_________",
						"___III___",
						"___I0I___",
						"___III___",
						"_________",
						"_________",
						"_________",
				}
		};
		IStateMatcher sm = PatchouliAPI.get().predicateMatcher(Blocks.IRON_BLOCK,
				state -> state.is(BlockTags.BEACON_BASE_BLOCKS));
		IMultiblock mb = PatchouliAPI.get().makeMultiblock(
				pat,
				'P', ModBlocks.gaiaPylon,
				'B', Blocks.BEACON,
				'I', sm,
				'0', sm
		);
		PatchouliAPI.get().registerMultiblock(prefix("gaia_ritual"), mb);

		ModBlocks.addDispenserBehaviours();

		ModStats.init();
		registerPaintables();

		BotaniaCapabilities.EXOFLAME_HEATABLE.registerFallback((world, pos, state, blockEntity, context) -> {
			if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
				return new ExoflameFurnaceHandler.FurnaceExoflameHeatable(furnace);
			}
			return null;
		});
		BotaniaCapabilities.HORN_HARVEST.registerForBlocks((w, p, s, be, c) -> (world, pos, stack, hornType) -> hornType == IHornHarvestable.EnumHornType.CANOPY,
				Blocks.VINE, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT, Blocks.TWISTING_VINES,
				Blocks.TWISTING_VINES_PLANT, Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT);
	}

	private void registerPaintables() {
		BotaniaAPI.instance().registerPaintableBlock(Blocks.GLASS, ColorHelper.STAINED_GLASS_MAP);
		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.STAINED_GLASS_MAP.apply(color), ColorHelper.STAINED_GLASS_MAP);
		}

		BotaniaAPI.instance().registerPaintableBlock(Blocks.GLASS_PANE, ColorHelper.STAINED_GLASS_PANE_MAP);
		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.STAINED_GLASS_PANE_MAP.apply(color), ColorHelper.STAINED_GLASS_PANE_MAP);
		}

		BotaniaAPI.instance().registerPaintableBlock(Blocks.TERRACOTTA, ColorHelper.TERRACOTTA_MAP);
		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.TERRACOTTA_MAP.apply(color), ColorHelper.TERRACOTTA_MAP);
		}

		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.GLAZED_TERRACOTTA_MAP.apply(color), ColorHelper.GLAZED_TERRACOTTA_MAP);
		}

		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.WOOL_MAP.apply(color), ColorHelper.WOOL_MAP);
		}

		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.CARPET_MAP.apply(color), ColorHelper.CARPET_MAP);
		}

		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.CONCRETE_MAP.apply(color), ColorHelper.CONCRETE_MAP);
		}

		for (DyeColor color : DyeColor.values()) {
			BotaniaAPI.instance().registerPaintableBlock(ColorHelper.CONCRETE_POWDER_MAP.apply(color), ColorHelper.CONCRETE_POWDER_MAP);
		}
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
		if (Botania.gardenOfGlassLoaded) {
			SkyblockCommand.register(dispatcher);
		}
		DataGenerators.registerCommands(dispatcher);
	}

	private void serverStopping(MinecraftServer server) {
		ManaNetworkHandler.instance.clear();
		TileCorporeaIndex.clearIndexCache();
	}

}
