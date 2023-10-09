package nl.jqno.equalsverifier.internal.prefabvalues;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.LinkedHashSet;
import nl.jqno.equalsverifier.internal.exceptions.RecursionException;
import nl.jqno.equalsverifier.internal.exceptions.ReflectionException;
import nl.jqno.equalsverifier.internal.prefabvalues.factories.FallbackFactory;
import nl.jqno.equalsverifier.internal.prefabvalues.factories.PrefabValueFactory;
import nl.jqno.equalsverifier.internal.util.PrimitiveMappers;

/**
 * Container and creator of prefabricated instances of objects and classes.
 *
 * <p>Only creates values ones, and caches them once they've been created. Takes generics into
 * account; i.e., {@code List<Integer>} is different from {@code List<String>}.
 */
public class PrefabValues {

    private final Cache cache = new Cache();
    private final FactoryCache factoryCache;
    private final PrefabValueFactory<?> fallbackFactory = new FallbackFactory<>();

    /**
     * Constructor.
     *
     * @param factoryCache The factories that can be used to create values.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "A cache is inherently mutable.")
    public PrefabValues(FactoryCache factoryCache) {
        this.factoryCache = factoryCache;
    }

    /**
     * Returns the "red" prefabricated value of the specified type.
     *
     * <p>It's always a different value from the "blue" one.
     *
     * @param <T> The return value is cast to this type.
     * @param tag A description of the desired type, including generic parameters.
     * @return The "red" prefabricated value.
     */
    public <T> T giveRed(TypeTag tag) {
        return this.<T>giveTuple(tag).getRed();
    }

    /**
     * Returns the "blue" prefabricated value of the specified type.
     *
     * <p>It's always a different value from the "red" one.
     *
     * @param <T> The return value is cast to this type.
     * @param tag A description of the desired type, including generic parameters.
     * @return The "blue" prefabricated value.
     */
    public <T> T giveBlue(TypeTag tag) {
        return this.<T>giveTuple(tag).getBlue();
    }

    /**
     * Returns a shallow copy of the "red" prefabricated value of the specified type.
     *
     * <p>When possible, it's equal to but not the same as the "red" object.
     *
     * @param <T> The return value is cast to this type.
     * @param tag A description of the desired type, including generic parameters.
     * @return A shallow copy of the "red" prefabricated value.
     */
    public <T> T giveRedCopy(TypeTag tag) {
        return this.<T>giveTuple(tag).getRedCopy();
    }

    /**
     * Returns a tuple of two different prefabricated values of the specified type.
     *
     * @param <T> The returned tuple will have this generic type.
     * @param tag A description of the desired type, including generic parameters.
     * @return A tuple of two different values of the given type.
     */
    public <T> Tuple<T> giveTuple(TypeTag tag) {
        realizeCacheFor(tag, emptyStack());
        return cache.getTuple(tag);
    }

    /**
     * Returns a prefabricated value of the specified type, that is different from the specified
     * value.
     *
     * @param <T> The type of the value.
     * @param tag A description of the desired type, including generic parameters.
     * @param value A value that is different from the value that will be returned.
     * @return A value that is different from {@code value}.
     */
    public <T> T giveOther(TypeTag tag, T value) {
        Class<T> type = tag.getType();
        if (
            value != null &&
            !type.isAssignableFrom(value.getClass()) &&
            !wraps(type, value.getClass())
        ) {
            throw new ReflectionException("TypeTag does not match value.");
        }

        Tuple<T> tuple = giveTuple(tag);
        if (tuple.getRed() == null) {
            return null;
        }
        if (type.isArray() && arraysAreDeeplyEqual(tuple.getRed(), value)) {
            return tuple.getBlue();
        }
        if (!type.isArray() && value != null && tuple.getRed().equals(value)) {
            return tuple.getBlue();
        }
        return tuple.getRed();
    }

    private boolean wraps(Class<?> expectedClass, Class<?> actualClass) {
        return PrimitiveMappers.PRIMITIVE_OBJECT_MAPPER.get(expectedClass) == actualClass;
    }

    private boolean arraysAreDeeplyEqual(Object x, Object y) {
        // Arrays.deepEquals doesn't accept Object values so we need to wrap them in another array.
        return Arrays.deepEquals(new Object[] { x }, new Object[] { y });
    }

    private LinkedHashSet<TypeTag> emptyStack() {
        return new LinkedHashSet<>();
    }

    /**
     * Makes sure that values for the specified type are present in the cache, but doesn't return
     * them.
     *
     * @param <T> The desired type.
     * @param tag A description of the desired type, including generic parameters.
     * @param typeStack Keeps track of recursion in the type.
     */
    public <T> void realizeCacheFor(TypeTag tag, LinkedHashSet<TypeTag> typeStack) {
        if (!cache.contains(tag)) {
            Tuple<T> tuple = createTuple(tag, typeStack);
            addToCache(tag, tuple);
        }
    }

    private <T> Tuple<T> createTuple(TypeTag tag, LinkedHashSet<TypeTag> typeStack) {
        if (typeStack.contains(tag)) {
            throw new RecursionException(typeStack);
        }

        Class<T> type = tag.getType();
        if (factoryCache.contains(type)) {
            PrefabValueFactory<T> factory = factoryCache.get(type);
            return factory.createValues(tag, this, typeStack);
        }

        @SuppressWarnings("unchecked")
        Tuple<T> result = (Tuple<T>) fallbackFactory.createValues(tag, this, typeStack);
        return result;
    }

    private void addToCache(TypeTag tag, Tuple<?> tuple) {
        cache.put(tag, tuple.getRed(), tuple.getBlue(), tuple.getRedCopy());
    }
}
