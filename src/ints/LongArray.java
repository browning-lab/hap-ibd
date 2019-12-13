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
 * <p>Interface {@code LongArray} represents an immutable {@code long[]} array.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class LongArray {

    private final long[] values;

    /**
     * Constructs a {@code LongArray} instance from the specified values.
     * @param values a long array
     * @throws NullPointerException if {@code values == null}
     */
    public LongArray(long[] values) {
        this.values = values.clone();
    }

    /**
     * Returns the number of elements in this {@code LongArray}.
     * @return the number of elements in this {@code LongArray}
     */
    public int size() {
        return values.length;
    }

    /**
     * Returns the specified array element.
     * @param index an array index
     * @return the specified array element
     * @throws IndexOutOfBoundsException if
     * {@code index < 0 ||  index >= this.size()}
     */
    public long get(int index) {
        return values[index];
    }

    /**
     * Returns a copy of the specified array.
     * @param la a list of longs
     * @return a copy of the specified array
     * @throws NullPointerException if {@code ia == null}
     */
    public static long[] toArray(LongArray la) {
        long[] copy = new long[la.size()];
        for (int j=0; j<copy.length; ++j) {
            copy[j] = la.get(j);
        }
        return copy;
    }

    /**
     * Returns a string representation of this {@code LongArray} by applying
     * {@code java.utils.Arrays.toString()} to an equivalent {@code int[]}
     * object.
     *
     * @param ia a list of longs
     * @return a string representation of this {@code LongArray}.
     * @throws NullPointerException if {@code ia == null}
     */
    public static String asString(LongArray ia) {
        return Arrays.toString(toArray(ia));
    }

    /**
     * Returns {@code true} if the specified {@code LongArray} objects
     * represent the same sequence of long values, and returns {@code false}
     * otherwise.
     * @param a a sequence of long values
     * @param b a sequence of long values
     * @return {@code true} if the specified {@code LongArray} objects
     * represent the same sequence of long values
     */
    public static boolean equals(LongArray a, LongArray b) {
        return Arrays.equals(a.values, b.values);
    }
}
