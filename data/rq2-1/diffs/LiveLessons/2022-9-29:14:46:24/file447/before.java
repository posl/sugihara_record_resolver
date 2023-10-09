package utils;

import datamodels.Flight;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Define a {@code collector} that converts a stream of
 * {@code Flight}s into a {@code Flux} that emits the
 * cheapest priced trips(s).
 */
public class CheapestPriceCollectorFlux
        implements Collector<Flight,
        List<Flight>,
        Flux<Flight>> {
    /**
     * The minimum value seen by the collector.
     */
    double mMin = Double.MAX_VALUE;

    /**
     * A function that creates and returns a new mutable result
     * container that will hold all the Flights in the stream.
     *
     * @return a function which returns a new, mutable result container
     */
    @Override
    public Supplier<List<Flight>> supplier() {
        return ArrayList::new;
    }

    /**
     * A function that folds a Flight into the mutable result
     * container.
     *
     * @return a function which folds a value into a mutable result container
     */
    @Override
    public BiConsumer<List<Flight>, Flight> accumulator() {
        return (lowestPrices, Flight) -> {
            // If the price of the trip is less than the current min
            // Add it to the lowestPrices List and update the current
            // min price.
            if (Flight.getPrice() < mMin) {
                lowestPrices.clear();
                lowestPrices.add(Flight);
                mMin = Flight.getPrice();

                // If the price of the trip is equal to the current min
                // add it to the lowestPrices List.
            } else if (Flight.getPrice() == mMin) {
                lowestPrices.add(Flight);
            }
        };
    }

    /**
     * A function that accepts two partial results and merges them.
     * The combiner function may fold state from one argument into the
     * other and return that, or may return a new result container.
     *
     * @return a function which combines two partial results into a
     * combined result
     */
    @Override
    public BinaryOperator<List<Flight>> combiner() {
        // Merge two Lists together.
        return (one, another) -> {
            one.addAll(another);
            return one;
        };
    }

    /**
     * Perform the final transformation from the intermediate
     * accumulation type {@code A} to the final result type {@code R}.
     *
     * @return a function which transforms the intermediate result (a
     * List<Flight>) to the final result (a Flux<Flight)
     */
    @Override
    public Function<List<Flight>, Flux<Flight>> finisher() {
        // Convert the List into a Flux stream.
        return Flux::fromIterable;
    }

    /**
     * Returns a {@code Set} of {@code Collector.Characteristics}
     * indicating the characteristics of this Collector.
     *
     * @return An emptySet()
     */
    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }

    /**
     * This static factory method creates a new
     * CheapestFlightCollector.
     *
     * @return A new CheapestFlightCollector()
     */
    public static Collector<Flight, List<Flight>, Flux<Flight>>
    toFlux() {
        return new CheapestPriceCollectorFlux();
    }
}