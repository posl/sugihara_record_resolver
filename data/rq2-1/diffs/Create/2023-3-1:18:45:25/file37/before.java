package com.simibubi.create.foundation.blockEntity.behaviour;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

public abstract class ValueBoxTransform {

	protected float scale = getScale();

	protected abstract Vec3 getLocalOffset(BlockState state);

	protected abstract void rotate(BlockState state, PoseStack ms);

	public boolean testHit(BlockState state, Vec3 localHit) {
		Vec3 offset = getLocalOffset(state);
		if (offset == null)
			return false;
		return localHit.distanceTo(offset) < scale / 2;
	}

	public void transform(BlockState state, PoseStack ms) {
		Vec3 position = getLocalOffset(state);
		if (position == null)
			return;
		ms.translate(position.x, position.y, position.z);
		rotate(state, ms);
		ms.scale(scale, scale, scale);
	}

	public boolean shouldRender(BlockState state) {
		return state.getMaterial() != Material.AIR && getLocalOffset(state) != null;
	}

	protected Vec3 rotateHorizontally(BlockState state, Vec3 vec) {
		float yRot = 0;
		if (state.hasProperty(BlockStateProperties.FACING))
			yRot = AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.FACING));
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			yRot = AngleHelper.horizontalAngle(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
		return VecHelper.rotateCentered(vec, yRot, Axis.Y);
	}

	protected float getScale() {
		return .4f;
	}

	protected float getFontScale() {
		return 1 / 64f;
	}

	public static abstract class Dual extends ValueBoxTransform {

		protected boolean first;

		public Dual(boolean first) {
			this.first = first;
		}

		public boolean isFirst() {
			return first;
		}

		public static Pair<ValueBoxTransform, ValueBoxTransform> makeSlots(Function<Boolean, ? extends Dual> factory) {
			return Pair.of(factory.apply(true), factory.apply(false));
		}

		public boolean testHit(BlockState state, Vec3 localHit) {
			Vec3 offset = getLocalOffset(state);
			if (offset == null)
				return false;
			return localHit.distanceTo(offset) < scale / 3.5f;
		}

	}

	public static abstract class Sided extends ValueBoxTransform {

		protected Direction direction = Direction.UP;

		public Sided fromSide(Direction direction) {
			this.direction = direction;
			return this;
		}

		@Override
		protected Vec3 getLocalOffset(BlockState state) {
			Vec3 location = getSouthLocation();
			location = VecHelper.rotateCentered(location, AngleHelper.horizontalAngle(getSide()), Axis.Y);
			location = VecHelper.rotateCentered(location, AngleHelper.verticalAngle(getSide()), Axis.X);
			return location;
		}

		protected abstract Vec3 getSouthLocation();

		@Override
		protected void rotate(BlockState state, PoseStack ms) {
			float yRot = AngleHelper.horizontalAngle(getSide()) + 180;
			float xRot = getSide() == Direction.UP ? 90 : getSide() == Direction.DOWN ? 270 : 0;
			TransformStack.cast(ms)
				.rotateY(yRot)
				.rotateX(xRot);
		}

		@Override
		public boolean shouldRender(BlockState state) {
			return super.shouldRender(state) && isSideActive(state, getSide());
		}

		@Override
		public boolean testHit(BlockState state, Vec3 localHit) {
			return isSideActive(state, getSide()) && super.testHit(state, localHit);
		}

		protected boolean isSideActive(BlockState state, Direction direction) {
			return true;
		}

		public Direction getSide() {
			return direction;
		}

	}

}
