package vazkii.botania.forge.integration.corporea;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import vazkii.botania.api.corporea.ICorporeaRequest;
import vazkii.botania.api.corporea.ICorporeaSpark;
import vazkii.botania.common.impl.corporea.AbstractCorporeaNode;

import java.util.List;

public class ForgeCapCorporeaNode extends AbstractCorporeaNode {
	protected final IItemHandler inv;

	public ForgeCapCorporeaNode(Level world, BlockPos pos, IItemHandler inv, ICorporeaSpark spark) {
		super(world, pos, spark);
		this.inv = inv;
	}

	@Override
	public List<ItemStack> countItems(ICorporeaRequest request) {
		return iterateOverSlots(request, false);
	}

	@Override
	public List<ItemStack> extractItems(ICorporeaRequest request) {
		return iterateOverSlots(request, true);
	}

	protected List<ItemStack> iterateOverSlots(ICorporeaRequest request, boolean doit) {
		ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

		for (int i = inv.getSlots() - 1; i >= 0; i--) {
			ItemStack stackAt = inv.getStackInSlot(i);
			if (request.getMatcher().test(stackAt)) {
				request.trackFound(stackAt.getCount());

				int rem = Math.min(stackAt.getCount(), request.getStillNeeded() == -1 ? stackAt.getCount() : request.getStillNeeded());
				if (rem > 0) {
					request.trackSatisfied(rem);

					if (doit) {
						ItemStack copy = stackAt.copy();
						builder.addAll(breakDownBigStack(inv.extractItem(i, rem, getSpark().isCreative())));
						getSpark().onItemExtracted(copy);
						request.trackExtracted(rem);
					} else {
						builder.add(inv.extractItem(i, rem, true));
					}
				}
			}
		}

		return builder.build();
	}

}
