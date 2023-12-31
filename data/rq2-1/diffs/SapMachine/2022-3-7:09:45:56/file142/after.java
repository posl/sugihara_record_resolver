/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.jfr.internal.consumer.filter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.LongMap;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.consumer.ChunkHeader;
import jdk.jfr.internal.consumer.FileAccess;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.consumer.RecordingInput;
import jdk.jfr.internal.consumer.Reference;

/**
 * Class that can filter out events and associated constants from a recording
 * file.
 * <p>
 * All positional values are relative to file start, not the chunk.
 */
public final class ChunkWriter implements Closeable {
    private LongMap<Constants> pools = new LongMap<>();
    private final Deque<CheckPointEvent> checkPoints = new ArrayDeque<>();
    private final Path destination;
    private final RecordingInput input;
    private final RecordingOutput output;
    private final Predicate<RecordedEvent> filter;

    private long chunkStartPosition;
    private boolean chunkComplete;
    private long lastCheckPoint;

    public ChunkWriter(Path source, Path destination, Predicate<RecordedEvent> filter) throws IOException {
        this.destination = destination;
        this.output = new RecordingOutput(destination.toFile());
        this.input = new RecordingInput(source.toFile(), FileAccess.UNPRIVILEGED);
        this.filter = filter;
    }

    Constants getPool(Type type) {
        long typeId = type.getId();
        Constants pool = pools.get(typeId);
        if (pool == null) {
            pool = new Constants(type);
            pools.put(typeId, pool);
        }
        return pool;
    }

    public CheckPointEvent newCheckPointEvent(long startPosition) {
        CheckPointEvent event = new CheckPointEvent(this, startPosition);
        checkPoints.add(event);
        return event;
    }

    public boolean accept(RecordedEvent event) {
        return filter.test(event);
    }

    public void touch(Object object) {
        if (object instanceof Object[] array) {
            for (int i = 0; i < array.length; i++) {
                touch(array[i]);
            }
            return;
        }
        if (object instanceof Reference ref) {
            touchRef(ref);
        }
    }

    private void touchRef(Reference ref) {
        Constants pool = pools.get(ref.type().getId());
        if (pool == null) {
            String msg = "Can't resolve " + ref.type().getName() + "[" + ref.key() + "]";
            Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.DEBUG, msg);
            return;
        }
        PoolEntry entry = pool.get(ref.key());
        if (entry != null && !entry.isTouched()) {
            entry.touch();
            touch(entry.getReferences());
        }
    }
    public void writeEvent(long startPosition, long endPosition) throws IOException {
        writeCheckpointEvents(startPosition);
        write(startPosition, endPosition);
    }

    // Write check point events before a position
    private void writeCheckpointEvents(long before) throws IOException {
        CheckPointEvent cp = checkPoints.peek();
        while (cp != null && cp.getStartPosition() < before) {
            checkPoints.poll();
            long delta = 0;
            if (lastCheckPoint != 0) {
                delta = lastCheckPoint - output.position();
            }
            lastCheckPoint = output.position();
            write(cp, delta);
            cp = checkPoints.peek();
        }
    }

    public void write(long startPosition, long endPosition) throws IOException {
        if (endPosition < startPosition) {
            throw new IOException("Start position must come before end position, start=" + startPosition + ", end=" + endPosition);
        }
        long backup = input.position();
        input.position(startPosition);
        long n = endPosition - startPosition;
        for (long i = 0; i < n; i++) {
            output.writeByte(input.readByte());
        }
        input.position(backup);
    }

    @Override
    public void close() throws IOException {
        try {
            output.close();
        } finally {
            if (!chunkComplete) {
                // Error occurred, clean up
                if (Files.exists(destination)) {
                    Files.delete(destination);
                }
            }
        }
    }

    public void beginChunk(ChunkHeader header) throws IOException {
        this.chunkComplete = false;
        this.chunkStartPosition = output.position();
        input.position(header.getAbsoluteChunkStart());
        for (int i = 0; i < ChunkHeader.HEADER_SIZE; i++) {
            output.writeByte(input.readByte());
        }
    }

    public void endChunk(ChunkHeader header) throws IOException {
        // write all outstanding checkpoints
        writeCheckpointEvents(Long.MAX_VALUE);
        long metadata = output.position();
        writeMetadataEvent(header);
        updateHeader(output.position(), lastCheckPoint, metadata);
        pools = new LongMap<>();
        chunkComplete = true;
        lastCheckPoint = 0;
    }

    private void writeMetadataEvent(ChunkHeader header) throws IOException {
        long metadataposition = header.getMetadataPosition() + header.getAbsoluteChunkStart();
        input.position(metadataposition);
        long size = input.readLong();
        input.position(metadataposition);
        for (int i = 0; i < size; i++) {
            output.writeByte(input.readByte());
        }
    }

    private void write(CheckPointEvent event, long delta) throws IOException {
        input.position(event.getStartPosition());
        long startPosition = output.position();

        input.readLong(); // Read size
        output.writePaddedUnsignedInt(0); // Size, 4 bytes reserved
        output.writeLong(input.readLong()); // Constant pool id
        output.writeLong(input.readLong()); // Start time
        output.writeLong(input.readLong()); // Duration
        input.readLong(); // Read delta
        output.writeLong(delta); // Delta
        output.writeByte(input.readByte()); // flush marker

        // Write even if touched pools are zero, checkpoint works as sync point
        output.writeLong(event.touchedPools()); // Pool count
        for (CheckPointPool pool : event.getPools()) {
            if (pool.isTouched()) {
                output.writeLong(pool.getTypeId());
                output.writeLong(pool.getTouchedCount());
                for (PoolEntry pe : pool.getEntries()) {
                    if (pe.isTouched()) {
                        write(pe.getStartPosition(), pe.getEndPosition()); // key + value
                    }
                }
            }
        }
        long endPosition = output.position();
        long size = endPosition - startPosition;
        output.position(startPosition);
        output.writePaddedUnsignedInt(size);
        output.position(endPosition);
    }

    private void updateHeader(long size, long constantPosition, long metadataPosition) throws IOException {
        long backup = output.position();
        output.position(ChunkHeader.CHUNK_SIZE_POSITION + chunkStartPosition);
        // Write chunk relative values
        output.writeRawLong(size - chunkStartPosition);
        output.writeRawLong(constantPosition - chunkStartPosition);
        output.writeRawLong(metadataPosition - chunkStartPosition);
        output.position(backup);
    }

    public RecordingInput getInput() {
        return input;
    }
}