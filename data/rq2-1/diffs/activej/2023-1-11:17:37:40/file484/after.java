/*
 * Copyright (C) 2020 ActiveJ LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.activej.memcache.protocol;

import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.ByteBufPool;
import io.activej.codegen.expression.Expression;
import io.activej.codegen.expression.Variable;
import io.activej.serializer.AbstractSerializerDef;
import io.activej.serializer.BinaryInput;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.SerializerDef;
import io.activej.serializer.impl.SerializerDef_Nullable;
import io.activej.serializer.impl.SerializerDef_WithNullable;
import io.activej.serializer.util.BinaryOutputUtils;

import static io.activej.codegen.expression.Expressions.*;
import static io.activej.serializer.CompatibilityLevel.LEVEL_3;

@SuppressWarnings("unused")
public class SerializerDef_ByteBuf extends AbstractSerializerDef implements SerializerDef_WithNullable {
	private final boolean writeWithRecycle;
	private final boolean wrap;
	private final boolean nullable;

	public SerializerDef_ByteBuf(boolean writeWithRecycle, boolean wrap) {
		this(writeWithRecycle, wrap, false);
	}

	private SerializerDef_ByteBuf(boolean writeWithRecycle, boolean wrap, boolean nullable) {
		this.writeWithRecycle = writeWithRecycle;
		this.wrap = wrap;
		this.nullable = nullable;
	}

	@Override
	public SerializerDef ensureNullable(CompatibilityLevel compatibilityLevel) {
		if (compatibilityLevel.getLevel() < LEVEL_3.getLevel()) {
			return new SerializerDef_Nullable(this);
		}
		return new SerializerDef_ByteBuf(writeWithRecycle, wrap, true);
	}

	@Override
	public Class<?> getEncodeType() {
		return ByteBuf.class;
	}

	@Override
	public Expression encoder(StaticEncoders staticEncoders, Expression buf, Variable pos, Expression value, int version, CompatibilityLevel compatibilityLevel) {
		return set(pos,
				staticCall(SerializerDef_ByteBuf.class,
						"write" + (writeWithRecycle ? "Recycle" : "") + (nullable ? "Nullable" : ""),
						buf, pos, cast(value, ByteBuf.class)));
	}

	@Override
	public Expression decoder(StaticDecoders staticDecoders, Expression in, int version, CompatibilityLevel compatibilityLevel) {
		return staticCall(SerializerDef_ByteBuf.class,
				"read" + (wrap ? "Slice" : "") + (nullable ? "Nullable" : ""),
				in);
	}

	public static int write(byte[] output, int offset, ByteBuf buf) {
		offset = BinaryOutputUtils.writeVarInt(output, offset, buf.readRemaining());
		offset = BinaryOutputUtils.write(output, offset, buf.array(), buf.head(), buf.readRemaining());
		return offset;
	}

	public static int writeNullable(byte[] output, int offset, ByteBuf buf) {
		if (buf == null) {
			output[offset] = 0;
			return offset + 1;
		} else {
			offset = BinaryOutputUtils.writeVarInt(output, offset, buf.readRemaining() + 1);
			offset = BinaryOutputUtils.write(output, offset, buf.array(), buf.head(), buf.readRemaining());
			return offset;
		}
	}

	public static int writeRecycle(byte[] output, int offset, ByteBuf buf) {
		offset = BinaryOutputUtils.writeVarInt(output, offset, buf.readRemaining());
		offset = BinaryOutputUtils.write(output, offset, buf.array(), buf.head(), buf.readRemaining());
		buf.recycle();
		return offset;
	}

	public static int writeRecycleNullable(byte[] output, int offset, ByteBuf buf) {
		if (buf == null) {
			output[offset] = 0;
			return offset + 1;
		} else {
			offset = BinaryOutputUtils.writeVarInt(output, offset, buf.readRemaining() + 1);
			offset = BinaryOutputUtils.write(output, offset, buf.array(), buf.head(), buf.readRemaining());
			buf.recycle();
			return offset;
		}
	}

	public static ByteBuf read(BinaryInput in) {
		int length = in.readVarInt();
		ByteBuf byteBuf = ByteBufPool.allocate(length);
		in.read(byteBuf.array(), 0, length);
		byteBuf.tail(length);
		return byteBuf;
	}

	public static ByteBuf readNullable(BinaryInput in) {
		int length = in.readVarInt();
		if (length == 0) return null;
		length--;
		ByteBuf byteBuf = ByteBufPool.allocate(length);
		in.read(byteBuf.array(), 0, length);
		byteBuf.tail(length);
		return byteBuf;
	}

	public static ByteBuf readSlice(BinaryInput in) {
		int length = in.readVarInt();
		ByteBuf result = ByteBuf.wrap(in.array(), in.pos(), in.pos() + length);
		in.pos(in.pos() + length);
		return result;
	}

	public static ByteBuf readSliceNullable(BinaryInput in) {
		int length = in.readVarInt();
		if (length == 0) return null;
		length--;
		ByteBuf result = ByteBuf.wrap(in.array(), in.pos(), in.pos() + length);
		in.pos(in.pos() + length);
		return result;
	}
}
