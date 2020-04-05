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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Class {@code SynchFileOutputStream} is a synchronized
 * wrapper for a buffered {@code java.io.FileOutputStream}.
 *
 * <p>Instances of class {@code SynchFileOutputStream} are thread-safe.</p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class SynchFileOutputStream implements AutoCloseable {

    private final OutputStream outStream;

    /**
     * Constructs a buffered {@code SynchFileOutputStream} that writes
     * bytes to the specified file.
     * @param file an output file that will be opened for writing
     *
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code file == null}
     */
    public SynchFileOutputStream(File file) throws IOException {
        this.outStream = new BufferedOutputStream(new FileOutputStream(file));
    }

    /**
     * Constructs a buffered {@code SynchFileOutputStream} that writes
     * bytes to the specified file.
     * @param file an output file that will be opened for writing
     * @param buffer the buffer size
     *
     * @throws IllegalArgumentException if {@code size <= 0}
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code file == null}
     */
    public SynchFileOutputStream(File file, int buffer) throws IOException {
        this.outStream = new BufferedOutputStream(
                new FileOutputStream(file), buffer);
    }

    /**
     * Writes the specified bytes to the buffered output stream.
     * @param ba an arrayof bytes to be written
     *
     * @throws IOException if an I/O error occurs
     */
    public synchronized void write(byte[] ba) throws IOException {
        outStream.write(ba);
    }

    /**
     * Flushes this output stream and causes any buffered output bytes
     * to are passed to the operating system for writing.
     *
     * @throws IOException if an I/O error occurs
     */
    public synchronized void flush() throws IOException {
        outStream.flush();
    }

    /**
     * Closes this {@code SynchFileOutputStream} and releases any
     * associated system resources.  A closed
     * {@code SynchFileOutputStream} cannot perform any output
     * operations and cannot be reopened.  If the
     * {@code SynchFileOutputStream} is already closed, then invoking
     * this method has no effect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public synchronized void close() throws IOException {
        outStream.close();
    }

    /**
     * Write an empty BGZIP block to this buffered {@code SynchFileOutputStream}.
     *
     * @throws IOException if an I/O error occurs
     */
    public synchronized void writeEmptyBgzipBlock() throws IOException {
        BGZIPOutputStream.writeEmptyBlock(outStream);
    }
}
