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
 * <p>Class {@code PackedIntArray} represents an immutable array of
 * nonnegative integer values, which are stored in compressed form.
 * </p>
 * Instances of {@code PackedIntArray} are immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class PackedIntArray implements IntArray {

    private static final byte maxPackIndex = (byte) Integer.numberOfTrailingZeros(Integer.SIZE);
    private final byte packIndex;

    private final int size;
    private final int[] ia;

    private PackedIntArray(int[] ia, int size, byte packIndex) {
        this.packIndex = packIndex;
        this.size = size;
        this.ia = ia;
    }

    /**
     * Constructs a new {@code PackedIntArray} instance from the specified data.
     * @param ia an array of nonnegative integer values
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public PackedIntArray(int[] ia, int valueSize) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        this.packIndex = packIndex(valueSize);
        byte bitsPerValue = (byte) (1 << packIndex);
        int valuesPerIntM1 = (Integer.SIZE >> packIndex) - 1;

        this.size = ia.length;
        this.ia = new int[(size + valuesPerIntM1)/(valuesPerIntM1+1)];
        for (int j=0; j<size; ++j) {
            int value = ia[j];
            if (value < 0 || value >= valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            this.ia[j >> (maxPackIndex-packIndex)] |= (value << (j & valuesPerIntM1)*bitsPerValue);
        }
    }

    /**
     * Constructs a new {@code PackedIntArray} instance from the specified data.
     * @param il an array of nonnegative integer values
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (il.get(j) < 0 || il.get(j) > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < il.size())}
     * @throws NullPointerException if {@code il == null}
     */
    public PackedIntArray(IntList il, int valueSize) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        this.packIndex = packIndex(valueSize);
        byte bitsPerValue = (byte) (1 << packIndex);
        int valuesPerIntM1 = (Integer.SIZE >> packIndex) - 1;

        this.size = il.size();
        this.ia = new int[(size + valuesPerIntM1)/(valuesPerIntM1+1)];
        for (int j=0; j<size; ++j) {
            int value = il.get(j);
            if (value < 0 || value >= valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            ia[j >> (maxPackIndex-packIndex)] |= (value << (j & valuesPerIntM1)*bitsPerValue);
        }
    }

    /**
     * Constructs and returns a new {@code PackedIntArray} instance from the
     * specified data.
     * @param ba an array of non-negative integer values
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code PackedIntArray} instance
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public static PackedIntArray fromSignedByteArray(byte[] ba, int valueSize) {
        boolean useUnsignedByte = false;
        return fromByteArray(ba, 0, ba.length, valueSize, useUnsignedByte);
    }

    /**
     * Constructs a new {@code PackedIntArray} instance from the
     * specified data.
     * @param ba an array of non-negative integer values
     * @param from the first element to be included (inclusive)
     * @param to the last element to be included (exclusive)
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code PackedIntArray} instance
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws IndexOutOfBoundsException if {@code from < 0 || to > ia.length}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code ia == null}
     */
    public static PackedIntArray fromSignedByteArray(byte[] ba, int from,
            int to, int valueSize) {
        boolean useUnsignedByte = false;
        return fromByteArray(ba, from, to, valueSize, useUnsignedByte);
    }

    /**
     * Constructs a new {@code PackedIntArray} instance from the
     * specified data.
     * @param ba an array of non-negative integer values represented
     * as unsigned bytes
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code PackedIntArray} instance
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws NullPointerException if {@code ia == null}
     */
    public static PackedIntArray fromUnsignedByteArray(byte[] ba, int valueSize) {
        boolean useUnsignedByte = true;
        return fromByteArray(ba, 0, ba.length, valueSize, useUnsignedByte);
    }

    /**
     * Constructs a new {@code PackedIntArray} instance from the
     * specified data.
     * @param ba an array of non-negative integer values represented
     * as unsigned bytes
     * @param from the first element to be included (inclusive)
     * @param to the last element to be included (exclusive)
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code PackedIntArray} instance
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws IndexOutOfBoundsException if {@code from < 0 || to > ia.length}
     * @throws NegativeArraySizeException if {@code to > from}
     * @throws NullPointerException if {@code ia == null}
     */
    public static PackedIntArray fromUnsignedByteArray(byte[] ba, int from,
            int to, int valueSize) {
        boolean useUnsignedByte = true;
        return fromByteArray(ba, from, to, valueSize, useUnsignedByte);
    }

    private static PackedIntArray fromByteArray(byte[] ba, int from, int to,
            int valueSize, boolean useUnsignedValues) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        int mask = useUnsignedValues ? Byte.MAX_VALUE : 0xff;
        byte packIndex = packIndex(valueSize);
        byte bitsPerValue = (byte) (1 << packIndex);
        int valuesPerIntM1 = (Integer.SIZE >> packIndex) - 1;

        int size = to - from;
        int[] ia = new int[(size + valuesPerIntM1)/(valuesPerIntM1+1)];
        for (int j=from; j<to; ++j) {
            int offset = j - from;
            int value = ba[j] & mask;
            if (value < 0 || value >= valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            ia[offset >> (maxPackIndex-packIndex)] |= (value << (offset & valuesPerIntM1)*bitsPerValue);
        }
        return new PackedIntArray(ia, size, packIndex);
    }

    /**
     * Constructs a new {@code PackedIntArray} instance from the
     * specified data.  The specified array represents {@code ba.length/2}
     * unsigned two-byte values.  The {@code j}-th unsigned two-byte value is
     * stored in array elements {@code 2*j} and {@code (2*j + 1)}, with the
     * higher-order byte stored in element {@code 2*j}.
     * @param ba an array of non-negative integer values represented
     * as unsigned two-byte integers.
     * @param valueSize the exclusive end of the range of non-negative
     * array values
     * @return a new {@code PackedIntArray} instance
     * @throws IllegalArgumentException if {@code valueSize < 1}
     * @throws IllegalArgumentException if
     * {@code (ia[j] < 0 || ia[j] > valueSize)} for any index {@code j}
     * satisfying  {@code (j >= 0 && j < ia.length)}
     * @throws IllegalArgumentException if {@code (ba.length & 1) != 0}
     * @throws NullPointerException if {@code ia == null}
     */
    public static PackedIntArray fromUnsignedTwoByteArray(byte[] ba, int valueSize) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        if ((ba.length & 1)!=0) {
            throw new IllegalArgumentException(String.valueOf(ba.length));
        }
        byte packIndex = packIndex(valueSize);
        byte bitsPerValue = (byte) (1 << packIndex);
        int valuesPerIntM1 = (Integer.SIZE >> packIndex) - 1;

        int size = ba.length/2;
        int[] ia = new int[(size + valuesPerIntM1)/(valuesPerIntM1+1)];
        for (int j=0; j<size; ++j) {    // NB: size != ia.length in general
            int value = 0xffff & (((ba[2*j] & 0xff) << 8) | (ba[2*j+1] & 0xff));
            if (value < 0 || value >= valueSize) {
                throw new IllegalArgumentException(String.valueOf(value));
            }
            ia[j >> (maxPackIndex-packIndex)] |= (value << (j & valuesPerIntM1)*bitsPerValue);
        }
        return new PackedIntArray(ia, size, packIndex);
    }

    /**
     * Returns the log base 2 of the smallest number of bits that is a power of
     * 2 and is {@code >= (valueSize - 1)}.
     * @param valueSize the number of values
     * @return the log base 2 of the smallest number of bits that is a power of
     * 2 and is {@code >= (valueSize - 1)}.
     * @throws IllegalArgumentException if {@code valueSize < 1}
     */
    private static byte packIndex(int valueSize) {
        if (valueSize < 1) {
            throw new IllegalArgumentException(String.valueOf(valueSize));
        }
        else if (valueSize==1) {
            return 0;
        }
        else {
            int nextPowerOf2 = roundUpToPowerOfTwo(valueSize);
            int nMaskBits = Integer.numberOfTrailingZeros(nextPowerOf2);

            nextPowerOf2 = roundUpToPowerOfTwo(nMaskBits);
            return (byte) Integer.numberOfTrailingZeros(nextPowerOf2);
        }
    }

    private static int roundUpToPowerOfTwo(int x) {
        x--;
        x |= (x >> 1);  // handle  2 bit numbers
        x |= (x >> 2);  // handle  4 bit numbers
        x |= (x >> 4);  // handle  8 bit numbers
        x |= (x >> 8);  // handle 16 bit numbers
        x |= (x >> 16); // handle 32 bit numbers
        return ++x;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int get(int index) {
        if (index>size) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
        int bitsPerValue = (1 << packIndex);
        int valueMask = (1 << bitsPerValue) - 1;
        int valuesPerIntM1 = (byte) ((Integer.SIZE >> packIndex) - 1);
        return ((ia[index >> (maxPackIndex-packIndex)] >>> (index & valuesPerIntM1)*bitsPerValue) & valueMask);
    }

//    public static void main(String[] args) {
//        int nTests = 10;
//        int size = 20_000;
//        long t0 = System.nanoTime();
//        for (int j=0; j<nTests; ++j) {
//            for (int valueSize=1; valueSize<=5000; ++valueSize) {
//                runTest(size, valueSize);
//            }
//        }
//        long nanos = System.nanoTime() - t0;
//        System.out.println("milis=" + ((nanos >> 20)/nTests));
//    }
//
//    private static void runTest(int size, int valueSize) {
//        IntList il = new IntList(size);
//        for (int j=0; j<size; ++j) {
//            il.add(j % valueSize);
//        }
//        PackedIntArray pba = new PackedIntArray(il, valueSize);
//        for (int j=0; j<pba.size(); ++j) {
//            if (pba.get(j) != ( j % valueSize)) {
//                System.out.println("pba.get(" + j + ")=" + pba.get(j));
//                assert false;
//            }
//        }
//
//        pba = new PackedIntArray(il.toArray(), valueSize);
//        for (int j=0; j<pba.size(); ++j) {
//            if (pba.get(j) != ( j % valueSize)) {
//                System.out.println("pba.get(" + j + ")=" + pba.get(j));
//                assert false;
//            }
//        }
//    }
}
