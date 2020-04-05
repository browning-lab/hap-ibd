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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * <p>Class {@code BlockLineReader} is a {@code blbutil.FileIt} that reads
 * blocks of lines from a file.  The order of lines in the source file is
 * preserved by the returned string arrays. The {@code hasNext()} method
 * always returns {@code true}. After the final block of lines is returned
 * by the {@code next()} method, the {@code next()} method returns
 * {@code BlockLineReader.SENTINAL} on all subsequent invocations.
 * {@code BlockLineReader.SENTINAL} is guaranteed to be the only returned
 * array that has length 0.
 *
 * <p>Instances of class {@code BlockLineReader} are thread-safe.</p>
 *
 * <p>Methods of this class will terminate the Java Virtual Machine with
 * an error message if an I/O Exception is encountered.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class BlockLineReader implements FileIt<String[]> {

    /**
     * The string array returned by {@code next()} after all blocks
     * of lines have been read.
     */
    public static String[] SENTINAL = new String[0];

    private final FileIt<String> it;
    private final LinkedBlockingDeque<String[]> q;
    private final int blockSize;
    private final ExecutorService fileReaderService;
    private final CountDownLatch latch;

    private volatile boolean shutDownNow;

    /**
     * Constructs and returns a new {@code BlockLineReader} for the specified
     * data. The {@code close()} method of the returned object will invoke the
     * {@code close()} method on the specified {@code FileIt<String>} iterator.
     * The calling thread should not directly invoke any methods of the
     * specified {@code FileIt<String>} after it is passed to the
     * {@code BlockLineReader.create()} method.
     * @param it a file iterator that returns the lines of text
     * @param blockSize the maximum length a string array returned by
     * {@code next()}
     * @param nBlocks the maximum number of non-empty string arrays that will be
     * buffered
     * @return a {@code BlockLineReader} for the specified data.
     * @throws IllegalArgumentException if {@code blockSize < 1 || nBlocks < 1}
     * @throws NullPointerException if {@code it == null}
     */
    public static BlockLineReader create(FileIt<String> it, int blockSize,
            int nBlocks) {
        BlockLineReader reader = new BlockLineReader(it, blockSize, nBlocks);
        reader.startFileReadingThread();
        return reader;
    }

    private BlockLineReader(FileIt<String> it, int blockSize, int nBlocks) {
        if (blockSize<1) {
            throw new IllegalArgumentException(String.valueOf(blockSize));
        }
        if (nBlocks<1) {
            throw new IllegalArgumentException(String.valueOf(nBlocks));
        }
        this.it = it;
        this.q = new LinkedBlockingDeque<>(nBlocks);
        this.blockSize = blockSize;
        this.fileReaderService = Executors.newSingleThreadExecutor();
        this.latch = new CountDownLatch(1);
        this.shutDownNow = false;
    }

    private void startFileReadingThread() {
        fileReaderService.submit(() -> {
            try {
                List<String> buffer = new ArrayList<>(blockSize);
                while (it.hasNext()) {
                    buffer.add(it.next());
                    if (buffer.size()==blockSize) {
                        flushBuffer(buffer);
                        if (shutDownNow) {
                            break;
                        }
                    }
                }
                if (buffer.size()>0) {
                    flushBuffer(buffer);
                }
                latch.countDown();
                MultiThreadUtils.putInBlockingQ(q, SENTINAL);
            }
            catch (Throwable t) {
                Utilities.exit(t);
            }
        });
    }

    /*
     * Returns {@code false} if no more blocks of lines will be enqueued.
     */
    private void flushBuffer(List<String> buffer) {
        String[] sa = buffer.toArray(new String[0]);
        buffer.clear();

        boolean success = false;
        while (success==false && shutDownNow==false) {
            success = MultiThreadUtils.putInBlockingQ(q, sa, 100,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public File file() {
        return it.file();
    }

    @Override
    public void close() {
        shutDownNow = true;
        MultiThreadUtils.await(latch);
        it.close();
        String[] tail = q.pollLast();
        while (tail!=null && tail!=SENTINAL) {
            tail = q.pollLast();
        }
        if (tail==SENTINAL) {
            boolean success = q.offer(SENTINAL);
            assert success;
        }
        MultiThreadUtils.shutdownExecService(fileReaderService);
    }

    /**
     * Returns the next element in the iteration.
     * @return the next element in the iteration
     * @throws java.util.NoSuchElementException if the iteration has no more elements
     */
    @Override
    public boolean hasNext() {
        return true;
    }

    /**
     * Returns {@code true} if the iteration has more elements, and returns
     * {@code false} otherwise.
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public String[] next() {
        assert hasNext();
        String[] value = MultiThreadUtils.takeFromBlockingQ(q);
        if (value==SENTINAL) {
            boolean success = q.offer(SENTINAL);
            assert success;
        }
        return value;
    }

    /**
     * The {@code remove} method is not supported by this iterator.
     * @throws UnsupportedOperationException if this method is invoked
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(this.getClass().toString());
    }
}
