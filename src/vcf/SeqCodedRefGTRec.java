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
 * <p>Class {@code SeqCodedRefGT}  represents phased, non-missing
 * genotypes for a list of reference samples at a single marker.
 * Genotype emission probabilities are determined by the sample
 * genotypes.
 * </p>
 * <p>Instances of class {@code SeqCodedRefGT} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class SeqCodedRefGTRec implements RefGTRec {

    private final Marker marker;
    private final Samples samples;
    private final IntArray hapToSeq;
    private final IntArray seqToAllele;

    /**
     * Creates a new {@code SeqCodedRefGT} instance with phased,
     * non-missing genotypes from the specified marker, samples,
     * and haplotype alleles.  The contract for the constructed object
     * is undefined if any element of {@code hapToSeq} is negative or
     * greater than or equal to {@code seqToAllele.size()} or if any element
     * of {@code seqToAllele} is negative or greater than or equal to
     * {@code marker.nAlleles()}.
     *
     * @param marker the marker
     * @param samples the samples
     * @param hapToSeq an array whose {@code j}-th element is the index
     * of the distinct allele sequence carried by the {@code j}-th haplotype
     * @param seqToAllele an array whose {@code j}-th element is the marker
     * allele carried by the {@code j}-th distinct allele sequence
     *
     * @throws IllegalArgumentException if
     * {@code hapToSeq.size() != 2*samples.nSamples()}
     * @throws NullPointerException if any parameter is {@code null}
     */
    public SeqCodedRefGTRec(Marker marker, Samples samples, IntArray hapToSeq,
        IntArray seqToAllele) {
        if (hapToSeq.size() != 2*samples.nSamples()) {
            throw new IllegalArgumentException("inconsistent data");
        }
        this.marker = marker;
        this.samples = samples;
        this.hapToSeq = hapToSeq;
        this.seqToAllele = seqToAllele;
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
    public int nSamples() {
        return samples.nSamples();
    }

    @Override
    public Samples samples() {
        return samples;
    }

    @Override
    public int size() {
        return hapToSeq.size();
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
        boolean match = allele1 == allele1(sample)
                && allele2 == allele2(sample);
        return match ? 1f : 0f;
    }

    @Override
    public int[][] hapIndices() {
        int[] alCnts = new int[marker().nAlleles()];
        for (int h=0, n=size(); h<n; ++h) {
            ++alCnts[get(h)];
        }
        int majAllele = 0;
        for (int al=1; al<alCnts.length; ++al) {
            if (alCnts[al] > alCnts[majAllele]) {
                majAllele = al;
            }
        }
        int[][] hapIndices = new int[alCnts.length][];
        for (int al=0; al<alCnts.length; ++al) {
            if (al!=majAllele) {
                hapIndices[al] = new int[alCnts[al]];
            }
        }
        Arrays.fill(alCnts, 0);
        for (int h=0, n=size(); h<n; ++h) {
            int al = get(h);
            if (al!=majAllele) {
                hapIndices[al][alCnts[al]++] = h;
            }
        }
        return hapIndices;
    }

    @Override
    public boolean isAlleleCoded() {
        return false;
    }

    @Override
    public int majorAllele() {
        int[][] hapIndices = hapIndices();
        int majAllele = -1;
        for (int j=0; j<hapIndices.length && majAllele==-1; ++j) {
            if (hapIndices[j]==null) {
                majAllele = j;
            }
        }
        return majAllele;
    }

    @Override
    public int alleleCount(int allele) {
        int[][] hapIndices = hapIndices();
        if (hapIndices[allele]==null) {
            throw new IllegalArgumentException("major allele");
        }
        else {
            return hapIndices[allele].length;
        }
    }

    @Override
    public int allele1(int sample) {
        return seqToAllele.get(hapToSeq.get(sample<<1));
    }

    @Override
    public int allele2(int sample) {
        return seqToAllele.get(hapToSeq.get((sample<<1) | 0b1));
    }

    @Override
    public int get(int hap) {
        return seqToAllele.get(hapToSeq.get(hap));
    }

    @Override
    public int[] alleles() {
        return IntStream.range(0, hapToSeq.size())
                .map(h -> get(h))
                .toArray();
    }

    @Override
    public int nAlleles() {
        return this.marker().nAlleles();
    }

    @Override
    public int hapIndex(int allele, int copy) {
        int[][] hapIndices = hapIndices();
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
        return 2;
    }

    @Override
    public IntArray[] maps() {
        return new IntArray[] {hapToSeq, seqToAllele};
    }

    @Override
    public IntArray map(int index) {
        if (index==0) {
            return hapToSeq;
        }
        else if (index==1) {
            return seqToAllele;
        }
        else {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
    }
}
