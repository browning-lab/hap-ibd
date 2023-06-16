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
package hapibd;

import beagleutil.PbwtUpdater;
import blbutil.BGZIPOutputStream;
import blbutil.Const;
import blbutil.SynchFileOutputStream;
import blbutil.Utilities;
import ints.IntArray;
import ints.IntList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import vcf.GT;
import vcf.MarkerMap;
import vcf.RefGT;
import vcf.Samples;

/**
 * <p>Instances of class {@code PbwtIbd} detect IBS segments in phased
 * genotype data.</p>
 *
 * <p>Instances of class {@code PbwtIbd} are not thread-safe</p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class PbwtIbd implements Runnable {

    private static final int BAOS_THRESHOLD = 1<<18;
    private static final int SEED_LIST_THRESHOLD = 1<<16;
    private static final String[] HAP_TO_STRING = new String[] {"1", "2"};
    private static final AtomicLong N_IBD_SEGS = new AtomicLong(0);
    private static final AtomicLong N_HBD_SEGS = new AtomicLong(0);
    private static final AtomicInteger FINISHED_CNT = new AtomicInteger(0);

    private final Samples samples;
    private final RefGT gt;
    private final String chrom;
    private final String[] ids;
    private final int nHaps;
    private final int lastMarker;
    private final int[] pos;
    private final double[] genPos;
    private final int windowStart;
    private final int windowEnd;
    private final float minSeed;
    private final int maxGap;
    private final float minExtend;
    private final float minOutput;
    private final int minSeedMarkersM1;
    private final int minExtendMarkersM1;

    private final PbwtUpdater pbwt;
    private final int[] a;
    private final int[] d;
    private final int[] alleles;
    private final boolean[] isDiploid;

    private final ByteArrayOutputStream hbdBaos
            = new ByteArrayOutputStream(3*BAOS_THRESHOLD/2 + 1);
    private final ByteArrayOutputStream ibdBaos
            = new ByteArrayOutputStream(3*BAOS_THRESHOLD/2 + 1);
    private final SynchFileOutputStream hbdOS;
    private final SynchFileOutputStream ibdOS;

    private PrintWriter hbdOut = printWriter(hbdBaos);
    private PrintWriter ibdOut = printWriter(ibdBaos);

    private boolean useSeedQ = false;
    private final int nWindows;
    private final IntList seedList;
    private final BlockingQueue<int[]> seedQ;

    private static PrintWriter printWriter(ByteArrayOutputStream out) {
        return new PrintWriter(new BGZIPOutputStream(out, false));
    }

    /**
     * Constructs an {@code PbwtIbd} instance for the specified data.
     * @param par the command line parameters
     * @param gt phased, non-missing genotype data
     * @param map the genetic map
     * @param windowStart the start marker index
     * @param windowEnd the end marker index (exclusive)
     * @param nWindows the number of windows
     * @param seedQ a queue to which arrays of seed segments will be stored
     * @param hbdOS an output stream to which compressed HBD segments will be written
     * @param ibdOS an output stream to which compressed IBD segments will be written
     * @throws IllegalArgumentException if {@code gt.isPhased() == false}
     * @throws IllegalArgumentException if
     * {@code windowStart < 0 || windowStart>=windowEnd || windowEnd >= gt.nMarkers()}
     * @throws IllegalArgumentException if
     * {@code gt.nMarkers() != map.genPos().size()}
     * @throws NullPointerException if
     * {@code (par == null || gt ==  null || map == null || hbdQ == null|| ibdQ == null}
     */
    public PbwtIbd(HapIbdPar par, RefGT gt, MarkerMap map,
            int windowStart, int windowEnd, int nWindows,
            BlockingQueue<int[]> seedQ,
            SynchFileOutputStream hbdOS, SynchFileOutputStream ibdOS) {
        if (gt.isPhased()==false) {
            throw new IllegalArgumentException("unphased data");
        }
        if (windowStart<0 || windowStart>=windowEnd || windowEnd>gt.nMarkers()) {
            throw new IllegalArgumentException(String.valueOf(windowEnd));
        }
        if (gt.nMarkers()!=map.genPos().size()) {
            throw new IllegalArgumentException("inconsistent data");
        }
        this.samples = gt.samples();
        this.gt = gt;
        this.chrom = gt.marker(0).chrom();
        this.ids = gt.samples().ids();
        this.nHaps = gt.nHaps();
        this.lastMarker = gt.nMarkers()-1;
        this.pos = IntStream.range(0, gt.nMarkers())
                .parallel()
                .map(i -> gt.marker(i).pos())
                .toArray();
        this.genPos = map.genPos().toArray();
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.minSeed = par.min_seed();
        this.maxGap = par.max_gap();
        this.minExtend = par.min_extend();
        this.minOutput = par.min_output();
        this.minSeedMarkersM1 = par.min_markers() - 1;
        this.minExtendMarkersM1 = (int) Math.floor((minExtend/minSeed)*par.min_markers()) - 1;
        this.nWindows = nWindows;
        this.seedList = new IntList(3*SEED_LIST_THRESHOLD/2 + 1);
        this.seedQ = seedQ;
        this.hbdOS = hbdOS;
        this.ibdOS = ibdOS;

        this.pbwt = new PbwtUpdater(nHaps);
        this.a = IntStream.range(0, nHaps).toArray();
        this.d = IntStream.range(0, nHaps).map(i -> windowStart).toArray();
        this.alleles = new int[nHaps];
        this.isDiploid = new boolean[nHaps];
    }

    /**
     * Runs IBD segment detection.
     */
    @Override
    public void run() {
        try {
            int maxIbsStart = windowStart;
            for (int m=advancePbwtToFirstIbsEnd(); m<windowEnd; ++m) {
                if ((m & 0b11)==0b11 && useSeedQ==false && FINISHED_CNT.get()>0) {
                    useSeedQ = true;
                }
                int nAlleles = gt.marker(m).nAlleles();
                pbwt.fwdUpdate(rec(gt, m), nAlleles, m, a, d);
                maxIbsStart = updateMaxIbsStart(m, maxIbsStart);
                storeSeedSegments(m, maxIbsStart);
            }
            FINISHED_CNT.incrementAndGet();
            int[] seeds;
            while (FINISHED_CNT.get()<nWindows || seedQ.peek()!=null) {
                while ((seeds=seedQ.poll(50, TimeUnit.MILLISECONDS))!=null) {
                    processSeedList(seeds);
                }
            }
            flushHbdBuffer(0);
            flushIbdBuffer(0);
            hbdOut.close();
            ibdOut.close();
        }
        catch (Throwable t) {
            Utilities.exit(t);
        }
    }

    private int advancePbwtToFirstIbsEnd() {
        int insPt = Arrays.binarySearch(genPos, windowStart, windowEnd,
                genPos[windowStart] + minSeed);
        int end = insPt<0 ? -insPt-1 : insPt;
        int minEnd = windowStart + minSeedMarkersM1;
        if (end<minEnd) {
            end = Math.min(minEnd, gt.nMarkers());
        }
        for (int m=windowStart; m<end; ++m) {
            int nAlleles = gt.marker(m).nAlleles();
            pbwt.fwdUpdate(rec(gt, m), nAlleles, m, a, d);
        }
//        assert (end - windowStart + 1) >= (minMarkersM1 + 1)
//                && (genPos[end] - genPos[windowStart]) >= minSeed;
//        assert ((end-1) - windowStart + 1) <  (minMarkersM1 + 1)
//                || (genPos[end-1] - genPos[windowStart]) < minSeed;
        return end;
    }

    private int updateMaxIbsStart(int m, int lastMaxIbsStart) {
        int maxIbsStart = lastMaxIbsStart;
        while ( ((genPos[m] - genPos[maxIbsStart+1]) >= minSeed)
                && ((m - maxIbsStart)>minSeedMarkersM1) ) {
            ++maxIbsStart;
        }
//        assert (m - maxIbsStart + 1) >= (minMarkersM1 + 1) &&
//                (genPos[m] - genPos[maxIbsStart]) >= minSeed;
//        assert (m - (maxIbsStart+1) + 1) < (minMarkersM1 + 1)
//                || (genPos[m] - genPos[maxIbsStart+1]) < minSeed;
        return maxIbsStart;
    }

    private void storeSeedSegments(int m, int maxIbsStart) {
        int indexStart = -1;
        for (int j=1; j<a.length; ++j) {
            if (d[j] <= maxIbsStart) {
                if (indexStart == -1) {
                    indexStart = j-1;
                }
            }
            else if (indexStart>=0) {
                storeSeeds(m, indexStart, j);
                indexStart = -1;
            }
        }
        if (indexStart>=0) {
            storeSeeds(m, indexStart, nHaps);
        }
        flushSeeds();
    }

    private void storeSeeds(int ibsInclEnd, int indexStart, int indexEnd) {
        boolean isPolymorphic = setAllelesAndIsDiploid(ibsInclEnd+1, indexStart, indexEnd);
        if (isPolymorphic) {
            for (int j=indexStart, n=indexEnd-1; j<n; ++j) {
                int a1 = alleles[j];
                if (isDiploid[j] || (a[j] & 0b1)==0) {
                    int ibsStart = Integer.MIN_VALUE;
                    for (int k=j+1; k<indexEnd; ++k) {
                        if (isDiploid[k] || (a[k] & 0b1)==0) {
                            ibsStart = Math.max(d[k], ibsStart);
                            if (ibsStart>windowStart || windowStart==0
                                    || gt.allele(ibsStart-1, a[j])!=gt.allele(ibsStart-1, a[k])) {
                                // seed not detected in preceding window
                                if (alleles[k]!=a1) {
                                    seedList.add(a[j]);
                                    seedList.add(a[k]);
                                    seedList.add(ibsStart);
                                    seedList.add(ibsInclEnd);
                                }
                                if (seedList.size()>SEED_LIST_THRESHOLD) {
                                    flushSeeds();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean setAllelesAndIsDiploid(int m, int indexStart, int indexEnd) {
        if (m==windowEnd) {
            for (int j=indexStart; j<indexEnd; ++j) {
                alleles[j] = j;
                isDiploid[j] = samples.isDiploid(a[j]>>1);
            }
            return true;
        }
        boolean isPolymorphic = false;
        alleles[indexStart] = gt.allele(m, a[indexStart]);
        isDiploid[indexStart] = samples.isDiploid(a[indexStart]>>1);
        for (int j=indexStart+1; j<indexEnd; ++j) {
            alleles[j] = gt.allele(m, a[j]);
            isDiploid[j] = samples.isDiploid(a[j]>>1);
            isPolymorphic |= (alleles[j]!=alleles[j-1]);
        }
        return isPolymorphic;
    }

    private void flushSeeds() {
        int[] seeds = seedList.toArray();
        seedList.clear();
        if (useSeedQ==false || seedQ.offer(seeds)==false) {
            processSeedList(seeds);
        }
    }

    private void processSeedList(int[] seeds) {
        for (int j=0; j<seeds.length; j+=4) {
            processSeed(seeds[j], seeds[j+1], seeds[j+2], seeds[j+3]);
        }
        flushHbdBuffer(BAOS_THRESHOLD);
        flushIbdBuffer(BAOS_THRESHOLD);
    }

    private void processSeed(int hap1, int hap2, int start, int inclEnd) {
        start = extendStart(hap1, hap2, start);
        if (start>=0) {
            inclEnd = extendInclEnd(hap1, hap2, inclEnd);
            if ((genPos[inclEnd] - genPos[start])>=minOutput) {
                if ((hap1>>1)==(hap2>>1)) {
                    writeSegment(hap1, hap2, start, inclEnd, hbdOut);
                    N_HBD_SEGS.incrementAndGet();
                }
                else {
                    writeSegment(hap1, hap2, start, inclEnd, ibdOut);
                    N_IBD_SEGS.incrementAndGet();
                }
            }
        }
    }

    /* Returns the extended start or -1 if segment should be discarded */
    private int extendStart(int hap1, int hap2, int start) {
        assert gt.allele(start, hap1)==gt.allele(start, hap2);
        assert start==0 || gt.allele(start-1, hap1)!=gt.allele(start-1, hap2);
        int prevStart = start;
        int nextStart = nextStart(hap1, hap2, prevStart);
        while (nextStart>=0 && nextStart<prevStart) {
            prevStart = nextStart;
            nextStart = nextStart(hap1, hap2, prevStart);
        }
        return nextStart;
    }

    private int extendInclEnd(int hap1, int hap2, int inclEnd) {
        while (inclEnd<lastMarker
                && gt.allele(inclEnd+1, hap1)==gt.allele(inclEnd+1, hap2)) {
            ++inclEnd;
        }
        int prevInclEnd = inclEnd;
        int nextInclEnd = nextInclEnd(hap1, hap2, prevInclEnd);
        while (nextInclEnd>prevInclEnd) {
            prevInclEnd = nextInclEnd;
            nextInclEnd = nextInclEnd(hap1, hap2, prevInclEnd);
        }
        assert nextInclEnd==prevInclEnd;
        return nextInclEnd;
    }

    private int nextStart(int hap1, int hap2, int start) {
        if (start<2 || maxGap<0) {
            return start;
        }
        int m = start - 1;
        int firstMismatchPos = gt.marker(m).pos();
        assert gt.allele(start, hap1)==gt.allele(start, hap2);
        assert gt.allele(m, hap1)!=gt.allele(m, hap2);
        int firstMatch = start-2;
        while (m>0) {
            --m;
            int a1 = gt.allele(m, hap1);
            int a2 = gt.allele(m, hap2);
            if (a1!=a2) {
                if ((firstMismatchPos - gt.marker(m).pos()) > maxGap) {
                    ++m;
                    break;
                }
                else if (m>0) {
                    firstMatch = m-1;
                }
            }
        }
        double len = (genPos[firstMatch] - genPos[m]);
        if (len>=minSeed && (firstMatch - m)>=minSeedMarkersM1) {
            // skip seed since preceding seed exists for extended segment
            return -1;
        }
        else {
            return (len<minExtend || (firstMatch-m)<minExtendMarkersM1) ? start : m;
        }
    }

    private int nextInclEnd(int hap1, int hap2, int inclEnd) {
        assert gt.isPhased();
        if (inclEnd>(lastMarker-2) || maxGap<0) {
            return inclEnd;
        }
        int m = inclEnd + 1;
        assert gt.allele(inclEnd, hap1)==gt.allele(inclEnd, hap2);
        assert gt.allele(m, hap1)!=gt.allele(m, hap2);
        int firstMismatchPos = gt.marker(m).pos();
        int firstMatch = inclEnd + 2;
        while (m<lastMarker) {
            ++m;
            int a1 = gt.allele(m, hap1);
            int a2 = gt.allele(m, hap2);
            if (a1!=a2) {
                if ((gt.marker(m).pos() - firstMismatchPos) > maxGap) {
                    --m;
                    break;
                }
                else if (m<lastMarker) {
                    firstMatch = m+1;
                }
            }
        }
        double len = (genPos[m] - genPos[firstMatch]);
        return (len<minExtend || (m-firstMatch)<minExtendMarkersM1) ? inclEnd : m;
    }

    private static IntArray rec(final GT gt, final int marker) {
        return new IntArray() {

            @Override
            public int size() {
                return gt.nHaps();
            }

            @Override
            public int get(int index) {
                return gt.allele(marker, index);
            }
        };
    }

    private void flushHbdBuffer(int byteThreshold) {
        if (hbdBaos.size() >= byteThreshold) {
            try {
                hbdOut.close();
                hbdOS.write(hbdBaos.toByteArray());
                hbdBaos.reset();
                hbdOut = printWriter(hbdBaos);
            } catch (IOException ex) {
                Utilities.exit("ERROR: ", ex);
            }
        }
    }

    private void flushIbdBuffer(int byteThreshold) {
        if (ibdBaos.size() >= byteThreshold) {
            try {
                ibdOut.close();
                ibdOS.write(ibdBaos.toByteArray());
                ibdBaos.reset();
                ibdOut = printWriter(ibdBaos);
            } catch (IOException ex) {
                Utilities.exit("ERROR: ", ex);
            }
        }
    }

    private void writeSegment(int hap1, int hap2, int start, int inclEnd,
            PrintWriter out) {
        double cmLength = genPos[inclEnd] - genPos[start];
        if (hap1>hap2) {
            int tmp = hap1;
            hap1 = hap2;
            hap2 = tmp;
        }
        out.print(ids[hap1>>1]);
        out.print(Const.tab);
        out.print(HAP_TO_STRING[hap1 & 0b1]);
        out.print(Const.tab);
        out.print(ids[hap2>>1]);
        out.print(Const.tab);
        out.print(HAP_TO_STRING[hap2 & 0b1]);
        out.print(Const.tab);
        out.print(chrom);
        out.print(Const.tab);
        out.print(pos[start]);
        out.print(Const.tab);
        out.print(pos[inclEnd]);
        out.print(Const.tab);
        print3(cmLength, out);
        out.print(Const.nl);
    }

    public static void print3(double d, PrintWriter out) {
        if (d<0) {
            out.print('-');
            d = -d;
        }
        d += 5e-4;
        long integerPart = (long) d;
        double fraction = Math.floor(1000*(d - integerPart));
        out.print(integerPart);
        out.print('.');
        if (fraction<100) {
            out.print('0');
            if (fraction<10) {
                out.print('0');
            }
        }
        out.print((int) fraction);
    }

    /**
     * Returns the number of output HBD segments.
     * @return the number of output HBD segments
     */
    public static long nHbdSegs() {
        return N_HBD_SEGS.get();
    }

    /**
     * Returns the number of output IBD segments.
     * @return the number of output IBD segments
     */
    public static long nIbdSegs() {
        return N_IBD_SEGS.get();
    }
}
