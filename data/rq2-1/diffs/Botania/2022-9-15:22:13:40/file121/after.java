/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.block_entity.corporea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import vazkii.botania.api.corporea.CorporeaHelper;
import vazkii.botania.api.corporea.CorporeaRequestMatcher;
import vazkii.botania.api.corporea.CorporeaRequestor;
import vazkii.botania.api.corporea.CorporeaSpark;
import vazkii.botania.common.block.tile.ModTiles;
import vazkii.botania.common.helper.InventoryHelper;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.ArrayList;
import java.util.List;

public class CorporeaFunnelBlockEntity extends BaseCorporeaBlockEntity implements CorporeaRequestor {
	public CorporeaFunnelBlockEntity(BlockPos pos, BlockState state) {
		super(ModTiles.CORPOREA_FUNNEL, pos, state);
	}

	public void doRequest() {
		CorporeaSpark spark = getSpark();
		if (spark != null && spark.getMaster() != null) {
			List<ItemStack> filter = getFilter();
			if (!filter.isEmpty()) {
				ItemStack stack = filter.get(level.random.nextInt(filter.size()));

				if (!stack.isEmpty()) {
					doCorporeaRequest(CorporeaHelper.instance().createMatcher(stack, true), stack.getCount(), spark);
				}
			}
		}
	}

	public List<ItemStack> getFilter() {
		List<ItemStack> filter = new ArrayList<>();

		final int[] rotationToStackSize = new int[] {
				1, 2, 4, 8, 16, 32, 48, 64
		};

		for (Direction dir : Direction.values()) {
			List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, new AABB(worldPosition.relative(dir), worldPosition.relative(dir).offset(1, 1, 1)));
			for (ItemFrame frame : frames) {
				Direction orientation = frame.getDirection();
				if (orientation == dir) {
					ItemStack stack = frame.getItem();
					if (!stack.isEmpty()) {
						ItemStack copy = stack.copy();
						copy.setCount(rotationToStackSize[frame.getRotation()]);
						filter.add(copy);
					}
				}
			}
		}

		return filter;
	}

	@Override
	public void doCorporeaRequest(CorporeaRequestMatcher request, int count, CorporeaSpark spark) {
		BlockPos invPos = getInvPos();

		List<ItemStack> stacks = CorporeaHelper.instance().requestItem(request, count, spark, true).stacks();
		spark.onItemsRequested(stacks);
		for (ItemStack reqStack : stacks) {
			if (invPos != null
					&& IXplatAbstractions.INSTANCE.insertToInventory(level, invPos, Direction.UP, reqStack, true).isEmpty()) {
				InventoryHelper.checkEmpty(
						IXplatAbstractions.INSTANCE.insertToInventory(level, invPos, Direction.UP, reqStack, false)
				);
			} else {
				ItemEntity item = new ItemEntity(level, spark.entity().getX(), spark.entity().getY(), spark.entity().getZ(), reqStack);
				level.addFreshEntity(item);
			}
		}
	}

	@Nullable
	private BlockPos getInvPos() {
		BlockPos downOne = worldPosition.below();
		if (IXplatAbstractions.INSTANCE.hasInventory(level, downOne, Direction.UP)) {
			return downOne;
		}

		BlockPos downTwo = worldPosition.below(2);
		if (IXplatAbstractions.INSTANCE.hasInventory(level, downTwo, Direction.UP)) {
			return downTwo;
		}

		return null;
	}

}
