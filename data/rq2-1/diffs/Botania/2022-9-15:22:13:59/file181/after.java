/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.common.block.*;
import vazkii.botania.common.block.decor.BotaniaMushroomBlock;
import vazkii.botania.common.block.decor.FloatingFlowerBlock;
import vazkii.botania.common.block.decor.FlowerMotifBlock;
import vazkii.botania.common.block.decor.PetalBlock;
import vazkii.botania.common.block.mana.ManaPoolBlock;
import vazkii.botania.common.block.mana.ManaSpreaderBlock;
import vazkii.botania.common.item.lens.LensItem;
import vazkii.botania.common.item.material.MysticalPetalItem;
import vazkii.botania.common.lib.LibMisc;
import vazkii.botania.data.util.ModelWithOverrides;
import vazkii.botania.data.util.OverrideHolder;
import vazkii.botania.data.util.SimpleModelSupplierWithOverrides;
import vazkii.botania.mixin.AccessorTextureSlot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static vazkii.botania.common.item.BotaniaItems.*;
import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;
import static vazkii.botania.data.BlockstateProvider.takeAll;

public class ItemModelProvider implements DataProvider {
	private static final TextureSlot LAYER1 = AccessorTextureSlot.make("layer1");
	private static final TextureSlot LAYER2 = AccessorTextureSlot.make("layer2");
	private static final TextureSlot LAYER3 = AccessorTextureSlot.make("layer3");
	private static final ModelTemplate GENERATED_1 = new ModelTemplate(Optional.of(new ResourceLocation("item/generated")), Optional.empty(), TextureSlot.LAYER0, LAYER1);
	private static final ModelTemplate GENERATED_2 = new ModelTemplate(Optional.of(new ResourceLocation("item/generated")), Optional.empty(), TextureSlot.LAYER0, LAYER1, LAYER2);
	private static final ModelTemplate HANDHELD_1 = new ModelTemplate(Optional.of(new ResourceLocation("item/handheld")), Optional.empty(), TextureSlot.LAYER0, LAYER1);
	private static final ModelTemplate HANDHELD_3 = new ModelTemplate(Optional.of(new ResourceLocation("item/handheld")), Optional.empty(), TextureSlot.LAYER0, LAYER1, LAYER2, LAYER3);
	private static final TextureSlot OUTSIDE = AccessorTextureSlot.make("outside");
	private static final TextureSlot CORE = AccessorTextureSlot.make("core");
	private static final ModelTemplate SPREADER = new ModelTemplate(Optional.of(prefix("block/shapes/spreader_item")), Optional.empty(), TextureSlot.SIDE, TextureSlot.BACK, TextureSlot.INSIDE, OUTSIDE, CORE);
	private static final ModelWithOverrides GENERATED_OVERRIDES = new ModelWithOverrides(new ResourceLocation("item/generated"), TextureSlot.LAYER0);
	private static final ModelWithOverrides GENERATED_OVERRIDES_1 = new ModelWithOverrides(new ResourceLocation("item/generated"), TextureSlot.LAYER0, LAYER1);
	private static final ModelWithOverrides HANDHELD_OVERRIDES = new ModelWithOverrides(new ResourceLocation("item/handheld"), TextureSlot.LAYER0);
	private static final ModelWithOverrides HANDHELD_OVERRIDES_2 = new ModelWithOverrides(new ResourceLocation("item/handheld"), TextureSlot.LAYER0, LAYER1, LAYER2);

	private final DataGenerator generator;

	public ItemModelProvider(DataGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void run(CachedOutput cache) {
		Set<Item> items = Registry.ITEM.stream().filter(i -> LibMisc.MOD_ID.equals(Registry.ITEM.getKey(i).getNamespace()))
				.collect(Collectors.toSet());
		Map<ResourceLocation, Supplier<JsonElement>> map = new HashMap<>();
		registerItemBlocks(takeAll(items, i -> i instanceof BlockItem).stream().map(i -> (BlockItem) i).collect(Collectors.toSet()), map::put);
		registerItemOverrides(items, map::put);
		registerItems(items, map::put);

		for (Map.Entry<ResourceLocation, Supplier<JsonElement>> e : map.entrySet()) {
			ResourceLocation id = e.getKey();
			Path out = generator.getOutputFolder().resolve("assets/" + id.getNamespace() + "/models/" + id.getPath() + ".json");
			try {
				DataProvider.saveStable(cache, e.getValue().get(), out);
			} catch (IOException ex) {
				BotaniaAPI.LOGGER.error("Failed to generate {}", out, ex);
			}
		}
	}

	private static void registerItems(Set<Item> items, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		// Written manually
		items.remove(manaGun);

		takeAll(items, i -> i instanceof LensItem).forEach(i -> {
			ResourceLocation lens;
			if (i == lensTime || i == lensWarp || i == lensFire) {
				// To avoid z-fighting
				lens = prefix("item/lens_small");
			} else {
				lens = prefix("item/lens");
			}
			GENERATED_1.create(ModelLocationUtils.getModelLocation(i),
					TextureMapping.layer0(lens).put(LAYER1, TextureMapping.getItemTexture(i)), consumer);
		});

		GENERATED_1.create(ModelLocationUtils.getModelLocation(bloodPendant),
				TextureMapping.layer0(TextureMapping.getItemTexture(bloodPendant))
						.put(LAYER1, TextureMapping.getItemTexture(bloodPendant, "_overlay")),
				consumer);
		items.remove(bloodPendant);

		HANDHELD_1.create(ModelLocationUtils.getModelLocation(enderDagger),
				TextureMapping.layer0(TextureMapping.getItemTexture(enderDagger))
						.put(LAYER1, TextureMapping.getItemTexture(enderDagger, "_overlay")),
				consumer);
		items.remove(enderDagger);

		GENERATED_1.create(ModelLocationUtils.getModelLocation(incenseStick),
				TextureMapping.layer0(TextureMapping.getItemTexture(incenseStick))
						.put(LAYER1, TextureMapping.getItemTexture(incenseStick, "_overlay")),
				consumer);
		items.remove(incenseStick);

		GENERATED_1.create(ModelLocationUtils.getModelLocation(manaMirror),
				TextureMapping.layer0(TextureMapping.getItemTexture(manaMirror))
						.put(LAYER1, TextureMapping.getItemTexture(manaMirror, "_overlay")),
				consumer);
		items.remove(manaMirror);

		GENERATED_1.create(ModelLocationUtils.getModelLocation(manaTablet),
				TextureMapping.layer0(TextureMapping.getItemTexture(manaTablet))
						.put(LAYER1, TextureMapping.getItemTexture(manaTablet, "_overlay")),
				consumer);
		items.remove(manaTablet);

		GENERATED_2.create(ModelLocationUtils.getModelLocation(thirdEye),
				new TextureMapping().put(TextureSlot.LAYER0, TextureMapping.getItemTexture(thirdEye, "_0"))
						.put(LAYER1, TextureMapping.getItemTexture(thirdEye, "_1"))
						.put(LAYER2, TextureMapping.getItemTexture(thirdEye, "_2")),
				consumer);
		items.remove(thirdEye);

		takeAll(items, cobbleRod, dirtRod, diviningRod, elementiumAxe, elementiumPick, elementiumShovel, elementiumHoe, elementiumSword,
				exchangeRod, fireRod, glassPick, gravityRod, manasteelAxe, manasteelPick, manasteelShears, manasteelShovel, manasteelHoe,
				missileRod, obedienceStick, rainbowRod, smeltRod, starSword, terraSword, terraformRod, thunderSword, waterRod,
				kingKey, skyDirtRod).forEach(i -> ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(i), consumer));

		takeAll(items, i -> true).forEach(i -> ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(i), consumer));
	}

	private static void singleGeneratedOverride(Item item, ResourceLocation overrideModel, ResourceLocation predicate, double value, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		ModelTemplates.FLAT_ITEM.create(overrideModel, TextureMapping.layer0(overrideModel), consumer);
		GENERATED_OVERRIDES.create(ModelLocationUtils.getModelLocation(item),
				TextureMapping.layer0(item),
				new OverrideHolder()
						.add(overrideModel, Pair.of(predicate, value)),
				consumer);
	}

	private static void singleGeneratedSuffixOverride(Item item, String suffix, ResourceLocation predicate, double value, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		singleGeneratedOverride(item, ModelLocationUtils.getModelLocation(item, suffix), predicate, value, consumer);
	}

	private static void singleHandheldOverride(Item item, ResourceLocation overrideModel, ResourceLocation predicate, double value, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		ModelTemplates.FLAT_HANDHELD_ITEM.create(overrideModel, TextureMapping.layer0(overrideModel), consumer);
		HANDHELD_OVERRIDES.create(ModelLocationUtils.getModelLocation(item),
				TextureMapping.layer0(item),
				new OverrideHolder()
						.add(overrideModel, Pair.of(predicate, value)),
				consumer);
	}

	private static void singleHandheldSuffixOverride(Item item, String suffix, ResourceLocation predicate, double value, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		singleHandheldOverride(item, ModelLocationUtils.getModelLocation(item, suffix), predicate, value, consumer);
	}

	private static void registerItemOverrides(Set<Item> items, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		// Written manually
		items.remove(livingwoodBow);
		items.remove(crystalBow);

		singleGeneratedSuffixOverride(blackHoleTalisman, "_active", prefix("active"), 1.0, consumer);
		items.remove(blackHoleTalisman);

		OverrideHolder flaskOverrides = new OverrideHolder();
		for (int i = 1; i <= 5; i++) {
			ResourceLocation overrideModel = ModelLocationUtils.getModelLocation(brewFlask, "_" + i);
			GENERATED_1.create(overrideModel,
					TextureMapping.layer0(flask).put(LAYER1, overrideModel),
					consumer);

			flaskOverrides.add(overrideModel, Pair.of(prefix("swigs_taken"), (double) i));
		}
		GENERATED_OVERRIDES_1.create(ModelLocationUtils.getModelLocation(brewFlask),
				TextureMapping.layer0(flask).put(LAYER1, TextureMapping.getItemTexture(brewFlask, "_0")),
				flaskOverrides,
				consumer);
		items.remove(brewFlask);

		OverrideHolder vialOverrides = new OverrideHolder();
		for (int i = 1; i <= 3; i++) {
			ResourceLocation overrideModel = ModelLocationUtils.getModelLocation(brewVial, "_" + i);
			GENERATED_1.create(overrideModel,
					TextureMapping.layer0(vial).put(LAYER1, overrideModel),
					consumer);
			vialOverrides.add(overrideModel, Pair.of(prefix("swigs_taken"), (double) i));
		}
		GENERATED_OVERRIDES_1.create(ModelLocationUtils.getModelLocation(brewVial),
				TextureMapping.layer0(vial).put(LAYER1, TextureMapping.getItemTexture(brewVial, "_0")),
				vialOverrides, consumer);
		items.remove(brewVial);

		singleHandheldOverride(elementiumShears, prefix("item/dammitreddit"), prefix("reddit"), 1, consumer);
		items.remove(elementiumShears);

		ResourceLocation vuvuzela = prefix("item/vuvuzela");
		ModelTemplates.FLAT_HANDHELD_ITEM.create(vuvuzela, TextureMapping.layer0(vuvuzela), consumer);
		for (Item i : new Item[] { grassHorn, leavesHorn, snowHorn }) {
			GENERATED_OVERRIDES.create(ModelLocationUtils.getModelLocation(i),
					TextureMapping.layer0(i),
					new OverrideHolder()
							.add(vuvuzela, Pair.of(prefix("vuvuzela"), 1.0)),
					consumer
			);
		}
		items.remove(grassHorn);
		items.remove(leavesHorn);
		items.remove(snowHorn);

		singleGeneratedOverride(infiniteFruit, prefix("item/dasboot"), prefix("boot"), 1, consumer);
		items.remove(infiniteFruit);

		singleGeneratedSuffixOverride(lexicon, "_elven", prefix("elven"), 1.0, consumer);
		items.remove(lexicon);

		singleGeneratedSuffixOverride(magnetRing, "_active", prefix("active"), 1.0, consumer);
		items.remove(magnetRing);

		singleGeneratedSuffixOverride(magnetRingGreater, "_active", prefix("active"), 1.0, consumer);
		items.remove(magnetRingGreater);

		OverrideHolder bottleOverrides = new OverrideHolder();
		for (int i = 1; i <= 5; i++) {
			ResourceLocation overrideModel = ModelLocationUtils.getModelLocation(manaBottle, "_" + i);
			ModelTemplates.FLAT_ITEM.create(overrideModel, TextureMapping.layer0(overrideModel), consumer);
			bottleOverrides.add(overrideModel, Pair.of(prefix("swigs_taken"), (double) i));
		}
		GENERATED_OVERRIDES.create(ModelLocationUtils.getModelLocation(manaBottle),
				TextureMapping.layer0(manaBottle),
				bottleOverrides,
				consumer);
		items.remove(manaBottle);

		singleGeneratedOverride(manaCookie, prefix("item/totalbiscuit"), prefix("totalbiscuit"), 1.0, consumer);
		items.remove(manaCookie);

		singleHandheldOverride(manasteelSword, prefix("item/elucidator"), prefix("elucidator"), 1.0, consumer);
		items.remove(manasteelSword);

		singleGeneratedSuffixOverride(manaweaveHelm, "_holiday", prefix("holiday"), 1.0, consumer);
		items.remove(manaweaveHelm);

		singleGeneratedSuffixOverride(manaweaveChest, "_holiday", prefix("holiday"), 1.0, consumer);
		items.remove(manaweaveChest);

		singleGeneratedSuffixOverride(manaweaveLegs, "_holiday", prefix("holiday"), 1.0, consumer);
		items.remove(manaweaveLegs);

		singleGeneratedSuffixOverride(manaweaveBoots, "_holiday", prefix("holiday"), 1.0, consumer);
		items.remove(manaweaveBoots);

		singleGeneratedSuffixOverride(slimeBottle, "_active", prefix("active"), 1.0, consumer);
		items.remove(slimeBottle);

		singleGeneratedSuffixOverride(spawnerMover, "_full", prefix("full"), 1.0, consumer);
		items.remove(spawnerMover);

		singleGeneratedSuffixOverride(temperanceStone, "_active", prefix("active"), 1.0, consumer);
		items.remove(temperanceStone);

		singleHandheldSuffixOverride(terraAxe, "_active", prefix("active"), 1.0, consumer);
		items.remove(terraAxe);

		ResourceLocation enabledModel = ModelLocationUtils.getModelLocation(terraPick, "_active");
		HANDHELD_1.create(enabledModel, TextureMapping.layer0(terraPick).put(LAYER1, enabledModel), consumer);

		ResourceLocation tippedModel = ModelLocationUtils.getModelLocation(terraPick, "_tipped");
		ModelTemplates.FLAT_HANDHELD_ITEM.create(tippedModel, TextureMapping.layer0(tippedModel), consumer);

		ResourceLocation tippedEnabledModel = ModelLocationUtils.getModelLocation(terraPick, "_tipped_active");
		HANDHELD_1.create(tippedEnabledModel,
				TextureMapping.layer0(tippedModel).put(LAYER1, TextureMapping.getItemTexture(terraPick, "_active")),
				consumer);

		HANDHELD_OVERRIDES.create(ModelLocationUtils.getModelLocation(terraPick),
				TextureMapping.layer0(terraPick),
				new OverrideHolder()
						.add(enabledModel, Pair.of(prefix("active"), 1.0))
						.add(tippedModel, Pair.of(prefix("tipped"), 1.0))
						.add(tippedEnabledModel, Pair.of(prefix("tipped"), 1.0), Pair.of(prefix("active"), 1.0)),
				consumer);
		items.remove(terraPick);

		singleHandheldSuffixOverride(tornadoRod, "_active", prefix("active"), 1.0, consumer);
		items.remove(tornadoRod);

		TextureMapping twigWandTextures = TextureMapping.layer0(twigWand)
				.put(LAYER1, TextureMapping.getItemTexture(twigWand, "_top"))
				.put(LAYER2, TextureMapping.getItemTexture(twigWand, "_bottom"));
		ResourceLocation twigWandBind = ModelLocationUtils.getModelLocation(twigWand, "_bind");
		HANDHELD_3.create(twigWandBind,
				twigWandTextures.copyAndUpdate(LAYER3, TextureMapping.getItemTexture(twigWand, "_bind")),
				consumer);
		HANDHELD_OVERRIDES_2.create(ModelLocationUtils.getModelLocation(twigWand),
				twigWandTextures,
				new OverrideHolder()
						.add(twigWandBind, Pair.of(prefix("bindmode"), 1.0)),
				consumer);
		items.remove(twigWand);

		TextureMapping dreamwoodWandTextures = TextureMapping.layer0(dreamwoodWand)
				.put(LAYER1, TextureMapping.getItemTexture(dreamwoodWand, "_top"))
				.put(LAYER2, TextureMapping.getItemTexture(dreamwoodWand, "_bottom"));
		ResourceLocation dreamwoodWandBind = ModelLocationUtils.getModelLocation(dreamwoodWand, "_bind");
		HANDHELD_3.create(dreamwoodWandBind,
				dreamwoodWandTextures.copyAndUpdate(LAYER3, TextureMapping.getItemTexture(dreamwoodWand, "_bind")),
				consumer);
		HANDHELD_OVERRIDES_2.create(ModelLocationUtils.getModelLocation(dreamwoodWand),
				dreamwoodWandTextures,
				new OverrideHolder()
						.add(dreamwoodWandBind, Pair.of(prefix("bindmode"), 1.0)),
				consumer);
		items.remove(dreamwoodWand);
	}

	private void registerItemBlocks(Set<BlockItem> itemBlocks, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		// Manually written
		itemBlocks.remove(BotaniaBlocks.corporeaCrystalCube.asItem());

		// Generated by FloatingFlowerModelProvider
		itemBlocks.removeIf(i -> {
			var id = Registry.BLOCK.getKey(i.getBlock());
			return id.getNamespace().equals(LibMisc.MOD_ID) && i.getBlock() instanceof FloatingFlowerBlock;
		});

		GENERATED_1.create(ModelLocationUtils.getModelLocation(BotaniaBlocks.animatedTorch.asItem()),
				TextureMapping.layer0(Blocks.REDSTONE_TORCH).put(LAYER1, prefix("block/animated_torch_glimmer")), consumer);
		itemBlocks.remove(BotaniaBlocks.animatedTorch.asItem());

		ModelTemplates.SKULL_INVENTORY.create(ModelLocationUtils.getModelLocation(BotaniaBlocks.gaiaHead.asItem()), new TextureMapping(), consumer);
		itemBlocks.remove(BotaniaBlocks.gaiaHead.asItem());

		takeAll(itemBlocks, i -> i.getBlock() instanceof BotaniaDoubleFlowerBlock).forEach(i -> {
			ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(TextureMapping.getBlockTexture(i.getBlock(), "_top")), consumer);
		});

		takeAll(itemBlocks, i -> i.getBlock() instanceof PetalBlock).forEach(i -> {
			consumer.accept(ModelLocationUtils.getModelLocation(i), new DelegatedModel(prefix("block/petal_block")));
		});

		takeAll(itemBlocks, BotaniaBlocks.livingwoodFramed.asItem(), BotaniaBlocks.dreamwoodFramed.asItem()).forEach(i -> {
			String name = i == BotaniaBlocks.livingwoodFramed.asItem() ? "livingwood" : "dreamwood";
			consumer.accept(ModelLocationUtils.getModelLocation(i), new DelegatedModel(prefix("block/framed_" + name + "_horizontal_z")));
		});

		consumer.accept(ModelLocationUtils.getModelLocation(BotaniaBlocks.livingwoodFramed.asItem()), new DelegatedModel(prefix("block/framed_livingwood_horizontal_z")));
		consumer.accept(ModelLocationUtils.getModelLocation(BotaniaBlocks.dreamwoodFramed.asItem()), new DelegatedModel(prefix("block/framed_dreamwood_horizontal_z")));
		itemBlocks.remove(BotaniaBlocks.livingwoodFramed.asItem());
		itemBlocks.remove(BotaniaBlocks.dreamwoodFramed.asItem());

		takeAll(itemBlocks, i -> i.getBlock() instanceof IronBarsBlock).forEach(i -> {
			String name = Registry.ITEM.getKey(i).getPath();
			String baseName = name.substring(0, name.length() - "_pane".length());
			ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(prefix("block/" + baseName)), consumer);
		});

		Predicate<BlockItem> defaultGenerated = i -> {
			Block b = i.getBlock();
			return b instanceof SpecialFlowerBlock || b instanceof BotaniaMushroomBlock
					|| b instanceof LuminizerBlock
					|| b instanceof BotaniaFlowerBlock
					|| b == BotaniaBlocks.ghostRail;
		};
		takeAll(itemBlocks, defaultGenerated).forEach(i -> {
			ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(i.getBlock()), consumer);
		});

		takeAll(itemBlocks, b -> b.getBlock() instanceof FlowerMotifBlock).forEach(i -> {
			String name = Registry.ITEM.getKey(i).getPath();
			ResourceLocation texName = prefix("block/" + name.replace("_motif", ""));
			ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(texName), consumer);
		});

		takeAll(itemBlocks, i -> i.getBlock() instanceof ManaPoolBlock).forEach(i -> {
			ResourceLocation fullModel = ModelLocationUtils.getModelLocation(i.getBlock(), "_full");
			OverrideHolder overrides = new OverrideHolder().add(fullModel, Pair.of(prefix("full"), 1.0));
			consumer.accept(ModelLocationUtils.getModelLocation(i),
					new SimpleModelSupplierWithOverrides(ModelLocationUtils.getModelLocation(i.getBlock()), overrides));
		});
		takeAll(itemBlocks, Stream.of(BotaniaFluffBlocks.livingwoodWall, BotaniaFluffBlocks.livingwoodStrippedWall,
				BotaniaFluffBlocks.dreamwoodWall, BotaniaFluffBlocks.dreamwoodStrippedWall)
				.map(b -> (BlockItem) b.asItem())
				.toArray(BlockItem[]::new)).forEach(i -> {

					String name = Registry.ITEM.getKey(i).getPath();
					String baseName = name.substring(0, name.length() - "_wall".length()) + "_log";
					ModelTemplates.WALL_INVENTORY.create(ModelLocationUtils.getModelLocation(i),
							new TextureMapping().put(TextureSlot.WALL, prefix("block/" + baseName)), consumer);
				});

		takeAll(itemBlocks, i -> i.getBlock() instanceof WallBlock).forEach(i -> {
			String name = Registry.ITEM.getKey(i).getPath();
			String baseName = name.substring(0, name.length() - "_wall".length());
			ModelTemplates.WALL_INVENTORY.create(ModelLocationUtils.getModelLocation(i),
					new TextureMapping().put(TextureSlot.WALL, prefix("block/" + baseName)), consumer);
		});

		takeAll(itemBlocks, i -> i.getBlock() instanceof ManaSpreaderBlock).forEach(i -> {
			String name = Registry.ITEM.getKey(i).getPath();
			String outside;
			if (i.getBlock() == BotaniaBlocks.elvenSpreader) {
				outside = "dreamwood_log_3";
			} else if (i.getBlock() == BotaniaBlocks.gaiaSpreader) {
				outside = name + "_outside";
			} else {
				outside = "livingwood_log";
			}
			String inside;
			if (i.getBlock() == BotaniaBlocks.elvenSpreader) {
				inside = "stripped_dreamwood_log_3";
			} else if (i.getBlock() == BotaniaBlocks.gaiaSpreader) {
				inside = name + "_inside";
			} else {
				inside = "stripped_livingwood_log";
			}
			SPREADER.create(ModelLocationUtils.getModelLocation(i),
					new TextureMapping()
							.put(TextureSlot.SIDE, TextureMapping.getBlockTexture(i.getBlock(), "_side"))
							.put(OUTSIDE, prefix("block/" + outside))
							.put(TextureSlot.BACK, TextureMapping.getBlockTexture(i.getBlock(), "_back"))
							.put(TextureSlot.INSIDE, prefix("block/" + inside))
							.put(CORE, TextureMapping.getBlockTexture(i.getBlock(), "_core")),
					consumer);
		});

		takeAll(itemBlocks, BotaniaBlocks.avatar.asItem(), BotaniaBlocks.bellows.asItem(),
				BotaniaBlocks.brewery.asItem(), BotaniaBlocks.corporeaIndex.asItem(), BotaniaBlocks.gaiaPylon.asItem(),
				BotaniaBlocks.hourglass.asItem(), BotaniaBlocks.manaPylon.asItem(), BotaniaBlocks.naturaPylon.asItem(), BotaniaBlocks.teruTeruBozu.asItem())
						.forEach(i -> builtinEntity(i, consumer));

		takeAll(itemBlocks, i -> i instanceof MysticalPetalItem).forEach(i -> {
			ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(i), TextureMapping.layer0(prefix("item/petal")), consumer);
		});

		ModelTemplates.FENCE_INVENTORY.create(ModelLocationUtils.getModelLocation(BotaniaFluffBlocks.dreamwoodFence.asItem()),
				TextureMapping.defaultTexture(BotaniaBlocks.dreamwoodPlanks), consumer);
		itemBlocks.remove(BotaniaFluffBlocks.dreamwoodFence.asItem());

		ModelTemplates.FENCE_INVENTORY.create(ModelLocationUtils.getModelLocation(BotaniaFluffBlocks.livingwoodFence.asItem()),
				TextureMapping.defaultTexture(BotaniaBlocks.livingwoodPlanks), consumer);
		itemBlocks.remove(BotaniaFluffBlocks.livingwoodFence.asItem());

		consumer.accept(ModelLocationUtils.getModelLocation(BotaniaBlocks.elfGlass.asItem()), new DelegatedModel(prefix("block/elf_glass_0")));
		itemBlocks.remove(BotaniaBlocks.elfGlass.asItem());

		itemBlocks.forEach(i -> {
			consumer.accept(ModelLocationUtils.getModelLocation(i), new DelegatedModel(ModelLocationUtils.getModelLocation(i.getBlock())));
		});
	}

	// [VanillaCopy] item/chest.json
	// Scuffed af.....but it works :wacko:
	private static final String BUILTIN_ENTITY_DISPLAY_STR =
			"""
					{
						"gui": {
							"rotation": [30, 45, 0],
							"translation": [0, 0, 0],
							"scale": [0.625, 0.625, 0.625]
						},
						"ground": {
							"rotation": [0, 0, 0],
							"translation": [0, 3, 0],
							"scale": [0.25, 0.25, 0.25]
						},
						"head": {
							"rotation": [0, 180, 0],
							"translation": [0, 0, 0],
							"scale": [1, 1, 1]
						},
						"fixed": {
							"rotation": [0, 180, 0],
							"translation": [0, 0, 0],
							"scale": [0.5, 0.5, 0.5]
						},
						"thirdperson_righthand": {
							"rotation": [75, 315, 0],
							"translation": [0, 2.5, 0],
							"scale": [0.375, 0.375, 0.375]
						},
						"firstperson_righthand": {
							"rotation": [0, 315, 0],
							"translation": [0, 0, 0],
							"scale": [0.4, 0.4, 0.4]
						}
					}""";
	private static final JsonElement BUILTIN_ENTITY_DISPLAY = new Gson().fromJson(BUILTIN_ENTITY_DISPLAY_STR, JsonElement.class);

	protected void builtinEntity(Item i, BiConsumer<ResourceLocation, Supplier<JsonElement>> consumer) {
		consumer.accept(ModelLocationUtils.getModelLocation(i), () -> {
			JsonObject json = new JsonObject();
			json.addProperty("parent", "minecraft:builtin/entity");
			json.add("display", BUILTIN_ENTITY_DISPLAY);
			return json;
		});
	}

	@NotNull
	@Override
	public String getName() {
		return "Botania item models";
	}
}
