package com.simibubi.create.content.contraptions.components.structureMovement.elevator;

import java.lang.ref.WeakReference;
import java.util.Collection;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.components.actors.controls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.components.actors.controls.ContraptionControlsMovement.ElevatorFloorSelection;
import com.simibubi.create.content.contraptions.components.actors.controls.ContraptionControlsTileEntity;
import com.simibubi.create.content.contraptions.components.actors.controls.ContraptionControlsTileEntity.ControlsSlot;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandler;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionHandlerClient;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.Couple;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ElevatorControlsHandler {

	private static ControlsSlot slot = new ContraptionControlsTileEntity.ControlsSlot();

	@OnlyIn(Dist.CLIENT)
	public static boolean onScroll(double delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null)
			return false;
		if (player.isSpectator())
			return false;
		if (mc.level == null)
			return false;

		Couple<Vec3> rayInputs = ContraptionHandlerClient.getRayInputs(player);
		Vec3 origin = rayInputs.getFirst();
		Vec3 target = rayInputs.getSecond();
		AABB aabb = new AABB(origin, target).inflate(16);

		Collection<WeakReference<AbstractContraptionEntity>> contraptions =
			ContraptionHandler.loadedContraptions.get(mc.level)
				.values();

		for (WeakReference<AbstractContraptionEntity> ref : contraptions) {
			AbstractContraptionEntity contraptionEntity = ref.get();
			if (contraptionEntity == null)
				continue;

			Contraption contraption = contraptionEntity.getContraption();
			if (!(contraption instanceof ElevatorContraption ec))
				continue;

			if (!contraptionEntity.getBoundingBox()
				.intersects(aabb))
				continue;

			BlockHitResult rayTraceResult =
				ContraptionHandlerClient.rayTraceContraption(origin, target, contraptionEntity);
			if (rayTraceResult == null)
				continue;

			BlockPos pos = rayTraceResult.getBlockPos();
			StructureBlockInfo info = contraption.getBlocks()
				.get(pos);

			if (info == null)
				continue;
			if (!AllBlocks.CONTRAPTION_CONTROLS.has(info.state))
				continue;

			if (!slot.testHit(info.state, rayTraceResult.getLocation()
				.subtract(Vec3.atLowerCornerOf(pos))))
				continue;

			MovementContext ctx = null;
			for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
				if (info.equals(pair.left)) {
					ctx = pair.right;
					break;
				}
			}

			if (!(ctx.temporaryData instanceof ElevatorFloorSelection))
				ctx.temporaryData = new ElevatorFloorSelection();

			ElevatorFloorSelection efs = (ElevatorFloorSelection) ctx.temporaryData;
			int prev = efs.currentIndex;
			efs.currentIndex += delta;
			ContraptionControlsMovement.tickFloorSelection(efs, ec);

			if (prev != efs.currentIndex && !ec.namesList.isEmpty()) {
				float pitch = (efs.currentIndex) / (float) (ec.namesList.size());
				pitch = Mth.lerp(pitch, 1f, 1.5f);
				AllSoundEvents.SCROLL_VALUE.play(mc.player.level, mc.player,
					new BlockPos(contraptionEntity.toGlobalVector(rayTraceResult.getLocation(), 1)), 1, pitch);
			}

			return true;
		}

		return false;
	}

}
