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

/**
 * <p>Interface {@code DuplicatesGTRec} represents marker alleles for a
 * list of samples.  The samples in the list of samples are not
 * required to be unique.
 * </p>
 * All instances of {@code HapsMarkers} are required to be
 * immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface DuplicatesGTRec extends MarkerContainer, IntArray {

    /**
     * Returns the first allele for the specified sample or
     * -1 if the allele is missing.  The two alleles for a sample
     * are arbitrarily ordered if
     * {@code this.unphased(marker, sample) == false}.
     * @param sample a sample index
     * @return the first allele for the specified sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    int allele1(int sample);

    /**
     * Returns the second allele for the specified sample or
     * -1 if the allele is missing.  The two alleles for a sample
     * are arbitrarily ordered if
     * {@code this.unphased(marker, sample) == false}.
     * @param sample a sample index
     * @return the second allele for the specified sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    int allele2(int sample);

    /**
     * Returns the specified allele for the specified haplotype or
     * -1 if the allele is missing.  The two alleles for a sample
     * at a marker are arbitrarily ordered if
     * {@code this.unphased(marker, hap/2) == false}.
     * @param hap a haplotype index
     * @return the specified allele for the specified sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code hap < 0 || hap >= this.size()}
     */
    @Override
    int get(int hap);

    /**
     * Returns an array of length {@code this.size()} whose {@code j}-th
     * element is equal to {@code this.allele(j}}
     * @return an array of length {@code this.size()} whose {@code j}-th
     * element is equal to {@code this.allele(j}}
     */
    int[] alleles();

    /**
     * Returns the number of haplotypes.  The returned value is equal to
     * {@code 2*this.nSamples()}.
     * @return the number of haplotypes
     */
    @Override
    int size();

    /**
     * Returns the number of samples.  The returned value is
     * equal to {@code this.size()/2}.
     * @return the number of samples
     */
    int nSamples();

    /**
     * Returns {@code true} if the genotype for the specified sample
     * has non-missing alleles and is either haploid or diploid with
     * a phased allele separator, and returns {@code false} otherwise.
     * @param sample a sample index
     * @return {@code true} if the genotype for the specified sample
     * is a phased, nonmissing genotype
     *
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    boolean isPhased(int sample);

    /**
     * Returns {@code true} if every genotype for each sample is a phased,
     * non-missing genotype, and returns {@code false} otherwise.
     * @return {@code true} if the genotype for each sample is a phased,
     * non-missing genotype
     */
    boolean isPhased();
}
