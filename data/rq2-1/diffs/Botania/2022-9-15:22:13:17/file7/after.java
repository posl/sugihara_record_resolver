package vazkii.botania.forge.integration.corporea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import vazkii.botania.api.corporea.CorporeaNode;
import vazkii.botania.api.corporea.CorporeaNodeDetector;
import vazkii.botania.api.corporea.CorporeaSpark;

public class ForgeCapCorporeaNodeDetector implements CorporeaNodeDetector {
	@Nullable
	@Override
	public CorporeaNode getNode(Level world, CorporeaSpark spark) {
		IItemHandler inv = getInventory(world, spark.getAttachPos());
		if (inv != null) {
			return new ForgeCapCorporeaNode(world, spark.getAttachPos(), inv, spark);
		}
		return null;
	}

	@Nullable
	private static IItemHandler getInventory(Level level, BlockPos pos) {
		var be = level.getBlockEntity(pos);

		if (be == null) {
			return null;
		}

		LazyOptional<IItemHandler> ret = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP);
		if (!ret.isPresent()) {
			ret = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
		}
		return ret.orElse(null);
	}
}
