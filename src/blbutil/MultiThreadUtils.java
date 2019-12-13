/*
 * Copyright 2019 Brian L. Browning
 *
 * This file is part of the HapIbd program.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blbutil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class {@code Utilities} contains miscellaneous static utility methods
 * for multi-threaded programming.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class MultiThreadUtils {

    private MultiThreadUtils() {
        // private constructor to prevent instantiation
    }

    /**
     * Inserts the specified element at the tail of the specified blocking
     * queue, waiting for space to become available if the queue is full.
     * The Java Virtual Machine is terminated if an {@code InterruptedException}
     * is thrown while waiting for space to be come available in the queue.
     * @param <E> the element type
     * @param q a blocking queue
     * @param e the element to add
     * @throws NullPointerException if {@code q == null || e == null}
     */
    public static <E> void putInBlockingQ(BlockingQueue<E> q, E e) {
        try {
            q.put(e);
        } catch (InterruptedException ex) {
            Utilities.exit("ERROR: " , ex);
        }
    }

    /**
     * Inserts the specified element at the tail of the specified blocking
     * queue, waiting up to the specified time for space to become available
     * if the queue is full.
     * The Java Virtual Machine is terminated if an {@code InterruptedException}
     * is thrown while waiting for space to be come available in the queue.
     * @param <E> the element type
     * @param q a blocking queue
     * @param e the element to add
     * @param timeout the number of time units to wait before giving up
     * @param unit the time unit
     * @return {@code true} if element was added to the queue, and
     * false otherwise
     * @throws NullPointerException if
     * {@code q == null || e == null || unit == null}
     */
    public static <E> boolean putInBlockingQ(BlockingQueue<E> q, E e,
            long timeout, TimeUnit unit) {
        try {
            return q.offer(e, timeout, unit);
        } catch (InterruptedException ex) {
            Utilities.exit("ERROR: " , ex);
        }
        return false;
    }

    /**
     * Removes and returns the element at the head of the specified blocking
     * queue, waiting if necessary for an element to become available.
     * The Java Virtual Machine is terminated if an {@code InterruptedException}
     * is thrown while waiting for space to be come available in the queue.
     * @param <E> the element type
     * @param q a blocking queue
     * @return the element at the head of the queue
     */
    public static <E> E takeFromBlockingQ(BlockingQueue<E> q) {
        try {
            return q.take();
        } catch (InterruptedException ex) {
            Utilities.exit("ERROR: " , ex);
        }
        assert false;
        return null;
    }

    /**
     * Blocks the current thread until the specified {@code CountDownLatch}
     * has counted down to 0. The Java Virtual Machine is terminated if an
     * {@code InterruptedException} is thrown while waiting for for the
     * {@code CountDownLatch} to count down to 0.
     * @param latch the count down latch
     * @throws NullPointerException if {@code latch == null}
     */
    public static void await(CountDownLatch latch) {
        try {
            latch.await();
        }
        catch (InterruptedException e) {
            Utilities.exit("ERROR", e);
        }
    }

    /**
     * Shuts down and awaits termination of the specified
     * {@code ExecutorService}. The Java Virtual Machine is terminated if an
     * {@code InterruptedException} is thrown while awaiting termination
     * of the executor service.
     * @param es the executor service to be shut down
     * @throws NullPointerException if {@code es == null}
     */
    public static void shutdownExecService(ExecutorService es) {
        try {
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }
        catch (InterruptedException e) {
            Utilities.exit("ERROR", e);
        }
    }

}

