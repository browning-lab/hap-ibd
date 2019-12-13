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

import java.util.Comparator;

/**
 * <p>Interface {@code IntInterval} represents an interval of
 * consecutive integers.
 * </p>
 * Instances of class {@code IntInterval} are immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface IntInterval {

    /**
     * Returns the start of the interval (inclusive).
     * @return the start of the interval (inclusive).
     */
    public int start();

    /**
     * Returns the end of the interval (inclusive).
     * @return the end of the interval (inclusive).
     */
    public int inclEnd();

    /**
     * Returns a {@code Comparator<IntInterval>} which orders
     * {@code IntInterval} objects in order of increasing {@code this.start()}
     * value and orders {@code IntInterval} objects with the same
     * {@code this.start()} value in order of increasing {@code this.inclEnd()}
     * value.
     * @return a {@code Comparator<IntInterval>} object
     */
    public static Comparator<IntInterval> incEndComp() {
        return (IntInterval t1, IntInterval t2) -> {
            if (t1.start() != t2.start()) {
                return (t1.start() < t2.start()) ? -1 : 1;
            }
            else if (t1.inclEnd() != t2.inclEnd()) {
                return (t1.inclEnd() < t2.inclEnd()) ? -1 : 1;
            }
            return 0;
        } ;
    }

    /**
     * Returns a {@code Comparator<IntInterval>} which orders
     * {@code IntInterval} objects in order of increasing {@code this.start()}
     * value and orders {@code IntInterval} objects with the same
     * {@code this.start()} value in order of decreasing {@code this.inclEnd()}
     * value.
     * @return a {@code Comparator<IntInterval>} object
     */
    public static Comparator<IntInterval> decEndComp() {
        return (IntInterval t1, IntInterval t2) -> {
            if (t1.start() != t2.start()) {
                return (t1.start() < t2.start()) ? -1 : 1;
            }
            else if (t1.inclEnd() != t2.inclEnd()) {
                return (t1.inclEnd() > t2.inclEnd()) ? -1 : 1;
            }
            return 0;
        } ;
    }
}
