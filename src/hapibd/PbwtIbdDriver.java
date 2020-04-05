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

import beagleutil.ChromInterval;
import blbutil.FileIt;
import blbutil.Filter;
import blbutil.InputIt;
import blbutil.MultiThreadUtils;
import blbutil.SampleFileIt;
import blbutil.SynchFileOutputStream;
import blbutil.Utilities;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vcf.FilterUtil;
import vcf.GTRec;
import vcf.GeneticMap;
import vcf.Marker;
import vcf.MarkerMap;
import vcf.RefGT;
import vcf. RefGTRec;
import vcf.RefIt;

/**
 * <p>Instances of class {@code PbwtIbsDriver} detect IBS segments in phased
 * genotype data.</p>
 *
 * <p>Instances of class {@code PbwtIbsDriver} are thread-safe.</p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class PbwtIbdDriver {

   /**
     * Runs an IBD segment detection analysis and returns a two-element
     * array containing the number of analyzed samples and markers
     * in that order.
     * @param par the analysis parameters
     * @return the number of analyzed samples and markers
     * @throws NullPointerException if {@code par == null}
     */
    public static long[] detectIbd(HapIbdPar par) {
        ChromInterval chromInt = null;
        GeneticMap genMap = GeneticMap.geneticMap(par.map(), chromInt);
        long[] nSamplesAndMarkers = new long[2];
        File hbdFile = new File(par.out() + ".hbd.gz");
        File ibdFile = new File(par.out() + ".ibd.gz");
        try (SampleFileIt<RefGTRec> it = refIt(par);
                SynchFileOutputStream hbdOS = new SynchFileOutputStream(hbdFile);
                SynchFileOutputStream ibdOS = new SynchFileOutputStream(ibdFile)) {
            try {
                nSamplesAndMarkers[0] = it.samples().nSamples();
                List<RefGTRec> recList = new ArrayList<>(1<<14);
                RefGTRec next = it.hasNext() ? it.next() : null;
                while (next!=null) {
                    recList.clear();
                    next = readChrom(it, next, par.min_mac(), recList);
                    if (recList.isEmpty()==false) {
                        RefGT gt = new RefGT(recList.toArray(new RefGTRec[0]));
                        MarkerMap map = MarkerMap.create(genMap, gt.markers());
                        PbwtIbdDriver.detectIBD(par, gt, map, hbdOS, ibdOS);
                        nSamplesAndMarkers[1] += gt.nMarkers();
                    }
                }
            } catch (Throwable t) {
                Utilities.exit(t);
            }
            hbdOS.writeEmptyBgzipBlock();
            ibdOS.writeEmptyBgzipBlock();
        } catch (IOException ex) {
            Utilities.exit(ex);
        }
        return nSamplesAndMarkers;
    }

    private static void detectIBD(HapIbdPar par, RefGT gt, MarkerMap map,
            SynchFileOutputStream hbdOS, SynchFileOutputStream ibdOS) {
        float minSeed = par.min_seed();
        int minMarkers = par.min_markers();
        double[] genPos = map.genPos().toArray();
        BlockingQueue<int[]> seedQ = new ArrayBlockingQueue<>(1<<16);
        int[][] startsAndEnds = overlappingWindows(genPos, minSeed, minMarkers,
                par.nthreads());
        int[] starts = startsAndEnds[0];
        int[] ends = startsAndEnds[1];
        ExecutorService execService = Executors.newFixedThreadPool(starts.length);
        for (int j=0; j<starts.length; ++j) {
            PbwtIbd pbwtIbs = new PbwtIbd(par, gt, map, starts[j], ends[j],
                    starts.length, seedQ, hbdOS, ibdOS);
            execService.submit(pbwtIbs);
        }
        MultiThreadUtils.shutdownExecService(execService);
    }

    private static SampleFileIt<RefGTRec> refIt(HapIbdPar par) {
        Filter<String> sFilter = FilterUtil.sampleFilter(par.excludesamples());
        Filter<Marker> mFilter = Filter.acceptAllFilter();
        FileIt<String> it0 = InputIt.fromGzipFile(par.gt());
        return RefIt.create(it0, sFilter, mFilter);
    }

    private static RefGTRec readChrom(SampleFileIt<RefGTRec> it, RefGTRec next,
            int minMac, List<RefGTRec> list) {
        int chromIndex = next.marker().chromIndex();
        while (next!=null && next.marker().chromIndex()==chromIndex) {
            list.add(next);
            next = it.hasNext() ? it.next() : null;
        }
        applyMacFilter(list, minMac);
        return next;
    }

    private static void applyMacFilter(List<RefGTRec> list, int minMac) {
        if (minMac>0) {
            RefGTRec[] filteredArray = list.stream()
                    .parallel()
                    .filter(rec -> mac(rec)>=minMac)
                    .toArray(RefGTRec[]::new);
            if (filteredArray.length<list.size()) {
                list.clear();
                list.addAll(Arrays.asList(filteredArray));
            }
        }
    }

    private static int mac(RefGTRec rec) {
        int[] alCnts = GTRec.alleleCounts(rec);
        Arrays.sort(alCnts);
        return alCnts.length==1 ? 0 : alCnts[alCnts.length-2];
    }

    private static int[][] overlappingWindows(double[] genPos, double minSeed,
            int minMarkers, int nThreads) {
        int[] starts = new int[nThreads];
        int[] ends = new int[nThreads];
        int nThreadsM1 = nThreads - 1;
        double totLength = genPos[genPos.length-1] - genPos[0];
        double step = Math.max((totLength - minSeed)/nThreads, 1e-6);
        int start = 0;
        int insPt = Arrays.binarySearch(genPos, (genPos[start] + minSeed + step));
        int end = (insPt<0) ? -insPt-1 : insPt;
        while (end<genPos.length && end<minMarkers) {
            ++end;
        }
        int index=0;
        while (index<nThreadsM1 && end<genPos.length) {
            starts[index] = start;
            ends[index++] = end;
            insPt = Arrays.binarySearch(genPos, start, end, (genPos[end-1] - minSeed));
            start = insPt<0 ? -insPt-1 : ++insPt;
            while ((start>0)
                    && ((end - (start-1))<minMarkers || (genPos[end-1] - genPos[start-1])<minSeed)) {
                --start;
            }
            assert (genPos[end-1] - genPos[start]) < minSeed || (end-start)<minMarkers;
            assert start==0 || (genPos[end-1] - genPos[start-1] >= minSeed && (end-(start-1)>=minMarkers));

            insPt = Arrays.binarySearch(genPos, end, genPos.length, (genPos[end] + step));
            end = (insPt<0) ? -insPt-1 : insPt;
        }
        starts[index] = start;
        ends[index++] = genPos.length;
        if (index<starts.length) {
            starts = Arrays.copyOf(starts, index);
            ends = Arrays.copyOf(ends, index);
        }
        return new int[][] {starts, ends};
    }
}
