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

import beagleutil.ChromIds;
import blbutil.Const;
import ints.LongArray;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Class {@code Markers} represent a list of markers in chromosome order.
 * </p>
 * <p>Instances of class {@code Markers} are immutable.
 * </p>
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class Markers {

    private static final int LOG2_BITS_PER_WORD = 6;

    private final Marker[] markerArray;
    private final Set<Marker> markerSet;
    private final int[] sumAlleles;
    private final int[] sumGenotypes;
    private final int[] sumHapBits;
    private final int hashCode;

    /**
     * Returns a new {@code Markers} instance that is constructed from
     * the specified data.
     * @param markers a list of markers in chromosome order
     * @return a new {@code Markers} instance corresponding to the
     * specified list of markers
     *
     * @throws IllegalArgumentException if markers on a chromosome are not
     * in chromosome order
     * @throws IllegalArgumentException if there are duplicate markers
     * @throws IllegalArgumentException if the markers on a chromosome
     * do not form a contiguous set of entries within the array
     *
     * @throws NullPointerException if
     * {@code markers == null} or if {@code markers[j] == null}
     * for any {@code j} satisfying {@code (0 <= j && j < markers.length)}
     */
    public static Markers create(Marker[] markers) {
        return new Markers(markers);
    }

    /**
     * Construct a new {@code Markers} instance that represents the
     * specified list of markers.
     * @param markers a list of markers in chromosome order
     *
     * @throws IllegalArgumentException if markers on a chromosome are not
     * in chromosome order
     * @throws IllegalArgumentException if there are duplicate markers
     * @throws IllegalArgumentException if the markers on a chromosome
     * do not form a contiguous set of entries within the array
     *
     * @throws NullPointerException if
     * {@code markers == null} or if {@code markers[j] == null}
     * for any {@code j} satisfying {@code (0 <= j && j < markers.length)}
     */
    private Markers(Marker[] markers) {
        checkMarkerPosOrder(markers);
        this.markerArray = markers.clone();
        this.markerSet = markerSet(markerArray);

        this.sumAlleles = cumSumAlleles(markerArray);
        this.sumGenotypes = cumSumGenotypes(markerArray);
        this.sumHapBits = cumSumHaplotypeBits(markerArray);
        this.hashCode = Arrays.deepHashCode(markerArray);
    }

    private static void checkMarkerPosOrder(Marker[] markers) {
        if (markers.length < 2) {
            return;
        }
        Set<Integer> chromIndices = new HashSet<>();
        chromIndices.add(markers[0].chromIndex());
        chromIndices.add(markers[1].chromIndex());
        for (int j=2; j<markers.length; ++j) {
            int chr0 = markers[j-2].chromIndex();
            int chr1 = markers[j-1].chromIndex();
            int chr2 = markers[j].chromIndex();
            if (chr0 == chr1 && chr1==chr2) {
                int pos0 = markers[j-2].pos();
                int pos1 = markers[j-1].pos();
                int pos2 = markers[j].pos();
                if ( (pos1<pos0 && pos1<pos2) || (pos1>pos0 && pos1>pos2) ) {
                    String s = "markers not in chromosomal order: "
                            + Const.nl + markers[j-2]
                            + Const.nl + markers[j-1]
                            + Const.nl + markers[j];
                    throw new IllegalArgumentException(s);
                }
            }
            else if (chr1!=chr2) {
                if (chromIndices.contains(chr2)) {
                    String s = "markers on chromosome are not contiguous: "
                            + ChromIds.instance().id(chr2);
                    throw new IllegalArgumentException(s);
                }
                chromIndices.add(chr2);
            }
        }
    }

    private static Set<Marker> markerSet(Marker[] markers) {
        Set<Marker> markerSet = new HashSet<>(markers.length);
        for (Marker m : markers) {
            if (markerSet.add(m)==false) {
                throw new IllegalArgumentException("Duplicate marker: " + m);
            }
        }
        return markerSet;
    }

    private static int[] cumSumAlleles(Marker[] markers) {
        int[] ia = new int[markers.length + 1];
        for (int j=1; j<ia.length; ++j) {
            ia[j] = ia[j-1] + markers[j-1].nAlleles();
        }
        return ia;
    }

    private static int[] cumSumGenotypes(Marker[] markers) {
        int[] ia = new int[markers.length + 1];
        for (int j=1; j<ia.length; ++j) {
            ia[j] = ia[j-1] + markers[j-1].nGenotypes();
        }
        return ia;
    }

    private static int[] cumSumHaplotypeBits(Marker[] markers) {
        int[] ia = new int[markers.length + 1];
        for (int j=1; j<ia.length; ++j) {
            int nAllelesM1 = markers[j-1].nAlleles() - 1;
            int nStorageBits = Integer.SIZE
                    - Integer.numberOfLeadingZeros(nAllelesM1);
            ia[j] = ia[j-1] + nStorageBits;
        }
        return ia;
    }

    /**
     * Returns a hash code value for the object.
     * The returned hash code equals
     * {@code Arrays.deepHashCode(this.markers())}.
     * @return a hash code value for the object
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Returns {@code true} if the specified object is a {@code Markers}
     * instance which represents the same list of markers as {@code this},
     * and returns {@code false} otherwise. Two lists of markers are
     * the same if the lists have the same size and if markers with the
     * same index in the two lists are equal.
     *
     * @param obj the object to be tested for equality with {@code this}
     *
     * @return {@code true} if the specified object is a {@code Markers}
     * instance which represents the same list of markers as {@code this}
     */
    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Markers other = (Markers) obj;
        return Arrays.deepEquals(this.markerArray, other.markerArray);
    }

    /**
     * Returns the number of markers.
     * @return the number of markers
     */
    public int nMarkers() {
        return markerArray.length;
    }

    /**
     * Returns the specified marker.
     * @param marker a marker index
     * @return the specified marker
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     */
    public Marker marker(int marker) {
        return markerArray[marker];
    }

    /**
     * Returns the list of markers.
     * @return the list of markers
     */
    public Marker[] markers() {
        return markerArray.clone();
    }

    /**
     * Returns {@code true} if the specified marker is not {@code null}
     * and is an element in the list of markers represented by {@code this},
     * and returns {@code false} otherwise.
     *
     * @param marker a marker
     *
     * @return {@code true} if the specified marker is not {@code null} and
     * is an element in the list of markers represented by {@code this}
     */
    public boolean contains(Marker marker) {
        return markerSet.contains(marker);
    }

    /**
     * Returns a {@code Markers} instance that represents
     * the specified range of marker indices.
     * @param start the starting marker index (inclusive)
     * @param end the ending marker index (exclusive)
     * @return a {@code Markers} instance that represents
     * the specified range of marker indices
     *
     * @throws IndexOutOfBoundsException if
     * {@code start < 0 || end > this.nMarkers()}
     * @throws IllegalArgumentException if {@code start >= end}.
     */
    public Markers restrict(int start, int end) {
        if (end > markerArray.length) {
            throw new IndexOutOfBoundsException("end > this.nMarkers(): " + end);
        }
        return new Markers(Arrays.copyOfRange(markerArray, start, end));
    }

    /**
     * Returns a {@code Markers} instance that represents
     * the specified markers.
     * @param indices a list of distinct marker indices in increasing order
     * @return a new {@code Markers} instance that represents the specified
     * markers
     *
     * @throws IndexOutOfBoundsException if there exists {@code j} such that
     * {@code (0 <= j && j < indices.length)} such that
     * {@code (indices[j] < 0 || indices[j] >= this.nMarkers())}
     * @throws IllegalArgumentException if there exists {@code j} such that
     * {@code (1 <= j && j < indices.length)} such that
     * {@code (indices[j] <= indice[j - 1])}
     * @throws NullPointerException if {@code indices == null}
     */
    public Markers restrict(int[] indices) {
        Marker[] ma = new Marker[indices.length];
        ma[0] = markerArray[indices[0]];
        for (int j=1; j<indices.length; ++j) {
            if (indices[j] <= indices[j-1]) {
                throw new IllegalArgumentException(String.valueOf(indices[j]));
            }
            ma[j] = markerArray[indices[j]];
        }
        return new Markers(ma);
    }

    /**
     * Returns the sum of the number of alleles for
     * the markers with index less than the specified index.
     * @param marker a marker index
     * @return the sum of the number of alleles for
     * the markers with index less than the specified index
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker > this.nMarkers()}
     */
    public int sumAlleles(int marker) {
        return sumAlleles[marker];
    }

    /**
     * Returns {@code this.sumAlleles(this.nMarkers())}.
     * @return {@code this.sumAlleles(this.nMarkers())}
     */
    public int sumAlleles() {
        return sumAlleles[markerArray.length];
    }

    /**
     * Returns the sum of the number of possible genotypes for the markers
     * with index less than the specified index.
     * @param marker a marker index
     * @return the sum of the number of possible genotypes for the markers
     * with index less than the specified index
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker > this.nMarkers()}
     */
    public int sumGenotypes(int marker) {
        return sumGenotypes[marker];
    }

    /**
     * Returns {@code this.sumGenotypes(this.nMarkers())}.
     * @return {@code this.sumGenotypes(this.nMarkers())}
     */
    public int sumGenotypes() {
        return sumGenotypes[markerArray.length];
    }

    /**
     * Returns the number of bits requires to store a haplotype for the
     * markers with index less than the specified index.
     * @param marker a marker index
     * @return the number of bits requires to store a haplotype for the
     * markers with index less than the specified index
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker > this.nMarkers()}
     */
    public int sumHaplotypeBits(int marker) {
        return sumHapBits[marker];
    }

    /**
     * Returns {@code this.sumHaplotypeBits(this.nMarkers())}.
     * @return {@code this.sumHaplotypeBits(this.nMarkers())}
     */
    public int sumHaplotypeBits() {
        return sumHapBits[markerArray.length];
    }

    /**
     * Returns a bit array storing the specified haplotype.
     * @param alleles the alleles at each marker
     * @return a bit array storing the specified haplotype.
     * @throws IllegalArgumentException if
     * {@code alleles.length != this.nMarkers()}
     * @throws NullPointerException if {@code alleles == null}
     */
    public LongArray allelesToBits(int[] alleles) {
        if (alleles.length != markerArray.length) {
            throw new IllegalArgumentException(String.valueOf(alleles.length));
        }
        int nWords = (sumHapBits[markerArray.length] + (Long.SIZE-1)) >> LOG2_BITS_PER_WORD;
        long[] bits = new long[nWords];
        int bitIndex = 0;
        for (int k=0; k<alleles.length; ++k) {
            int allele = alleles[k];
            if (allele < 0 || allele >= markerArray[k].nAlleles()) {
                String s = "allele \"" + allele + "\" out of bounds for marker: "
                        + markerArray[k];
                throw new IllegalArgumentException(s);
            }
            int mask = 1;
            int nBits = sumHapBits[k+1] - sumHapBits[k];
            for (int l=0; l<nBits; ++l) {
                if ((allele & mask)==mask) {
                    int wordIndex =  bitIndex >> LOG2_BITS_PER_WORD;
                    bits[wordIndex] |= (1L << bitIndex);
                }
                bitIndex++;
                mask <<= 1;
            }
        }
        return new LongArray(bits);
    }

    /**
     * Returns the specified allele stored in the specified {@code hapBits}
     * array.  The contract for this method is undefined if the specified
     * {@code hapBits} array was not created with the {@code this.allelesToBits()}
     * method.
     * @param hapBits the bit array storing the haplotype alleles
     * @param marker a marker index
     * @return the specified allele stored in the specified {@code hapBits}
     * array.
     * @throws IndexOutOfBoundsException if
     * {@code marker < 0 || marker >= this.nMarkers()}
     */
    public int bitsToAllele(LongArray hapBits, int marker) {
        int start = sumHapBits[marker];
        int end = sumHapBits[marker+1];
        if (end==(start+1)) {
            int wordIndex =  start >> LOG2_BITS_PER_WORD;
            return (int) (hapBits.get(wordIndex) >> start) & 1;
        }
        int allele = 0;
        int mask = 1;
        for (int j=start; j<end; ++j) {
            int wordIndex =  j >> LOG2_BITS_PER_WORD;
            if ((hapBits.get(wordIndex) & (1L << j)) != 0) {
                allele |= mask;
            }
            mask <<= 1;
        }
        return allele;
    }

//    public int altBitsToAllele(LongArray hapBits, int marker) {
//        // if employing this method, need to add extra 0 word to end of hapBits LongArrays
//        int start = sumHapBits[marker];
//        int end = sumHapBits[marker+1];
//        int index =  start >> LOG2_BITS_PER_WORD;
//        int offset = start & 0b111111;
//        int mask1 = (1 << (end-start)) - 1;
//        int mask2 = (1 << offset) - 1;
//        // NB: if offset==0, then (Long.SIZE-offset) in left-shift is 0
//        return (int) ((hapBits.get(index) >>> offset)
//            | ((hapBits.get(index+1) & mask2) << (Long.SIZE-offset))) & mask1;
//    }

    /**
     * Returns the specified allele stored in the specified {@code hapBits}
     * array.  The contract for this method is undefined if the specified
     * {@code hapBits} array was not created with the 
     * {@code this.allelesToBits()} method.
     * @param hapBits the bit array storing the haplotype alleles
     * @return the specified allele stored in the specified {@code hapBits}
     * array.
     * @throws NullPointerException if {@code hapBits == null}
     */
    public int[] bitsToAlleles(LongArray hapBits) {
        int[] alleles = new int[markerArray.length];
        for (int m=0; m<alleles.length; ++m) {
            int start = sumHapBits[m];
            int end = sumHapBits[m+1];
            if (end==(start+1)) {
                int wordIndex =  start >> LOG2_BITS_PER_WORD;
                alleles[m] = (int) (hapBits.get(wordIndex) >> start) & 1;
            }
            int allele = 0;
            int mask = 1;
            for (int j=start; j<end; ++j) {
                int wordIndex =  j >> LOG2_BITS_PER_WORD;
                if ((hapBits.get(wordIndex) & (1L << j)) != 0) {
                    allele |= mask;
                }
                mask <<= 1;
            }
            alleles[m] = allele;
        }
        return alleles;
    }

    /**
     * Returns a string representation of {@code this}.
     * The exact details of the representation are unspecified and
     * subject to change.
     * @return a string representation of {@code this}
     */
    @Override
    public String toString() {
        return Arrays.toString(markerArray);
    }
}

