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

package io.activej.launchers.crdt;

import io.activej.crdt.function.CrdtFunction;
import io.activej.crdt.util.BinarySerializer_CrdtData;

import java.lang.reflect.Type;

public record CrdtDescriptor<K extends Comparable<K>, S>(CrdtFunction<S> crdtFunction,
														 BinarySerializer_CrdtData<K, S> serializer,
														 Type keyManifest,
														 Type stateManifest) {
}