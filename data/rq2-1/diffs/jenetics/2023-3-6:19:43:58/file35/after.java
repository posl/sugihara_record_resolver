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
package io.jenetics.internal.math;

/**
 * Mathematical functions regarding probabilities.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 1.4
 * @version 5.2
 */
public final class Probabilities {
	private Probabilities() {}

	private static final long RANGE =
		(long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE;

	/**
	 * Maps the probability, given in the range {@code [0, 1]}, to an
	 * integer in the range {@code [Integer.MIN_VALUE, Integer.MAX_VALUE]}.
	 *
	 * @see #toFloat(int)
	 *
	 * @param probability the probability to widen.
	 * @return the widened probability.
	 */
	public static int toInt(final double probability) {
		return (int)(RANGE*probability + Integer.MIN_VALUE);
	}

	/**
	 * Maps the <i>integer</i> probability, within the range
	 * {@code [Integer.MIN_VALUE, Integer.MAX_VALUE]} back to a float
	 * probability within the range {@code [0, 1]}.
	 *
	 * @see  #toInt(double)
	 *
	 * @param probability the <i>integer</i> probability to map.
	 * @return the mapped probability within the range {@code [0, 1]}.
	 */
	public static float toFloat(final int probability) {
		final long value = (long)probability + Integer.MAX_VALUE + 1;
		return (float)(value/(double) RANGE);
	}
}
