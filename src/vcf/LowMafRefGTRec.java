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

import ints.IntArray;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * <p>Class {@code LowMafRefGT} represent represents phased, non-missing
 * genotypes for a list of reference samples at a single marker.
 * Genotype emission probabilities are determined by the sample
 * genotypes.
 * </p>
 * <p>
 * Class {@code LowMafRefGT} stores the non-major allele indices.
 * </p>
 * <p>Instances of class {@code LowMemRefGT} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class LowMafRefGTRec implements RefGTRec {

    private final Marker marker;
    private final Samples samples;
    private final int nHaps;
    private final int majorAllele;
    private final int[][] hapIndices;

    /**
     * Constructs a new {@code LowMafRefGT} instance with phased
     * non-missing genotypes from the specified data.
     *
     * @param rec the phased, non-missing genotype data
     * @throws NullPointerException if {@code rec == null}
     */
    public LowMafRefGTRec(RefGTRec rec) {
        this.hapIndices = rec.hapIndices();
        int majAllele = 0;
        while (hapIndices[majAllele]!=null) {
            ++majAllele;
        }
        this.marker = rec.marker();
        this.samples = rec.samples();
        this.nHaps = rec.size();
        this.majorAllele = majAllele;
    }

    /**
     * Constructs a new {@code LowMafRefGT} instance with phased
     * non-missing genotypes from the specified {@code VcfRecGTParser}
     * object.
     *
     * @param gtp a VCF record parser that extracts sample genotypes
     * @throws IllegalArgumentException if the VCF record contains an
     * unphased genotype or missing allele
     * @throws IllegalArgumentException if a format error is detected in the
     * VCF record
     * @throws NullPointerException if {@code gtp == null}
     */
    public LowMafRefGTRec(VcfRecGTParser gtp) {
        this.marker = gtp.marker();
        this.samples = gtp.samples();
        this.nHaps = 2*gtp.nSamples();
        this.hapIndices = gtp.nonMajRefIndices();
        int majAl = -1;
        for (int j=0; j<hapIndices.length; ++j) {
            if (hapIndices[j]==null) {
                majAl = j;
                break;
            }
        }
        this.majorAllele = majAl;
    }

    /**
     * Constructs a new {@code LowMafRefGT} instance from the specified data.
     * The contract for this class is unspecified if a haplotype index is
     * duplicated in the specified {@code hapIndices} array.
     *
     * @param marker the marker
     * @param samples the samples
     * @param hapIndices an array whose {@code j}-th element is {@code null}
     * if {@code j} is the major allele with lowest index, and otherwise is
     * an array of indices of haplotypes that carry the {@code j}-th allele
     * sorted in increasing order
     *
     * @throws IllegalArgumentException if the {@code (hapIndices[j] == null)}
     * and {@code j} is not the major allele with lowest index, or if
     * {@code (hapIndices[j] != null)} and {@code j} is the major allele with
     * lowest index
     * @throws IllegalArgumentException if any element of {@code hapIndices}
     * is not not a sorted array of distinct haplotype indices
     * between 0 (inclusive) and {@code 2*samples.nSamples()} (exclusive)
     * @throws IllegalArgumentException if
     * {@code marker.nAlleles() != hapIndices.length}
     * @throws NullPointerException if
     * {@code marker == null || samples == null || hapIndices == null}
     */
    public LowMafRefGTRec(Marker marker, Samples samples, int[][] hapIndices) {
        this.marker = marker;
        this.samples = samples;
        this.nHaps = 2*samples.nSamples();
        this.majorAllele = checkIndicesAndReturnMajorAllele(hapIndices, nHaps);
        this.hapIndices = deepCopy(hapIndices);
    }

    static int checkIndicesAndReturnMajorAllele(int[][] hapIndices, int nHaps) {
        int majAllele = -1;
        int majCnt = nHaps;
        int maxMinorCnt = 0;
        for (int j=0; j<hapIndices.length; ++j) {
            if (hapIndices[j]==null) {
                if (majAllele == -1) {
                    majAllele = j;
                }
                else {
                    throw new IllegalArgumentException("invalid index array");
                }
            }
            else {
                checkSorted(hapIndices[j], nHaps);
                majCnt -= hapIndices[j].length;
                if (hapIndices[j].length > maxMinorCnt) {
                    maxMinorCnt = hapIndices[j].length;
                }
            }
        }
        if (majCnt < maxMinorCnt) {
            throw new IllegalArgumentException("invalid index array");
        }
        if (majAllele == -1) {
            throw new IllegalArgumentException("invalid index array");
        }
        return majAllele;
    }

    private static void checkSorted(int[] ia, int nHaps) {
        if (ia.length>0 && (ia[0] < 0 || ia[ia.length - 1] >= nHaps)) {
            throw new IllegalArgumentException("invalid index array");
        }
        for (int k=1; k<ia.length; ++k) {
            if (ia[k-1] >= ia[k]) {
                throw new IllegalArgumentException("invalid index array");
            }
        }
    }

    static int[][] deepCopy(int[][] ia) {
        int[][] copy = new int[ia.length][];
        for (int j=0; j<ia.length; ++j) {
            if (ia[j]!=null) {
                copy[j] = ia[j].clone();
            }
        }
        return ia;
    }

    @Override
    public int[][] hapIndices() {
        return deepCopy(hapIndices);
    }

    @Override
    public boolean isPhased(int sample) {
        if (sample < 0 || sample >= this.nSamples()) {
            throw new IndexOutOfBoundsException(String.valueOf(sample));
        }
        return true;
    }

    /**
     * Returns {@code true}.
     * @return {@code true}
     */
    @Override
    public boolean isPhased() {
        return true;
    }

    @Override
    public Samples samples() {
        return samples;
    }

    @Override
    public int nSamples() {
        return samples.nSamples();
    }

    @Override
    public int size() {
        return nHaps;
    }

    @Override
    public Marker marker() {
        return marker;
    }

    @Override
    public boolean isGTData() {
        return true;
    }

    @Override
    public float gl(int sample, int allele1, int allele2) {
        if (allele1 < 0 || allele1 >= hapIndices.length) {
            throw new IndexOutOfBoundsException(String.valueOf(allele1));
        }
        if (allele2 < 0 || allele2 >= hapIndices.length) {
            throw new IndexOutOfBoundsException(String.valueOf(allele2));
        }
        boolean matches = (allele1==allele1(sample) && allele2==allele2(sample));
        return matches ? 1.0f : 0.0f;
    }

    @Override
    public int allele1(int sample) {
        return get(sample<<1);
    }

    @Override
    public int allele2(int sample) {
        return get((sample<<1) | 0b1);
    }

    @Override
    public int get(int hap) {
        if (hap < 0 || hap >= nHaps) {
            throw new IndexOutOfBoundsException(String.valueOf(hap));
        }
        for (int j=0; j<hapIndices.length; ++j) {
            if (j != majorAllele) {
                if (Arrays.binarySearch(hapIndices[j], hap) >= 0) {
                    return j;
                }
            }
        }
        return majorAllele;
    }

    @Override
    public int[] alleles() {
        int[] ia = IntStream.range(0, nHaps)
                .map(h -> majorAllele)
                .toArray();
        for (int al=0; al<hapIndices.length; ++al) {
            if (al != majorAllele) {
                for (int h : hapIndices[al]) {
                    ia[h] = al;
                }
            }
        }
        return ia;
    }

    @Override
    public int nAlleles() {
        return this.marker().nAlleles();
    }

    @Override
    public boolean isAlleleCoded() {
        return true;
    }

    @Override
    public int majorAllele() {
        return majorAllele;
    }

    @Override
    public int alleleCount(int allele) {
        if (hapIndices[allele]==null) {
            throw new IllegalArgumentException("major allele");
        }
        else {
            return hapIndices[allele].length;
        }
    }

    @Override
    public int hapIndex(int allele, int copy) {
        if (hapIndices[allele]==null) {
            throw new IllegalArgumentException("major allele");
        }
        else {
            return hapIndices[allele][copy];
        }
    }

    @Override
    public boolean isCarrier(int allele, int hap) {
        return get(hap)==allele;
    }

    /**
     * Returns the data represented by {@code this} as a VCF
     * record with a GT format field. The returned VCF record
     * will have missing QUAL and INFO fields, will have "PASS"
     * in the filter field, and will have a GT format field.
     * @return the data represented by {@code this} as a VCF
     * record with a GT format field
     */
    @Override
    public String toString() {
        return GTRec.toVcfRec(this);
    }

    @Override
    public int nMaps() {
        return 1;
    }

    @Override
    public IntArray[] maps() {
        return new IntArray[] {toIntArray()};
    }

    @Override
    public IntArray map(int index) {
        if (index!=0) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        return toIntArray();
    }

    private IntArray toIntArray() {
        int[] ia = IntStream.range(0, nHaps)
                .map(i -> majorAllele)
                .toArray();
        for (int al=0; al<hapIndices.length; ++al) {
            if (hapIndices[al]!=null) {
                for (int i : hapIndices[al]) {
                    ia[i] = al;
                }
            }
        }
        return IntArray.create(ia, hapIndices.length);
    }
}
