package com.fastasyncworldedit.core.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;

public class ExtremaMask extends AngleMask {

    public ExtremaMask(Extent extent, double min, double max, boolean overlay, int distance) {
        super(extent, min, max, overlay, distance);
    }

    @Override
    protected boolean testSlope(Extent extent, int x, int y, int z) {
        double slope;
        double tmp;
        boolean aboveMin;
        lastY = y;

        int base = getHeight(extent, x, y, z);

        slope = getHeight(extent, base, x, y, z, 1, 0, distance) * ADJACENT_MOD;

        tmp = getHeight(extent, base, x, y, z, 0, 1, distance) * ADJACENT_MOD;
        if (Math.abs(tmp) > Math.abs(slope)) {
            slope = tmp;
        }

        tmp = getHeight(extent, base, x, y, z, 1, 1, distance) * DIAGONAL_MOD;
        if (Math.abs(tmp) > Math.abs(slope)) {
            slope = tmp;
        }

        tmp = getHeight(extent, base, x, y, z, 1, -1, distance) * DIAGONAL_MOD;
        if (Math.abs(tmp) > Math.abs(slope)) {
            slope = tmp;
        }

        return lastValue = (slope > min && slope < max);
    }

    private int getHeight(Extent extent, int base, int x, int y, int z, int OX, int OZ, int iterations) {
        int sign = 0;
        int lastHeight1 = base;
        int lastHeight2 = base;

        int cox = OX;
        int coz = OZ;
        for (int i = 0; i < iterations; i++, cox += OX, coz += OZ) {
            int x1 = x + cox;
            int z1 = z + coz;
            int x2 = x - cox;
            int z2 = z - coz;
            int height1 = getHeight(extent, x1, y, z1);
            int height2 = getHeight(extent, x2, y, z2);
            int diff1 = height1 - lastHeight1;
            int diff2 = height2 - lastHeight2;
            int sign1 = Integer.signum(diff1);
            int sign2 = Integer.signum(diff2);

            if (sign == 0) {
                if (sign1 != 0) {
                    sign = sign1;
                } else if (sign2 != 0) {
                    sign = sign2;
                }
            }
            if (sign1 == 0) {
                sign1 = sign;
            }
            if (sign2 == 0) {
                sign2 = sign;
            }
            if (sign1 != sign2) {
                return (lastHeight1 - base) + (lastHeight2 - base);
            }

            lastHeight1 = height1;
            lastHeight2 = height2;
        }
        return (lastHeight1 - base) + (lastHeight2 - base);
    }

    @Override
    public Mask copy() {
        return new ExtremaMask(getExtent(), min, max, overlay, distance);
    }

}
