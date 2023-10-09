/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.block_entity.corporea;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import vazkii.botania.api.corporea.CorporeaHelper;
import vazkii.botania.api.corporea.CorporeaSpark;
import vazkii.botania.common.block.block_entity.BotaniaBlockEntity;

public abstract class BaseCorporeaBlockEntity extends BotaniaBlockEntity {

	protected BaseCorporeaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public CorporeaSpark getSpark() {
		return CorporeaHelper.instance().getSparkForBlock(level, getBlockPos());
	}

}
