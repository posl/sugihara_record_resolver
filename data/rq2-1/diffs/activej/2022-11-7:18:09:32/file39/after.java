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

package io.activej.serializer.impl;

import io.activej.codegen.expression.Expression;
import io.activej.codegen.expression.Variable;
import io.activej.serializer.AbstractSerializerDef;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.SerializerDef;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.activej.codegen.expression.Expressions.*;
import static io.activej.serializer.impl.SerializerExpressions.*;

public final class SerializerDefReference extends AbstractSerializerDef implements SerializerDef {
	@SuppressWarnings("unused")
	public static final ThreadLocal<IdentityHashMap<Object, Integer>> MAP_ENCODE = ThreadLocal.withInitial(IdentityHashMap::new);
	@SuppressWarnings("unused")
	public static final ThreadLocal<HashMap<Integer, Object>> MAP_DECODE = ThreadLocal.withInitial(HashMap::new);

	private final SerializerDef serializer;

	public SerializerDefReference(@NotNull SerializerDef serializer) {
		this.serializer = serializer;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(serializer);
	}

	@Override
	public boolean isInline(int version, CompatibilityLevel compatibilityLevel) {
		return false;
	}

	@Override
	public Class<?> getEncodeType() {
		return serializer.getEncodeType();
	}

	@Override
	public Map<Object, Expression> getEncoderInitializer() {
		return clearMap(encodeMap());
	}

	@Override
	public Map<Object, Expression> getEncoderFinalizer() {
		return clearMap(encodeMap());
	}

	@Override
	public Map<Object, Expression> getDecoderInitializer() {
		return clearMap(decodeMap());
	}

	@Override
	public Map<Object, Expression> getDecoderFinalizer() {
		return clearMap(decodeMap());
	}

	@Override
	public Expression encoder(StaticEncoders staticEncoders, Expression buf, Variable pos, Expression value, int version, CompatibilityLevel compatibilityLevel) {
		return let(encodeMap(),
				map -> let(call(map, "get", value),
						index -> ifNull(index,
								sequence(
										call(map, "put", value, cast(add(call(map, "size"), value(1)), Integer.class)),
										writeByte(buf, pos, value((byte) 0)),
										serializer.defineEncoder(staticEncoders, buf, pos, value, version, compatibilityLevel)
								),
								writeVarInt(buf, pos, cast(index, Integer.class)))));
	}

	@Override
	public Expression decoder(StaticDecoders staticDecoders, Expression in, int version, CompatibilityLevel compatibilityLevel) {
		return let(decodeMap(),
				map -> let(readVarInt(in),
						index -> {
							UnaryOperator<Expression> instanceInitializer = instance -> call(map, "put", cast(add(call(map, "size"), value(1)), Integer.class), instance);
							return ifEq(index, value(0),
									serializer instanceof SerializerDefClass ?
											((SerializerDefClass) serializer).decoder(staticDecoders, in, version, compatibilityLevel, instanceInitializer) :
											let(serializer.decoder(staticDecoders, in, version, compatibilityLevel),
													item -> sequence(
															instanceInitializer.apply(item),
															item)),
									cast(call(map, "get", cast(index, Integer.class)), serializer.getDecodeType()));
						}));
	}

	private static Expression encodeMap() {
		return cast(call(staticField(SerializerDefReference.class, "MAP_ENCODE"), "get"), Map.class);
	}

	private static Expression decodeMap() {
		return cast(call(staticField(SerializerDefReference.class, "MAP_DECODE"), "get"), Map.class);
	}

	private static Map<Object, Expression> clearMap(Expression map) {
		return Map.of(SerializerDefReference.class, call(map, "clear"));
	}
}