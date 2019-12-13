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
package ints;

import java.util.Arrays;

/**
 * <p>Interface {@code IntArray} represents an immutable {@code int[]} array.
 * </p>
 * Instances of class {@code IntArray} are required to be immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface IntArray {

    /**
     * Returns the number of elements in this {@code IntArray}.
     * @return the number of elements in this {@code IntArray}
     */
    int size();

    /**
     * Returns the specified array element.
     * @param index an array index
     * @return the specified array element
     * @throws IndexOutOfBoundsException if
     * {@code index < 0 ||  index >= this.size()}
     */
    int get(int index);

    /**
     * Returns an {@code IntArray} instance of the specified size whose
     * {@code k}-th element is {@code k}.
     * @param size the size of the returned array
     * @return the identity {@code IntArray} having the specified size.
     */
    static IntArray identity(int size) {
        return new IntArray() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public int get(int index) {
                if (index<0 || index>=size) {
                    throw new IllegalArgumentException(String.valueOf(size));
                }
                return index;
            }
        } ;
    }

    /**
     * Returns a copy of the specified array.
     * @param ia a list of integers
     * @return a copy of the specified array
     * @throws NullPointerException if {@code ia == null}
     */
    static int[] toArray(IntArray ia) {
        int[] copy = new int[ia.size()];
        for (int j=0; j<copy.length; ++j) {
            copy[j] = ia.get(j);
        }
        return copy;
    }

    /**
     * Returns a string representation of this {@code IntArray} by applying
     * {@code java.utils.Arrays.toString()} to an equivalent {@code int[]}
     * object.
     *
     * @param ia a list of integers
     * @return a string representation of this {@code IntArray}.
     * @throws NullPointerException if {@code ia == null}
     */
    static String asString(IntArray ia) {
        return Arrays.toString(toArray(ia));
    }

    /**
     * Returns {@code true} if the specified {@code IntArray} objects
     * represent the same sequence of integer values, and returns {@code false}
     * otherwise.
     * @param a a sequence of integer values
     * @param b a sequence of integer values
     * @return {@code true} if the specified {@code IntArray} objects
     * represent the same sequence of integer values
     */
    static boolean equals(IntArray a, IntArray b) {
        if (a==b) {
            return true;
        }
        else if (a.size()!=b.size()) {
            return false;
        }
        else {
            for (int j=0, n=a.size(); j<n; ++j) {
                if (a.get(j)!=b.get(j)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the maximum element, or {@code Integer.MIN_VALUE} if
     * {@code this.size() == 0}.
     * @param ia a list of integers
     * @return the maximum element
     * @throws NullPointerException if {@code ia == null}
     */
    static int max(IntArray ia) {
        int max = Integer.MIN_VALUE;
        for (int j=0, n=ia.size(); j<n; ++j) {
            if (ia.get(j) > max) {
                max = ia.get(j);
            }
        }
        return max;
    }

    /**
     * Returns the minimum element, or {@code Integer.MAX_VALUE} if
     * {@code this.size() == 0}.
     * @param ia a list of integers
     * @return the minimum element
     * @throws NullPointerException if {@code ia == null}
     */
    static int min(IntArray ia) {
        int min = Integer.MAX_VALUE;
        for (int j=0, n=ia.size(); j<n; ++j) {
            if (ia.get(j) < min) {
                min = ia.get(j);
            }
        }
        return min;
    }

    /**
     * Returns a new {@code IntArray} instance that has the same
     * sequence of integers as the specified array.
     * @param ia the array of integers to be copied
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code IntArray} instance that has
     * the same sequence of integers as the specified array
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    static IntArray create(int[] ia, int valueSize) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        if (valueSize<=16) {
            return new PackedIntArray(ia, valueSize);
        }
        else if (valueSize<=256) {
            return new UnsignedByteArray(ia, valueSize);
        }
        else if (valueSize<=65536) {
            return new CharArray(ia, valueSize);
        }
        else {
            return new WrappedIntArray(ia, valueSize);
        }
    }

    /**
     * Returns a new {@code IntArray} instance that has the same
     * sequence of integers as the specified list.
     * @param il the list of integers to be copied
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code IntArray} instance that has
     * the same sequence of integers as the specified list
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    static IntArray create(IntList il, int valueSize) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        if (valueSize<=16) {
            return new PackedIntArray(il, valueSize);
        }
        else if (valueSize<=256) {
            return new UnsignedByteArray(il, valueSize);
        }
        else if (valueSize<=65536) {
            return new CharArray(il, valueSize);
        }
        else {
            return new WrappedIntArray(il, valueSize);
        }
    }
}
