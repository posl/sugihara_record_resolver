/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.common.block.tile;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TilePlatform extends TileMod implements RenderAttachmentBlockEntity {
	private static final String TAG_CAMO = "camo";

	@Nullable
	private BlockState camoState;

	public TilePlatform(BlockPos pos, BlockState state) {
		super(ModTiles.PLATFORM, pos, state);
	}

	public boolean onWanded(Player player) {
		if (player != null) {
			if (getCamoState() == null || player.isShiftKeyDown()) {
				swapSelfAndPass(this, true);
			} else {
				swapSurroudings(this, false);
			}
			return true;
		}

		return false;
	}

	@Nullable
	public BlockState getCamoState() {
		return camoState;
	}

	public void setCamoState(@Nullable BlockState state) {
		this.camoState = state;

		if (level != null) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
			if (!level.isClientSide) {
				level.blockUpdated(worldPosition, getBlockState().getBlock());
			}
		}
	}

	private void swapSelfAndPass(TilePlatform tile, boolean empty) {
		swap(tile, empty);
		swapSurroudings(tile, empty);
	}

	private void swapSurroudings(TilePlatform tile, boolean empty) {
		for (Direction dir : Direction.values()) {
			BlockPos pos = tile.getBlockPos().relative(dir);
			BlockEntity tileAt = level.getBlockEntity(pos);
			if (tileAt instanceof TilePlatform) {
				TilePlatform platform = (TilePlatform) tileAt;
				if (empty == (platform.getCamoState() != null)) {
					swapSelfAndPass(platform, empty);
				}
			}
		}
	}

	private void swap(TilePlatform tile, boolean empty) {
		tile.setCamoState(empty ? null : getCamoState());
	}

	@Override
	public void writePacketNBT(CompoundTag cmp) {
		if (getCamoState() != null) {
			cmp.put(TAG_CAMO, NbtUtils.writeBlockState(getCamoState()));
		}
	}

	@Override
	public void readPacketNBT(CompoundTag cmp) {
		BlockState state = NbtUtils.readBlockState(cmp.getCompound(TAG_CAMO));
		if (state.isAir()) {
			state = null;
		}
		setCamoState(state);
	}

	@Override
	public void fromClientTag(CompoundTag tag) {
		super.fromClientTag(tag);
		if (level instanceof ClientLevel) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 0);
		}
	}

	@Override
	public Object getRenderAttachmentData() {
		return new PlatformData(getBlockPos().immutable(), camoState);
	}

	public record PlatformData(BlockPos pos, @Nullable BlockState state) {
	}
}
