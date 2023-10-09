package utils;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * This class implements a "timed-memoizing" cache that maps a key to
 * the value produced by a function.  If a value has previously been
 * computed it is returned rather than calling the function to compute
 * it again. The Java ConcurrentHashMap.computeIfAbsent() method is
 * used to ensure only a single call to the function is run when a
 * key/value pair is first added to the cache.  The Java
 * ScheduledThreadExecutorService is used in a "one-shot" manner
 * together with Java AtomicLong and ConcurrentHashMap.remove() to
 * limit the amount of time a key/value is retained in the cache.
 *
 * More information on memoization is available at
 * https://en.wikipedia.org/wiki/Memoization.
 */
public class TimedMemoizer<K, V>
       extends Memoizer<K, V> {
    /**
     * Debugging tag used by the logger.
     */
    private final String TAG = getClass().getSimpleName();

    /**
     * A map associating a key K w/a value V produced by a function.
     * A RefCountedValue is used to keep track of how many times a
     * key/value pair is accessed during mTimeoutInMillisecs period.
     */
    private final ConcurrentHashMap<K, RefCountedValue> mCache;

    /**
     * The amount of time to retain a value in the cache.
     */
    private final long mTimeoutInMillisecs;

    /**
     * Executes a runnable after a given timeout to remove expired
     * keys.
     */
    private ScheduledExecutorService mScheduledExecutorService;

    /**
     * Track # of times a key is referenced within
     * mTimeoutInMillisecs.
     */
    private class RefCountedValue {
        /**
         * Track # of times a key is referenced within
         * mTimeoutInMillisecs.
         */
        final AtomicLong mRefCount;

        /**
         * The value that's being reference counted.
         */
        final V mValue;

        /**
         * Constructor initializes the fields.
         */
        RefCountedValue(V value, long initialCount) {
            mValue = value;
            mRefCount = new AtomicLong(initialCount);
        }

        /**
         * Increment the ref count atomically and return the value.
         */
        V incrementAndGet() {
            // Increment ref count atomically.
            mRefCount.incrementAndGet();

            // Return the value;
            return mValue;
        }

        /**
         * @return Return the value.
         */
        V get() {
            // Return the value;
            return mValue;
        }

        /**
         * This method is used by the ConcurrentHashMap.remove()
         * method to determine if an key/value pair hasn't been
         * accessed during mTimeoutInMillisecs.
         *
         * @return true if the ref counts are equal, else false.
         */
        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass())
                return false;
            else {
                @SuppressWarnings("unchecked")
                final RefCountedValue t = (RefCountedValue) obj;
                // Objects are equal if their ref counts are equal!
                return mRefCount.get() == t.mRefCount.get();
            }
        }

        /**
         * Use the ScheduledExecutorService to schedule a runnable
         * that removes {@code key} from the cache if its timeout
         * expires and it hasn't been accessed in mTimeoutInMillisecs.
         */
        void schedule(K key) {
            // Runnable that checks if the cached entry became "stale"
            // (i.e., not accessed within mTimeoutInMillisecs) and if
            // so will remove that entry.
            Runnable removeIfStale = new Runnable() {
                    @Override
                    public void run() {
                        // Store the current ref count.
                        long oldCount = mRefCount.get();

                        // Remove the key only if it hasn't been
                        // accessed in mTimeoutInMillisecs.
                        if (mCache.remove(key,
                                          mNonAccessedValue)) {
                            Options.debug(TAG,
                                          "key "
                                          + key
                                          + " removed from cache (with size "
                                          + mCache.size()
                                          + ") since it wasn't accessed recently");
                        } else {
                            Options.debug(TAG,
                                          "key "
                                          + key
                                          + " NOT removed from cache (with size "
                                          + mCache.size()
                                          + ") since it was accessed recently (with ref count "
                                          + mRefCount.get()
                                          + ") and ("
                                          + mNonAccessedValue.mRefCount.get()
                                  + ")");

                            assert(mCache.get(key) != null);

                            // Try to reset ref count to 1 so it won't
                            // be considered as accessed (yet).  Don't
                            // reset it to 1, however, if ref count
                            // increased between remove() and here.
                            mRefCount
                                .getAndUpdate(curCount -> 
                                              curCount > oldCount ? curCount : 1);

                            // Reschedule this runnable to run again
                            // in mTimeoutInMillisecs.
                            mScheduledExecutorService.schedule
                                (this,
                                 mTimeoutInMillisecs,
                                 TimeUnit.MILLISECONDS);
                        }
                    }
                };

            // Initially schedule runnable to execute after
            // mTimeoutInMillisecs.
            mScheduledExecutorService.schedule
                (removeIfStale,
                 mTimeoutInMillisecs,
                 TimeUnit.MILLISECONDS);
        }
    }

    /**
     * A ref count of 1 is used to check if a key's not been accessed
     * in mTimeoutInMillisecs.
     */
    private final RefCountedValue mNonAccessedValue =
            new RefCountedValue(null, 1);

    /**
     * Constructor initializes the fields.
     */
    public TimedMemoizer(Function<K, V> function,
                         long timeoutInMillisecs) {
        // Initialize the super class.
        super(function);

        // Do some sanity checking.
        if (timeoutInMillisecs <= 0)
            throw new IllegalArgumentException("timeoutInMillisecs must be great than 0");

        // Store the timeout for subsequent use.
        mTimeoutInMillisecs = timeoutInMillisecs;

        // Create a concurrent hash map.
        mCache = new ConcurrentHashMap<>();

        // Create a ScheduledThreadPoolExecutor with one thread.
        mScheduledExecutorService = 
            new ScheduledThreadPoolExecutor
                    (1,
                    // Make thread a daemon so it shuts down automatically!
                    r -> {
                        Thread t = new Thread(r, "reaper");
                        t.setDaemon(true);
                        return t;
                    });

        // Get an object to set policies that clean everything up on
        // shutdown.
        ScheduledThreadPoolExecutor exec =
            (ScheduledThreadPoolExecutor) mScheduledExecutorService;

        // Remove scheduled runnables on cancellation.
        exec.setRemoveOnCancelPolicy(true);

        // Disable periodic tasks at shutdown.
        exec.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

        // Disable delayed tasks at shutdown.
        exec.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    /**
     * Returns the value associated with the key in cache.  If there
     * is no value associated with the key then the function is called
     * to create the value and store it in the cache before returning
     * it.  A key/value entry will be purged from the cache if it's
     * not used within the timeout passed to the constructor.
     */
    public V apply(K key) {
        // The mapping function atomically computes the value
        // associated with the key and returns a unique
        // RefCountedValue containing this value.
        Function<K, RefCountedValue> mappingFunction = k -> {
            // Apply the function and store the result.
            RefCountedValue rcv = new RefCountedValue(mFunction.apply(k),
                                                      0);

            // Bail-out if we're interrupted.
            if (!Thread.currentThread().isInterrupted())
                // Schedule a runnable that removes key from the cache
                // if its timeout expires and it hasn't been accessed
                // in mTimeoutInMillisecs.
                rcv.schedule(key);

            // Return the ref-counted value.
            return rcv;
        };

        // Return the value associated with the key.
        return mCache
            // Try to find key in cache.  If key isn't present then
            // use the mapping function defined above to compute it.
            .computeIfAbsent(key, 
                             // The mappingFunction *must* be called
                             // here so it's protected by a
                             // ConcurrentHashMap synchronizer.
                             mappingFunction)

            // Return the value of the RefCountedValue, which
            // increments its ref count atomically.
            .incrementAndGet();
    }

    /**
     * Removes the key (and its corresponding value) from this
     * memoizer.  This method does nothing if the key is not in the
     * map.
     *
     * @param key The key to remove
     * @ @return The previous value associated with key, or null if
     * there was no mapping for key.
     */
    public V remove(K key) {
        return mCache.remove(key).get();
    }

    /**
     * @return The number of keys in the cache.
     */
    public long size() {
        return mCache.size();
    }

    /**
     * @return A map containing the key/value entries in the cache.
     */
    public Map<K, V> getCache() {
        // Create a new concurrent hash map.
        ConcurrentHashMap<K, V> cacheCopy =
                new ConcurrentHashMap<>();

        // Copy the contents of the cache into the new map.
        mCache.forEach((k, v) -> cacheCopy.put(k, v.get()));

        // Return the copy.
        return cacheCopy;
    }

    /**
     * Shutdown the TimedMemoizer and remove all the entries from its
     * ScheduledExecutorService.
     */
    public void shutdown() {
        // Shutdown the ScheduledExecutorService.
        mScheduledExecutorService.shutdownNow();
        mScheduledExecutorService = null;

        // Remove all the keys/values in the map.
        mCache.clear();
    }
}
