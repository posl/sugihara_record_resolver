package com.simibubi.create.content.contraptions.relays.encased;

import java.util.Optional;

import com.jozufozu.flywheel.api.InstanceData;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.content.contraptions.base.KineticBlockEntity;
import com.simibubi.create.content.contraptions.base.KineticBlockEntityInstance;
import com.simibubi.create.content.contraptions.base.flwdata.RotatingData;
import com.simibubi.create.foundation.utility.Iterate;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class EncasedCogInstance extends KineticBlockEntityInstance<KineticBlockEntity> {

	private boolean large;

	protected RotatingData rotatingModel;
	protected Optional<RotatingData> rotatingTopShaft;
	protected Optional<RotatingData> rotatingBottomShaft;

	public static EncasedCogInstance small(MaterialManager modelManager, KineticBlockEntity blockEntity) {
		return new EncasedCogInstance(modelManager, blockEntity, false);
	}

	public static EncasedCogInstance large(MaterialManager modelManager, KineticBlockEntity blockEntity) {
		return new EncasedCogInstance(modelManager, blockEntity, true);
	}

	public EncasedCogInstance(MaterialManager modelManager, KineticBlockEntity blockEntity, boolean large) {
		super(modelManager, blockEntity);
		this.large = large;
	}

	@Override
	public void init() {
		rotatingModel = setup(getCogModel().createInstance());

		Block block = blockState.getBlock();
		if (!(block instanceof IRotate))
			return;

		IRotate def = (IRotate) block;
		rotatingTopShaft = Optional.empty();
		rotatingBottomShaft = Optional.empty();

		for (Direction d : Iterate.directionsInAxis(axis)) {
			if (!def.hasShaftTowards(blockEntity.getLevel(), blockEntity.getBlockPos(), blockState, d))
				continue;
			RotatingData data = setup(getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, blockState, d)
				.createInstance());
			if (d.getAxisDirection() == AxisDirection.POSITIVE)
				rotatingTopShaft = Optional.of(data);
			else
				rotatingBottomShaft = Optional.of(data);
		}
	}

	@Override
	public void update() {
		updateRotation(rotatingModel);
		rotatingTopShaft.ifPresent(this::updateRotation);
		rotatingBottomShaft.ifPresent(this::updateRotation);
	}

	@Override
	public void updateLight() {
		relight(pos, rotatingModel);
		rotatingTopShaft.ifPresent(d -> relight(pos, d));
		rotatingBottomShaft.ifPresent(d -> relight(pos, d));
	}

	@Override
	public void remove() {
		rotatingModel.delete();
		rotatingTopShaft.ifPresent(InstanceData::delete);
		rotatingBottomShaft.ifPresent(InstanceData::delete);
	}

	protected Instancer<RotatingData> getCogModel() {
		BlockState referenceState = blockEntity.getBlockState();
		Direction facing =
			Direction.fromAxisAndDirection(referenceState.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
		PartialModel partial = large ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL : AllPartialModels.SHAFTLESS_COGWHEEL;

		return getRotatingMaterial().getModel(partial, referenceState, facing, () -> {
			PoseStack poseStack = new PoseStack();
			TransformStack.cast(poseStack)
				.centre()
				.rotateToFace(facing)
				.multiply(Vector3f.XN.rotationDegrees(90))
				.unCentre();
			return poseStack;
		});
	}

}
