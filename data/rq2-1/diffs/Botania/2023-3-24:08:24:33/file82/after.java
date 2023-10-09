/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;

import vazkii.botania.common.lib.LibMisc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public final class BotaniaBannerPatterns {
	private static final List<BannerPattern> ALL = new ArrayList<>();
	public static final ResourceKey<BannerPattern> FLOWER = make("flower");
	public static final ResourceKey<BannerPattern> LEXICON = make("lexicon");
	public static final ResourceKey<BannerPattern> LOGO = make("logo");
	public static final ResourceKey<BannerPattern> SAPLING = make("sapling");
	public static final ResourceKey<BannerPattern> TINY_POTATO = make("tiny_potato");
	public static final ResourceKey<BannerPattern> SPARK_DISPERSIVE = make("spark_dispersive");
	public static final ResourceKey<BannerPattern> SPARK_DOMINANT = make("spark_dominant");
	public static final ResourceKey<BannerPattern> SPARK_RECESSIVE = make("spark_recessive");
	public static final ResourceKey<BannerPattern> SPARK_ISOLATED = make("spark_isolated");

	public static final ResourceKey<BannerPattern> FISH = make("fish");
	public static final ResourceKey<BannerPattern> AXE = make("axe");
	public static final ResourceKey<BannerPattern> HOE = make("hoe");
	public static final ResourceKey<BannerPattern> PICKAXE = make("pickaxe");
	public static final ResourceKey<BannerPattern> SHOVEL = make("shovel");
	public static final ResourceKey<BannerPattern> SWORD = make("sword");

	private static ResourceKey<BannerPattern> make(String hashName) {
		BannerPattern pattern = new BannerPattern(LibMisc.MOD_ID + ":" + hashName);
		ALL.add(pattern);
		return ResourceKey.create(Registries.BANNER_PATTERN, prefix(hashName));
	}

	public static void submitRegistrations(BiConsumer<BannerPattern, ResourceLocation> consumer) {
		for (var pattern : ALL) {
			consumer.accept(pattern, new ResourceLocation(pattern.getHashname()));
		}
	}
}
