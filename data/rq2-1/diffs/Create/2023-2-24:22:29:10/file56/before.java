package com.simibubi.create.content.contraptions.base.flwdata;

import com.jozufozu.flywheel.core.materials.BasicData;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.BlockPos;

public class KineticData extends BasicData {
    float x;
    float y;
    float z;
    float rotationalSpeed;
    float rotationOffset;

    public KineticData setPosition(BlockPos pos) {
        return setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public KineticData setPosition(Vector3f pos) {
        return setPosition(pos.x(), pos.y(), pos.z());
    }

    public KineticData setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        markDirty();
        return this;
    }

    public KineticData nudge(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        markDirty();
        return this;
    }

    public KineticData setColor(KineticTileEntity te) {
        if (te.hasNetwork()) {
            setColor(Color.generateFromLong(te.network));
        }else {
            setColor(0xFF, 0xFF, 0xFF);
        }
        return this;
    }

    public KineticData setColor(Color c) {
    	setColor(c.getRed(), c.getGreen(), c.getBlue());
    	return this;
    }

    public KineticData setRotationalSpeed(float rotationalSpeed) {
		this.rotationalSpeed = rotationalSpeed;
		return this;
	}

	public KineticData setRotationOffset(float rotationOffset) {
		this.rotationOffset = rotationOffset;
		return this;
	}

}
