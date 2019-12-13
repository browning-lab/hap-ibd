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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * <p>Class {@code BGZIPOutputStream} is an output stream filter that performs
 * BGZIP compression.
 * </p>
 * <p>The GZIP file format specification is described
 * <a href="https://www.ietf.org/rfc/rfc1952.txt">RFC 1952</a>
 * and the BGZIP file format specification is described in the
 * <a href="https://samtools.github.io/hts-specs/SAMv1.pdf">
 * Sequence Alignment/Map Format Specification</a>
 * </p>
 * <p>Instances of class {@code BGZIPOutputStream} are not thread safe.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class BGZIPOutputStream extends OutputStream {

    // overhead bytes: 26 w/compression, 31 w/o compression
    private static final boolean USE_GZIP = true;
    private static final int NOCOMPRESS_XTRA_BYTES = 5;
    private static final int MAX_INPUT_BYTES = (1 << 16) - 31;
    private static final int MAX_OUTPUT_BYTES =
            (MAX_INPUT_BYTES + NOCOMPRESS_XTRA_BYTES);

    private final boolean writeEmptyBlock;
    private final Deflater gzipDef;
    private final CRC32 crc;
    private final OutputStream os;

    private final byte[] input = new byte[MAX_INPUT_BYTES];
    private final byte[] output = new byte[MAX_OUTPUT_BYTES + 1];

    private int iSize = 0;

    /**
     * Applies BGZIP compression on the specified files. The filename of
     * each compressed file will be the original filename followed by ".gz".
     * The original files are not deleted or overwritten.  The program
     * exits with an error message if any input filename ends with ".gz".
     *
     * @param args a list of files that will be compressed
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            Utilities.exit("usage: java -ea -jar bgzip.jar [file1] [file2] ...");
        }
        for (String name : args) {
            if (name.endsWith(".gz")) {
                Utilities.exit("ERROR: input filename ends in \".gz\"");
            }
            File inFile = new File(name);
            File outFile = new File(name + ".gz");
            try (InputStream is = new FileInputStream(inFile)) {
                try (FileOutputStream fos = new FileOutputStream(outFile);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        BGZIPOutputStream os = new BGZIPOutputStream(bos, true)) {
                    byte[] ba = new byte[2000];
                    int len = is.read(ba);
                    while (len != -1) {
                        os.write(ba, 0, len);
                        len = is.read(ba);
                    }
                }
            }
        }
    }

    /**
     * Creates a new {@code BGZIPOutputStream} instance that writes
     * to the specified output stream.
     *
     * @param os the output stream
     * @param writeEmptyBlock {@code true} if the {@code close()} method will
     * write an empty BGZIP block to the end of the stream
     *
     * @throws NullPointerException if {@code os == null}
     */
    public BGZIPOutputStream(OutputStream os, boolean writeEmptyBlock) {
        if (os==null) {
            throw new NullPointerException(OutputStream.class.toString());
        }
        this.writeEmptyBlock = writeEmptyBlock;
        this.gzipDef = new Deflater(Deflater.DEFAULT_COMPRESSION, USE_GZIP);
        this.os = os;
        this.crc = new CRC32();
        assert crc.getValue()==0;
    }

    private void compressAndFlushBuffer() throws IOException {
        crc.update(input, 0, iSize);
        int crc32 = (int) crc.getValue();

        gzipDef.setInput(input, 0, iSize);
        gzipDef.finish();
        int len = gzipDef.deflate(output, 0, output.length, Deflater.SYNC_FLUSH);
        if (len > MAX_OUTPUT_BYTES) {
            len = setOutputNoCompression();
        }
        writeBgzipBlock(iSize, crc32, output, len, os);
        gzipDef.reset();
        crc.reset();
        iSize = 0;
    }

    /* Returns the number of bytes written to the output array */
    private int setOutputNoCompression() {
        output[0] = 1;
        output[1] = (byte) (iSize & 0xff);
        output[2] = (byte) ((iSize >> 8) & 0xff);
        output[3] = (byte) (~output[1]);
        output[4] = (byte) (~output[2]);
        System.arraycopy(input, 0, output, 5, iSize);
        return iSize + 5;
    }

    private static void writeBgzipBlock(int iSize, int crc32,
            byte[] out, int outLength, OutputStream os) throws IOException {
        if (iSize > (1<<16)) {
            throw new IllegalArgumentException(String.valueOf(iSize));
        }
        writeBGZIPHeader(outLength, os);
        os.write(out, 0, outLength);
        writeInt(crc32, os);
        writeInt(iSize, os);
    }

    private static void writeBGZIPHeader(int nCompressedBytes, OutputStream os)
            throws IOException {
        int bsize = nCompressedBytes + 25;
        if ((bsize >> 16) != 0) {
            throw new IllegalArgumentException(String.valueOf(nCompressedBytes));
        }
        byte[] ba = new byte[]
            {   31, (byte) 139,     // GZIP_MAGIC
                Deflater.DEFLATED,  // CM
                4,                  // FLG
                0, 0, 0, 0,         // MTIME
                0,                  // XFL
                (byte) 255,         // OS
                6, 0,               // XLEN
                66, 67,             // BGZIP_SUBFIELD_MAGIC
                2, 0,               // SLEN  (Subfield LENgth)
                (byte)(bsize & 0xff),
                (byte)((bsize >> 8) & 0xff) // BGZIP block size - 1
            };
        os.write(ba);
    }

    private static void writeInt(int i, OutputStream os) throws IOException {
        os.write((byte)(i & 0xff));
        os.write((byte)((i >> 8) & 0xff));
        os.write((byte)((i >> 16) & 0xff));
        os.write((byte)((i >> 24) & 0xff));
    }

    @Override
    public void write(int b) throws IOException {
        input[iSize++] = (byte) b;
        if (iSize==MAX_INPUT_BYTES) {
            compressAndFlushBuffer();
        }
    }

    @Override
    public void write(byte[] ba) throws IOException {
        write(ba, 0, ba.length);
    }

    @Override
    public void write(byte[] buf, int off, int len)
            throws IOException {
        int availSize = input.length - iSize;
        while ((len - off) >= availSize) {
            System.arraycopy(buf, off, this.input, iSize, availSize);
            iSize += availSize;
            off += availSize;
            len -= availSize;
            assert iSize==MAX_INPUT_BYTES;
            compressAndFlushBuffer();
            assert iSize==0;
            availSize = this.input.length - iSize;
        }
        if (len>0) {
            System.arraycopy(buf, off, this.input, iSize, len);
            iSize += len;
        }
    }

    @Override
    public void flush() throws IOException {
        compressAndFlushBuffer();
        os.flush();
    }

    @Override
    public void close() throws IOException {
        if (iSize > 0) {
            compressAndFlushBuffer();
        }
        if (writeEmptyBlock) {
            compressAndFlushBuffer();
        }
        os.close();
    }

}
