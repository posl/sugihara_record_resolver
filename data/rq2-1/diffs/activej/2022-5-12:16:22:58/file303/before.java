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

import java.lang.reflect.Modifier;
import java.util.*;

import static io.activej.codegen.expression.Expressions.*;
import static io.activej.serializer.CompatibilityLevel.LEVEL_3;
import static io.activej.serializer.impl.SerializerExpressions.readByte;
import static io.activej.serializer.impl.SerializerExpressions.writeByte;
import static io.activej.serializer.util.Utils.get;
import static org.objectweb.asm.Type.getType;

public final class SerializerDefSubclass extends AbstractSerializerDef implements SerializerDefWithNullable {
	private final Class<?> dataType;
	private final LinkedHashMap<Class<?>, SerializerDef> subclassSerializers;
	private final boolean nullable;
	private final int startIndex;

	public SerializerDefSubclass(@NotNull Class<?> dataType, LinkedHashMap<Class<?>, SerializerDef> subclassSerializers, int startIndex) {
		checkSubclasses(subclassSerializers.keySet());

		this.startIndex = startIndex;
		this.dataType = dataType;
		this.subclassSerializers = new LinkedHashMap<>(subclassSerializers);
		this.nullable = false;
	}

	private SerializerDefSubclass(@NotNull Class<?> dataType, LinkedHashMap<Class<?>, SerializerDef> subclassSerializers, boolean nullable, int startIndex) {
		checkSubclasses(subclassSerializers.keySet());

		this.startIndex = startIndex;
		this.dataType = dataType;
		this.subclassSerializers = new LinkedHashMap<>(subclassSerializers);
		this.nullable = nullable;
	}

	@Override
	public SerializerDef ensureNullable(CompatibilityLevel compatibilityLevel) {
		if (compatibilityLevel.getLevel() < LEVEL_3.getLevel()) {
			return new SerializerDefNullable(this);
		}
		return new SerializerDefSubclass(dataType, subclassSerializers, true, startIndex);
	}

	@Override
	public void accept(Visitor visitor) {
		for (Map.Entry<Class<?>, SerializerDef> entry : subclassSerializers.entrySet()) {
			visitor.visit(entry.getKey().getName(), entry.getValue());
		}
	}

	@Override
	public boolean isInline(int version, CompatibilityLevel compatibilityLevel) {
		return false;
	}

	@Override
	public Class<?> getEncodeType() {
		return dataType;
	}

	@Override
	public Expression encoder(StaticEncoders staticEncoders, Expression buf, Variable pos, Expression value, int version, CompatibilityLevel compatibilityLevel) {
		int subClassIndex = (nullable && startIndex == 0 ? 1 : startIndex);

		List<Expression> listKey = new ArrayList<>();
		List<Expression> listValue = new ArrayList<>();
		for (Map.Entry<Class<?>, SerializerDef> entry : subclassSerializers.entrySet()) {
			SerializerDef subclassSerializer = entry.getValue();
			listKey.add(cast(value(getType(entry.getKey())), Object.class));
			listValue.add(sequence(
					writeByte(buf, pos, value((byte) subClassIndex)),
					subclassSerializer.defineEncoder(staticEncoders, buf, pos, cast(value, subclassSerializer.getEncodeType()), version, compatibilityLevel)
			));

			subClassIndex++;
			if (nullable && subClassIndex == 0) {
				subClassIndex++;
			}
		}
		if (nullable) {
			return ifThenElse(isNotNull(value),
					switchByKey(call(value, "getClass"), listKey, listValue),
					writeByte(buf, pos, value((byte) 0)));
		} else {
			return switchByKey(call(value, "getClass"), listKey, listValue);
		}
	}

	@Override
	public Expression decoder(StaticDecoders staticDecoders, Expression in, int version, CompatibilityLevel compatibilityLevel) {
		return let(startIndex != 0 ? sub(readByte(in), value(startIndex)) : cast(readByte(in), int.class),
				idx -> cast(
						switchByIndex(idx,
								get(() -> {
									List<Expression> versions = new ArrayList<>();
									for (SerializerDef subclassSerializer : subclassSerializers.values()) {
										versions.add(cast(subclassSerializer.defineDecoder(staticDecoders, in, version, compatibilityLevel), dataType));
									}
									if (nullable) versions.add(-startIndex, nullRef(getDecodeType()));
									return versions;
								})),
						dataType));
	}

	private static void checkSubclasses(Set<Class<?>> subclasses) {
		for (Class<?> subclass : subclasses) {
			if (Modifier.isAbstract(subclass.getModifiers())) {
				throw new IllegalArgumentException("A subclass should not be an " +
						(subclass.isInterface() ? "interface" : "abstract class") + ": " + subclass);
			}
		}
	}
}
