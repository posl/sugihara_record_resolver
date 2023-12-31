/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.stat;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.StatUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.jenetics.Optimize;
import io.jenetics.internal.util.Named;
import io.jenetics.util.ISeq;
import io.jenetics.util.RandomRegistry;

/**
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 */
public class MinMaxTest {

	private static final Named<Comparator<Integer>> NORMAL = Named.of(
		"NORMAL",
		Optimize.MAXIMUM::compare
	);

	private static final Named<Comparator<Integer>> REVERSE = Named.of(
		"REVERSE",
		Optimize.MINIMUM::compare
	);

	@Test(dataProvider = "minTestValues")
	public void min(
		final Named<Comparator<Integer>> comparator,
		final Integer a,
		final Integer b,
		final Integer min
	) {
		assertEquals(MinMax.min(comparator.value, a, b), min);
	}

	@DataProvider(name = "minTestValues")
	public Object[][] minTestValues() {
		return new Object[][] {
			{NORMAL, null, null, null},
			{NORMAL, null, 1, 1},
			{NORMAL, 1, null, 1},
			{NORMAL, 1, 2, 1},
			{NORMAL, 2, 1, 1},
			{NORMAL, 1, 1, 1},

			{REVERSE, null, null, null},
			{REVERSE, null, 2, 2},
			{REVERSE, 2, null, 2},
			{REVERSE, 1, 2, 2},
			{REVERSE, 2, 1, 2},
			{REVERSE, 2, 2, 2}
		};
	}

	@Test(dataProvider = "maxTestValues")
	public void max(
		final Named<Comparator<Integer>> comparator,
		final Integer a,
		final Integer b,
		final Integer max
	) {
		assertEquals(MinMax.max(comparator.value, a, b), max);
	}

	@DataProvider(name = "maxTestValues")
	public Object[][] maxTestValues() {
		return new Object[][] {
			{NORMAL, null, null, null},
			{NORMAL, null, 1, 1},
			{NORMAL, 1, null, 1},
			{NORMAL, 1, 2, 2},
			{NORMAL, 2, 1, 2},
			{NORMAL, 1, 1, 1},

			{REVERSE, null, null, null},
			{REVERSE, null, 2, 2},
			{REVERSE, 2, null, 2},
			{REVERSE, 1, 2, 1},
			{REVERSE, 2, 1, 1},
			{REVERSE, 2, 2, 2}
		};
	}

	@Test
	public void acceptNormalMinMax() {
		final var random = RandomRegistry.random();
		final double[] numbers = random.doubles().limit(1000).toArray();

		final MinMax<Double> minMax = MinMax.of();
		Arrays.stream(numbers)
			.mapToObj(Double::valueOf)
			.forEach(minMax);

		assertEquals(minMax.min().doubleValue(), StatUtils.min(numbers));
		assertEquals(minMax.max().doubleValue(), StatUtils.max(numbers));
	}

	@Test
	public void acceptReverseMinMax() {
		final var random = RandomRegistry.random();
		final double[] numbers = random.doubles().limit(1000).toArray();

		final MinMax<Double> minMax = MinMax.of(Comparator.reverseOrder());
		Arrays.stream(numbers)
			.mapToObj(Double::valueOf)
			.forEach(minMax);

		assertEquals(minMax.min().doubleValue(), StatUtils.max(numbers));
		assertEquals(minMax.max().doubleValue(), StatUtils.min(numbers));
	}

	@Test
	public void toMinMaxNormal() {
		final var random = RandomRegistry.random();
		final double[] numbers = random.doubles().limit(1000).toArray();

		final MinMax<Double> minMax = Arrays.stream(numbers)
			.mapToObj(Double::valueOf)
			.collect(MinMax.toMinMax());

		assertEquals(minMax.min().doubleValue(), StatUtils.min(numbers));
		assertEquals(minMax.max().doubleValue(), StatUtils.max(numbers));
	}

	@Test
	public void toMinMaxReverse() {
		final var random = RandomRegistry.random();
		final double[] numbers = random.doubles().limit(1000).toArray();

		final MinMax<Double> minMax = Arrays.stream(numbers).boxed()
			.collect(MinMax.toMinMax(Comparator.reverseOrder()));

		assertEquals(minMax.min().doubleValue(), StatUtils.max(numbers));
		assertEquals(minMax.max().doubleValue(), StatUtils.min(numbers));
	}

	@Test
	public void parallelMinMax() {
		final Stream<Integer> stream = IntStream.range(0, 100).boxed().parallel();
		final MinMax<Integer> minMax = stream.collect(
			MinMax::of,
			MinMax::accept,
			MinMax::combine
		);

		assertEquals(minMax.max(), Integer.valueOf(99));
		assertEquals(minMax.min(), Integer.valueOf(0));
		assertEquals(100, minMax.count());
	}

	@Test
	public void sameState() {
		final MinMax<Long> mm1 = MinMax.of();
		final MinMax<Long> mm2 = MinMax.of();

		final Random random = new Random();
		for (int i = 0; i < 100; ++i) {
			final long value = random.nextInt(1_000_000);
			mm1.accept(value);
			mm2.accept(value);

			Assert.assertTrue(mm1.sameState(mm2));
			Assert.assertTrue(mm2.sameState(mm1));
			Assert.assertTrue(mm1.sameState(mm1));
			Assert.assertTrue(mm2.sameState(mm2));
		}
	}

	@Test
	public void toStrictlyIncreasing() {
		final ISeq<Integer> values = new Random().ints(0, 100)
			.boxed()
			.limit(500)
			.flatMap(MinMax.toStrictlyIncreasing())
			.collect(ISeq.toISeq());

		Assert.assertTrue(values.isSorted());
	}

	@Test
	public void toStrictlyImproving() {
		final ISeq<Integer> values = new Random().ints(0, 100)
			.boxed()
			.limit(500)
			.flatMap(MinMax.toStrictlyImproving(Comparator.naturalOrder()))
			.collect(ISeq.toISeq());

		Assert.assertTrue(values.isSorted());
	}

	@Test
	public void toStrictlyDecreasing() {
		final ISeq<Integer> values = new Random().ints(0, 100)
			.boxed()
			.limit(100)
			.flatMap(MinMax.toStrictlyDecreasing())
			.collect(ISeq.toISeq());

		Assert.assertTrue(values.copy().reverse().isSorted());
	}

}
