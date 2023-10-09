/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.client.gui.box.ContainerBaubleBox;
import vazkii.botania.common.handler.EquipmentHandler;
import vazkii.botania.common.helper.InventoryHelper;
import vazkii.botania.xplat.IXplatAbstractions;

import java.util.stream.IntStream;

public class ItemBaubleBox extends Item {
	public static final int SIZE = 24;

	public ItemBaubleBox(Properties props) {
		super(props);
	}

	public static SimpleContainer getInventory(ItemStack stack) {
		return new ItemBackedInventory(stack, SIZE) {
			@Override
			public boolean canPlaceItem(int index, @NotNull ItemStack stack) {
				return EquipmentHandler.instance.isAccessory(stack);
			}
		};
	}

	@NotNull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
		if (!world.isClientSide) {
			ItemStack stack = player.getItemInHand(hand);
			IXplatAbstractions.INSTANCE.openMenu((ServerPlayer) player, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return stack.getHoverName();
				}

				@Override
				public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
					return new ContainerBaubleBox(syncId, inv, stack);
				}
			}, buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
		}
		return InteractionResultHolder.success(player.getItemInHand(hand));
	}

	@Override
	public void onDestroyed(@NotNull ItemEntity entity) {
		var container = getInventory(entity.getItem());
		var stream = IntStream.range(0, container.getContainerSize())
				.mapToObj(container::getItem)
				.filter(s -> !s.isEmpty());
		ItemUtils.onContainerDestroyed(entity, stream);
		container.clearContent();
	}

	@Override
	public boolean overrideStackedOnOther(
			@NotNull ItemStack box, @NotNull Slot slot,
			@NotNull ClickAction clickAction, @NotNull Player player) {
		return InventoryHelper.overrideStackedOnOther(
				ItemBaubleBox::getInventory,
				player.containerMenu instanceof ContainerBaubleBox,
				box, slot, clickAction, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(
			@NotNull ItemStack box, @NotNull ItemStack toInsert,
			@NotNull Slot slot, @NotNull ClickAction clickAction,
			@NotNull Player player, @NotNull SlotAccess cursorAccess) {
		return InventoryHelper.overrideOtherStackedOnMe(
				ItemBaubleBox::getInventory,
				player.containerMenu instanceof ContainerBaubleBox,
				box, toInsert, clickAction, cursorAccess);
	}
}
