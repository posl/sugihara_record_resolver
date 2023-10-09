/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.menu.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEMonitorStorage;
import appeng.helpers.IMenuCraftingPacket;

/**
 * The crafting result slot in a pattern terminal.
 */
public class PatternTermSlot extends CraftingTermSlot {

    private final int groupNum;
    private final IOptionalSlotHost host;

    public PatternTermSlot(Player player, IActionSource mySrc, IEnergySource energySrc,
            MEMonitorStorage storage, InternalInventory cMatrix, InternalInventory secondMatrix,
            IOptionalSlotHost h, int groupNumber,
            IMenuCraftingPacket c) {
        super(player, mySrc, energySrc, storage, cMatrix, secondMatrix, c);

        this.host = h;
        this.groupNum = groupNumber;
    }

    @Override
    public ItemStack getItem() {
        if (!this.isSlotEnabled() && !this.getDisplayStack().isEmpty()) {
            this.clearStack();
        }

        return super.getItem();
    }

    @Override
    public boolean isSlotEnabled() {
        if (this.host == null) {
            return false;
        }

        return this.host.isSlotEnabled(this.groupNum);
    }
}