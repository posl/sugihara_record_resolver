/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.api.corporea;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The unit of interaction for the Corporea network
 * All Corporea Sparks are attached to one of these
 * Note that not all implementations of this are actual inventories (e.g. interceptors)
 */
public interface CorporeaNode {

	Level getWorld();

	BlockPos getPos();

	/**
	 * Counts items in the node matching the request
	 *
	 * @param request specifies what should be found
	 * @return List of ItemStack. Individual stacks may be over-sized (exceed the item's maxStackSize) for
	 *         purposes of counting huge amounts. The list should not be modified.
	 */
	List<ItemStack> countItems(CorporeaRequest request);

	/**
	 * Convenience method for accessing the spark over this node
	 */
	CorporeaSpark getSpark();

	/**
	 * Extracts items matching request from the node.<br/>
	 * {@link CorporeaRequest#getStillNeeded()} is updated to reflect how many items are
	 * yet to be extracted.<br/>
	 * {@link CorporeaRequest#getFound()} and
	 * {@link CorporeaRequest#getExtracted()} are updated to reflect how many
	 * items were found and extracted.
	 *
	 * @return List of ItemStacks to be delivered to the destination. The list should not be modified.
	 */
	List<ItemStack> extractItems(CorporeaRequest request);
}
