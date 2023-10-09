package com.simibubi.create.content.contraptions.components.structureMovement.bearing;

import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BearingBlock extends DirectionalKineticBlock {

	public BearingBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING).getOpposite();
	}
	
	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	public boolean showCapacityWithAnnotation() {
		return true;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		InteractionResult resultType = super.onWrenched(state, context);
		if (!context.getLevel().isClientSide && resultType.consumesAction()) {
			BlockEntity te = context.getLevel().getBlockEntity(context.getClickedPos());
			if (te instanceof MechanicalBearingTileEntity) {
				((MechanicalBearingTileEntity) te).disassemble();
			}
		}
		return resultType;
	}
}
