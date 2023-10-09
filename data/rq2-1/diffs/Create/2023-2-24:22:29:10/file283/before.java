package com.simibubi.create.content.contraptions.fluids.actors;

import java.util.Random;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.contraptions.processing.EmptyingByBasin;
import com.simibubi.create.content.contraptions.relays.belt.BeltHelper;
import com.simibubi.create.content.contraptions.relays.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.simibubi.create.foundation.tileEntity.renderer.SmartTileEntityRenderer;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

public class ItemDrainRenderer extends SmartTileEntityRenderer<ItemDrainTileEntity> {

	public ItemDrainRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ItemDrainTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(te, partialTicks, ms, buffer, light, overlay);
		renderFluid(te, partialTicks, ms, buffer, light);
		renderItem(te, partialTicks, ms, buffer, light, overlay);
	}

	protected void renderItem(ItemDrainTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		TransportedItemStack transported = te.heldItem;
		if (transported == null)
			return;

		TransformStack msr = TransformStack.cast(ms);
		Vec3 itemPosition = VecHelper.getCenterOf(te.getBlockPos());

		Direction insertedFrom = transported.insertedFrom;
		if (!insertedFrom.getAxis()
			.isHorizontal())
			return;

		ms.pushPose();
		ms.translate(.5f, 15 / 16f, .5f);
		msr.nudge(0);
		float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
		float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);

		Vec3 offsetVec = Vec3.atLowerCornerOf(insertedFrom.getOpposite()
			.getNormal())
			.scale(.5f - offset);
		ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
		boolean alongX = insertedFrom.getClockWise()
			.getAxis() == Axis.X;
		if (!alongX)
			sideOffset *= -1;
		ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);

		ItemStack itemStack = transported.stack;
		Random r = new Random(0);
		ItemRenderer itemRenderer = Minecraft.getInstance()
			.getItemRenderer();
		int count = (int) (Mth.log2((int) (itemStack.getCount()))) / 2;
		boolean renderUpright = BeltHelper.isItemUpright(itemStack);
		boolean blockItem = itemRenderer.getModel(itemStack, null, null, 0)
			.isGui3d();

		if (renderUpright)
			ms.translate(0, 3 / 32d, 0);

		int positive = insertedFrom.getAxisDirection()
			.getStep();
		float verticalAngle = positive * offset * 360;
		if (insertedFrom.getAxis() != Axis.X)
			msr.rotateX(verticalAngle);
		if (insertedFrom.getAxis() != Axis.Z)
			msr.rotateZ(-verticalAngle);

		if (renderUpright) {
			Entity renderViewEntity = Minecraft.getInstance().cameraEntity;
			if (renderViewEntity != null) {
				Vec3 positionVec = renderViewEntity.position();
				Vec3 vectorForOffset = itemPosition.add(offsetVec);
				Vec3 diff = vectorForOffset.subtract(positionVec);

				if (insertedFrom.getAxis() != Axis.X)
					diff = VecHelper.rotate(diff, verticalAngle, Axis.X);
				if (insertedFrom.getAxis() != Axis.Z)
					diff = VecHelper.rotate(diff, -verticalAngle, Axis.Z);

				float yRot = (float) Mth.atan2(diff.z, -diff.x);
				ms.mulPose(Vector3f.YP.rotation((float) (yRot - Math.PI / 2)));
			}
			ms.translate(0, 0, -1 / 16f);
		}

		for (int i = 0; i <= count; i++) {
			ms.pushPose();
			if (blockItem)
				ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);
			ms.scale(.5f, .5f, .5f);
			if (!blockItem && !renderUpright)
				msr.rotateX(90);
			itemRenderer.renderStatic(itemStack, TransformType.FIXED, light, overlay, ms, buffer, 0);
			ms.popPose();

			if (!renderUpright) {
				if (!blockItem)
					msr.rotateY(10);
				ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
			} else
				ms.translate(0, 0, -1 / 16f);
		}

		ms.popPose();
	}

	protected void renderFluid(ItemDrainTileEntity te, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light) {
		SmartFluidTankBehaviour tank = te.internalTank;
		if (tank == null)
			return;

		TankSegment primaryTank = tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank.getRenderedFluid();
		float level = primaryTank.getFluidLevel()
			.getValue(partialTicks);

		if (!fluidStack.isEmpty() && level != 0) {
			float yMin = 5f / 16f;
			float min = 2f / 16f;
			float max = min + (12 / 16f);
			float yOffset = (7 / 16f) * level;
			ms.pushPose();
			ms.translate(0, yOffset, 0);
			FluidRenderer.renderFluidBox(fluidStack, min, yMin - yOffset, min, max, yMin, max, buffer, ms, light,
				false);
			ms.popPose();
		}

		ItemStack heldItemStack = te.getHeldItemStack();
		if (heldItemStack.isEmpty())
			return;
		FluidStack fluidStack2 = EmptyingByBasin.emptyItem(te.getLevel(), heldItemStack, true)
			.getFirst();
		if (fluidStack2.isEmpty()) {
			if (fluidStack.isEmpty())
				return;
			fluidStack2 = fluidStack;
		}

		int processingTicks = te.processingTicks;
		float processingPT = te.processingTicks - partialTicks;
		float processingProgress = 1 - (processingPT - 5) / 10;
		processingProgress = Mth.clamp(processingProgress, 0, 1);
		float radius = 0;

		if (processingTicks != -1) {
			radius = (float) (Math.pow(((2 * processingProgress) - 1), 2) - 1);
			AABB bb = new AABB(0.5, 1.0, 0.5, 0.5, 0.25, 0.5).inflate(radius / 32f);
			FluidRenderer.renderFluidBox(fluidStack2, (float) bb.minX, (float) bb.minY, (float) bb.minZ,
				(float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, buffer, ms, light, true);
		}

	}

}
