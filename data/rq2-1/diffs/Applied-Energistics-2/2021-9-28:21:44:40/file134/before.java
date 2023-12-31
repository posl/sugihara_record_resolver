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

package appeng.crafting;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageChannels;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.NullMenu;
import appeng.util.Platform;

public class CraftingTreeProcess {

    private final CraftingTreeNode parent;
    final ICraftingPatternDetails details;
    private final CraftingJob job;
    private final Map<CraftingTreeNode, Long> nodes = new HashMap<>();
    private final int depth;
    boolean possible = true;
    private Level level;
    private long crafts = 0;
    private boolean containerItems;
    private boolean limitQty;
    private boolean fullSimulation;
    private long bytes = 0;

    public CraftingTreeProcess(final ICraftingService cc, final CraftingJob job, final ICraftingPatternDetails details,
            final CraftingTreeNode craftingTreeNode, final int depth) {
        this.parent = craftingTreeNode;
        this.details = details;
        this.job = job;
        this.depth = depth;
        final Level level = job.getLevel();

        if (details.isCraftable()) {
            final IAEItemStack[] list = details.getSparseInputs();

            final CraftingContainer ic = new CraftingContainer(new NullMenu(), 3, 3);
            final IAEItemStack[] is = details.getSparseInputs();
            for (int x = 0; x < ic.getContainerSize(); x++) {
                ic.setItem(x, is[x] == null ? ItemStack.EMPTY : is[x].createItemStack());
            }

            CraftingEvent.fireAutoCraftingEvent(level, details, ic);

            for (int x = 0; x < ic.getContainerSize(); x++) {
                final ItemStack g = ic.getItem(x);
                if (!g.isEmpty() && g.getCount() > 1) {
                    this.fullSimulation = true;
                }
            }

            for (final IAEItemStack part : details.getInputs()) {
                final ItemStack g = part.createItemStack();

                boolean isAnInput = false;
                for (final IAEItemStack a : details.getOutputs()) {
                    if (!g.isEmpty() && a != null && a.equals(g)) {
                        isAnInput = true;
                    }
                }

                if (isAnInput) {
                    this.limitQty = true;
                }

                if (g.getItem().hasContainerItem(g)) {
                    this.limitQty = this.containerItems = true;
                }
            }

            final boolean complicated = false;

            if (this.containerItems || complicated) {
                for (int x = 0; x < list.length; x++) {
                    final IAEItemStack part = list[x];
                    if (part != null) {
                        this.nodes.put(new CraftingTreeNode(cc, job, part.copy(), this, x, depth + 1),
                                part.getStackSize());
                    }
                }
            } else {
                // this is minor different then below, this slot uses the pattern, but kinda
                // fudges it.
                for (final IAEItemStack part : details.getInputs()) {
                    for (int x = 0; x < list.length; x++) {
                        final IAEItemStack comparePart = list[x];
                        if (part != null && part.equals(comparePart)) {
                            // use the first slot...
                            this.nodes.put(new CraftingTreeNode(cc, job, part.copy(), this, x, depth + 1),
                                    part.getStackSize());
                            break;
                        }
                    }
                }
            }
        } else {
            for (final IAEItemStack part : details.getInputs()) {
                final ItemStack g = part.createItemStack();

                boolean isAnInput = false;
                for (final IAEItemStack a : details.getOutputs()) {
                    if (!g.isEmpty() && a != null && a.equals(g)) {
                        isAnInput = true;
                    }
                }

                if (isAnInput) {
                    this.limitQty = true;
                }
            }

            for (final IAEItemStack part : details.getInputs()) {
                this.nodes.put(new CraftingTreeNode(cc, job, part.copy(), this, -1, depth + 1), part.getStackSize());
            }
        }
    }

    boolean notRecursive(final ICraftingPatternDetails details) {
        return this.parent == null || this.parent.notRecursive(details);
    }

    long getTimes(final long remaining, final long stackSize) {
        if (this.limitQty || this.fullSimulation) {
            return 1;
        }
        return remaining / stackSize + (remaining % stackSize != 0 ? 1 : 0);
    }

    void request(final MECraftingInventory inv, final long i, final IActionSource src)
            throws CraftBranchFailure, InterruptedException {
        this.job.handlePausing();

        if (this.fullSimulation) {
            final CraftingContainer ic = new CraftingContainer(new NullMenu(), 3, 3);

            for (final Entry<CraftingTreeNode, Long> entry : this.nodes.entrySet()) {
                final IAEItemStack item = entry.getKey().getStack(entry.getValue());
                final IAEItemStack stack = entry.getKey().request(inv, item.getStackSize(), src);

                ic.setItem(entry.getKey().getSlot(), stack.createItemStack());
            }

            CraftingEvent.fireAutoCraftingEvent(level, details, ic);

            for (int x = 0; x < ic.getContainerSize(); x++) {
                ItemStack is = ic.getItem(x);
                is = Platform.getContainerItem(is);

                final IAEItemStack o = StorageChannels.items()
                        .createStack(is);
                if (o != null) {
                    this.bytes++;
                    inv.injectItems(o, Actionable.MODULATE, src);
                }
            }
        } else {
            // request and remove inputs...
            for (final Entry<CraftingTreeNode, Long> entry : this.nodes.entrySet()) {
                final IAEItemStack item = entry.getKey().getStack(entry.getValue());
                final IAEItemStack stack = entry.getKey().request(inv, item.getStackSize() * i, src);

                if (this.containerItems) {
                    final ItemStack is = Platform.getContainerItem(stack.createItemStack());
                    final IAEItemStack o = StorageChannels.items()
                            .createStack(is);
                    if (o != null) {
                        this.bytes++;
                        inv.injectItems(o, Actionable.MODULATE, src);
                    }
                }
            }
        }

        // assume its possible.

        // add crafting results..
        for (final IAEItemStack out : this.details.getOutputs()) {
            final IAEItemStack o = out.copy();
            o.setStackSize(o.getStackSize() * i);
            inv.injectItems(o, Actionable.MODULATE, src);
        }

        this.crafts += i;
    }

    void dive(final CraftingJob job) {
        job.addTask(this.getAmountCrafted(this.parent.getStack(1)), this.crafts, this.details, this.depth);
        for (final CraftingTreeNode pro : this.nodes.keySet()) {
            pro.dive(job);
        }

        job.addBytes(8 + this.crafts + this.bytes);
    }

    IAEItemStack getAmountCrafted(IAEItemStack what2) {
        for (final IAEItemStack is : this.details.getOutputs()) {
            if (is.equals(what2)) {
                what2 = what2.copy();
                what2.setStackSize(is.getStackSize());
                return what2;
            }
        }

        // more fuzzy!
        for (final IAEItemStack is : this.details.getOutputs()) {
            if (is.getItem() == what2.getItem()
                    && (is.getItem().canBeDepleted() || is.getItemDamage() == what2.getItemDamage())) {
                what2 = is.copy();
                what2.setStackSize(is.getStackSize());
                return what2;
            }
        }

        throw new IllegalStateException("Crafting Tree construction failed.");
    }

    void setSimulate() {
        this.crafts = 0;
        this.bytes = 0;

        for (final CraftingTreeNode pro : this.nodes.keySet()) {
            pro.setSimulate();
        }
    }

    void setJob(final MECraftingInventory storage, final CraftingCPUCluster craftingCPUCluster, final IActionSource src)
            throws CraftBranchFailure {
        craftingCPUCluster.addCrafting(this.details, this.crafts);

        for (final CraftingTreeNode pro : this.nodes.keySet()) {
            pro.setJob(storage, craftingCPUCluster, src);
        }
    }

    void getPlan(final IItemList<IAEItemStack> plan) {
        for (IAEItemStack i : this.details.getOutputs()) {
            i = i.copy();
            i.setCountRequestable(i.getStackSize() * this.crafts);
            plan.addRequestable(i);
        }

        for (final CraftingTreeNode pro : this.nodes.keySet()) {
            pro.getPlan(plan);
        }
    }
}
