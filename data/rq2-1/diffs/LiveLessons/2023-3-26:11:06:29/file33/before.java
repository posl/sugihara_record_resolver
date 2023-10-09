package edu.vandy.lockmanager.server;

import edu.vandy.lockmanager.common.Lock;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static edu.vandy.lockmanager.utils.Logger.log;

/**
 * This Spring {@code Service} implements the {@link LockManagerController}
 * endpoint handler methods via an {@link ArrayBlockingQueue}.
 */
@Service
public class LockManagerService {
    /**
     * Create a new {@code VirtualThreadPerTaskExecutor}.
     */
    ExecutorService mExecutor = Executors
        .newVirtualThreadPerTaskExecutor();

    /**
     * An ArrayBlockingQueue that limits concurrent access to the
     * fixed number of available locks managed by the
     * {@link LockManagerService}.
     */
    private ArrayBlockingQueue<Lock> mAvailableLocks;

    /**
     * Initialize the {@link Lock} manager.
     *
     * @param permitCount The number of {@link Lock} objects to
     *                    manage
     */
    public void create(Integer permitCount) {
        log("creating " + permitCount + " locks");

        if (mAvailableLocks == null) {
            mAvailableLocks =
                // Make an ArrayBlockQueue with "fair" semantics.
                new ArrayBlockingQueue<>(permitCount,
                    true);
        } else if (permitCount != mAvailableLocks.size()) {
            mAvailableLocks.clear();
            mAvailableLocks =
                // Make an ArrayBlockQueue with "fair" semantics.
                new ArrayBlockingQueue<>(permitCount,
                    true);
        } else
            return;

        // Add each Lock to the queue.
        mAvailableLocks.addAll(makeLocks(permitCount));
    }

    /**
     * Create the requested number of {@link Lock} objects.
     *
     * @param permitCount The number of {@link Lock} objects to create
     */
    private List<Lock> makeLocks(int permitCount) {
        return IntStream
            // Iterate from 0 to count - 1.
            .range(0, permitCount)

            // Convert Integer to String.
            .mapToObj(Integer::toString)

            // Create a new Lock.
            .map(Lock::new)

            // Convert the Stream to a List.
            .toList();
    }

    /**
     * Acquire a {@link Lock}, blocking until one is available.
     *
     * @return A {@link DeferredResult<Lock>}
     */
    public DeferredResult<Lock> acquire() {
        log("LockService.acquire()");

        // Create a DeferredResult containing the List of Lock
        // objects.
        DeferredResult<Lock> deferredResult =
            new DeferredResult<>();

        try {
            // Run the computation off the Servlet thread.
            mExecutor
                .submit(() -> {
                    log("LockService - requesting a Lock");

                    try {
                        var lock = mAvailableLocks
                            .poll();

                        if (lock == null) {
                            log("LockService -- blocking for lock acquire");

                            // Block until a Lock is available.
                            lock = mAvailableLocks.take();
                        } else
                            log("LockService -- non-blocking lock acquire");

                        // Set the result to the acquired Lock.
                        deferredResult
                            .setResult(lock);

                        log("LockService - returning Lock "
                            + lock);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (Exception exception) {
            log("Catch exception "
                + exception.getMessage());
            deferredResult
                .setResult(new Lock(exception.getMessage()));
        }
        log("returning deferredResult");

        // Return the deferredResult before the lock
        // are obtained.
        return deferredResult;
    }

    /**
     * Acquire {@code permits} number of {@link Lock} objects.
     *
     * @param permits The number of permits to acquire
     * @return A {@link DeferredResult<List>} containing {@code
     * permits} number of acquired {@link Lock} objects
     */
    public DeferredResult<List<Lock>> acquire(int permits) {
        log("LockService.acquire(permits)");

        // Create a DeferredResult containing the List of Lock
        // objects.
        DeferredResult<List<Lock>> deferredResult =
            new DeferredResult<>();

        try {
            // Run the computation off the Servlet thread.
            mExecutor
                .submit(() -> {
                    // Create a List to hold the acquired Lock objects.
                    List<Lock> acquiredLocks =
                        new ArrayList<>(permits);

                    while (tryAcquireLock(acquiredLocks) != permits)
                        continue;

                    log("LockService - got all "
                        + acquiredLocks.size()
                        + " lock(s) "
                        + acquiredLocks);

                    // Set the deferredResult to the acquired List
                    // of Lock objects.
                    deferredResult
                        .setResult(acquiredLocks);

                    log("LockService - returning acquired locks "
                        + acquiredLocks);
                });
        } catch (Exception exception) {
            log("Catch exception "
                + exception.getMessage());
            // Return an error message.
            var lock = new Lock(exception.getMessage());

            deferredResult
                .setResult(List.of(lock));
        }
        log("returning deferredResult");

        // Return the deferredResult before the locks
        // are obtained.
        return deferredResult;
    }

    /**
     * This helper method tries to acquire a {@link Lock}.
     *
     * @param acquiredLocks The {@link List} of {@link Lock} objects
     *                      we're trying to acquire
     * @return The number of {@link Lock} objects in {@code
     *         acquiredLocks}
     */
    private Integer tryAcquireLock(List<Lock> acquiredLocks) {
        // Perform a non-blocking poll().
        var lock = mAvailableLocks.poll();

        if (lock == null) {
            // Not enough locks available, so release
            // the acquired locks;.
            acquiredLocks
                .forEach(mAvailableLocks::offer);

            // Clear out the acquiredLocks List.
            acquiredLocks.clear();

            // Indicate that we need to start from the beginning.
            return 0;
        }
        // Add the acquired lock to the List.
        acquiredLocks.add(lock);

        // Return the number of locks acquired.
        return acquiredLocks.size();
    }

    /**
     * Release the {@link Lock} so other Beings can acquire it.
     *
     * @param lock The {@link Lock} to release
     */
    public void release(Lock lock) {
        log("LockService.release() " + lock);
        // Put the Lock parameter back to the queue.
        mAvailableLocks.offer(lock);
    }

    /**
     * Release the {@code locks}.
     *
     * @param locks A {@link List} that contains {@link Lock}
     *              objects to release
     */
    public void release(List<Lock> locks) {
        log("LockService.release(locks) "
            + locks);

        locks
            // Put all locks back in the queue.
            .forEach(mAvailableLocks::offer);
    }
}
