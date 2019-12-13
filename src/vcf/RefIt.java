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
package vcf;

import blbutil.FileIt;
import blbutil.Filter;
import blbutil.MultiThreadUtils;
import blbutil.SampleFileIt;
import blbutil.Utilities;
import bref.SeqCoder3;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * <p>Class {@code RefIt} represents  an iterator whose {@code next()}
 * method returns an object storing data from a VCF record with
 * phased, non-missing genotypes.
 * </p>
 * <p>Instances of class {@code RefIt} are not thread-safe.
 * </p>
 * <p>Methods of this class will terminate the Java Virtual Machine with
 * an error message if an I/O error or file format error is detected.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class RefIt implements SampleFileIt<RefGTRec> {

    /**
     * The default number of {@code GTRec} objects that are
     * stored in a buffer.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1<<10;

    private final VcfHeader vcfHeader;
    private final FileIt<String> it;
    private final Function<String, RefGTRec> mapper;
    private final Filter<Marker> markerFilter;

    private int lastChrom;
    private boolean eof = false;

    private final ArrayBlockingQueue<String[]> stringBuffer;
    private final List<RefGTRec> midBuffer;
    private final Deque<RefGTRec> recBuffer;
    private final SeqCoder3 seqCoder;
    private final int maxSeqCodedAlleles;
    private final int maxSeqCodingMajorCnt;

    private final ExecutorService es;

    /**
     * Create and returns a new {@code RefIt} instance from the specified
     * iterator.
     * @param strIt an iterator that returns lines of a VCF file
     * @return a new {@code RefIt} instance
     * @throws IllegalArgumentException if a format error is detected in a
     * line of a VCF file returned by {@code strIt}
     * @throws NullPointerException if {@code strIt == null}
     */
    public static RefIt create(FileIt<String> strIt) {
        return RefIt.create(strIt, Filter.acceptAllFilter(),
                Filter.acceptAllFilter(), DEFAULT_BUFFER_SIZE);
    }

    /**
     * Create and returns a new {@code RefIt} instance from the specified
     * objects.
     * @param it an iterator that returns lines of a VCF file
     * @param sampleFilter a sample filter or {@code null}
     * @param markerFilter a marker filter or {@code null}
     * @return a new {@code RefIt} instance
     * @throws IllegalArgumentException if a format error is detected in a
     * line of a VCF file returned by {@code strItt}
     * @throws IllegalArgumentException if {@code bufferSize < 1}
     * @throws NullPointerException if {@code strIt == null}
     */
    public static RefIt create(FileIt<String> it, Filter<String> sampleFilter,
            Filter<Marker> markerFilter) {
        return new RefIt(it, sampleFilter, markerFilter, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Create and returns a new {@code RefIt} instance from the specified
     * objects.
     * @param it an iterator that returns lines of a VCF file
     * @param sampleFilter a sample filter or {@code null}
     * @param markerFilter a marker filter or {@code null}
     * @param bufferSize the number of VCF records stored in a buffer
     * @return a new {@code RefIt} instance
     * @throws IllegalArgumentException if a format error is detected in a
     * line of a VCF file returned by {@code strItt}
     * @throws IllegalArgumentException if {@code bufferSize < 1}
     * @throws NullPointerException if {@code strIt == null}
     */
    public static RefIt create(FileIt<String> it, Filter<String> sampleFilter,
            Filter<Marker> markerFilter, int bufferSize) {
        return new RefIt(it, sampleFilter, markerFilter, bufferSize);
    }

    private RefIt(FileIt<String> it, Filter<String> sampleFilter,
            Filter<Marker> markerFilter, int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException(String.valueOf(bufferSize));
        }
        if (markerFilter==null) {
            markerFilter = Filter.acceptAllFilter();
        }
        String src = it.file()==null ? "stdin" : it.file().getName();
        String[] head = VcfIt.head(src, it);
        String[] nonDataLines = Arrays.copyOf(head, head.length-1);
        String firstDataLine = head[head.length-1];
        boolean[] isDiploid = VcfHeader.isDiploid(firstDataLine);
        this.it = it;
        this.vcfHeader = new VcfHeader(src, nonDataLines, isDiploid, sampleFilter);
        this.mapper = (String s) -> {
            return RefGTRec.alleleCodedInstance(new VcfRecGTParser(vcfHeader, s));
        } ;
        this.markerFilter = markerFilter;
        this.seqCoder = new SeqCoder3(vcfHeader.samples());
        this.maxSeqCodedAlleles = Math.min(seqCoder.maxNSeq(), SeqCoder3.MAX_NALLELES);
        this.maxSeqCodingMajorCnt = maxSeqCodingMajorCnt(vcfHeader.samples());

        this.lastChrom = -1;
        this.stringBuffer = new ArrayBlockingQueue<>(1);
        this.midBuffer = new ArrayList<>();
        this.recBuffer = new ArrayDeque<>(bufferSize);

        this.es = Executors.newSingleThreadExecutor();
        startFileReadingThread(es, stringBuffer, firstDataLine, it, bufferSize);
        fillEmissionBuffer();
    }

    private int maxSeqCodingMajorCnt(Samples samples) {
        int nHaps = samples.nSamples() << 1;
        return (int) Math.floor(nHaps*SeqCoder3.COMPRESS_FREQ_THRESHOLD - 1);
    }

    private static void startFileReadingThread(ExecutorService es,
            ArrayBlockingQueue<String[]> q, String firstRec, FileIt<String> it,
            int bufferSize) {
        es.submit(() -> {
            List<String> recs = new ArrayList<>(bufferSize);
            recs.add(firstRec);
            while (it.hasNext()) {
                if (recs.size()==bufferSize) {
                    String[] sa = recs.toArray(new String[0]);
                    MultiThreadUtils.putInBlockingQ(q, sa);
                    recs.clear();
                }
                recs.add(it.next());
            }
            try {
                assert recs.size()>0;
                q.put(recs.toArray(new String[0]));
                q.put(new String[0]);
                recs.clear();
            } catch (InterruptedException ex) {
                Utilities.exit("ERROR: " + it.file(), ex);
            }
        });
    }

    private void fillEmissionBuffer() {
        assert recBuffer.isEmpty();
        boolean finished = false;
        while (recBuffer.isEmpty() && finished==false) {
            String[] sa = MultiThreadUtils.takeFromBlockingQ(stringBuffer);
            if (sa.length==0) {
                finished = true;
                stringBuffer.add(sa); // put sentinal back in qeueue
            }
            else {
                RefGTRec[] recs = parseLines(sa);
                for (int j=0; j<recs.length; ++j) {
                    RefGTRec rec = recs[j];
                    int chrom = rec.marker().chromIndex();
                    if (lastChrom == -1) {
                        lastChrom = chrom;
                    }
                    if (chrom!=lastChrom || midBuffer.size()==Integer.MAX_VALUE) {
                        flushMidBufferToRecBuffer();
                        lastChrom = chrom;
                    }
                    if (applySeqCoding(rec)==false) {
                        midBuffer.add(rec);
                    }
                    else {
                        boolean success = seqCoder.add(rec);
                        if (success == false) {
                            flushMidBufferToRecBuffer();
                            success = seqCoder.add(rec);
                            assert success;
                        }
                        midBuffer.add(null);
                    }
                }
            }
        }
        if (finished) {
            flushMidBufferToRecBuffer();
            eof = true;
        }
    }

    private RefGTRec[] parseLines(String[] lines) {
        return Arrays.stream(lines)
                .parallel()
                .map(mapper)
                .filter(e -> markerFilter.accept(e.marker()))
                .toArray(RefGTRec[]::new);
    }

    private void flushMidBufferToRecBuffer() {
        List<RefGTRec> list = seqCoder.getCompressedList();
        int index = 0;
        for (int j=0, n=midBuffer.size(); j<n; ++j) {
            GTRec ve = midBuffer.get(j);
            if (ve==null) {
                midBuffer.set(j, list.get(index++));
            }
        }
        recBuffer.addAll(midBuffer);
        midBuffer.clear();
    }

    @Override
    public void close() {
        it.close();
        recBuffer.clear();
        midBuffer.clear();
        // empty string buffer in order to stop ExecutorService
        String[] sa = MultiThreadUtils.takeFromBlockingQ(stringBuffer);
        while (sa.length>0) {
            sa = MultiThreadUtils.takeFromBlockingQ(stringBuffer);
        }
        MultiThreadUtils.shutdownExecService(es);
    }

    /**
     * Returns {@code true} if the iteration has more elements, and returns
     * {@code false} otherwise.
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return !recBuffer.isEmpty();
    }

    /**
     * Returns the next element in the iteration.
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public RefGTRec next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        RefGTRec first = recBuffer.removeFirst();
        if (recBuffer.isEmpty() && eof==false) {
            fillEmissionBuffer();
        }
        return first;
    }

    /**
     * The {@code remove} method is not supported by this iterator.
     * @throws UnsupportedOperationException if this method is invoked
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(this.getClass().toString());
    }

    @Override
    public File file() {
        return it.file();
    }

    @Override
    public Samples samples() {
        return vcfHeader.samples();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        sb.append(this.getClass().toString());
        sb.append(" : ");
        sb.append(it.file()==null ? "stdin" : it.file().toString());
        return sb.toString();
    }

    private boolean applySeqCoding(RefGTRec rec) {
        assert rec.isAlleleCoded();
        if (rec.marker().nAlleles() >= maxSeqCodedAlleles) {
            return false;
        }
        int nHaps = rec.size();
        int majAllele = rec.majorAllele();
        int majCnt = nHaps;
        for (int a=0, n=rec.nAlleles(); a<n; ++a) {
            if (a!=majAllele) {
                majCnt -= rec.alleleCount(a);
            }
        }
        return majCnt<=maxSeqCodingMajorCnt;
    }
}
