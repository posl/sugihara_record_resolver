package utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A concurrent collector that accumulates input elements of type
 * {@code E} into a {@link ConcurrentHashMap.KeySetView} and the returns a type
 * {@link S} that extends {@link Set}.
 */
public class ConcurrentSetCollector<E, S extends Set<E>>
       implements Collector<E,
                            Set<E>,
                            S> {
    /**
     * A {@link Supplier} that returns a new, empty {@code Set} into
     * which the results will be inserted.
     */
    private final Supplier<S> mSetSupplier;

    /**
     * Create a {@link Collector} that accumulates elements into a
     * {@link Set} whose keys are the result of applying the provided
     * mapping functions to the input elements.
     * 
     * @param mapSupplier a supplier that returns a new, empty {@link
     *                    Set} into which the results will be inserted
     */
    public ConcurrentSetCollector(Supplier<S> mapSupplier) {
        mSetSupplier = mapSupplier;
    }

    /**
     * A factory method that creates and returns a new mutable result
     * container of type {@link ConcurrentHashMap.KeySetView} that holds all the
     * elements in the stream.
     *
     * @return a function which returns a new, mutable result container
     */
    @Override
    public Supplier<Set<E>> supplier() {
        return ConcurrentHashMap::newKeySet;
    }

    /**
     * A method that folds an element into the {@link ConcurrentHashMap.KeySetView}.
     *
     * @return a function that folds a value into a mutable result container
     */
    @Override
    public BiConsumer<Set<E>, E> accumulator() {
        // Add element to the map.
        return Set::add;
    }

    /**
     * A method that accepts two partial results and merges them.
     *
     * @return A {@link BinaryOperator} that merges two maps together
     */
    @Override
    public BinaryOperator<Set<E>> combiner() {
        // Merge the two sets together.
        return (first, second) -> {
            first.addAll(second);
            return first;
        };
    }

    /**
     * Perform the final transformation from the intermediate
     * accumulation type {@link ConcurrentHashMap.KeySetView} to the final result
     * type {@code S}, which extends {@link Set}.
     *
     * @return A {@link Set} containing the contents of the stream
     */
    @Override
    public Function<Set<E>, S> finisher() {
        return set -> {
            // Create the appropriate map.
            S newSet = mSetSupplier.get();

            // Check whether we've been instantiated to return a
            // ConcurrentHashSet, in which case there's no need to
            // convert anything!
            if (newSet instanceof ConcurrentHashMap.KeySetView)
                //noinspection unchecked
                return (S) set;
            else {
                // Put the contents of the set mutable result
                // container into the new set.
                newSet.addAll(set);

                // Return the new map.
                return newSet;
            }
        };
    }

    /**
     * Returns a {@link Set} of {@link Collector.Characteristics}
     * indicating the characteristics of this Collector.  This set is
     * immutable.
     *
     * @return An immutable set of collector characteristics, which in
     * this case is [UNORDERED|CONCURRENT].
     */
    @Override
    public Set<Characteristics> characteristics() {
        return Collections
            .unmodifiableSet(EnumSet.of(Collector.Characteristics.CONCURRENT,
                                        Collector.Characteristics.UNORDERED));
    }

    @Override
    public String toString() {
        return "ConcurrentSetCollector<>";
    }

    /**
     * This static factory method creates a {@link Collector} that
     * accumulates elements into a {@link Set}.
     * 
     * @param mapSupplier a supplier that returns a new, empty {@link
     *                    Set} into which the results will be inserted
     * @return A {@link Collector} that collects elements into a {@link Set}
     */
    public static <T, S extends Set<T>> Collector<T, ?, S>
    toSet(Supplier<S> mapSupplier) {
        return new ConcurrentSetCollector<>(mapSupplier);
    }
}


