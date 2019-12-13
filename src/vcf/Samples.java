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

import beagleutil.SampleIds;
import java.util.Arrays;

/**
 * <p>Class {@code Samples} stores a list of samples.
 * </p>
 * Instances of class {@code Samples} are immutable.
 *
 * @author Brian L. Browning
 */
public final class Samples {

    private static final SampleIds sampleIds = SampleIds.instance();
    private final int[] indexToIdIndex;
    private final boolean[] isDiploid;

    /**
     * Constructs a new instance of {@code Samples} corresponding to
     * the specified list of diploid sample identifier indices.
     * @param idIndices an array of sample identifier indices
     * @param isDiploid a boolean array whose {@code k}-th value is {@code true}
     * if the {@code k}-th sample is diploid, and is {@code false} if the
     * {@code k}-th sample is haploid
     * @throws IllegalArgumentException if
     * {@code idIndices.length != isDiploid.length}
     * @throws IllegalArgumentException if the specified {@code idIndices} array
     * has two or more elements that are equal
     * @throws IndexOutOfBoundsException if any element of the specified
     * {@code idIndices} array is negative or greater than or equal to
     * {@code beagleutil.SampleIds.size()}
     * @throws NullPointerException if
     * {@code idIndices == null || isDiploid == null}
     */
    public Samples(int[] idIndices, boolean[] isDiploid) {
        if (idIndices.length!=isDiploid.length) {
            throw new IllegalArgumentException(String.valueOf(isDiploid));
        }
        checkForDuplicates(idIndices);
        this.indexToIdIndex = idIndices.clone();
        this.isDiploid = isDiploid.clone();
    }

    private static void checkForDuplicates(int[] idIndices) {
        int[] copy = Arrays.stream(idIndices).parallel().sorted().toArray();
        if (copy[0]<0) {
            throw new IllegalArgumentException(String.valueOf(copy[0]));
        }
        for (int j=1; j<copy.length; ++j) {
            if (copy[j-1]==copy[j]) {
                throw new IllegalArgumentException(String.valueOf(copy[j]));
            }
        }
    }

    /**
     * Returns an array mapping sample identifier indices to sample indices.
     * Indices for sample identifiers not present in this list of samples
     * are mapped to {@code -1}.
     * @return an array mapping sample identifier indices to sample indices
     */
    public int[] idIndexToIndex() {
        int[] idIndexToIndex = new int[sampleIds.size()];
        Arrays.fill(idIndexToIndex, -1);
        for (int j=0; j<indexToIdIndex.length; ++j) {
            int idIndex = indexToIdIndex[j];
            assert idIndexToIndex[idIndex] == -1; // no duplicate sample IDs
            idIndexToIndex[idIndex] = j;
        }
        return idIndexToIndex;
    }

    /**
     * Constructs and returns a {@code Samples} instance
     * corresponding to the specified list of sample identifiers.
     * @param ids an array of sample identifiers
     * @param isDiploid a boolean array whose {@code k}-th value is {@code true}
     * if the {@code k}-th sample is diploid, and is {@code false} if the
     * {@code k}-th sample is haploid
     * @return a {@code Samples} instance corresponding to the specified
     * list of sample identifiers
     *
     * @throws IllegalArgumentException if
     * {@code ids.length != isDiploid.length}
     * @throws IllegalArgumentException if the specified array
     * has two or more elements that are equal as strings
     * @throws NullPointerException if {@code ids == null || isDiploid == null}
     */
    public static Samples fromIds(String[] ids, boolean[] isDiploid) {
        return new Samples(sampleIds.getIndices(ids), isDiploid);
    }

    /**
     * <p>Returns a hash code value for the object.
     * </p>
     * <p>The hash code is defined by the following calculation:
     * </p>
     * <pre>
        int hash = 1;
        for (int j, n=this.nSamples(); j&lt;n; ++j) {
            hash = 31 * hash + this.idIndex(j);
        }
     * </pre>
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.indexToIdIndex);
    }

    /**
     * Returns {@code true} if the specified object is a
     * {@code Samples} object which represents the same ordered
     * list of samples as {@code this}, and returns {@code false}
     * otherwise.
     * @param obj the object to be tested for equality with {@code this}
     * @return {@code true} if the specified object is a
     * {@code Samples} object which represents the same ordered
     * list of samples as {@code this}
     */
    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final Samples other = (Samples) obj;
        return Arrays.equals(this.indexToIdIndex, other.indexToIdIndex);
    }

    /**
     * Returns the sample identifier index corresponding to the sample
     * with the specified index in this list of samples.
     * @param index a sample index
     * @return the sample identifier index corresponding to the sample
     * with the specified index in this list of samples
     * @throws IndexOutOfBoundsException if
     * {@code index < 0 || index >= this.nSamples()}
     */
    public int idIndex(int index) {
        return indexToIdIndex[index];
    }

    /**
     * Returns the number of samples in this list.
     * @return the number of samples in this list
     */
    public int nSamples() {
        return indexToIdIndex.length;
    }

    /**
     * Returns the identifier for the sample with the specified
     * index in this list of samples.
     * @param index a sample index
     * @return the identifier for the sample with the specified
     * index in this list of samples
     * @throws IndexOutOfBoundsException if
     * {@code index < 0 || index >= this.nSamples()}
     */
    public String id(int index) {
        return sampleIds.id(indexToIdIndex[index]);
    }

    /**
     * Returns this list of samples as an array of sample identifiers.
     * The returned array has length {@code this.nSamples()}, and it
     * satisfies {@code this.ids()[j].equals(this.id(j))} for
     * {@code 0 <= j && j < this.nSamples()}
     * @return this list of samples as an array of sample identifiers
     */
    public String[] ids() {
        return sampleIds.ids(indexToIdIndex);
    }

     /**
      * Returns {@code true} if the specified sample has two alleles per
      * genotype, and returns {@code false} if the sample has one allele
      * per genotype.
      * @param sample a sample index
      * @return {@code true} if the specified sample is diploid
      * @throws IndexOutOfBoundsException if
      * {@code sample < 0 || sample >= this.nSamples()}
      */
    public boolean isDiploid(int sample) {
        return isDiploid[sample];
    }

    /**
     * Returns {@code java.util.Arrays.toString(this.ids())}.
     *
     * @return a string representation of {@code this}
     */
    @Override
    public String toString() {
        return Arrays.toString(ids());
    }
}
