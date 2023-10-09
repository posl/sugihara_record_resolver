import org.jetbrains.annotations.NotNull;
import utils.ConcurrentMapCollector;
import utils.RunTimer;
import utils.TestDataFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Map.Entry;
import static java.util.stream.Collectors.toList;

/**
 * This program creates various {@link Map} objects that associate
 * unique words in the complete works of William Shakespeare with the
 * number of times each word appears and prints the top 50 words in
 * Shakespeare's works.  It also shows the difference in overhead
 * between collecting results in a parallel stream vs. sequential
 * stream using concurrent and non-concurrent collectors for various
 * types of Java {@link Map} implementations, including {@link
 * HashMap} and {@link TreeMap}.
 */
@SuppressWarnings("ALL")
public class ex37 {
    /**
     * The maximum number of top Bard words to print.
     */
    private static long sMAX_WORDS = 50;

    /**
     * This interface converts four params to a result type.
     */
    @FunctionalInterface
    interface QuadFunction<P1, P2, P3, P4, R> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4);
    }
    
    /**
     * Number of iterations to run the timing tests.
     */
    private static final int sMAX_ITERATIONS = 10;

    /**
     * The complete works of William Shakespeare.
     */
    private static final String sSHAKESPEARE_DATA_FILE =
        "completeWorksOfShakespeare.txt";

    /**
     * A regular expression that matches whitespace and punctuation to
     * split the text of the complete works of Shakepeare into
     * individual words.
     */
    private static final String sSPLIT_WORDS =
        "[\\t\\n\\x0B\\f\\r'!()\"#&-.,;0-9:@<>\\[\\]}_|? ]+";

    /**
     * Main entry point into the tests program.
     */
    static public void main(String[] argv) {
        System.out.println("Entering the test program with "
                           + Runtime.getRuntime().availableProcessors()
                           + " cores available");

        // Warm up the threads in the fork/join pool so the timing
        // results will be more accurate.
        warmUpForkJoinPool();

        // Run tests that demonstrate the performance differences
        // between concurrent and non-concurrent collectors when
        // collecting results in Java sequential and parallel streams
        // that use HashMaps, which are unordered.
        runMapCollectorTests("HashMap",
                             Function.identity(),
                             initialKeyValue(),
                             HashMap::new,
                             ConcurrentHashMap::new,
                             ex37::timeStreamCollect);

        // Run tests that demonstrate the performance differences
        // between concurrent and non-concurrent collectors when
        // collecting results in Java sequential and parallel streams
        // that use TreeMaps, which are ordered.
        runMapCollectorTests("TreeMap",
                             Function.identity(),
                             initialKeyValue(),
                             TreeMap::new,
                             TreeMap::new,
                             ex37::timeStreamCollect);

        // Print the results.
        printResults(getResults(true,
                                TestDataFactory
                                .getInput(sSHAKESPEARE_DATA_FILE,
                                          sSPLIT_WORDS,
                                          1_000_000),
                                ConcurrentMapCollector
                                .toMap(Function.identity(),
                                       initialKeyValue(),
                                       mergeDuplicateKeyValues(),
                                       TreeMap::new)),
                     "Final results");

        System.out.println("Exiting the test program");
    }

    /**
     * @return The initial value for a key (which is 1)
     */
    @NotNull
    private static Function<String, Integer> initialKeyValue() {
        return (s) -> 1;
    }

    /**
     * Run tests that demonstrate the performance differences between
     * concurrent and non-concurrent collectors when collecting
     * results in Java sequential and parallel streams for various
     * types of Java {@link Map} types.
     * 
     * @param testType The type of test, i.e., HashMap or TreeMap
     * @param mapSupplier A {@link Supplier} that creates the given
     *                    non-concurrent {@link Map}
     * @param concurrentMapSupplier A {@link Supplier} that creates
     *                              the given concurrent {@link Map}
     * @param streamCollect A {@link QuadFunction} that performs the test
     *                using either a non-concurrent or concurrent
     *                {@link Collector}
     */
    private static void runMapCollectorTests
        (String testType,
         Function<String, String> keyMapper,
         Function<String, Integer> valueMapper,
         Supplier<Map<String, Integer>> mapSupplier,
         Supplier<Map<String, Integer>> concurrentMapSupplier,
         QuadFunction<String,
         Boolean,
         List<String>,
         Collector<String, ?, Map<String, Integer>>,
         Void> streamCollect) {
        Arrays
            // Create tests for different sizes of input data.
            .asList(1_000, 10_000, 100_000, 1_000_000)

            // Run the tests for various input data sizes.
            .forEach (limit -> {
                    // Create a List of Strings containing
                    // 'limit' words from the works of Shakespeare.
                    List<String> arrayWords = TestDataFactory
                        .getInput(sSHAKESPEARE_DATA_FILE,
                                  // Split input into "words" by
                                  // ignoring whitespace and
                                  // punctuation.
                                  sSPLIT_WORDS,
                                  limit);

                    // Print a message when the test starts.
                    System.out.println("Starting "
                                       + testType
                                       + " test for "
                                       + arrayWords.size() 
                                       + " words..");

                    // Collect results into a sequential stream via a
                    // non-concurrent collector.
                    streamCollect
                        .apply("non-concurrent " + testType,
                               false,
                               arrayWords,
                               Collectors
                               .toMap(keyMapper,
                                      valueMapper,
                                      mergeDuplicateKeyValues(),
                                      mapSupplier));

                    // Collect results into a parallel stream via a
                    // non-concurrent collector.
                    streamCollect
                        .apply("non-concurrent " + testType,
                               true,
                               arrayWords,
                               Collectors
                               .toMap(keyMapper,
                                      valueMapper,
                                      mergeDuplicateKeyValues(),
                                      mapSupplier));

                    // Collect results into a sequential stream via a
                    // concurrent collector.
                    streamCollect
                        .apply("concurrent " + testType,
                               false,
                               arrayWords,
                               ConcurrentMapCollector
                               .toMap(keyMapper,
                                      valueMapper,
                                      mergeDuplicateKeyValues(),
                                      concurrentMapSupplier));

                    // Collect results into a parallel stream via a
                    // concurrent collector.
                    streamCollect
                        .apply("concurrent " + testType,
                               true,
                               arrayWords,
                               ConcurrentMapCollector
                               .toMap(keyMapper,
                                      valueMapper,
                                      mergeDuplicateKeyValues(),
                                      concurrentMapSupplier));

                    // Print the results.
                    System.out.println("..printing results\n"
                                       + RunTimer.getTimingResults());
                });
    }

    /**
     * Duplicate keys are handled by adding their values together and
     * updating the map.
     *
     * @return The result of adding the values of duplicate keys together
     */
    @NotNull
    private static BinaryOperator<Integer> mergeDuplicateKeyValues() {
        // Add the values of duplicate keys together.
        return (o1, o2) -> o1 + o2;
    }

    /**
     * Determines how long it takes to lowercase a {@link List} of
     * {@code words} and collect the results using the given {@link
     * Collector}.  
     *
     * @param testType The type of test, i.e., HashMap or TreeMap
     * @param parallel If true then a parallel stream is used, else a
     *                 sequential stream is used
     * @param words A {@link List} of words to lowercase
     * @param collector The {@link Collector} used to combine the
     *                  results
     */
    private static Void timeStreamCollect
        (String testType,
         boolean parallel,
         List<String> words,
         Collector<String, ?, Map<String, Integer>> collector) {
        // Run the garbage collector before each test.
        System.gc();

        // Update the name of the test to indicate whether the
        // stream is running sequentially or in parallel.
        String testName =
            (parallel ? " parallel" : " sequential")
            + " "
            + testType;

        RunTimer
            // Time how long it takes to run the test.
            .timeRun(() -> {
                    IntStream
                        // Iterate sMAX_ITERATIONS times.
                        .range(0, sMAX_ITERATIONS)

                        // Perform computations that create a
                        // Map of unique Shakespeare words.
                        .forEach(i -> getResults(parallel, words, collector));
                },
                testName);
        return null;
    }

    /**
     * Perform computations that create a {@link Map} of unique words
     * in Shakespeare's works.
     * 
     * @param parallel If true then a parallel stream is used, else a
     *                 sequential stream is used
     * @param words A {@link List} of words to lowercase
     * @param collector The {@link Collector} used to combine the
     *                  results
     * @return A {@link Map} containing the unique words in
     *         Shakespeare's works
     */
    private static Map<String, Integer> getResults
        (boolean parallel,
         List<String> words,
         Collector<String, ?, Map<String, Integer>> collector) {
        // Return a Map of unique words in Shakespeare's works.
        return // Create a parallel or sequential stream.
            (parallel ? words.parallelStream()
             : words.stream())

            // Lower case each String.
            .map(word -> word.toString().toLowerCase())

            // Trigger intermediate processing and collect the
            // unique words into the given collector.
            .collect(collector);
    }

    /**
     * Print the {@code result} of the {@code testName}.
     *
     * @param result The results of applying the test
     * @param testName The name of the test
     */
    private static void printResults(Map<String, Integer> results,
                                     String testName) {
        // Convert the first sMAX_WORDS elements of the Map contents
        // into a String.
        var allWords = results
            .entrySet()
            .stream()
            .map(Objects::toString)
            .toList();

        // Convert the top sMAX_WORDS most common words into a String.
        var topWords = results
            .entrySet()
            .stream()
            .sorted(Entry.<String, Integer>comparingByValue().reversed())
            .limit(sMAX_WORDS)
            .map(Objects::toString)
            .toList();

        // Print the results.
        System.out.println("Results for "
                           + testName
                           + " of size "
                           + results.size()
                           + " was:\n"
                           + allWords
                           + "\nwith top "
                           + sMAX_WORDS
                           + " words being\n"
                           + topWords);
    }

    /**
     * Warm up the threads in the fork/join pool so the timing results
     * will be more accurate.
     */
    private static void warmUpForkJoinPool() {
        System.out.println("\n++Warming up the fork/join pool\n");

        List<String> words = TestDataFactory
            .getInput(sSHAKESPEARE_DATA_FILE,
                      // Split input into "words" by ignoring
                      // whitespace.
                      "\\s+");

        // Create an empty list.
        List<String> list = new ArrayList<>();

        for (int i = 0; i < sMAX_ITERATIONS; i++) 
            // Append the new words to the end of the list.
            list.addAll(words
                        // Convert the list into a parallel stream
                        // (which uses a spliterator internally).
                        .parallelStream()

                        // Lowercase each string.
                        .map(word -> word.toString().toLowerCase())

                        // Collect the stream into a list.
                        .collect(toList()));
    }
}
