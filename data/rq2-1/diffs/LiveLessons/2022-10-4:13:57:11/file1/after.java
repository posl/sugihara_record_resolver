import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import reactor.core.publisher.Flux;
import utils.RunTimer;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * This program implements various ways of computing factorials for
 * BigIntegers to demonstrate the performance of alternative parallel
 * and sequential algorithms, as well as the dangers of sharing
 * unsynchronized state between threads.  It illustrates both Java
 * sequential/parallel streams and sequential/parallel reactive
 * streams implementations via RxJava and Project Reactor.
 *
 * A conventional Java parallel streams solution would look like this
 * for 'n' of type long:
 *
 * LongStream
 *   // Generate long values from 1 to n
 *   .rangeClosed(1, n)
 *
 *   // Run the stream in parallel.
 *   .parallel()
 *
 *   // Reduce the results of the parallel chunks.
 *   .reduce(1, (a, b) -> a * b);
 *
 * However, due to the exponential growth rate of factorials we use
 * a BigInteger result instead of a long result.  A more interesting
 * (albeit more complicated) solution that also uses BigIntegers for
 * the type of 'n' appears <a href="https://github.com/jevad/egfactorial">here</a>.
 */
public class ex16 {
    /**
     * Max number of times to run the tests.
     */
    private static final int sMAX_ITERATIONS = 20_000;

    /**
     * Default factorial number.  
     */
    private static final int sDEFAULT_N = 1_000;

    /**
     * This is the entry point into the test program.
     */
    public static void main(String[] args) {
        System.out.println("Starting Factorial Tests");

        // Initialize to the default value or the
        final BigInteger n = args.length > 0
            ? BigInteger.valueOf(Long.parseLong(args[0]))
            : BigInteger.valueOf(sDEFAULT_N);

        // Warm up the fork-join pool to ensure accurate timings.
        warmUpForkJoinThreads();

        // Run the various tests.

        runTest("SequentialFactorial",
                SequentialFactorial::factorial,
                n);

        runTest("SequentialStreamFactorial",
                SequentialStreamFactorial::factorial,
                n);

        runTest("BuggyFactorial1",
                BuggyFactorial1::factorial,
                n);

        runTest("BuggyFactorial2",
                BuggyFactorial1::factorial,
                n);

        runTest("SynchronizedParallelFactorial",
                SynchronizedParallelFactorial::factorial,
                n);

        runTest("ParallelStreamFactorial",
                ParallelStreamFactorial::factorial,
                n);

        runTest("ParallelStreamFactorialEx",
                ParallelStreamFactorialEx::factorial,
                n);

        runTest("SequentialRxJavaObservableFactorial",
                SequentialRxJavaObservableFactorial::factorial,
                n);

        runTest("SequentialReactorFluxFactorial",
                SequentialReactorFluxFactorial::factorial,
                n);

        runTest("RxJavaParallelFlowableFactorial",
                RxJavaParallelFlowableFactorial::factorial,
                n);

        runTest("ReactorParallelFluxFactorial",
                ReactorParallelFluxFactorial::factorial,
                n);

        System.out.println(RunTimer.getTimingResults());

        System.out.println("Ending Factorial Tests");
    }

    /**
     * Run the given {@code factorialTest} to compute the factorial of
     * {@code n} and print the result.
     */
    private static <T> void runTest(String factorialTest,
                                    Function<T, T> factorial,
                                    T n) {
        // Help out the garbage collector.
        System.gc();

        RunTimer
            // Time the test.
            .timeRun(() -> {
                    IntStream
                        // Iterate sMAX_ITERATION times.
                        .range(0, sMAX_ITERATIONS)
                        
                        // Apply the factorial computation.
                        .forEach(i -> factorial.apply(n));
            },
            factorialTest);

        System.out.println(factorialTest
                           + " factorial for "
                           + n 
                           + " = \n"
                           + factorial.apply(n));
    }

    /**
     * This class demonstrates a baseline factorial implementation
     * using a sequential algorithm.
     */
    private static class SequentialFactorial {
        /**
         * Return the factorial for the given {@code n} using a
         * sequential algorithm.
         */
        static BigInteger factorial(BigInteger n) {
            BigInteger f = new BigInteger("1");

            for (int i = 2; i <= n.intValue(); i++)
                f = f.multiply(BigInteger.valueOf(i));

            return f;
        }
    }

    /**
     * This class demonstrates a baseline factorial implementation
     * using a sequential Java Stream.
     */
    private static class SequentialStreamFactorial {
        /**
         * Return the factorial for the given {@code n} using a
         * sequential stream and the reduce() terminal operation.
         */
        static BigInteger factorial(BigInteger n) {
            return LongStream
                // Create a stream of longs from 1 to n.
                .rangeClosed(1, n.longValue())

                // Create a BigInteger from the long value.
                .mapToObj(BigInteger::valueOf)

                // Performs a reduction on the elements of this stream
                // to compute the factorial.
                .reduce(BigInteger.ONE,
                        BigInteger::multiply);
        }
    }

    /**
     * This class demonstrates how race conditions can occur when
     * state is shared between worker threads in the Java common
     * fork-join pool via a mutable shared object and the forEach()
     * terminal operation.
     */
    private static class BuggyFactorial1 {
        /**
         * This class keeps a running total of the factorial and
         * provides a (buggy) method for multiplying this running
         * total with a value n.
         */
        static class Total {
            /**
             * The running total of the factorial.
             */
            BigInteger mTotal = BigInteger.ONE;

            /**
             * Multiply the running total by {@code n}.  This method
             * is not synchronized, so it will incur race conditions
             * when run on a multi-core processor!
             */
            void multiply(BigInteger n) {
                mTotal = mTotal.multiply(n);
            }
        }  

        /**
         * Attempts to return the factorial for the given {@code n}.
         * There are race conditions wrt accessing shared state,
         * however, so the result may not always be correct.
         */
        static BigInteger factorial(BigInteger n) {
            Total t = new Total();

            LongStream
                // Create a stream of longs from 1 to n.
                .rangeClosed(1, n.longValue())

                // Run the forEach() terminal operation concurrently.
                .parallel()

                // Create a BigInteger from the long value.
                .mapToObj(BigInteger::valueOf)

                // Multiply the latest value in the range by the
                // running total (not properly synchronized).
                .forEach(t::multiply);

            // Return the total, which is also not properly
            // synchronized.
            return t.mTotal;
        }
    }

    /**
     * This class demonstrates how race conditions can occur when
     * state is shared between worker threads in the Java common
     * fork-join pool via a mutable shared object and the reduce()
     * terminal operation.
     */
    private static class BuggyFactorial2 {
        /**
         * This class intentionally provides a buggy method for
         * multiplying {@link BigInteger} objects together.
         */
        static class BIMultiplier {
            /**
             * Stores the {@link BigInteger}.
             */
            BigInteger mBigInteger;

            /**
             * Constructor converts the {@code long} value to a {@link
             * BigInteger}.
             *
             * @param l The {@code long} value to convert to a {@link
             * BigInteger}
             */
            BIMultiplier(long l) {
                // Convert 'l' to a BigInteger.
                mBigInteger = BigInteger.valueOf(l);
            }

            /**
             * Multiply this {@link BIMultiplier} by {@code biMultiplier}.
             * This method is not synchronized and thus may incur race
             * conditions when run on a multi-core processor!
             */
            BIMultiplier multiply(BIMultiplier biMultiplier) {
                mBigInteger = mBigInteger
                    // Multiply the values together.
                    .multiply(biMultiplier.mBigInteger);

                // Return the updated value.
                return this;
            }

            /**
             * @return The current value of the {@link BigInteger}
             */
            BigInteger bigInteger() {
                return mBigInteger;
            }
        }

        /**
         * Attempts to return the factorial for the given {@code n}.
         * There are race conditions wrt accessing shared state,
         * however, so the result may not always be correct.
         */
        static BigInteger factorial(BigInteger n) {
            return LongStream
                // Create a stream of longs from 1 to n.
                .rangeClosed(1, n.longValue())

                // Run the forEach() terminal operation concurrently.
                .parallel()

                // Create a BIMultiplier from the long value.
                .mapToObj(BIMultiplier::new)

                // Perform a reduction (not properly synchronized).
                .reduce(new BIMultiplier(0L),
                        BIMultiplier::multiply)

                // Return the total.
                .bigInteger();
        }
    }

    /**
     * This class demonstrates how a synchronized statement can avoid
     * race conditions when state is shared between Java threads.
     */
    private static class SynchronizedParallelFactorial {
        /**
         * This class keeps a running total of the factorial and
         * provides a synchronized method for multiplying this running
         * total with a value n.
         */
        static class Total {
            /**
             * The running total of the factorial.
             */
            BigInteger mTotal = BigInteger.ONE;

            /**
             * Multiply the running total by {@code n}.  This method
             * is synchronized to avoid race conditions.
             */
            synchronized void multiply(BigInteger n) {
                mTotal = mTotal.multiply(n);
            }

            /**
             * Synchronize get() to ensure visibility of the data.
             */
            synchronized BigInteger get() {
                return mTotal;
            }
        }

        /**
         * Return the factorial for the given {@code n} using a
         * parallel stream and the forEach() terminal operation.
         */
        static BigInteger factorial(BigInteger n) {
            Total t = new Total();

            LongStream
                // Create a stream of longs from 1 to n.
                .rangeClosed(1, n.longValue())

                // Run the forEach() terminal operation concurrently.
                .parallel()

                // Create a BigInteger from the long value.
                .mapToObj(BigInteger::valueOf)

                // Multiply the latest value in the range by the
                // running total (properly synchronized).
                .forEach(t::multiply);

            // Return the total.
            return t.get();
        }
    }

    /**
     * This class demonstrates how the two-parameter modern Java
     * reduce() operation avoids sharing state between Java threads
     * altogether.
     */
    private static class ParallelStreamFactorial {
        /**
         * Return the factorial for the given {@code n} using a
         * parallel stream and the reduce() terminal operation.
         */
        static BigInteger factorial(BigInteger n) {
            return LongStream
                // Create a stream of longs from 1 to n.
                .rangeClosed(1, n.longValue())

                // Run the reduce() terminal operation concurrently.
                .parallel()

                // Create a BigInteger from the long value.
                .mapToObj(BigInteger::valueOf)

                // Use the two parameter variant of reduce() to
                // perform a reduction on the elements of this stream
                // to compute the factorial.  Note that there's no
                // shared state at all!
                .reduce(BigInteger.ONE,
                        BigInteger::multiply);
        }
    }

    /**
     * This class demonstrates how the three-parameter modern Java
     * reduce() operation avoids sharing state between Java threads
     * altogether.
     */
    private static class ParallelStreamFactorialEx {
        /**
         * Return the factorial for the given {@code n} using a
         * parallel stream and the reduce() terminal operation.
         */
        static BigInteger factorial(BigInteger n) {
            return LongStream
                // Create a stream of longs from 1 to n.
                .rangeClosed(1, n.longValue())

                // Run the reduce() terminal operation concurrently.
                .parallel()

                // Create a BigInteger from the long value.
                .mapToObj(BigInteger::valueOf)

                // Use the three parameter variant of reduce() to
                // perform a reduction on the elements of this stream
                // to compute the factorial.  Note that there's no
                // shared state at all!
                .reduce(BigInteger.ONE,
                        BigInteger::multiply,
                        BigInteger::multiply);
        }
    }

    /**
     * This class demonstrates a baseline factorial implementation
     * using a sequential RxJava Observable.
     */
    private static class SequentialRxJavaObservableFactorial {
        /**
         * Return the factorial for the given {@code n} using a
         * sequential RxJava Observable and its reduce() operation.
         */
        static BigInteger factorial(BigInteger n) {
            return Observable
                // Create a stream of longs from 1 to n.
                .rangeLong(1, n.longValue())

                // Create a BigInteger from the long value.
                .map(BigInteger::valueOf)

                // Use reduce() to perform a reduction on the elements
                // of this stream to compute the factorial.
                .reduce(BigInteger::multiply)

                // Block until all the results are finished.  If n was
                // 0 then return 1.
                .blockingGet(BigInteger.ONE);
        }
    }

    /**
     * This class demonstrates a baseline factorial implementation
     * using a sequential Project Reactor Flux.
     */
    private static class SequentialReactorFluxFactorial {
        /**
         * Return the factorial for the given {@code n} using a
         * sequential Project Reactor Flux and its reduce() operation.
         */
        static BigInteger factorial(BigInteger n) {
            return Flux
                // Create a stream of longs from 1 to n.
                .range(1, n.intValue())

                // Create a BigInteger from the long value.
                .map(BigInteger::valueOf)

                // Use reduce() to perform a reduction on the elements
                // of this stream to compute the factorial.
                .reduce(BigInteger::multiply)

                // Block until all the results are finished.
                .block();
        }
    }

    /**
     * This class demonstrates how to apply the RxJava
     * ParallelFlowable feature to compute factorials in parallel.
     */
    private static class RxJavaParallelFlowableFactorial {
        /**
         * Return the factorial for the given {@code n} using the
         * RxJava ParallelFlowable and its reduce() operation.
         */
        static BigInteger factorial(BigInteger n) {
            return Flowable
                // Create a stream of longs from 1 to n.
                .rangeLong(1, n.longValue())

                // Convert the Flowable into a ParallelFlowable.
                .parallel()

                // Create a BigInteger from the long value.
                .map(BigInteger::valueOf)

                // Use reduce() to perform a reduction on the elements
                // of this stream to compute the factorial.
                .reduce(BigInteger::multiply)

                // Block until all the results are finished.  If n was
                // 0 then return 1.
                .blockingSingle(BigInteger.ONE);
        }
    }

    /**
     * This class demonstrates how to apply the Project Reactor
     * ParallelFlux feature to compute factorials in parallel.
     */
    private static class ReactorParallelFluxFactorial {
        /**
         * Return the factorial for the given {@code n} using the
         * Project Reactor ParallelFlux and its reduce() operation.
         */
        static BigInteger factorial(BigInteger n) {
            return Flux
                // Create a stream of longs from 1 to n.
                .range(1, n.intValue())

                // Convert the Flux into a ParallelFlux.
                .parallel()

                // Create a BigInteger from the long value.
                .map(BigInteger::valueOf)

                // Use reduce() to perform a reduction on the elements
                // of this stream to compute the factorial.
                .reduce(BigInteger::multiply)

                // Block until all the results are finished.
                .block();
        }
    }

    /**
     * Warm up the threads in the fork/join pool so that the timing
     * results will be more accurate.
     */
    private static void warmUpForkJoinThreads() {
        System.out.println("Warming up the fork/join pool\n");

        for (int i = 0; i < sMAX_ITERATIONS; i++)
            ParallelStreamFactorial.factorial(BigInteger.valueOf(sDEFAULT_N));
    }
}

