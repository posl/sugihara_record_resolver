/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.util.NullableWrapper;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * Simple domain service to convert query results into a dedicated type.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
class QueryExecutionResultHandler {

	private static final TypeDescriptor WRAPPER_TYPE = TypeDescriptor.valueOf(NullableWrapper.class);

	private final GenericConversionService conversionService;

	private final Object mutex = new Object();

	// concurrent access guarded by mutex.
	private Map<Method, ReturnTypeDescriptor> descriptorCache = Collections.emptyMap();

	/**
	 * Creates a new {@link QueryExecutionResultHandler}.
	 */
	QueryExecutionResultHandler(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Post-processes the given result of a query invocation to match the return type of the given method.
	 *
	 * @param result can be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	@Nullable
	Object postProcessInvocationResult(@Nullable Object result, Method method) {

		if (!processingRequired(result, method.getReturnType())) {
			return result;
		}

		var descriptor = getOrCreateReturnTypeDescriptor(method);

		return postProcessInvocationResult(result, 0, descriptor);
	}

	private ReturnTypeDescriptor getOrCreateReturnTypeDescriptor(Method method) {

		var descriptorCache = this.descriptorCache;
		var descriptor = descriptorCache.get(method);

		if (descriptor == null) {

			descriptor = ReturnTypeDescriptor.of(method);

			Map<Method, ReturnTypeDescriptor> updatedDescriptorCache;

			if (descriptorCache.isEmpty()) {
				updatedDescriptorCache = Collections.singletonMap(method, descriptor);
			} else {
				updatedDescriptorCache = new HashMap<>(descriptorCache.size() + 1, 1);
				updatedDescriptorCache.putAll(descriptorCache);
				updatedDescriptorCache.put(method, descriptor);

			}

			synchronized (mutex) {
				this.descriptorCache = updatedDescriptorCache;
			}
		}

		return descriptor;
	}

	/**
	 * Post-processes the given result of a query invocation to the given type.
	 *
	 * @param result can be {@literal null}.
	 * @param nestingLevel
	 * @param descriptor must not be {@literal null}.
	 * @return
	 */
	@Nullable
	Object postProcessInvocationResult(@Nullable Object result, int nestingLevel, ReturnTypeDescriptor descriptor) {

		var returnTypeDescriptor = descriptor.getReturnTypeDescriptor(nestingLevel);

		if (returnTypeDescriptor == null) {
			return result;
		}

		var expectedReturnType = returnTypeDescriptor.getType();

		result = unwrapOptional(result);

		if (QueryExecutionConverters.supports(expectedReturnType)
				|| ReactiveWrapperConverters.supports(expectedReturnType)) {

			// For a wrapper type, try nested resolution first
			result = postProcessInvocationResult(result, nestingLevel + 1, descriptor);

			if (conversionRequired(WRAPPER_TYPE, returnTypeDescriptor)) {
				return conversionService.convert(new NullableWrapper(result), returnTypeDescriptor);
			}

			if (result != null) {

				var source = TypeDescriptor.valueOf(result.getClass());

				if (conversionRequired(source, returnTypeDescriptor)) {
					return conversionService.convert(result, returnTypeDescriptor);
				}
			}
		}

		if (result != null) {

			if (ReactiveWrapperConverters.supports(expectedReturnType)) {
				return ReactiveWrapperConverters.toWrapper(result, expectedReturnType);
			}

			if (result instanceof Collection<?>) {

				var elementDescriptor = descriptor.getReturnTypeDescriptor(nestingLevel + 1);
				var requiresConversion = requiresConversion((Collection<?>) result, expectedReturnType, elementDescriptor);

				if (!requiresConversion) {
					return result;
				}
			}

			var resultDescriptor = TypeDescriptor.forObject(result);
			return conversionService.canConvert(resultDescriptor, returnTypeDescriptor)
					? conversionService.convert(result, returnTypeDescriptor)
					: result;
		}

		return Map.class.equals(expectedReturnType) //
				? CollectionFactory.createMap(expectedReturnType, 0) //
				: null;

	}

	private boolean requiresConversion(Collection<?> collection, Class<?> expectedReturnType,
			@Nullable TypeDescriptor elementDescriptor) {

		if (Streamable.class.isAssignableFrom(expectedReturnType) || !expectedReturnType.isInstance(collection)) {
			return true;
		}

		if (elementDescriptor == null || !Iterable.class.isAssignableFrom(expectedReturnType)) {
			return false;
		}

		var type = elementDescriptor.getType();

		for (Object o : collection) {

			if (!type.isInstance(o)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns whether the configured {@link ConversionService} can convert between the given {@link TypeDescriptor}s and
	 * the conversion will not be a no-op.
	 *
	 * @param source
	 * @param target
	 * @return
	 */
	private boolean conversionRequired(TypeDescriptor source, TypeDescriptor target) {

		return conversionService.canConvert(source, target) //
				&& !conversionService.canBypassConvert(source, target);
	}

	/**
	 * Unwraps the given value if it's a JDK 8 {@link Optional}.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	private static Object unwrapOptional(@Nullable Object source) {

		if (source == null) {
			return null;
		}

		return Optional.class.isInstance(source) //
				? Optional.class.cast(source).orElse(null) //
				: source;
	}

	/**
	 * Returns whether we have to process the given source object in the first place.
	 *
	 * @param source can be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return
	 */
	private static boolean processingRequired(@Nullable Object source, Class<?> targetType) {

		return !targetType.isInstance(source) //
				|| source == null //
				|| Collection.class.isInstance(source);
	}

	/**
	 * Value object capturing {@link MethodParameter} and {@link TypeDescriptor}s for top and nested levels.
	 */
	static class ReturnTypeDescriptor {

		private final MethodParameter methodParameter;
		private final TypeDescriptor typeDescriptor;
		private final @Nullable TypeDescriptor nestedTypeDescriptor;

		private ReturnTypeDescriptor(Method method) {
			this.methodParameter = new MethodParameter(method, -1);
			this.typeDescriptor = TypeDescriptor.nested(this.methodParameter, 0);
			this.nestedTypeDescriptor = TypeDescriptor.nested(this.methodParameter, 1);
		}

		/**
		 * Create a {@link ReturnTypeDescriptor} from a {@link Method}.
		 *
		 * @param method
		 * @return
		 */
		public static ReturnTypeDescriptor of(Method method) {
			return new ReturnTypeDescriptor(method);
		}

		/**
		 * Return the {@link TypeDescriptor} for a nested type declared within the method parameter described by
		 * {@code nestingLevel} .
		 *
		 * @param nestingLevel the nesting level. {@code 0} is the first level, {@code 1} the next inner one.
		 * @return the {@link TypeDescriptor} or {@literal null} if it could not be obtained.
		 * @see TypeDescriptor#nested(MethodParameter, int)
		 */
		@Nullable
		TypeDescriptor getReturnTypeDescriptor(int nestingLevel) {

			// optimizing for nesting level 0 and 1 (Optional<T>, List<T>)
			// nesting level 2 (Optional<List<T>>) uses the slow path.

			switch (nestingLevel) {
				case 0:
					return typeDescriptor;
				case 1:
					return nestedTypeDescriptor;
				default:
					return TypeDescriptor.nested(this.methodParameter, nestingLevel);
			}
		}
	}
}
