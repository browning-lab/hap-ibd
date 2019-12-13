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

/**
/**
 * <p>Class {@code UnsignedByteArray} represents an immutable
 * array of integer values between 0 and 65,535 inclusive that is stored
 * as a {@code char[]} array.
 * </p>
 * Instances of {@code CharArray} are immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class CharArray implements IntArray {

    private final char[] ca;

    /**
     * Constructs a new {@code CharArray} instance from the specified data.
     * The array of bytes is interpreted as a stream of characters with
     * the high byte preceding the low byte.
     * @param ba an array of char values expressed as pairs of bytes.
     * @throws IllegalArgumentException if {@code (ba.length % 1) != 0}
     * @throws NullPointerException if {@code ca == null}
     */
    public CharArray(byte[] ba) {
        if ((ba.length % 1)!=0) {
            throw new IllegalArgumentException(String.valueOf(ba.length));
        }
        this.ca = new char[ba.length/2];
        for (int j=0; j<ba.length; j+=2) {
            int b1 = ba[j] & 0xff;
            int b2 = ba[j+1] & 0xff;
            ca[j/2] = (char) ((b1 << 8) + b2);
        }
    }

    /**
     * Constructs a new {@code CharArray} instance from
     * the specified data.
     * @param ca an array of integer values between 0 and Character.MAX_VALUE
     * inclusive
     * @throws NullPointerException if {@code ca == null}
     */
    public CharArray(char[] ca) {
        this.ca = ca.clone();
    }

    /**
     * Constructs a new {@code CharArray} instance from the specified data.
     * @param ia an array of integers
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > Character.MAX_VALUE)} for any index
     * {@code j} satisfying  {@code (j >= from && j < to)}
     * @throws NullPointerException if {@code ia == null}
     */
    public CharArray(int[] ia) {
        this(ia, 0, ia.length);
    }

    /**
     * Constructs a new {@code UnsignedByteArray} instance from the specified data.
     * @param il an list of integer values between 0 and
     * {@code Character.MAX_VALUE} inclusive
     * @throws IllegalArgumentException if
     * {@code (il.get(j) < 0 || il.get(j) > Character.MAX_VALUE)} for any index
     * {@code j} satisfying  {@code (j >= 0 && j < il.size())}
     * @throws NullPointerException if {@code il == null}
     */
    public CharArray(IntList il) {
        this(il, 0, il.size());
    }

    /**
     * Constructs a new {@code CharArray} instance from the specified data.
     * @param ia an array of nonnegative integers
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if
     * {@code (valueSize < 1) || (valueSize >= (Character.MAX_VALUE + 1))}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public CharArray(int[] ia, int valueSize) {
        if (valueSize < 1 || valueSize > Character.MAX_VALUE+1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        this.ca = new char[ia.length];
        for (int j=0; j<ia.length; ++j) {
            if (ia[j]<0 || ia[j]>=valueSize) {
                throw new IllegalArgumentException(String.valueOf(ia[j]));
            }
            this.ca[j] = (char) ia[j];
        }
    }

    /**
     * Constructs a new {@code CharArray} instance from the specified data.
     * @param il an list of nonnegative integer
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if
     * {@code (valueSize < 1) || (valueSize >= (Character.MAX_VALUE + 1))}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code il == null}
     */
    public CharArray(IntList il, int valueSize) {
        if (valueSize < 1 || valueSize > Character.MAX_VALUE+1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        this.ca = new char[il.size()];
        for (int j=0; j<ca.length; ++j) {
            int value = il.get(j);
            if (value<0 || value>=valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            this.ca[j] = (char) value;
        }
    }

    /**
     * Constructs a new {@code CharArray} instance from the
     * specified data.
     * @param ia an array of integer values between 0 and
     * {@code Character.MAX_VALUE} inclusive
     * @param to the first element to be included (inclusive)
     * @param from the last element to be included (exclusive)
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > Character.MAX_VALUE)} for any index
     * {@code j} satisfying  {@code (j >= from && j < to)}
     * @throws IndexOutOfBoundsException if {@code (from < 0 || to > ia.length)}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code ia == null}
     */
    public CharArray(int[] ia, int to, int from) {
        this.ca = new char[from - to];
        for (int j=to; j<from; ++j) {
            if (ia[j] < 0 || ia[j] > Character.MAX_VALUE) {
                throw new IllegalArgumentException(String.valueOf(ia[j]));
            }
            ca[j - to] = (char) ia[j];
        }
    }

    /**
     * Constructs a new {@code CharArray} instance from the specified data.
     * @param il an list of integer values between 0 and
     * {@code Character.MAX_VALUE} inclusive
     * @param from the first element to be included (inclusive)
     * @param to the last element to be included (exclusive)
     * @throws IllegalArgumentException if
     * {@code (il.get(j) < 0 || il.get(j) > Character.MAX_VALUE)} for any index {@code j}
     * satisfying  {@code (j >= from && j < to)}
     * @throws IndexOutOfBoundsException if {@code from < 0 || to > il.length}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code il == null}
     */
    public CharArray(IntList il, int from, int to) {
        this.ca = new char[to - from];
        for (int j=from; j<to; ++j) {
            int value = il.get(j);
            if (value < 0 || value > Character.MAX_VALUE) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            ca[j - from] = (char) value;
        }
    }

    @Override
    public int size() {
        return ca.length;
    }

    @Override
    public int get(int index) {
        return ca[index];
    }
}
