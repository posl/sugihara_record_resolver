/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.regions;

import com.fastasyncworldedit.core.math.BlockVectorSet;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.iterator.RegionIterator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

//FAWE start - extends AbstractSet<BlockVector3>
public abstract class AbstractRegion extends AbstractSet<BlockVector3> implements Region {
//FAWE end

    @Nullable
    protected World world;

    public AbstractRegion(@Nullable World world) {
        this.world = world;
    }

    //FAWE start
    @Override
    public int size() {
        return com.google.common.primitives.Ints.saturatedCast(getVolume());
    }
    //FAWE end

    @Override
    public Vector3 getCenter() {
        return getMinimumPoint().add(getMaximumPoint()).toVector3().divide(2);
    }

    /**
     * Get the iterator.
     *
     * @return iterator of points inside the region
     */
    @Override
    public Iterator<BlockVector3> iterator() {
        return new RegionIterator(this);
    }

    @Override
    public @Nullable
    World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public void shift(BlockVector3 change) throws RegionOperationException {
        expand(change);
        contract(change);
    }

    @Override
    public AbstractRegion clone() {
        try {
            return (AbstractRegion) super.clone();
        } catch (CloneNotSupportedException exc) {
            return null;
        }
    }

    @Override
    public List<BlockVector2> polygonize(int maxPoints) {
        if (maxPoints >= 0 && maxPoints < 4) {
            throw new IllegalArgumentException(
                    "Cannot polygonize an AbstractRegion with no overridden polygonize method into less than 4 points.");
        }

        final BlockVector3 min = getMinimumPoint();
        final BlockVector3 max = getMaximumPoint();

        final List<BlockVector2> points = new ArrayList<>(4);

        points.add(BlockVector2.at(min.getX(), min.getZ()));
        points.add(BlockVector2.at(min.getX(), max.getZ()));
        points.add(BlockVector2.at(max.getX(), max.getZ()));
        points.add(BlockVector2.at(max.getX(), min.getZ()));

        return points;
    }

    @Override
    public long getVolume() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return (max.getX() - min.getX() + 1L)
                * (max.getY() - min.getY() + 1L)
                * (max.getZ() - min.getZ() + 1L);
    }

    /**
     * Get X-size.
     *
     * @return width
     */
    @Override
    public int getWidth() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return max.getX() - min.getX() + 1;
    }

    /**
     * Get Y-size.
     *
     * @return height
     */
    @Override
    public int getHeight() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return max.getY() - min.getY() + 1;
    }

    /**
     * Get Z-size.
     *
     * @return length
     */
    @Override
    public int getLength() {
        BlockVector3 min = getMinimumPoint();
        BlockVector3 max = getMaximumPoint();

        return max.getZ() - min.getZ() + 1;
    }

    /**
     * Get a list of chunks.
     *
     * @return a set of chunks
     */
    @Override
    public Set<BlockVector2> getChunks() {
        final Set<BlockVector2> chunks = new HashSet<>();

        final BlockVector3 minBlock = getMinimumPoint();
        final BlockVector3 maxBlock = getMaximumPoint();

        //FAWE start
        final BlockVector2 min = BlockVector2.at(minBlock.getX() >> 4, minBlock.getZ() >> 4);
        final BlockVector2 max = BlockVector2.at(maxBlock.getX() >> 4, maxBlock.getZ() >> 4);
        //FAWE end

        for (int X = min.getBlockX(); X <= max.getBlockX(); ++X) {
            for (int Z = min.getBlockZ(); Z <= max.getBlockZ(); ++Z) {
                if (containsChunk(X, Z)) {
                    chunks.add(BlockVector2.at(X, Z));
                }
            }
        }

        return chunks;
    }

    @Override
    public Set<BlockVector3> getChunkCubes() {
        final Set<BlockVector3> chunks = new BlockVectorSet();

        final BlockVector3 min = getMinimumPoint();
        final BlockVector3 max = getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); ++y) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                    if (!contains(BlockVector3.at(x, y, z))) {
                        continue;
                    }

                    chunks.add(BlockVector3.at(
                            x >> ChunkStore.CHUNK_SHIFTS,
                            y >> ChunkStore.CHUNK_SHIFTS,
                            z >> ChunkStore.CHUNK_SHIFTS
                    ));
                }
            }
        }

        return chunks;
    }

    // Sub-class utilities

    protected final int getWorldMinY() {
        //FAWE start > Integer.MIN_VALUE -> 0 (to avoid crazy for loops...) TODO: See if there's a way to find a "server default"
        return world == null ? 0 : world.getMinY();
        //FAWE end
    }

    protected final int getWorldMaxY() {
        //FAWE start > Integer.MAX_VALUE -> 255 (to avoid crazy for loops...) TODO: See if there's a way to find a "server default"
        return world == null ? 255 : world.getMaxY();
        //FAWE end
    }

    //FAWE start
    @Override
    public int hashCode() {
        int worldHash = this.world == null ? 7 : this.world.hashCode();
        int result = worldHash ^ (worldHash >>> 32);
        result = 31 * result + this.getMinimumPoint().hashCode();
        result = 31 * result + this.getMaximumPoint().hashCode();
        result = (int) (31 * result + this.getVolume());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Region)) {
            return false;
        }
        Region region = ((Region) o);

        return Objects.equals(this.getWorld(), region.getWorld())
                && this.getMinimumPoint().equals(region.getMinimumPoint())
                && this.getMaximumPoint().equals(region.getMaximumPoint())
                && this.getVolume() == region.getVolume();
    }
    //FAWE end

}
