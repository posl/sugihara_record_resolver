/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.gui.bag;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.NotNull;

import vazkii.botania.client.gui.SlotLocked;
import vazkii.botania.common.block.BlockModDoubleFlower;
import vazkii.botania.common.block.BlockModFlower;
import vazkii.botania.common.item.ItemFlowerBag;
import vazkii.botania.common.item.ModItems;

public class ContainerFlowerBag extends AbstractContainerMenu {
	public static ContainerFlowerBag fromNetwork(int windowId, Inventory inv, FriendlyByteBuf buf) {
		InteractionHand hand = buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		return new ContainerFlowerBag(windowId, inv, inv.player.getItemInHand(hand));
	}

	private final ItemStack bag;
	public final Container flowerBagInv;

	public ContainerFlowerBag(int windowId, Inventory playerInv, ItemStack bag) {
		super(ModItems.FLOWER_BAG_CONTAINER, windowId);

		this.bag = bag;
		if (!playerInv.player.level.isClientSide) {
			flowerBagInv = ItemFlowerBag.getInventory(bag);
		} else {
			flowerBagInv = new SimpleContainer(ItemFlowerBag.SIZE);
		}

		for (int row = 0; row < 4; ++row) {
			for (int col = 0; col < 8; ++col) {
				int slot = col + row * 8;
				addSlot(new Slot(flowerBagInv, slot, 17 + col * 18, 26 + row * 18) {
					@Override
					public boolean mayPlace(@NotNull ItemStack stack) {
						return ItemFlowerBag.isValid(this.getContainerSlot(), stack);
					}
				});
			}
		}

		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
			}
		}

		for (int i = 0; i < 9; ++i) {
			if (playerInv.getItem(i) == bag) {
				addSlot(new SlotLocked(playerInv, i, 8 + i * 18, 178));
			} else {
				addSlot(new Slot(playerInv, i, 8 + i * 18, 178));
			}
		}

	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		return !main.isEmpty() && main == bag || !off.isEmpty() && off == bag;
	}

	@NotNull
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);

		if (slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			if (slotIndex < 32) {
				if (!moveItemStackTo(itemstack1, 32, 68, true)) {
					return ItemStack.EMPTY;
				}
			} else {
				Block b = Block.byItem(itemstack.getItem());
				int slotId = -1;
				if (b instanceof BlockModDoubleFlower flower) {
					slotId = 16 + flower.color.getId();
				} else if (b instanceof BlockModFlower flower) {
					slotId = flower.color.getId();
				}
				if (slotId >= 0 && slotId < 32) {
					Slot destination = slots.get(slotId);
					if (destination.mayPlace(itemstack) && !moveItemStackTo(itemstack1, slotId, slotId + 1, true)) {
						return ItemStack.EMPTY;
					}
				}
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, itemstack1);
		}

		return itemstack;
	}

}
