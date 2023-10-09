/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.api.item.BlockProvider;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.client.gui.ItemsRemainingRenderHandler;
import vazkii.botania.common.handler.BotaniaSounds;
import vazkii.botania.common.helper.ItemNBTHelper;
import vazkii.botania.common.item.equipment.tool.ToolCommons;
import vazkii.botania.common.item.rod.ItemExchangeRod;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.ArrayList;
import java.util.List;

public class ItemAstrolabe extends Item {

	private static final String TAG_BLOCKSTATE = "blockstate";
	private static final String TAG_SIZE = "size";

	public ItemAstrolabe(Properties props) {
		super(props);
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		ItemStack stack = ctx.getItemInHand();
		BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
		Player player = ctx.getPlayer();

		if (player != null && player.isShiftKeyDown()) {
			if (setBlock(stack, state)) {
				displayRemainderCounter(player, stack);
				return InteractionResult.SUCCESS;
			}
		} else if (player != null) {
			boolean did = placeAllBlocks(stack, player);
			if (did) {
				displayRemainderCounter(player, stack);
			}

			return did ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	@NotNull
	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, @NotNull InteractionHand hand) {
		ItemStack stack = playerIn.getItemInHand(hand);
		if (playerIn.isShiftKeyDown()) {
			playerIn.playSound(BotaniaSounds.astrolabeConfigure, 1F, 1F);
			if (!worldIn.isClientSide) {
				int size = getSize(stack);
				int newSize = size == 11 ? 3 : size + 2;
				setSize(stack, newSize);
				ItemsRemainingRenderHandler.send(playerIn, stack, 0, Component.literal(newSize + "x" + newSize));
			}

			return InteractionResultHolder.success(stack);
		}

		return InteractionResultHolder.pass(stack);
	}

	public boolean placeAllBlocks(ItemStack stack, Player player) {
		List<BlockPos> blocksToPlace = getBlocksToPlace(stack, player);
		if (!hasBlocks(stack, player, blocksToPlace)) {
			return false;
		}

		int size = getSize(stack);
		int cost = size * 320;
		if (!ManaItemHandler.instance().requestManaExact(stack, player, cost, false)) {
			return false;
		}

		ItemStack stackToPlace = new ItemStack(getBlock(stack));
		for (BlockPos coords : blocksToPlace) {
			placeBlockAndConsume(player, stack, stackToPlace, coords);
		}
		ManaItemHandler.instance().requestManaExact(stack, player, cost, true);

		return true;
	}

	private void placeBlockAndConsume(Player player, ItemStack requestor, ItemStack blockToPlace, BlockPos coords) {
		if (blockToPlace.isEmpty()) {
			return;
		}

		Block block = Block.byItem(blockToPlace.getItem());
		BlockState state = block.defaultBlockState();
		player.level.setBlockAndUpdate(coords, state);
		player.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, coords, Block.getId(state));

		if (player.getAbilities().instabuild) {
			return;
		}

		List<BlockProvider> providers = new ArrayList<>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stackInSlot = player.getInventory().getItem(i);
			if (!stackInSlot.isEmpty() && stackInSlot.is(blockToPlace.getItem())) {
				stackInSlot.shrink(1);
				return;
			}

			if (!stackInSlot.isEmpty()) {
				var provider = IXplatAbstractions.INSTANCE.findBlockProvider(stackInSlot);
				if (provider != null) {
					providers.add(provider);
				}
			}
		}

		for (BlockProvider prov : providers) {
			if (prov.provideBlock(player, requestor, block, false)) {
				prov.provideBlock(player, requestor, block, true);
				return;
			}
		}
	}

	public static boolean hasBlocks(ItemStack stack, Player player, List<BlockPos> blocks) {
		if (player.getAbilities().instabuild) {
			return true;
		}

		Block block = getBlock(stack);
		ItemStack reqStack = new ItemStack(block);

		int required = blocks.size();
		int current = 0;
		List<BlockProvider> providersToCheck = new ArrayList<>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stackInSlot = player.getInventory().getItem(i);
			if (!stackInSlot.isEmpty() && stackInSlot.is(reqStack.getItem())) {
				current += stackInSlot.getCount();
				if (current >= required) {
					return true;
				}
			}
			if (!stackInSlot.isEmpty()) {
				var provider = IXplatAbstractions.INSTANCE.findBlockProvider(stackInSlot);
				if (provider != null) {
					providersToCheck.add(provider);
				}
			}
		}

		for (BlockProvider prov : providersToCheck) {
			int count = prov.getBlockCount(player, stack, block);
			if (count == -1) {
				return true;
			}

			current += count;

			if (current >= required) {
				return true;
			}
		}

		return false;
	}

	public static List<BlockPos> getBlocksToPlace(ItemStack stack, Player player) {
		List<BlockPos> coords = new ArrayList<>();
		BlockHitResult rtr = ToolCommons.raytraceFromEntity(player, 5, true);
		if (rtr.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = rtr.getBlockPos();
			BlockState state = player.level.getBlockState(pos);
			if (state.getMaterial().isReplaceable()) {
				pos = pos.below();
			}

			int range = (getSize(stack) ^ 1) / 2;

			Direction dir = rtr.getDirection();
			Direction rotationDir = Direction.fromYRot(player.getYRot());

			boolean pitchedVertically = Math.abs(player.getXRot()) > 50;

			boolean axisX = rotationDir.getAxis() == Axis.X;
			boolean axisZ = rotationDir.getAxis() == Axis.Z;

			int xOff = axisZ || pitchedVertically ? range : 0;
			int yOff = pitchedVertically ? 0 : range;
			int zOff = axisX || pitchedVertically ? range : 0;

			for (int x = -xOff; x < xOff + 1; x++) {
				for (int y = 0; y < yOff * 2 + 1; y++) {
					for (int z = -zOff; z < zOff + 1; z++) {
						int xp = pos.getX() + x + dir.getStepX();
						int yp = pos.getY() + y + dir.getStepY();
						int zp = pos.getZ() + z + dir.getStepZ();

						BlockPos newPos = new BlockPos(xp, yp, zp);
						BlockState state1 = player.level.getBlockState(newPos);
						if (player.level.getWorldBorder().isWithinBounds(newPos)
								&& (state1.isAir() || state1.getMaterial().isReplaceable())) {
							coords.add(newPos);
						}
					}
				}
			}

		}

		return coords;
	}

	public void displayRemainderCounter(Player player, ItemStack stack) {
		Block block = getBlock(stack);
		int count = ItemExchangeRod.getInventoryItemCount(player, stack, block.asItem());
		if (!player.level.isClientSide) {
			ItemsRemainingRenderHandler.send(player, new ItemStack(block), count);
		}
	}

	private boolean setBlock(ItemStack stack, BlockState state) {
		if (!state.isAir()) {
			ItemNBTHelper.setCompound(stack, TAG_BLOCKSTATE, NbtUtils.writeBlockState(state));
			return true;
		}
		return false;
	}

	private static void setSize(ItemStack stack, int size) {
		ItemNBTHelper.setInt(stack, TAG_SIZE, size | 1);
	}

	public static int getSize(ItemStack stack) {
		return ItemNBTHelper.getInt(stack, TAG_SIZE, 3) | 1;
	}

	public static Block getBlock(ItemStack stack) {
		return getBlockState(stack).getBlock();
	}

	public static BlockState getBlockState(ItemStack stack) {
		return NbtUtils.readBlockState(ItemNBTHelper.getCompound(stack, TAG_BLOCKSTATE, false));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tip, TooltipFlag flags) {
		Block block = getBlock(stack);
		int size = getSize(stack);

		tip.add(Component.literal(size + " x " + size));
		if (block != Blocks.AIR) {
			tip.add(new ItemStack(block).getHoverName().plainCopy().withStyle(ChatFormatting.GRAY));
		}
	}

}
