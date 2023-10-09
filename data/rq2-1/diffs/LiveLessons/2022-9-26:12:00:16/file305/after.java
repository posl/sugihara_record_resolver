import utils.RunTimer;
import utils.TestDataFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * This example demonstrates the performance differences between
 * concurrent and non-concurrent techniques for joining results in a
 * stream.  Finally, it demonstrates performance differences between
 * {@code forEach()} and {@code forEachOrdered()} terminal operations
 * when applied to accumulate results in a stream.
 */
@SuppressWarnings("ALL")
public class ex14 {
    /**
     * Number of iterations to run the timing tests.
     */
    private static final int sMAX_ITERATIONS = 1;

    /**
     * The complete works of William Shakespeare.
     */
    private static final String sSHAKESPEARE_DATA_FILE =
        "completeWorksOfShakespeare.txt";

    /**
     * A regular expression that matches whitespace and punctuation.
     */
    private static final String sWHITESPACE_AND_PUNCTUATION = 
        "[\\t\\n\\x0B\\f\\r'!()\"#&-.,;0-9:@<>\\[\\]? ]+";

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

        // Run tests that demonstrate performance differences between
        // concurrent and non-concurrent techniques for joining
        // results in a stream.
        runJoiningTests();

        // Run tests that demonstrate performance differences between
        // forEach() and forEachOrdered() terminal operations when
        // applied to accumulate results in a stream.
        runForEachTests();

        System.out.println("Exiting the test program");
    }

    /**
     * Run tests that demonstrate performance differences between
     * concurrent and non-concurrent techniques for joining results in
     * a stream.
     */
    private static void runJoiningTests() {
        Arrays
            // Create tests for different sizes of input data.
            .asList(1000, 10000, 100000, 1000000)

            // For each input data size run the following tests.
            .forEach (limit -> {
                    // Create a list of strings containing all the
                    // words in the complete works of Shakespeare.
                    List<CharSequence> arrayWords =
                        TestDataFactory.getInput(sSHAKESPEARE_DATA_FILE,
                                                 // Split input into "words" by
                                                 // ignoring whitespace.
                                                 "\\s+",
                                                 limit);

                    assert arrayWords != null;

                    // Print a message when the test starts.
                    System.out.println("Starting joining tests for "
                                       + arrayWords.size() 
                                       + " words..");

                    // Compute the time required to join arrayWords
                    // via collect() and Collectors.joining() in a
                    // sequential stream.  The performance of this
                    // test should be better than the parallel stream
                    // version below since there's less overhead for
                    // combining/joining the various partial results.
                    timeStreamJoining("ArrayList",
                                      false,
                                      arrayWords);

                    // Compute the time required to join arrayWords
                    // via collect() and Collectors.joining() in a
                    // parallel stream.  The performance of this test
                    // should be worse than the sequential stream
                    // version above due to the overhead of
                    // combining/joining the various partial results
                    // in parallel.
                    timeStreamJoining("ArrayList",
                                      true,
                                      arrayWords);

                    // Print the results.
                    System.out.println("..printing results\n"
                                       + RunTimer.getTimingResults());
                });
    }

    /**
     * Run tests that demonstrate the performance differences between
     * the {@code forEach()} and {@code forEachOrdered()} terminal
     * opperations when applied to accumulate results in a stream.
     */
    private static void runForEachTests() {
        Arrays
            // Create tests for different sizes of input data.
            .asList(1000, 10000, 100000, 1000000)

            // For each input data size run the following tests.
            .forEach (limit -> {
                    // Create a list of strings containing all the
                    // words in the complete works of Shakespeare.
                    List<CharSequence> arrayWords =
                        TestDataFactory.getInput(sSHAKESPEARE_DATA_FILE,
                                                 // Split input into "words" by
                                                 // ignoring whitespace.
                                                 "\\s+",
                                                 limit);

                    assert arrayWords != null;

                    // Print a message when the test starts.
                    System.out.println("Starting forEach* tests for "
                                       + arrayWords.size() 
                                       + " words..");

                    // Compute the time required to aggregate results
                    // into a ConcurrentHashMap.KeySetView using the forEach()
                    // terminal operation.
                    timeStreamForEachToSet("ArrayList",
                                           false,
                                           arrayWords);

                    // Compute the time required to aggregate results
                    // into a HashSet using the forEachOrdered()
                    // terminal operation.
                    timeStreamForEachToSet("ArrayList",
                                           true,
                                           arrayWords);

                    // Print the results.
                    System.out.println("..printing results\n"
                                       + RunTimer.getTimingResults());
                });
    }

    /**
     * Determines how long it takes to combine partial results in the
     * word list via {@code collect()} and {@code
     * Collectors.joining()} in a stream.  If {@code parallel} is true
     * then a parallel stream is used, else a sequential stream is
     * used.
     *
     * @param testName The name of the test being run
     * @param parallel True if the test should run in parallel, else
     *                 run sequentially
     * @param words The {@link List} of words to upper case
     */
    private static void timeStreamJoining(String testName,
                                          boolean parallel,
                                          List<CharSequence> words) {
        // Run the garbage collector before each test.
        System.gc();

        testName +=
            (parallel ? " parallel" : " sequential")
            + " timeStreamJoining()";

        // System.out.println("Starting " + testName);

        RunTimer.timeRun(() -> {
                StringBuilder results = new StringBuilder();

                for (int i = 0; i < sMAX_ITERATIONS; i++) {
                    Stream<CharSequence> wordStream = words
                        // Convert the list into a stream (which uses a
                        // spliterator internally).
                        .stream();

                    if (parallel)
                        // Convert to a parallel stream.
                        wordStream.parallel();

                    // A "real" application would likely do something
                    // interesting with the words at this point.

                    // Join all the words in the stream.
                    CharSequence charSequence = wordStream
                        .collect(joining(" "));

                    // Add the joined results to the string builder.
                    results.append(charSequence);
                }},
            testName);
    }

    /**
     * Determines how long it takes to collect results into a {@link
     * HashSet} using the {@code forEachOrdered()} terminal operation
     * and into a {@link ConcurrentHashMap.KeySetView} using the {@code
     * forEach()} terminal operation.

     * @param testName The name of the test being run
     * @param ordered If {@code ordered} is true then {@code
     *                forEachOrdered()} and a {@link HashMap} is used,
     *                else {@code forEach()} and a {@link
     *                ConcurrentHashMap.KeySetView} is used.
     * @param words The {@link List} of words to upper case
     */
    private static void timeStreamForEachToSet(String testName,
                                               boolean ordered,
                                               List<CharSequence> words) {
        // Run the garbage collector before each test.
        System.gc();

        testName +=
            (ordered ? " forEachOrdered()" : " forEach()")
            + " timeStreamForEachToSet()";

        if (ordered)
            RunTimer.timeRun(() -> {
                    // Create an unsynchronized HashSet since forEachOrdered()
                    // performs synchronization implicitly.
                    Set<CharSequence> uniqueWords = 
                        new HashSet<>();

                    for (int i = 0; i < sMAX_ITERATIONS; i++) {
                        words
                            // Convert the list into a stream (which
                            // uses a spliterator internally).
                            .parallelStream()

                            // Map each string to lower case.  A
                            // "real" application would likely do
                            // something interesting with the words at
                            // this point.
                            .map(charSeq -> charSeq.toString().toLowerCase())

                            // Trigger intermediate processing and
                            // collect unique words into a HashSet.
                            .forEachOrdered(uniqueWords::add);
                    }},
                testName);
        else
            RunTimer.timeRun(() -> {
                    // Create a ConcurrentHashMap.KeySetView since
                    // forEach() does not perform synchronization
                    // implicitly.
                    Set<CharSequence> uniqueWords = 
                        ConcurrentHashMap.newKeySet();

                    for (int i = 0; i < sMAX_ITERATIONS; i++) {
                        words
                            // Convert the list into a stream (which
                            // uses a spliterator internally).
                            .parallelStream()

                            // Map each string to lower case.  A
                            // "real" application would likely do
                            // something interesting with the words at
                            // this point.
                            .map(charSeq -> charSeq.toString().toLowerCase())

                            // Trigger intermediate processing and
                            // collect unique words into a HashSet.
                            .forEach(uniqueWords::add);
                    }},
                testName);
    }


    /**
     * Warm up the threads in the fork/join pool so the timing results
     * will be more accurate.
     */
    private static void warmUpForkJoinPool() {
        System.out.println("\n++Warming up the fork/join pool\n");

        List<CharSequence> words = Objects
            .requireNonNull(TestDataFactory
                            .getInput(sSHAKESPEARE_DATA_FILE,
                                      // Split input into "words" by
                                      // ignoring whitespace.
                                      sWHITESPACE_AND_PUNCTUATION));
        // Create an empty list.
        List<String> list = new ArrayList<>();

        for (int i = 0; i < sMAX_ITERATIONS; i++)
            // Append the new words to the end of the list.
            list.addAll(words
                        // Convert the list into a parallel stream
                        // (which uses a spliterator internally).
                        .parallelStream()

                        // Uppercase each string.  A "real"
                        // application would likely do something
                        // interesting with the words at this point.
                        .map(charSeq -> charSeq.toString().toUpperCase())

                        // Collect the stream into a list.
                        .collect(toList()));
    }
}
