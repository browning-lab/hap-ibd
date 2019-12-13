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
 * <p>Class {@code ShiftedByteIndexArray} represents an immutable
 * array of integer values between 0 and 255 inclusive that is stored
 * as a {@code byte[]} array whose values have been translated by -128.
 * </p>
 * <p>
 * Instances of {@code UnsignedByteArray} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class UnsignedByteArray implements IntArray {

    private final byte[] ba;

    /**
     * Constructs a new {@code UnsignedByteArray} instance from
     * the specified data.
     * @param ba an array of bytes which are interpreted as unsigned byte
     * values between 0 and 255
     * @throws NullPointerException if {@code ba == null}
     */
    public UnsignedByteArray(byte[] ba) {
        this.ba = ba.clone();
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from the
     * specified data.
     * @param ba an array of bytes which are interpreted as unsigned byte
     * values between 0 and 255
     * @param from the first element to be included (inclusive)
     * @param to the last element to be included (exclusive)
     * @throws IndexOutOfBoundsException if {@code (from < 0 || to > ia.length)}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code ba == null}
     */
    public UnsignedByteArray(byte[] ba, int from, int to) {
        this.ba = Arrays.copyOfRange(ba, from, to);
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from
     * the specified data.
     * @param ia an array of positive integer values whose lower order byte
     * will be stored
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > 255)} for any index {@code j}
     * satisfying {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public UnsignedByteArray(int[] ia) {
        this(ia, 0, ia.length);
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from the specified data.
     * @param il an list of integer values between 0 and 255 inclusive
     * @throws IllegalArgumentException if
     * {@code (il.get(j) < 0 || il.get(j) > 255)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < il.size())}
     * @throws NullPointerException if {@code il == null}
     */
    public UnsignedByteArray(IntList il) {
        this(il, 0, il.size());
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from
     * the specified data.
     * @param ia an array of nonnegative  integer values
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if {@code valueSize < 1 || valueSize > 256}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public UnsignedByteArray(int[] ia, int valueSize) {
        if (valueSize < 1 || valueSize > 256) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        this.ba = new byte[ia.length];
        for (int j=0; j<ia.length; ++j) {
            if (ia[j]<0 || ia[j]>=valueSize) {
                throw new IllegalArgumentException(String.valueOf(ia[j]));
            }
            this.ba[j] = (byte) ia[j];
        }
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from
     * the specified data.
     * @param il an list of nonnegative integers
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if
     * {@code (valueSize < 1) || (valueSize > 256)}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code il == null}
     */
    public UnsignedByteArray(IntList il, int valueSize) {
        if (valueSize < 1 || valueSize > 256) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        this.ba = new byte[il.size()];
        for (int j=0; j<ba.length; ++j) {
            int value = il.get(j);
            if (value<0 || value>=valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            this.ba[j] = (byte) value;
        }
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from the
     * specified data.
     * @param ia an array of integer values between 0 and 255 inclusive
     * @param from the first element to be included (inclusive)
     * @param to the last element to be included (exclusive)
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > 255)} for any index {@code j}
     * satisfying {@code (j >= from && j < to)}
     * @throws IndexOutOfBoundsException if {@code (from < 0 || to > ia.length)}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code ia == null}
     */
    public UnsignedByteArray(int[] ia, int from, int to) {
        this.ba = new byte[to - from];
        for (int j=from; j<to; ++j) {
            if (ia[j] < 0 || ia[j] > 255) {
                throw new IllegalArgumentException(String.valueOf(ia[j]));
            }
            ba[j - from] = (byte) ia[j];
        }
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from the
     * specified data.
     * @param il an list of integer values between 0 and 255 inclusive
     * @param from the first element to be included (inclusive)
     * @param to the last element to be included (exclusive)
     * @throws IllegalArgumentException if
     * {@code (il.get(j) < 0 || il.get(j) > 255)} for any index {@code j}
     * satisfying  {@code (j >= from && j < to)}
     * @throws IndexOutOfBoundsException if {@code from < 0 || to > il.length}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code il == null}
     */
    public UnsignedByteArray(IntList il, int from, int to) {
        this.ba = new byte[to - from];
        for (int j=from; j<to; ++j) {
            int value = il.get(j);
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            ba[j - from] = (byte) value;
        }
    }

    @Override
    public int size() {
        return ba.length;
    }

    @Override
    public int get(int index) {
        return ba[index] & 0xff;
    }

    @Override
    public String toString() {
        return IntArray.asString(this);
    }
}
