package com.simibubi.create.content.logistics.trains.management.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.DynamicComponent;
import com.simibubi.create.foundation.utility.NBTHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlapDisplayTileEntity extends KineticTileEntity {

	public List<FlapDisplayLayout> lines;
	public boolean isController;
	public boolean isRunning;
	public int xSize, ySize;
	public DyeColor[] colour;
	public boolean[] manualLines;

	public FlapDisplayTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
		isController = false;
		xSize = 1;
		ySize = 1;
		colour = new DyeColor[2];
		manualLines = new boolean[2];
	}

	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		updateControllerStatus();
	}

	public void updateControllerStatus() {
		if (level.isClientSide)
			return;

		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof FlapDisplayBlock))
			return;

		Direction leftDirection = blockState.getValue(FlapDisplayBlock.HORIZONTAL_FACING)
			.getClockWise();
		boolean shouldBeController = !blockState.getValue(FlapDisplayBlock.UP)
			&& level.getBlockState(worldPosition.relative(leftDirection)) != blockState;

		int newXSize = 1;
		int newYSize = 1;

		if (shouldBeController) {
			for (int xOffset = 1; xOffset < 32; xOffset++) {
				if (level.getBlockState(worldPosition.relative(leftDirection.getOpposite(), xOffset)) != blockState)
					break;
				newXSize++;
			}
			for (int yOffset = 0; yOffset < 32; yOffset++) {
				if (!level.getBlockState(worldPosition.relative(Direction.DOWN, yOffset))
					.getOptionalValue(FlapDisplayBlock.DOWN)
					.orElse(false))
					break;
				newYSize++;
			}
		}

		if (isController == shouldBeController && newXSize == xSize && newYSize == ySize)
			return;

		isController = shouldBeController;
		xSize = newXSize;
		ySize = newYSize;
		colour = Arrays.copyOf(colour, ySize * 2);
		manualLines = new boolean[ySize * 2];
		lines = null;
		sendData();
	}

	@Override
	public void tick() {
		super.tick();
		isRunning = super.isSpeedRequirementFulfilled();
		if (!level.isClientSide || !isRunning)
			return;
		List<FlapDisplayLayout> lines = getLines();
		lines.forEach(l -> l.getSections()
			.forEach(FlapDisplaySection::tick));
	}

	@Override
	public boolean isSpeedRequirementFulfilled() {
		return isRunning;
	}

	public void applyTextManually(int lineIndex, String rawComponentText) {
		FlapDisplayLayout layout = getLines().get(lineIndex);
		if (!layout.isLayout("Default"))
			layout.loadDefault(getMaxCharCount());
		List<FlapDisplaySection> sections = layout.getSections();

		if (rawComponentText == null) {
			manualLines[lineIndex] = false;
			sections.get(0)
				.setText(new TextComponent(""));
			notifyUpdate();
			return;
		}

		JsonElement json = DynamicComponent.getJsonFromString(rawComponentText);
		if (json == null)
			return;

		manualLines[lineIndex] = true;
		Component text = DynamicComponent.parseCustomText(level, worldPosition, json);
		sections.get(0)
			.setText(text);
		notifyUpdate();
	}

	public void setColour(int lineIndex, DyeColor color) {
		colour[lineIndex] = color == DyeColor.WHITE ? null : color;
		notifyUpdate();
	}

	public List<FlapDisplayLayout> getLines() {
		if (lines == null)
			initDefaultSections();
		return lines;
	}

	public void initDefaultSections() {
		lines = new ArrayList<>();
		for (int i = 0; i < ySize * 2; i++)
			lines.add(new FlapDisplayLayout(getMaxCharCount()));
	}
	
	public int getMaxCharCount() {
		return getMaxCharCount(0);
	}

	public int getMaxCharCount(int gaps) {
		return (int) ((xSize * 16f - 2f - 4f * gaps) / 3.5f);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);

		tag.putBoolean("Controller", isController);
		tag.putInt("XSize", xSize);
		tag.putInt("YSize", ySize);

		for (int j = 0; j < manualLines.length; j++)
			if (manualLines[j])
				NBTHelper.putMarker(tag, "CustomLine" + j);

		for (int j = 0; j < colour.length; j++)
			if (colour[j] != null)
				NBTHelper.writeEnum(tag, "Dye" + j, colour[j]);

		List<FlapDisplayLayout> lines = getLines();
		for (int i = 0; i < lines.size(); i++)
			tag.put("Display" + i, lines.get(i)
				.write());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		boolean wasActive = isController;
		int prevX = xSize;
		int prevY = ySize;

		isController = tag.getBoolean("Controller");
		xSize = tag.getInt("XSize");
		ySize = tag.getInt("YSize");

		manualLines = new boolean[ySize * 2];
		for (int i = 0; i < ySize * 2; i++)
			manualLines[i] = tag.contains("CustomLine" + i);

		colour = new DyeColor[ySize * 2];
		for (int i = 0; i < ySize * 2; i++)
			colour[i] = tag.contains("Dye" + i) ? NBTHelper.readEnum(tag, "Dye" + i, DyeColor.class) : null;

		if (clientPacket && wasActive != isController || prevX != xSize || prevY != ySize) {
			invalidateRenderBoundingBox();
			lines = null;
		}

		List<FlapDisplayLayout> lines = getLines();
		for (int i = 0; i < lines.size(); i++)
			lines.get(i)
				.read(tag.getCompound("Display" + i));
	}

	public int getLineIndexAt(double yCoord) {
		return (int) Mth.clamp(Math.floor(2 * (worldPosition.getY() - yCoord + 1)), 0, ySize * 2);
	}

	public FlapDisplayTileEntity getController() {
		if (isController)
			return this;

		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof FlapDisplayBlock))
			return null;

		MutableBlockPos pos = getBlockPos().mutable();
		Direction side = blockState.getValue(FlapDisplayBlock.HORIZONTAL_FACING)
			.getClockWise();

		for (int i = 0; i < 64; i++) {
			BlockState other = level.getBlockState(pos);

			if (other.getOptionalValue(FlapDisplayBlock.UP)
				.orElse(false)) {
				pos.move(Direction.UP);
				continue;
			}

			if (!level.getBlockState(pos.relative(side))
				.getOptionalValue(FlapDisplayBlock.UP)
				.orElse(true)) {
				pos.move(side);
				continue;
			}

			BlockEntity found = level.getBlockEntity(pos);
			if (found instanceof FlapDisplayTileEntity flap && flap.isController)
				return flap;

			break;
		}

		return null;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		AABB aabb = new AABB(worldPosition);
		if (!isController)
			return aabb;
		Vec3i normal = getDirection().getClockWise()
			.getNormal();
		return aabb.expandTowards(normal.getX() * xSize, -ySize, normal.getZ() * xSize);
	}

	public Direction getDirection() {
		return getBlockState().getOptionalValue(FlapDisplayBlock.HORIZONTAL_FACING)
			.orElse(Direction.SOUTH)
			.getOpposite();
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {}

}
