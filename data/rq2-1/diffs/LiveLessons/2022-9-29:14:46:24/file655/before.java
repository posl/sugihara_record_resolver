package livelessons.utils;

import java.util.*;
import java.util.function.Supplier;

/**
 * This class simplifies the computation of execution times.
 */
public class RunTimer {
    /**
     * Keep track of which SearchStreamGang performed the best.
     */
    private static final Map<String, Long> mResultsMap = new HashMap<>();

    /**
     * Keeps track of how long the test has run.
     */
    private static long sStartTime;

    /**
     * Keeps track of the execution time.
     */
    private static long mExecutionTime;

    /**
     * Start timing the test run.
     */
    private static void startTiming() {
        // Note the start time.
        sStartTime = System.nanoTime();
    }

    /**
     * Stop timing the test run.
     */
    private static void stopTiming() {
        mExecutionTime = (System.nanoTime() - sStartTime) / 1_000_000;
    }

    /**
     * Call {@code supplier.get()} and time how long it takes to run.
     *
     * @return The result returned by @a supplier.get()
     */
    public static <U> U timeRun(Supplier<U> supplier,
                                String testName) {
        startTiming();
        U result = supplier.get();
        stopTiming();

        // Store the execution times into the results map.
        mResultsMap.put(testName,
                        mExecutionTime);

        return result;
    }

    /**
     * Call {@code runnable.run()} and time how long it takes to run.
     */
    public static void timeRun(Runnable runnable,
                               String testName) {
        startTiming();
        runnable.run();
        stopTiming();

        // Store the execution times into the results map.
        mResultsMap.put(testName,
                        mExecutionTime);
    }

    /**
     * @return A string containing the timing results for all the test runs
     * ordered from fastest to slowest.
     */
    public static String getTimingResults() {
        StringBuilder stringBuilder =
            new StringBuilder();

        stringBuilder.append("\nPrinting ")
            .append(mResultsMap.entrySet().size())
            .append(" results from fastest to slowest\n");

        // Print out the contents of the mResultsMap in sorted order.
        mResultsMap
            // Get the entrySet for the mResultsMap.
            .entrySet()

            // Convert the entrySet into a stream.
            .stream()

            // Create a SimpleImmutableEntry containing the timing
            // results (value) followed by the test name (key).
            .map(entry
                 -> new AbstractMap.SimpleImmutableEntry<>(entry.getValue(),
                                                           entry.getKey()))

            // Sort the stream by the timing results (key).
            .sorted(Map.Entry.comparingByKey())

            // Append the entries in the sorted stream.
            .forEach(entry -> 
                     // Create the desired output.
                     stringBuilder
                     // Right-justify the runtime.
                     .append(String.format("%5d", entry.getKey()))
                     .append(" msecs needed to run ")
                     // Just get the last portion of the test name.
                     .append(StringUtils.lastSegment(entry.getValue(), '.'))
                     .append("\n"));

        // Convert the result into a string.
        return stringBuilder.toString();
    }
}
