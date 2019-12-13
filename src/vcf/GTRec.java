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

import blbutil.Const;
import java.util.Arrays;

/**
 * <p>Interface {@code GTRec} represents represents genotype data for one
 * marker.
 * </p>
 * <p>All instances of {@code GTRec} are required to be immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface GTRec extends DuplicatesGTRec {

    /**
     * Returns the list of samples.
     * @return the list of samples
     */
    Samples samples();

    /**
     * Returns {@code true} if the value returned by {@code this.gl()} is
     * determined by a called or missing genotype, and returns {@code false}
     * otherwise.
     * @return {@code true} if the value returned by {@code this.gl()} is
     * determined by a called or missing genotype
     *
     * @implSpec The default implementation returns {@code true}
     */
    boolean isGTData();

    /**
     * Returns the probability of the observed data for the specified sample
     * if the specified pair of ordered alleles is the true ordered genotype.
     * @param sample the sample index
     * @param allele1 the first allele index
     * @param allele2 the second allele index
     * @return the probability of the observed data for the specified sample
     * if the specified pair of ordered alleles is the true ordered genotype.
     *
     * @throws IndexOutOfBoundsException if
     * {@code samples < 0 || samples >= this.nSamples()}
     * @throws IndexOutOfBoundsException if
     * {@code allele1 < 0 || allele1 >= this.marker().nAlleles()}
     * @throws IndexOutOfBoundsException if
     * {@code allele2 < 0 || allele2 >= this.marker().nAlleles()}
     *
     * @implSpec The default implementation returns {@code 1.0f} if the
     * corresponding genotype determined by the {@code isPhased()},
     * {@code allele1()}, and {@code allele2()} methods is consistent
     * with the specified ordered genotype, and returns {@code 0.0f} otherwise.
     */
    float gl(int sample, int allele1, int allele2);

    /**
     * Returns the allele frequencies.  The {@code k}-th element of the
     * returned array is the frequency of the {@code k}-th allele.
     * @param rec the genotype data for a marker
     * @return the allele frequencies
     */
    static double[] alleleFreq(GTRec rec) {
        int[] cnts = alleleCounts(rec);
        int sum = Arrays.stream(cnts).sum();
        double[] freq = new double[cnts.length];
        if (sum>0) {
            for (int al=0; al<cnts.length; ++al) {
                freq[al] = (double) cnts[al]/sum;
            }
        }
        return freq;
    }

    /**
     * Returns the allele counts.  The {@code k}-th element of the
     * returned array is the count of the {@code k}-th allele.
     * @param rec the genotype data for a marker
     * @return the allele frequencies
     */
    static int[] alleleCounts(GTRec rec) {
        int nAlleles = rec.marker().nAlleles();
        int[] cnts = new int[nAlleles];
        for (int h=0, n = rec.size(); h<n; ++h) {
            int allele = rec.get(h);
            if (allele>=0) {
                ++cnts[allele];
            }
        }
        return cnts;
    }

    /**
     * Returns a VCF record corresponding to the specified {@code GTRec} object.
     * The returned VCF record will have missing QUAL and INFO fields,
     * will have "PASS" in the filter field, and will have a GT format field.
     * @param gtRec the genotype data
     * @return a VCF record corresponding to the specified {@code GTRec} object
     * @throws NullPointerException if {@code gtRec == null}
     */
    static String toVcfRec(GTRec gtRec) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(gtRec.marker());
        sb.append(Const.tab);
        sb.append(Const.MISSING_DATA_CHAR);  // QUAL
        sb.append(Const.tab);
        sb.append("PASS");                   // FILTER
        sb.append(Const.tab);
        sb.append(Const.MISSING_DATA_CHAR);  // INFO
        sb.append(Const.tab);
        sb.append("GT");                     // FORMAT
        for (int j=0, n=gtRec.nSamples(); j<n; ++j) {
            int a1 = gtRec.allele1(j);
            int a2 = gtRec.allele2(j);
            sb.append(Const.tab);
            if (a1==-1) {
                sb.append(Const.MISSING_DATA_CHAR);
            }
            else {
                sb.append(a1);
            }
            sb.append(gtRec.isPhased(j) ? Const.phasedSep : Const.unphasedSep);
            if (a2==-1) {
                sb.append(Const.MISSING_DATA_CHAR);
            }
            else {
                sb.append(a2);
            }
        }
        return sb.toString();
    }
}