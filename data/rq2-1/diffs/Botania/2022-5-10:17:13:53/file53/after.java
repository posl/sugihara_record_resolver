/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.api.mana;

/**
 * Any Block or Block Entity with this capability has custom collision logic for bursts.
 */
public interface IManaCollisionGhost {
	enum Behaviour {
		/**
		 * Skip all collision logic, the burst acts as if this block did not exist
		 */
		SKIP_ALL,
		/**
		 * Run all collision logic, including transferring mana, activating {@link IManaTrigger},
		 * calling Lenses, etc.
		 */
		RUN_ALL,
		/**
		 * Only transfer mana and activate {@link IManaTrigger}, do not run any other behaviour
		 * such as killing the burst and calling Lenses.
		 */
		RUN_RECEIVER_TRIGGER,
	}

	Behaviour getGhostBehaviour();

}
