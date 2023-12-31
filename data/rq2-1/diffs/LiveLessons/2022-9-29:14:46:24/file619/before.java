package utils;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.parallel.ParallelTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.SingleSubject;

import java.util.Collection;
import java.util.concurrent.ForkJoinPool;

/**
 * A utility class containing helpful methods for manipulating various
 * RxJava features.
 */
public class RxUtils {
    /**
     * A utility class should always define a private constructor.
     */
    private RxUtils() {
    }

    /**
     * @return Schedule a single to run on the common fork-join pool.
     */
    public static <T> SingleTransformer<T, T> commonPoolSingle() {
        return single -> single
            .subscribeOn(Schedulers.from(ForkJoinPool.commonPool()));
    }

    /**
     * @return Schedule a parallel flowable to run on the common
     * fork-join pool.
     */
    public static <T> ParallelTransformer<T, T> commonPoolParallelFlowable() {
        return observable -> observable
            .runOn(Schedulers.from(ForkJoinPool.commonPool()));
    }

    /**
     * @return Schedule an observable to run on the common fork-join
     * pool.
     */
    public static <T> ObservableTransformer<T, T> commonPoolObservable() {
        return observable -> observable
            .subscribeOn(Schedulers.from(ForkJoinPool.commonPool()));
    }

    /**
     * Conditionally enable concurrent processing if {@code parallel}
     * is true, otherwise, use sequential processing.

     * @return {@code commonPoolObservable()} if {@code parallel} is
     * true, else {@code callingObservable()}.
     */
    public static <T> ObservableTransformer<T, T> concurrentObservableIf(boolean parallel) {
        if (parallel)
            return RxUtils.commonPoolObservable();
        else
            return RxUtils.callingObservable();
    }

    /**
     * @return Schedule an observable to run on the calling thread.
     */
    public static <T> ObservableTransformer<T, T> callingObservable() {
        // No-op!!
        return observable -> observable;
    }

    /**
     * Convert {@code single} into a "hot" single that doesn't
     * regenerate values seen by earlier subscribers.
     *
     * @param single The single to make "hot"
     * @return A "hot" single.
     */
    public static <T> SingleSubject<T> makeHotSingle(Single<T> single) {
        SingleSubject<T> subject = SingleSubject.create();
        single.subscribe(subject);
        return subject;
    }

    /**
     * Emit {@code item} either concurrently or sequentially based on {@code parallel} flag.
     *
     * @param item Item to emit
     * @param parallel True if emit concurrently, false if emit
     * @return An observable that will be emitted concurrenty or sequentially.
     */
    public static <T> Observable<T> fromCallableConcurrentIf(T item, boolean parallel) {
        return Observable
                // Just omit this one item.
                .fromCallable(() -> item)

                // Conditionally convert to run concurrently.
                .compose(RxUtils.concurrentObservableIf(parallel));
    }

    /**
     * Emit {@code collection} as a parallel flowable that runs in the
     * common fork-join pool.
     *
     * @param collection The collection whos contents will be processed in parallel
     * @return A parallel flowable running on the common fork-join pool
     */
    public static <T> ParallelFlowable<T> fromIterableParallel(Collection<T> collection) {
        return Flowable
            // Convert collection into a flowable.
            .fromIterable(collection)

            // Create a parallel flowable.
            .parallel()

            // Run this flow of operations in the common fork-join pool.
            .runOn(Schedulers.from(ForkJoinPool.commonPool()));
    }
}
