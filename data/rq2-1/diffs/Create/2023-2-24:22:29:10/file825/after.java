package com.simibubi.create.foundation.gui.menu;

import java.util.function.Supplier;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class GhostItemSubmitPacket extends SimplePacketBase {

	private final ItemStack item;
	private final int slot;

	public GhostItemSubmitPacket(ItemStack item, int slot) {
		this.item = item;
		this.slot = slot;
	}

	public GhostItemSubmitPacket(FriendlyByteBuf buffer) {
		item = buffer.readItem();
		slot = buffer.readInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeItem(item);
		buffer.writeInt(slot);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get()
				.enqueueWork(() -> {
					ServerPlayer player = context.get()
							.getSender();
					if (player == null)
						return;

					if (player.containerMenu instanceof GhostItemMenu<?> menu) {
						menu.ghostInventory.setStackInSlot(slot, item);
						menu.getSlot(36 + slot).setChanged();
					}

				});
		context.get()
				.setPacketHandled(true);
	}

}
