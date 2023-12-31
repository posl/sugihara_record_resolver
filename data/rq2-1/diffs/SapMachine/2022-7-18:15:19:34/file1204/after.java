/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Phaser;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8066859
 * @summary Check that AQS-based locks, conditions, and CountDownLatches do not fail when encountering OOME
 * @run main/othervm -XX:-UseGCOverheadLimit -Xmx24M -XX:-UseTLAB OOMEInAQS
 */

public class OOMEInAQS extends Thread {
    static final int NTHREADS = 2; // intentionally not a scalable test; > 2 is very slow
    static final int NREPS = 100;
    // statically allocate
    static final ReentrantLock mainLock = new ReentrantLock();
    static final Condition condition = mainLock.newCondition();
    static final CountDownLatch started = new CountDownLatch(1);
    static final CountDownLatch filled = new CountDownLatch(1);
    static volatile Object data;
    static int turn;

    /**
     * For each of NTHREADS threads, REPS times: Take turns
     * executing. Introduce OOM using fillHeap during runs.
     */
    public static void main(String[] args) throws Throwable {
        OOMEInAQS[] threads = new OOMEInAQS[NTHREADS];
        for (int i = 0; i < NTHREADS; ++i)
            (threads[i] = new OOMEInAQS(i)).start();
        started.countDown();
        long t0 = System.nanoTime();
        data = fillHeap();
        filled.countDown();
        long t1 = System.nanoTime();
        for (int i = 0; i < NTHREADS; ++i)
            threads[i].join();
        data = null;  // free heap before reporting and terminating
        System.gc();
        System.out.println(
            "fillHeap time: " + (t1 - t0) / 1000_000 +
            " millis, whole test time: " + (System.nanoTime() - t0) / 1000_000 +
            " millis"
        );
    }

    final int tid;
    OOMEInAQS(int tid) {
        this.tid = tid;
    }

    @Override
    public void run() {
        int id = tid, nextId = (id + 1) % NTHREADS;
        final ReentrantLock lock = mainLock;
        final Condition cond = condition;
        try {
            started.await();
            for (int i = 0; i < NREPS; i++) {
                try {
                    lock.lock();
                    while (turn != id)
                        cond.await();
                    turn = nextId;
                    cond.signalAll();
                } finally {
                    lock.unlock();
                }
                if (i == 2) // Subsequent AQS methods encounter OOME
                    filled.await();
            }
        } catch (Throwable ex) { // Could be InterruptedExeption or OOME
            data = null;
            System.exit(0); // avoid getting stuck trying to recover
        }
    }

    static Object[] fillHeap() {
        Object[] first = null, last = null;
        int size = 1 << 20;
        while (size > 0) {
            try {
                Object[] array = new Object[size];
                if (first == null) {
                    first = array;
                } else {
                    last[0] = array;
                }
                last = array;
            } catch (OutOfMemoryError oome) {
                size = size >>> 1;
            }
        }
        return first;
    }
}
