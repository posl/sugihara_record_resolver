/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.util.concurrent.CountDown;
import org.elasticsearch.core.AbstractRefCounted;
import org.elasticsearch.core.RefCounted;
import org.elasticsearch.core.Releasable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A mechanism to trigger an action on the completion of some (dynamic) collection of other actions. Basic usage is as follows:
 *
 * <pre>
 * try (var refs = new RefCountingRunnable(finalRunnable)) {
 *     for (var item : collection) {
 *         runAsyncAction(item, refs.acquire()); // releases the acquired ref on completion
 *     }
 * }
 * </pre>
 *
 * The delegate action is completed when execution leaves the try-with-resources block and every acquired reference is released. Unlike a
 * {@link CountDown} there is no need to declare the number of subsidiary actions up front (refs can be acquired dynamically as needed) nor
 * does the caller need to check for completion each time a reference is released. Moreover even outside the try-with-resources block you
 * can continue to acquire additional listeners, even in a separate thread, as long as there's at least one listener outstanding:
 *
 * <pre>
 * try (var refs = new RefCountingRunnable(finalRunnable)) {
 *     for (var item : collection) {
 *         if (condition(item)) {
 *             runAsyncAction(item, refs.acquire());
 *         }
 *     }
 *     if (flag) {
 *         runOneOffAsyncAction(refs.acquire());
 *         return;
 *     }
 *     for (var item : otherCollection) {
 *         var itemRef = refs.acquire(); // delays completion while the background action is pending
 *         executorService.execute(() -> {
 *             try (var ignored = itemRef) {
 *                 if (condition(item)) {
 *                     runOtherAsyncAction(item, refs.acquire());
 *                 }
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * In particular (and also unlike a {@link CountDown}) this works even if you don't acquire any extra refs at all: in that case, the
 * delegate action executes at the end of the try-with-resources block.
 */
public final class RefCountingRunnable implements Releasable {

    private static final Logger logger = LogManager.getLogger(RefCountingRunnable.class);
    static final String ALREADY_CLOSED_MESSAGE = "already closed, cannot acquire or release any further refs";

    private final RefCounted refCounted;
    private final AtomicBoolean originalRefReleased = new AtomicBoolean();

    private class AcquiredRef implements Releasable {
        private final AtomicBoolean released = new AtomicBoolean();

        @Override
        public void close() {
            releaseRef(released);
        }

        @Override
        public String toString() {
            return RefCountingRunnable.this.toString();
        }
    }

    /**
     * Construct a {@link RefCountingRunnable} which executes {@code delegate} when all refs are released.
     * @param delegate The action to execute when all refs are released. This action must not throw any exception.
     */
    public RefCountingRunnable(Runnable delegate) {
        this.refCounted = AbstractRefCounted.of(delegate);
    }

    /**
     * Acquire a reference to this object and return an action which releases it. The delegate {@link Runnable} is called when all its
     * references have been released.
     *
     * Callers must take care to close the returned resource exactly once. This deviates from the contract of {@link java.io.Closeable}.
     */
    public Releasable acquire() {
        if (refCounted.tryIncRef()) {
            return new AcquiredRef();
        }
        assert false : ALREADY_CLOSED_MESSAGE;
        throw new IllegalStateException(ALREADY_CLOSED_MESSAGE);
    }

    /**
     * Acquire a reference to this object and return a listener which releases it when notified. The delegate {@link Runnable} is called
     * when all its references have been released.
     */
    public ActionListener<Void> acquireListener() {
        return ActionListener.releasing(acquire());
    }

    /**
     * Release the original reference to this object, which executes the delegate {@link Runnable} if there are no other references.
     *
     * Callers must take care to close this resource exactly once. This deviates from the contract of {@link java.io.Closeable}.
     */
    @Override
    public void close() {
        releaseRef(originalRefReleased);
    }

    private void releaseRef(AtomicBoolean released) {
        if (released.compareAndSet(false, true)) {
            try {
                refCounted.decRef();
            } catch (Exception e) {
                logger.error("exception in delegate", e);
                assert false : e;
            }
        } else {
            assert false : "already closed";
        }
    }

    @Override
    public String toString() {
        return refCounted.toString();
    }

}
