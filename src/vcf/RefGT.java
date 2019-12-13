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

import java.util.Arrays;

/**
 * <p>Class {@code BasicRefGT} stores a list of samples and a
 * haplotype pair for each sample.
 * </p>
 * <p>Instances of class {@code BasicRefGT} are immutable.<p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class RefGT implements GT {

    private final Markers markers;
    private final Samples samples;
    private final RefGTRec[] recs;

    /**
     * Constructs a new {@code RefHapPairs} instance.
     * @param markers the sequence of markers
     * @param samples the sequence of samples
     * @param refVcfRecs the sequence of per-marker genotype data
     *
     * @throws IllegalArgumentException if
     * {@code markers.nMarkers() != refVcfRecs.length}
     * @throws IllegalArgumentException if
     * {@code refVcfRecs[k].samples().equals(samples) == false} for any
     * {@code k} satisfying {@code 0 <= k  && k < refVcfRecs.length}
     * @throws IllegalArgumentException if
     * {@code refVcfRecs[k].marker().equals(markers.marker(k)) == false}
     * for any {@code k} satisfying {@code 0 <= k && k < refVcfRecs.length}
     * @throws IllegalArgumentException if
     * {@code refVcfRecs[k].isPhased() == false} for any {@code k}
     * satisfying {@code 0 <= k && k < refVcfRecs.length}
     * @throws NullPointerException if
     * {@code markers == null || samples == null || refVcfRecs == null
     * || refVcfRecs[k] == null} for any {@code k} satisfying
     * {@code 0 <= k && k <= refVcfRecs.length}
     */
    public RefGT(Markers markers, Samples samples, RefGTRec[] refVcfRecs) {
        checkData(markers, samples, refVcfRecs);
        this.markers = markers;
        this.samples = samples;
        this.recs = refVcfRecs.clone();
    }

    /**
     * Constructs a new {@code RefHapPairs} instance.
     * @param refVcfRecs the sequence of per-marker genotype data
     *
     * @throws IllegalArgumentException if {@code refVcfRecs.length == 0}
     * @throws IllegalArgumentException if
     * {@code refVcfRecs[k].samples().equals(samples) == false} for any
     * {@code k} satisfying {@code 0 <= k  && k < refVcfRecs.length}
     * @throws IllegalArgumentException if
     * {@code refVcfRecs[k].isPhased() == false} for any {@code k}
     * satisfying {@code 0 <= k && k < refVcfRecs.length}
     * @throws NullPointerException if
     * {@code samples == null || refVcfRecs == null}
     * @throws NullPointerException if
     * {@code (refVcfRecs[k] == null)} for any {@code k} satisfying
     * {@code (0 <= k && k <= refVcfRecs.length)}
     */
    public RefGT(RefGTRec[] refVcfRecs) {
        this.samples = checkData(refVcfRecs);
        Marker[] ma = Arrays.stream(refVcfRecs)
                .parallel()
                .map(rec -> rec.marker())
                .toArray(Marker[]::new);
        this.markers = Markers.create(ma);
        this.recs = refVcfRecs.clone();
    }

    private static Samples checkData(GTRec[] refVcfRecs) {
        if (refVcfRecs.length==0) {
            String s = "Missing data in VCF file";
            throw new IllegalArgumentException(s);
        }
        Samples samples = refVcfRecs[0].samples();
        for (int j=0; j<refVcfRecs.length; ++j) {
            if (refVcfRecs[j].samples().equals(samples)==false) {
                String s = "sample inconsistency at index " + j;
                throw new IllegalArgumentException(s);
            }
            if (refVcfRecs[j].isPhased()==false) {
                String s = "non-reference data at marker index " + j;
                throw new IllegalArgumentException(s);
            }
        }
        return samples;
    }

    private static void checkData(Markers markers, Samples samples,
            GTRec[] refVcfRecs) {
        if (markers.nMarkers()!=refVcfRecs.length) {
            String s = "markers.nMarkers()=" + markers.nMarkers()
                    + " refVcfRecs.length=" + refVcfRecs.length;
            throw new IllegalArgumentException(s);
        }
        for (int j=0; j<refVcfRecs.length; ++j) {
            if (refVcfRecs[j].samples().equals(samples)==false) {
                String s = "sample inconsistency at index " + j;
                throw new IllegalArgumentException(s);
            }
            if (refVcfRecs[j].marker().equals(markers.marker(j))==false) {
                String s = "marker inconsistency at index " + j;
                throw new IllegalArgumentException(s);
            }
            if (refVcfRecs[j].isPhased()==false) {
                String s = "non-reference data at marker index " + j;
                throw new IllegalArgumentException(s);
            }
        }
    }

    @Override
    public boolean isReversed() {
        return false;
    }

    @Override
    public int nMarkers() {
       return markers.nMarkers();
    }

    @Override
    public Marker marker(int marker) {
        return markers.marker(marker);
    }

    @Override
    public Markers markers() {
        return markers;
    }

    @Override
    public int nHaps() {
        return 2*samples.nSamples();
    }

    @Override
    public int nSamples() {
        return samples.nSamples();
    }

    @Override
    public Samples samples() {
        return samples;
    }

    @Override
    public boolean isPhased() {
        return true;
    }

    @Override
    public int allele1(int marker, int hapPair) {
        return recs[marker].allele1(hapPair);
    }

    @Override
    public int allele2(int marker, int hapPair) {
        return recs[marker].allele2(hapPair);
    }

    @Override
    public int allele(int marker, int haplotype) {
        return recs[marker].get(haplotype);
    }

    @Override
    public RefGT restrict(Markers markers, int[] indices) {
        RefGTRec[] rra = new RefGTRec[indices.length];
        for (int j=0; j<rra.length; ++j) {
            if (j>0 && indices[j] <= indices[j-1]) {
                throw new IllegalArgumentException(String.valueOf(indices[j]));
            }
            rra[j] = this.recs[indices[j]];
        }
        return new RefGT(markers, samples, rra);
    }

    /**
     * Returns the {@code RefGTRec} for the specified marker.
     * @param marker the marker index
     * @return the {@code RefGTRec} for the specified marker
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     */
    public RefGTRec get(int marker) {
        return recs[marker];
    }
}
