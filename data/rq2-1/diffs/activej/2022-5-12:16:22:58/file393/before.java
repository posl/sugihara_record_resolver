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

package io.activej.dataflow.dataset.impl;

import io.activej.dataflow.dataset.Dataset;
import io.activej.dataflow.dataset.SortedDataset;
import io.activej.dataflow.graph.DataflowContext;
import io.activej.dataflow.graph.StreamId;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public final class DatasetAlreadySorted<K, T> extends SortedDataset<K, T> {
	private final Dataset<T> dataset;

	public DatasetAlreadySorted(Dataset<T> dataset, Comparator<K> keyComparator, Class<K> keyType, Function<T, K> keyFunction) {
		super(dataset.valueType(), keyComparator, keyType, keyFunction);
		this.dataset = dataset;
	}

	@Override
	public List<StreamId> channels(DataflowContext context) {
		return dataset.channels(context);
	}

	@Override
	public Collection<Dataset<?>> getBases() {
		return singletonList(dataset);
	}
}
