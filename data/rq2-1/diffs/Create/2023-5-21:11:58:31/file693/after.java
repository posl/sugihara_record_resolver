package com.simibubi.create.content.kinetics.mechanicalArm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ArmItem extends BlockItem {

	public ArmItem(Block p_i48527_1_, Properties p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		if (ArmInteractionPoint.isInteractable(world, pos, world.getBlockState(pos)))
			return InteractionResult.SUCCESS;
		return super.useOn(ctx);
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, Player p_195943_3_, ItemStack p_195943_4_,
		BlockState p_195943_5_) {
		if (world.isClientSide)
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArmInteractionPointHandler.flushSettings(pos));
		return super.updateCustomBlockEntityTag(pos, world, p_195943_3_, p_195943_4_, p_195943_5_);
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level world, BlockPos pos,
		Player p_195938_4_) {
		return !ArmInteractionPoint.isInteractable(world, pos, state);
	}

}
