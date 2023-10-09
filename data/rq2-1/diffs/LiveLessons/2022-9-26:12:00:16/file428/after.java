import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import utils.BigFraction;
import utils.BigFractionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static utils.BigFractionUtils.*;

/**
 * This class shows how to apply Project Reactor features
 * synchronously to perform basic Flux operations, including just(),
 * fromIterable(), fromArray(), from(), doOnNext(), map(),
 * mapNotNull(), mergeWith(), repeat(), and subscribe().
 */
@SuppressWarnings("ALL")
public class FluxEx {
    /**
     * Test BigFraction multiplication using a synchronous Flux
     * stream.
     */
    public static Mono<Void> testFractionMultiplicationSync1() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionMultiplicationSync1()\n");

        Flux
            // Use just() to generate a stream of big fractions.
            .just(BigFraction.valueOf(100, 3),
                  BigFraction.valueOf(100, 4),
                  BigFraction.valueOf(100, 2),
                  BigFraction.valueOf(100, 1))

            // Log the contents of the computation.
            .doOnNext(bigFraction -> 
                      sb.append("    ["
                                + Thread.currentThread().getId()
                                + "] "
                                + bigFraction.toMixedString()
                                + " x "
                                + sBigReducedFraction.toMixedString()))

            // Use map() to multiply each element in the stream by a
            // constant.
            .map(bigFraction -> bigFraction
                 // Multiply and return result.
                 .multiply(sBigReducedFraction))

            // Use subscribe() to initiate all the processing and
            // handle the results.  This call runs synchronously since
            // the publisher (just()) is synchronous and runs in the
            // calling thread.  However, there are more interesting
            // types of publishers that enable asynchrony.
            .subscribe(// Handle next event.
                       multipliedBigFraction ->
                       // Add fraction to the string builder.
                       sb.append(" = " 
                                 + multipliedBigFraction.toMixedString() 
                                 + "\n"),
                       // Handle error result event.
                       error -> {
                           // Append the exception name.
                           sb.append(error.getMessage());

                           // Display results when processing is done.
                           BigFractionUtils.display(sb.toString());
                       },
                       // Handle final completion event.
                       () -> BigFractionUtils
                       // Display results when processing is done.
                       .display(sb.toString()));

        // Return empty mono to indicate to the AsyncTaskBarrier that
        // all the processing is done.
        return sVoidM;
    }

    /**
     * Another test of BigFraction multiplication using a synchronous
     * Flux stream.
     */
    public static Mono<Void> testFractionMultiplicationSync2() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionMultiplicationSync2()\n");

        // Create a list of BigFraction objects.
        List<BigFraction> bigFractionList = List.of
            (BigFraction.valueOf(100, 3),
             BigFraction.valueOf(100, 4),
             BigFraction.valueOf(100, 2),
             BigFraction.valueOf(100, 1));

        Flux
            // Use fromIterable() to generate a stream of big
            // fractions.
            .fromIterable(bigFractionList)

            // Log the contents of the computation.
            .doOnNext(bf -> logBigFraction(sBigReducedFraction, bf, sb))

            // Use map() to multiply each element in the stream by a
            // constant.
            .map(fraction -> fraction.multiply(sBigReducedFraction))

            // Use subscribe() to initiate all the processing and
            // handle the results.  This call runs synchronously since
            // the publisher (fromIterable()) is synchronous and runs
            // in the calling thread.  However, there are more
            // interesting types of publishers that enable asynchrony.
            .subscribe(// Handle next event.
                       multipliedBigFraction ->
                       // Add fraction to the string builder.
                       sb.append(" = " 
                                 + multipliedBigFraction.toMixedString() 
                                 + "\n"),
                       // Handle error result event.
                       error -> {
                           // Append the exception name.
                           sb.append(error.getMessage());

                           // Display results when processing is done.
                           BigFractionUtils.display(sb.toString());
                       },
                       // Handle final completion event.
                       () -> BigFractionUtils
                       // Display results when processing is done.
                       .display(sb.toString()));

        // Return empty mono to indicate to the AsyncTaskBarrier that
        // all the processing is done.
        return sVoidM;
    }

    /**
     * A test of BigFraction multiplication using a synchronous Flux
     * stream that merges results together.
     */
    public static Mono<Void> testFractionMultiplicationSync3() {
        StringBuilder sb =
            new StringBuilder(">> Calling testFractionMultiplicationSync3()\n");

        // Random number generator.
        Random random = new Random();

        // Create an array of BigFraction objects.
        BigFraction[] bigFractionArray = {
            BigFraction.valueOf(100, 3),
            BigFraction.valueOf(100, 4),
            BigFraction.valueOf(100, 2),
            BigFraction.valueOf(100, 1)
        };

        Flux<BigFraction> f1 = Flux
            // Use fromArray() to generate a stream of big
            // fractions.
            .fromArray(bigFractionArray);

        Flux<BigFraction> f2 = Flux
            // Use from() and Mono.fromCallable() to "lazily" generate a
            // stream of random big fractions since Flux lacks fromCallable().
            .from(Mono.fromCallable(() -> BigFractionUtils
                                    .makeBigFraction(random,
                                                     true)))

            // Generate random big fractions 3 + 1 times.
            .repeat(3);

        f1
            // Flatten Flux f1 and f2 into a single Flux sequence,
            // without any transformations.
            .mergeWith(f2)

            // Log the contents of the computation.
            .doOnNext(bf -> logBigFraction(sBigReducedFraction, bf, sb))

            // Use map() to multiply each element in the stream by
            // a constant.
            .map(fraction -> fraction.multiply(sBigReducedFraction))

            // Use subscribe() to initiate all the processing and
            // handle the results synchronously.
            .subscribe(// Handle next event.
                       fraction -> 
                       sb.append(" = " + fraction.toMixedString() + "\n"),
                       // Handle the error.
                       t -> {
                           // Append the error message to the
                           // StringBuilder.
                           sb.append(t.getMessage());

                           // Display results when processing is done.
                           BigFractionUtils.display(sb.toString());
                       },
                       () -> BigFractionUtils
                       // Display results when processing is done.
                       .display(sb.toString()));

        // Return empty mono to indicate to the AsyncTaskBarrier
        // that all the processing is done.
        return sVoidM;
    }

    /**
     * A test of the Flux.mapNotNull() operator using a synchronous
     * Flux stream.
     */
    public static Mono<Void> testMapNotNull() {
        StringBuilder sb =
            new StringBuilder(">> Calling testMapNotNull()\n");

        Flux
            // Use just() to generate a stream of big fractions.
            .just(BigFraction.valueOf(100, 3),
                  BigFraction.valueOf(100, 10),
                  BigFraction.valueOf(100, 2),
                  BigFraction.valueOf(100, 1))

            // Log the contents of the computation.
            .doOnNext(bf -> logBigFraction(sBigReducedFraction, bf, sb))

            // Do not emit null values.
            .mapNotNull(bf -> bf
                        // Return null if bf == 10.
                        .equals(BigFraction.TEN) ? null : bf)

            // Use subscribe() to initiate all the processing and
            // handle the results synchronously.
            .subscribe(// Handle next event.
                       fraction -> 
                       sb.append(" = " + fraction.toMixedString() + "\n"),
                       // Handle the error.
                       t -> {
                           // Append the error message to the
                           // StringBuilder.
                           sb.append(t.getMessage());

                           // Display results when processing is done.
                           BigFractionUtils.display(sb.toString());
                       },
                       () -> BigFractionUtils
                       // Display results when processing is done.
                       .display(sb.toString()));

        // that all the processing is done.
        return sVoidM;
    }
}
