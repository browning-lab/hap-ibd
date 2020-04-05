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

/**
 * <p>Interface {@code GT} represents genotype data
 * for a list of markers and a list of samples.
 * </p>
 * <p>All instances of {@code GT} are required to be immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface GT {

    /**
     * Returns {@code true} if the markers are ordered by decreasing chromosome
     * base position, and returns {@code false} otherwise.
     * @return {@code true} if the markers are ordered by decreasing chromosome
     * base position
     */
    boolean isReversed();

    /**
     * Returns the number of markers.
     * @return the number of markers
     */
    int nMarkers();

    /**
     * Returns the specified marker.
     * @param marker a marker index
     * @return the specified marker
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     */
    Marker marker(int marker);

    /**
     * Returns the list of markers in order of increasing chromosome position.
     * If {@code (this.isReversed() == false)} then
     * {@code (this.markers().marker(j).equals(this.marker(j)) == true)} for
     * all {@code (0 <= j && j < this.nMarkers())}.
     * If {@code (this.isReversed() == true)} then
     * {@code (this.markers().marker(this.nMarkers() - 1 - j).equals(this.marker(j)) == true)}
     * for all {@code (0 <= j && j < this.nMarkers())}
     * @return the list of markers in order of increasing chromosome position
     */
    Markers markers();

    /**
     * Returns the number of haplotypes.  The returned value is equal to
     * {@code 2*this.nSamples()}.
     * @return the number of haplotypes
     */
    int nHaps();

    /**
     * Returns the number of samples.
     * @return the number of samples
     */
    int nSamples();

   /**
     * Returns the list of samples.
     * @return the list of samples
     */
    Samples samples();

    /**
     * Returns {@code true} if the genotype for each marker and sample
     * has non-missing alleles and is either haploid or diploid with
     * a phased allele separator, and returns {@code false} otherwise.
     * @return {@code true} if the genotype for each marker and sample
     * is a phased, non-missing genotype
     */
    boolean isPhased();

    /**
     * Returns the first allele for the specified marker and sample
     * or return -1 if the allele is missing.  The two alleles for a
     * sample are arbitrarily ordered if
     * {@code this.unphased(marker, sample) == false}.
     * @param marker the marker index
     * @param sample the sample index
     * @return the first allele for the specified marker and sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    int allele1(int marker, int sample);

    /**
     * Returns the second allele for the specified marker and sample
     * or return -1 if the allele is missing.  The two alleles for a
     * sample are arbitrarily ordered if
     * {@code this.unphased(marker, sample) == false}.
     * @param marker the marker index
     * @param sample the sample index
     * @return the  allele for the specified marker and sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    int allele2(int marker, int sample);

    /**
     * Returns the allele on the specified haplotype for the specified marker
     * or return -1 if the allele is missing.  The two alleles for an
     * individual are arbitrarily ordered if
     * {@code this.unphased(marker, hap/2) == false}.
     * @param marker the marker index
     * @param hap the haplotype index
     * @return the allele on the specified haplotype for the specified marker
     *
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     * @throws IndexOutOfBoundsException if
     * {@code hap < 0 || hap  >= this.nHaps()}
     */
    int allele(int marker, int hap);

    /**
     * Returns a {@code GT} instance restricted to genotype data for
     * the specified markers.
     * @param markers the list of markers in the returned instance
     * @param indices a list of distinct marker indices (from
     * {@code this.markers())} in increasing order
     * @return a {@code GT} instance restricted to genotype data for
     * the specified markers
     *
     * @throws IndexOutOfBoundsException if there exists {@code j} such that
     * {@code (0 <= j && j < indices.length)} such that
     * {@code (indices[j] < 0 || indices[j] >= this.nMarkers())}
     * @throws IllegalArgumentException if there exists {@code j} such that
     * {@code (1 <= j && j < indices.length)} such that
     * {@code (indices[j] <= indice[j - 1])}
     * @throws IllegalArgumentException if there exists {@code j} such that
     * {@code (0 <= j && j < indices.length)} such that
     * {@code (this.marker(indices[j]).equals(markers.marker(j)) == false)}
     * @throws NullPointerException if {@code indices == null}
     * @throws UnsupportedOperationException if {@code this.isReversed() == true}
     */
    GT restrict(Markers markers, int[] indices);

    /**
     * Returns the probability of the observed data for the specified marker
     * and sample if the specified pair of unordered alleles is the true
     * genotype. Returns {@code 1.0f} if the corresponding genotype
     * determined by the {@code isPhased()}, {@code allele1()}, and
     * {@code allele2()} methods is consistent with the specified ordered
     * genotype, and returns {@code 0.0f} otherwise.
     *
     * @param gt the genotype data
     * @param marker the marker index
     * @param sample the sample index
     * @param allele1 the first allele index
     * @param allele2 the second allele index
     * @return the probability of the observed data for the specified marker
     * and sample if the specified pair of ordered alleles is the true
     * ordered genotype
     *
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     * @throws IndexOutOfBoundsException if
     * {@code samples < 0 || samples >= this.nSamples()}
     * @throws IndexOutOfBoundsException if
     * {@code allele1 < 0 || allele1 >= this.marker(marker).nAlleles()}
     * @throws IndexOutOfBoundsException if
     * {@code allele2 < 0 || allele2 >= this.marker(marker).nAlleles()}
     */
    static float gl(GT gt, int marker, int sample, int allele1, int allele2) {
        int nAlleles = gt.marker(marker).nAlleles();
        if (allele1 < 0 || allele1 >= nAlleles)  {
            String s = "invalid alleles: (" + allele1 + "): " + marker;
            throw new IllegalArgumentException(s);
        }
        if (allele2 < 0 || allele2 >= nAlleles) {
            String s = "invalid alleles: (" + allele2 + "): " + marker;
            throw new IllegalArgumentException(s);
        }
        int a1 = gt.allele1(marker, sample);
        int a2 = gt.allele2(marker, sample);
        boolean consistent = (a1==-1 || a1==allele1) && (a2==-1 || a2==allele2);
        if (consistent==false) {
            consistent = (a1==-1 || a1==allele2) && (a2==-1 || a2==allele1);
        }
        return consistent ? 1.0f : 0.0f;
    }
}
