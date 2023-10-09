/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.common.block.BlockMod;
import vazkii.botania.common.block.tile.ModTiles;
import vazkii.botania.common.block.tile.TileEnchanter;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockEnchanter extends BlockMod implements EntityBlock {

	public BlockEnchanter(Properties builder) {
		super(builder);
		registerDefaultState(defaultBlockState().setValue(BotaniaStateProps.ENCHANTER_DIRECTION, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BotaniaStateProps.ENCHANTER_DIRECTION);
	}

	@Nonnull
	@Override
	public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
		return new TileEnchanter(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModTiles.ENCHANTER, TileEnchanter::commonTick);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		TileEnchanter enchanter = (TileEnchanter) world.getBlockEntity(pos);
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.isEmpty() && stack.is(ModItems.twigWand)) {
			return InteractionResult.PASS;
		}

		boolean stackEnchantable = !stack.isEmpty()
				&& !stack.is(Items.BOOK)
				&& stack.isEnchantable()
				&& stack.getCount() == 1;

		if (enchanter.itemToEnchant.isEmpty()) {
			if (stackEnchantable) {
				enchanter.itemToEnchant = stack.copy();
				player.setItemInHand(hand, ItemStack.EMPTY);
				enchanter.sync();
			} else {
				return InteractionResult.PASS;
			}
		} else if (enchanter.stage == TileEnchanter.State.IDLE) {
			player.getInventory().placeItemBackInInventory(enchanter.itemToEnchant.copy());
			enchanter.itemToEnchant = ItemStack.EMPTY;
			enchanter.sync();
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void onRemove(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			BlockEntity tile = world.getBlockEntity(pos);

			if (tile instanceof TileEnchanter enchanter) {

				if (!enchanter.itemToEnchant.isEmpty()) {
					Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), enchanter.itemToEnchant);
				}
			}

			super.onRemove(state, world, pos, newState, isMoving);
		}
	}
}
