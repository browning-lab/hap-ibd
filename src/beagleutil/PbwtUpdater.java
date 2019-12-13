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
package beagleutil;

import ints.IntArray;
import ints.IntList;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * <p>Class {@code PbwtUpdater} updates prefix and divergence arrays using
 * the positional Burrows-Wheeler transform (PBWT).</p>
 *
 * <p>Instances of {@code PbwtUpdater} are not thread-safe.</p>
 *
 * <p>Reference: Durbin, Richard (2014) Efficient haplotype matching and storage
 *    using the positional Burrows-Wheeler transform (PBWT).
 *    Bioinformatics 30(9):166-1272. doi: 10.1093/bioinformatics/btu014</p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class PbwtUpdater {

    private final int nHaps;

    private IntList[] a; // next prefix array data
    private IntList[] d; // next div array data
    private int[] p;

    /**
     * Constructs a new {@code PbwtUpdater} instance for the specified data.
     * @param nHaps the number of haplotypes at each position
     * @throws NegativeArraySizeException if {@code nHaps < 0}
     */
    public PbwtUpdater(int nHaps) {
        int initSize = 4;
        this.nHaps = nHaps;
        this.p = new int[initSize];
        this.a = IntStream.range(0, initSize)
                .mapToObj(i -> new IntList())
                .toArray(IntList[]::new);
        this.d = IntStream.range(0, initSize)
                .mapToObj(i -> new IntList())
                .toArray(IntList[]::new);
    }

    /**
     * Returns the number of haplotypes.
     * @return the number of haplotypes
     */
    public int nHaps() {
        return nHaps;
    }

    /**
     * Update the specified prefix and divergence arrays using the forward
     * Positional Burrows-Wheeler Transform. The contract for this method is
     * undefined if the specified {@code prefix} array is not a permutation of
     * {@code 0, 1, 2, ..., (nHaps - 1)}, or if the elements of the
     * specified {@code div} arrays are not all less than or equal to
     * the specified {@code marker}.
     *
     * @param rec the haplotype alleles
     * @param nAlleles the number of alleles
     * @param marker the marker index for the specified haplotype alleles
     * @param prefix the prefix array
     * @param div the divergence array
     *
     * @throws IllegalArgumentException if {@code nAlleles < 1}
     * @throws IndexOutOfBoundsException if
     * {@code (rec.get[j] < 0 || rec.get(j) >= nAlleles)}
     * for any {@code j} satisfying {@code (0 <= j && j < this.nHaps())}
     * @throws IndexOutOfBoundsException if {@code prefix.length >= this.nHaps()}
     * @throws IndexOutOfBoundsException if {@code div.length >= this.nHaps()}
     * @throws NullPointerException if
     * {@code rec == null || prefix == null || div == null}
     */
    public void fwdUpdate(IntArray rec, int nAlleles, int marker, int[] prefix,
            int[] div) {
        initializeArrays(nAlleles, marker+1);
        for (int i=0; i<nHaps; ++i) {
            int allele = rec.get(prefix[i]);
            if (allele>=nAlleles) {
                throw new IndexOutOfBoundsException(String.valueOf(nAlleles));
            }
            for (int j=0; j<nAlleles; ++j) {
                if (div[i]>p[j]) {
                    p[j] = div[i];
                }
            }
            a[allele].add(prefix[i]);
            d[allele].add(p[allele]);
            p[allele] = Integer.MIN_VALUE;
        }
        updatePrefixAndDiv(nAlleles, prefix, div);
    }

    /**
     * Update the specified prefix and divergence arrays using the backward
     * Positional Burrows-Wheeler Transform. The contract for this method is
     * undefined if the specified {@code prefix} array is not a permutation of
     * {@code 0, 1, 2, ..., (nHaps - 1)}, or if the elements of the
     * specified {@code div} arrays are not all greater than or equal to
     * the specified {@code marker}.
     *
     * @param rec the haplotype alleles
     * @param nAlleles the number of alleles
     * @param marker the marker index for the specified haplotype alleles
     * @param prefix the prefix array
     * @param div the divergence array
     *
     * @throws IllegalArgumentException if {@code nAlleles < 1}
     * @throws IndexOutOfBoundsException if
     * {@code (rec.get[j] < 0 || rec.get(j) >= nAlleles)}
     * for any {@code j} satisfying {@code (0 <= j && j < this.nHaps())}
     * @throws IndexOutOfBoundsException if {@code prefix.length >= this.nHaps()}
     * @throws IndexOutOfBoundsException if {@code div.length >= this.nHaps()}
     * @throws NullPointerException if
     * {@code rec == null || prefix == null || div == null}
     */
    public void bwdUpdate(IntArray rec, int nAlleles, int marker, int[] prefix,
            int[] div) {
        initializeArrays(nAlleles, marker-1);
        for (int i=0; i<nHaps; ++i) {
            int allele = rec.get(prefix[i]);
            if (allele>=nAlleles) {
                throw new IndexOutOfBoundsException(String.valueOf(nAlleles));
            }
            for (int j=0; j<nAlleles; ++j) {
                if (div[i]<p[j]) {
                    p[j] = div[i];
                }
            }
            a[allele].add(prefix[i]);
            d[allele].add(p[allele]);
            p[allele] = Integer.MAX_VALUE;
        }
        updatePrefixAndDiv(nAlleles, prefix, div);
    }

    private void updatePrefixAndDiv(int nAlleles, int[] prefix, int[] div) {
        int start = 0;
        for (int al=0; al<nAlleles; ++al) {
            int size = a[al].size();
            System.arraycopy(a[al].toArray(), 0, prefix, start, size);
            System.arraycopy(d[al].toArray(), 0, div, start, size);
            start += size;
            a[al].clear();
            d[al].clear();
        }
        assert start == nHaps;
    }

    private void initializeArrays(int nAlleles, int initPValue) {
        if (nAlleles<1) {
            throw new IllegalArgumentException(String.valueOf(nAlleles));
        }
        ensureArrayCapacity(nAlleles);
        Arrays.fill(p, 0, nAlleles, initPValue);
    }

    private void ensureArrayCapacity(int nAlleles) {
        if (nAlleles>a.length) {
            int oldLength = a.length;
            p = Arrays.copyOf(p, nAlleles);
            a = Arrays.copyOf(a, nAlleles);
            d = Arrays.copyOf(d, nAlleles);
            for (int j = oldLength; j<a.length; ++j) {
                a[j] = new IntList();
                d[j] = new IntList();
            }
        }
    }
}
