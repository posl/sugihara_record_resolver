/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.api.mana.spark.SparkUpgradeType;
import vazkii.botania.common.block.tile.ModTiles;
import vazkii.botania.common.item.ItemSparkUpgrade;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SparkTinkererBlockEntity extends ExposedSimpleInventoryBlockEntity {
	public SparkTinkererBlockEntity(BlockPos pos, BlockState state) {
		super(ModTiles.SPARK_CHANGER, pos, state);
	}

	public void doSwap() {
		if (level.isClientSide) {
			return;
		}

		ItemStack changeStack = getItemHandler().getItem(0);
		List<SparkAttachable> attachables = new ArrayList<>();
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			var pos = worldPosition.relative(dir);
			var attach = IXplatAbstractions.INSTANCE.findSparkAttachable(level, pos, level.getBlockState(pos), level.getBlockEntity(pos), dir.getOpposite());
			if (attach != null) {
				ManaSpark spark = attach.getAttachedSpark();
				if (spark != null) {
					SparkUpgradeType upg = spark.getUpgrade();
					SparkUpgradeType newUpg = changeStack.isEmpty() ? SparkUpgradeType.NONE : ((ItemSparkUpgrade) changeStack.getItem()).type;
					if (upg != newUpg) {
						attachables.add(attach);
					}
				}
			}
		}

		if (attachables.size() > 0) {
			SparkAttachable attach = attachables.get(level.random.nextInt(attachables.size()));
			ManaSpark spark = attach.getAttachedSpark();
			SparkUpgradeType upg = spark.getUpgrade();
			ItemStack sparkStack = ItemSparkUpgrade.getByType(upg);
			SparkUpgradeType newUpg = changeStack.isEmpty() ? SparkUpgradeType.NONE : ((ItemSparkUpgrade) changeStack.getItem()).type;
			spark.setUpgrade(newUpg);
			Collection<ManaSpark> transfers = spark.getTransfers();
			if (transfers != null) {
				transfers.clear();
			}
			getItemHandler().setItem(0, sparkStack);
		}
	}

	@Override
	protected SimpleContainer createItemHandler() {
		return new SimpleContainer(1) {
			@Override
			public int getMaxStackSize() {
				return 1;
			}

			@Override
			public boolean canPlaceItem(int index, ItemStack stack) {
				return !stack.isEmpty() && stack.getItem() instanceof ItemSparkUpgrade;
			}
		};
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && !level.isClientSide) {
			VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
		}
	}

}
