/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public interface AccessorMob {
	@Nullable
	@Invoker("getAmbientSound")
	SoundEvent botania_getAmbientSound();

	@Accessor
	void setLootTable(ResourceLocation id);

	@Accessor
	GoalSelector getGoalSelector();

	@Accessor
	GoalSelector getTargetSelector();
}
