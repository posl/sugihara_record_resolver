/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Base class for all types that include parameterization of some kind. Crucial as we have to take note of the parent
 * class we will have to resolve generic parameters against.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class ParameterizedTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

	private final ParameterizedType type;
	private final Lazy<Boolean> resolved;

	/**
	 * Creates a new {@link ParameterizedTypeInformation} for the given {@link Type} and parent {@link TypeDiscoverer}.
	 *
	 * @param type must not be {@literal null}
	 * @param parent must not be {@literal null}
	 */
	public ParameterizedTypeInformation(ParameterizedType type, TypeDiscoverer<?> parent) {

		super(type, parent, calculateTypeVariables(type, parent));

		this.type = type;
		this.resolved = Lazy.of(() -> isResolvedCompletely());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#doGetMapValueType()
	 */
	@Override
	@Nullable
	protected TypeInformation<?> doGetMapValueType() {

		if (Map.class.isAssignableFrom(getType())) {

			var arguments = type.getActualTypeArguments();

			if (arguments.length > 1) {
				return createInfo(arguments[1]);
			}
		}

		Class<?> rawType = getType();

		Set<Type> supertypes = new HashSet<>();
		Optional.ofNullable(rawType.getGenericSuperclass()).ifPresent(supertypes::add);
		supertypes.addAll(Arrays.asList(rawType.getGenericInterfaces()));

		var result = supertypes.stream()//
				.map(it -> Pair.of(it, resolveType(it)))//
				.filter(it -> Map.class.isAssignableFrom(it.getSecond()))//
				.<TypeInformation<?>> map(it -> {

					var parameterizedSupertype = (ParameterizedType) it.getFirst();
					var arguments = parameterizedSupertype.getActualTypeArguments();
					return createInfo(arguments[1]);
				}).findFirst();

		return result.orElseGet(super::doGetMapValueType);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getTypeParameters()
	 */
	@Override
	public List<TypeInformation<?>> getTypeArguments() {

		List<TypeInformation<?>> result = new ArrayList<>();

		for (var argument : type.getActualTypeArguments()) {
			result.add(createInfo(argument));
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#isAssignableFrom(org.springframework.data.util.TypeInformation)
	 */
	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {

		if (this.equals(target)) {
			return true;
		}

		var rawType = getType();
		var rawTargetType = target.getType();

		if (!rawType.isAssignableFrom(rawTargetType)) {
			return false;
		}

		var otherTypeInformation = rawType.equals(rawTargetType) ? target
				: target.getSuperTypeInformation(rawType);

		var myParameters = getTypeArguments();
		List<TypeInformation<?>> typeParameters = otherTypeInformation == null ? Collections.emptyList()
				: otherTypeInformation.getTypeArguments();

		if (myParameters.size() != typeParameters.size()) {
			return false;
		}

		for (var i = 0; i < myParameters.size(); i++) {
			if (!myParameters.get(i).isAssignableFrom(typeParameters.get(i))) {
				return false;
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#doGetComponentType()
	 */
	@Override
	@Nullable
	protected TypeInformation<?> doGetComponentType() {

		var isCustomMapImplementation = isMap() && !getType().equals(Map.class);

		if (isCustomMapImplementation) {
			return getRequiredSuperTypeInformation(Map.class).getComponentType();
		}

		return createInfo(type.getActualTypeArguments()[0]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#specialize(org.springframework.data.util.ClassTypeInformation)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TypeInformation<? extends T> specialize(ClassTypeInformation<?> type) {

		if (isResolvedCompletely()) {
			return (TypeInformation<? extends T>) type;
		}

		var asSupertype = type.getSuperTypeInformation(getType());

		if (asSupertype == null || !ParameterizedTypeInformation.class.isInstance(asSupertype)) {
			return super.specialize(type);
		}

		return ((ParameterizedTypeInformation<?>) asSupertype).isResolvedCompletely() //
				? (TypeInformation<? extends T>) type //
				: super.specialize(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ParameterizedTypeInformation<?> that)) {
			return false;
		}

		if (this.isResolved() && that.isResolved()) {
			return this.type.equals(that.type);
		}

		return super.equals(obj);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#hashCode()
	 */
	@Override
	public int hashCode() {
		return isResolved() ? this.type.hashCode() : super.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format("%s<%s>", getType().getName(),
				StringUtils.collectionToCommaDelimitedString(getTypeArguments()));
	}

	private boolean isResolved() {
		return resolved.get();
	}

	private boolean isResolvedCompletely() {

		var typeArguments = type.getActualTypeArguments();

		if (typeArguments.length == 0) {
			return false;
		}

		for (var typeArgument : typeArguments) {

			var info = createInfo(typeArgument);

			if (info instanceof ParameterizedTypeInformation) {
				if (!((ParameterizedTypeInformation<?>) info).isResolvedCompletely()) {
					return false;
				}
			}

			if (!(info instanceof ClassTypeInformation)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Resolves the type variables to be used. Uses the parent's type variable map but overwrites variables locally
	 * declared.
	 *
	 * @param type must not be {@literal null}.
	 * @param parent must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private static Map<TypeVariable<?>, Type> calculateTypeVariables(ParameterizedType type, TypeDiscoverer<?> parent) {

		var resolvedType = parent.resolveType(type);
		TypeVariable<?>[] typeParameters = resolvedType.getTypeParameters();
		var arguments = type.getActualTypeArguments();

		Map<TypeVariable<?>, Type> localTypeVariables = new HashMap<>(parent.getTypeVariableMap());

		IntStream.range(0, typeParameters.length) //
				.mapToObj(it -> Pair.of(typeParameters[it], flattenTypeVariable(arguments[it], localTypeVariables))) //
				.forEach(it -> localTypeVariables.put(it.getFirst(), it.getSecond()));

		return localTypeVariables;
	}

	/**
	 * Recursively resolves the type bound to the given {@link Type} in case it's a {@link TypeVariable} and there's an
	 * entry in the given type variables.
	 *
	 * @param source must not be {@literal null}.
	 * @param variables must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private static Type flattenTypeVariable(Type source, Map<TypeVariable<?>, Type> variables) {

		if (!(source instanceof TypeVariable)) {
			return source;
		}

		var value = variables.get(source);

		return value == null ? source : flattenTypeVariable(value, variables);
	}
}
