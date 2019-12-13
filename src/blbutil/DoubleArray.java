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
package blbutil;

import java.util.Arrays;

/**
 * Class {@code DoubleArray} represents an immutable list of double floating
 * point values.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class DoubleArray {

    private final double[] values;

    /**
     * Constructs an {@code DoubleArray} object with the specified
     * values.
     * @param values the list of floating point values
     * @throws NullPointerException if {@code values == null}
     */
    public DoubleArray(double[] values) {
        this.values = values.clone();
    }

    /**
     * Returns the double at the specified position in this list.
     * @param index the index of the returned double
     * @return the double at the specified position in this list
     * @throws IndexOutOfBoundsException if
     * {@code index < 0 || index >= this.size()}
     */
    public double get(int index) {
        return values[index];
    }

    /**
     * Returns the number of elements in this list.
     * @return the number of elements in this list
     */
    public int size() {
        return values.length;
    }

    /**
     * Returns {@code true} if this list has no elements, and returns
     * {@code false} otherwise.
     * @return {@code true} if this list has no elements, and returns
     * {@code false} otherwise
     */
    public boolean isEmpty() {
        return values.length==0;
    }

     /**
     * Searches {@code this} for the specified value using the binary search
     * algorithm. This list must be sorted (as by the
     * {@code java.util.Arrays.sort(float[])} method)  prior to making
     * this call. If it is not sorted, the results are undefined.
     * If the list contains multiple elements with the specified
     * value, there is no guarantee which one will be found. This method
     * considers all NaN values to be equivalent and equal.
     *
     * @param key the value to be searched for
     *
     * @return index of the search key, if it is contained in the list;
     * otherwise, {@code (-(insertion point) - 1)}. The insertion point is
     * defined as the point at which the key would be inserted into the list:
     * the index of the first element greater than the key, or
     * {@code this.size()} if all elements in the list are less than the
     * specified key. Note that this guarantees that the return value will
     * be {@code >= 0} if and only if the key is found.
     */
    public int binarySearch(double key) {
        return Arrays.binarySearch(values, key);
    }

    /**
     * Searches the specified range of {@code this} for the specified value
     * using the binary search algorithm. This range must be sorted (as by the
     * {@code java.util.Arrays.sort(double[])} method) prior to making
     * this call. If it is not sorted, the results are undefined.
     * If the range contains multiple elements with the specified
     * value, there is no guarantee which one will be found. This method
     * considers all NaN values to be equivalent and equal.
     *
     * @param fromIndex the index of the first element (inclusive) to be searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     *
     * @return index of the search key, if it is contained in the list;
     * otherwise, {@code (-(insertion point) - 1)}. The insertion point is
     * defined as the point at which the key would be inserted into the list:
     * the index of the first element greater than the key, or
     * {@code this.size()} if all elements in the list are less than the
     * specified key. Note that this guarantees that the return value will
     * be {@code >= 0} if and only if the key is found.
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if
     * {@code fromIndex < 0 || toIndex > this.size()}
     */
    public int binarySearch(int fromIndex, int toIndex, double key) {
        return Arrays.binarySearch(values, fromIndex, toIndex, key);
    }

    /**
     * Returns an integer array containing the sequence of elements in this
     * list.
     * @return an integer array containing the sequence of elements in this
     * list
     */
    public double[] toArray() {
        return values.clone();
    }

    /**
     * Returns a string representation of this list that is
     * obtained by calling {@code java.util.Arrays.toString(this.toArray())}.
     *
     * @return a string representation of this list
     */
    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
