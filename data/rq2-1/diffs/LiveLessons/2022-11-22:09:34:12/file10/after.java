import common.BackpressureEmitter;
import common.BackpressureSubscriber;
import common.BackpressureEmitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import common.Options;
import org.reactivestreams.Subscriber;
import utils.PrimeUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This program applies RxJava {@link Flowable} features to
 * demonstrate a {@link Subscriber} running in on {@link Scheduler}
 * context can exert backpressure on a {@link Publisher} that runs in
 * a different {@link Scheduler} context.
 */
public class ex2 {
    /**
     * Debugging tag used by the logger.
     */
    private final String TAG = getClass().getSimpleName();

    /**
     * Count the # of pending items between {@link Publisher} and
     * {@link Subscriber}.
     */
    private final AtomicInteger mPendingItemCount =
        new AtomicInteger(0);

    /**
     * A {@link Subscriber} that applies backpressure.
     */
    private final BackpressureSubscriber mSubscriber;

    /**
     * Track all disposables to dispose of them all at once.
     */
    private final CompositeDisposable mDisposables;

    /**
     * Main entry point into the test program.
     */
    static public void main(String[] argv) {
        // Create an instance to run the test.
        new ex2(argv).run();
    }

    /**
     * Constructor initializes the fields.
     */
    ex2(String[] argv) {
        // Parse the command-line arguments.
        Options.instance().parseArgs(argv);

        // A subscriber that implements a backpressure model.
        mSubscriber = new BackpressureSubscriber
            (mPendingItemCount, Options.instance().requestSize());

        // Track all disposables to dispose them all at once.
        mDisposables = new CompositeDisposable(mSubscriber);
    }

    /**
     * Run the test and print the results.
     */
    private void run() {
        Options.print("Starting test with count = "
                      + Options.instance().count());

        this
            // Create a publisher that runs in a new scheduler thread
            // and return a Flowable that emits random Integers at a
            // rate determined by the subscriber.
            .publishRandomIntegers(Schedulers.newThread())

            // Concurrently check each random # to see if it's prime.
            .flatMap(checkForPrimality())

            // The blocking subscriber sets the program in motion.
            .blockingSubscribe(mSubscriber);

        // Dispose of the subscriber.
        mDisposables.dispose();

        Options.print("test complete");
    }

    /**
     * @return A {@link Function} that checks a number for primality
     *         on a given {@link Scheduler}
     */
    private static Function<Integer, Publisher<? extends PrimeUtils.Result>>
        checkForPrimality() {
        return number -> Flowable
            // This factory method emits the number.
            .fromCallable(() -> number)

            // Check the number for primality in the given scheduler.
            .observeOn(Options.instance().scheduler())

            // Check if the number is prime.
            .map(__ ->
                 PrimeUtils.checkIfPrime(number));
    }

    /**
     * Publish a stream of random numbers.
     *
     * @param scheduler {@link Scheduler} to publish the random
     *                  numbers on
     * @return A {@link Flowable} that publishes random numbers
     */
    private Flowable<Integer> publishRandomIntegers(Scheduler scheduler) {
        return Flowable
            // This factory method emits a stream of random integers.
            .generate(BackpressureEmitter
                      // Emit a stream of random integers.
                      .makeEmitter(Options.instance().count(),
                                   Options.instance().maxValue(),
                                   mPendingItemCount))

            // Handle errors/exceptions gracefully.
            .onErrorResumeNext(error -> {
                    Options.debug(error.getMessage());
                    return Flowable.empty();
                })

            // Subscribe on the given scheduler.
            .subscribeOn(scheduler);
    }
}
    
