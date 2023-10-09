/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;

import java.util.Map;
import java.util.UUID;

/**
 * Base interface for the Avatar Block Entity
 */
public interface IAvatarTile {

	/**
	 * Gets the avatar's inventory
	 */
	Container getInventory();

	/**
	 * Gets the avatar's facing.
	 */
	Direction getAvatarFacing();

	/**
	 * Gets the amount of ticks that have elapsed on this avatar while it's functional
	 * (has redstone signal).
	 */
	int getElapsedFunctionalTicks();

	/**
	 * Gets if this avatar is enabled (is powered by a redstone signal).
	 */
	boolean isEnabled();

	/**
	 * @return Tag of UUID -> cooldown for Rod of the Skies boosting
	 */
	Map<UUID, Integer> getBoostCooldowns();

}
