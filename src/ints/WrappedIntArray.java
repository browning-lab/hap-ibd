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
 * <p>Class {@code WrappedIntArray} represents an immutable
 * {@code int[]} array.
 * </p>
 * Instances of {@code WrappedIntArray} are immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class WrappedIntArray implements IntArray {

    private final int[] ia;

    /**
     * Constructs a new {@code WrappedIntArray} instance.
     * @param ia an array of integers
     * @throws NullPointerException if {@code ia == null}
     */
    public WrappedIntArray(int[] ia) {
        this.ia = ia.clone();
    }

    /**
     * Constructs a new {@code WrappedIntArray} instance.
     * @param ia an array of integers
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public WrappedIntArray(int[] ia, int valueSize) {
        this.ia = new int[ia.length];
        for (int j=0; j<ia.length; ++j) {
            if (ia[j]<0 || ia[j]>=valueSize) {
                throw new IllegalArgumentException(String.valueOf(ia[j]));
            }
            this.ia[j] = ia[j];
        }
    }

    /**
     * Constructs a new {@code WrappedIntArray} instance.
     * @param il a list of integers
     * @throws NullPointerException if {@code il == null}
     */
    public WrappedIntArray(IntList il) {
        this.ia = il.toArray();
    }

    /**
     * Constructs a new {@code WrappedIntArray} instance.
     * @param il a list of integers
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if
     * {@code (il[j] < 0 || il[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < il.length)}
     * @throws NullPointerException if {@code il == null}
     */
    public WrappedIntArray(IntList il, int valueSize) {
        this.ia = new int[il.size()];
        for (int j=0; j<ia.length; ++j) {
            int value = il.get(j);
            if (value<0 || value>=valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            ia[j] = value;
        }
    }

    @Override
    public int size() {
        return ia.length;
    }

    @Override
    public int get(int index) {
        return ia[index];
    }

    @Override
    public String toString() {
        return Arrays.toString(ia);
    }
}
