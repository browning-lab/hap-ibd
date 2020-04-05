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

import blbutil.Const;
import vcf.Marker;

/**
 * <p>Class {@code ChromInterval} represents a chromosome interval whose
 * end points are genome coordinates.
 * </p>
 *
 * Instances of class {@code ChromInterval} are immutable.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
*/
public final class ChromInterval implements IntInterval,
        Comparable<ChromInterval> {

    private final int chromIndex;
    private final int start;
    private final int end;

    /**
     * Constructs a new {@code ChromInterval} instance.
     *
     * @param start the first marker in the interval.
     * @param end the last marker in the interval.
     *
     * @throws IllegalArgumentException if
     * {@code start.chromIndex()!=end.chromIndex() ||
     * start.pos()<0 || start.pos()>end.pos()}.
     * @throws NullPointerException if
     * {@code start==null || end==null}.
     */
    public ChromInterval(Marker start, Marker end) {
        if (start.chromIndex() != end.chromIndex()) {
            String s = "start.chromIndex() != end.chromIndex()";
            throw new IllegalArgumentException(s);
        }
        if (start.pos() < 0 || start.pos() > end.pos()) {
            String s = "start=" + start + " end=" + end;
            throw new IllegalArgumentException(s);
        }
        this.chromIndex = start.chromIndex();
        this.start = start.pos();
        this.end = end.pos();
    }

    /**
     * Constructs a new {@code ChromInterval} instance.
     * @param chrom the chromosome,
     * @param start the first genome coordinate in the interval.
     * @param end the last genome coordinate in the interval.
     * @throws IllegalArgumentException if
     * {@code start>end || chrom.isEmpty()}
     * @throws NullPointerException if {@code chrom==null}
     */
    public ChromInterval(String chrom, int start, int end) {
        if (start > end) {
            String s = "start=" + start + " end=" + end;
            throw new IllegalArgumentException(s);
        }
        this.chromIndex = ChromIds.instance().getIndex(chrom);
        this.start = start;
        this.end = end;
    }

    /**
     * <p>Returns a {@code ChromInterval} instance corresponding to the
     * specified string, or returns {@code null} if the specified
     * string does not represent a valid chromosome interval or if the
     * specified string is {@code null}.
     * </p>
     * The string representation of the chromosome interval must have one
     * of the following forms:<br>
     * <pre>
     * [chrom]:[start]-[end]
     * [chrom]
     * [chrom]:
     * [chrom]:[start]-
     * [chrom]:-end
     * </pre>
     * where <br>
     * <br>
     * {@code [chrom]} is a chromosome identifier, and
     * {@code [start]} and {@code [end]} are non-negative
     * integers satisfying {@code [start]<=[end]}.  If the specified
     * string does not contain a start position, the {@code start()}
     * method of the returned {@code ChromInterval} instance returns
     * {@code Integer.MIN_VALUE}. If no end position is specified,
     * the {@code end()} method of the returned {@code ChromInterval}
     * instance returns {@code Integer.MAX_VALUE}.
     *
     * @param str a chromosome interval.
     * @return a {@code ChromInterval} instance corresponding to the
     * specified string, or returns {@code null} if the specified
     * string does not represent a valid chromosome interval or if the
     * specified string is {@code null}.
     */
    public static ChromInterval parse(String str) {
        if (str==null) {
            return null;
        }
        str = str.trim();
        int length = str.length();
        int start = Integer.MIN_VALUE;
        int end = Integer.MAX_VALUE;
        int chrDelim = str.lastIndexOf(Const.colon);
        int posDelim = str.lastIndexOf(Const.hyphen);
        if (length==0) {
            return null;
        }
        else if (chrDelim == -1) {
            return new ChromInterval(str, start, end);
        }
        else if (chrDelim == length-1) {
            return new ChromInterval(str.substring(0, length-1), start, end);
        }
        else {
            if ( (posDelim == -1) || (posDelim <= chrDelim)
                        || (chrDelim == length-2)
                        || (isValidPos(str, chrDelim+1, posDelim)==false)
                        || (isValidPos(str, posDelim+1, length)==false) ) {
                return null;
            }
            if (posDelim > chrDelim + 1) {
                start = Integer.parseInt(str.substring(chrDelim+1, posDelim));
            }
            if (length > posDelim + 1) {
                end = Integer.parseInt(str.substring(posDelim+1, length));
            }
            if (start < 0 || start > end) {
                return null;
            }
        }
        return new ChromInterval(str.substring(0, chrDelim), start, end);
    }

    /* endIndex is exclusive */
    private static boolean isValidPos(String s, int startIndex,
            int endIndex) {
        if (startIndex==endIndex) {
            return true;
        }
        int length = endIndex - startIndex;
        if ((length > 1) && s.charAt(startIndex)==0) {
            return false;
        }
        for (int j=startIndex; j<endIndex; ++j) {
            char c = s.charAt(j);
            if (Character.isDigit(c)==false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the chromosome index.  The chromosome index is equal to
     * {@code ChromIds.indexOf(this.chrom())}.
     * @return the chromosome index.
     */
    public int chromIndex() {
        return chromIndex;
    }

    /**
     * Returns the chromosome identifier.
     * @return the chromosome identifier.
     */
    public String chrom() {
        return ChromIds.instance().id(chromIndex);
    }

    /**
     * Returns the first genome coordinate in this chromosome interval.
     * @return the first genome coordinate in this chromosomet interval.
     */
    @Override
    public int start() {
        return start;
    }

    /**
     * Returns the last genome coordinate in this chromosome interval.
     * @return the last genome coordinate in this chromosome interval.
     */
    @Override
    public int inclEnd() {
        return end;
    }

    /**
     * <p>Compares this {@code ChromInteval} with the specified
     * {@code ChromInterval} instance for order, and
     * returns -1, 0, or 1 depending on whether {@code this}
     * is less than, equal or greater than the specified instance.
     * </p>
     * {@code ChromInterval} objects are ordered first by
     * {@code this.chromIndex()}, then by
     * {@code this.start()}, and finally by {@code this.end()}.
     * All fields are ordered in ascending order.
     * @param o the {@code ChromInterval} to be compared with {@code this}.
     * @return -1, 0, or 1 depending on whether {@code this}
     * is less than, equal or greater than the specified instance.
     */
    @Override
    public int compareTo(ChromInterval o) {
        if (this.chromIndex != o.chromIndex) {
            return (this.chromIndex < o.chromIndex) ? -1 : 1;
        }
        if (this.start != o.start) {
            return (this.start < o.start) ? -1 : 1;
        }
        if (this.end != o.end) {
            return (this.end < o.end) ? -1 : 1;
        }
        return 0;
    }

    /**
     * <p>Returns a hash code value for the object.
     * </p>
     * <p>The hash code is defined by the following calculation:
     * </p>
     * <pre>
        int hash = 7;
        hash = 67 * hash + this.chromIndex();
        hash = 67 * hash + this.start();
        hash = 67 * hash + this.end();
     * </pre>
     * @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.chromIndex;
        hash = 67 * hash + this.start;
        hash = 67 * hash + this.end;
        return hash;
    }

    /**
     * Returns {@code true} if the specified object is a
     * {@code ChromInterval} instance representing the same
     * interval of genome coordinates as {@code this}, and
     * returns {@code false} otherwise.
     *
     * @param obj the object to be compared with {@code this} for
     * equality.
     * @return {@code true} if the specified object is a
     * {@code ChromInterval} instance representing the same
     * interval of genome coordinates as {@code this}, and
     * returns {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChromInterval other = (ChromInterval) obj;
        if (this.chromIndex != other.chromIndex) {
            return false;
        }
        if (this.start != other.start) {
            return false;
        }
        return this.end == other.end;
    }

    /**
     * Returns the string:
     * {@code this.chrom() + ":" + this.start() + "-" + this.end()}
     * @return {@code this.chrom() + ":" + this.start() + "-" + this.end()}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ChromIds.instance().id(chromIndex));
        sb.append(Const.colon);
        sb.append(start);
        sb.append(Const.hyphen);
        sb.append(end);
        return sb.toString();
    }

    /**
     * Returns {@code true} if the specified marker is in this chromosome
     * interval and returns {@code false} otherwise.
     * @param marker a marker
     * @return {@code true} if the specified marker is in this chromosome
     * interval
     * @throws NullPointerException if {@code marker == null}
     */
    public boolean contains(Marker marker) {
        int pos = marker.pos();
        return marker.chromIndex()==chromIndex && start <= pos && pos <= end;
    }

    /**
     * Returns {@code true} if the specified chromosome intervals
     * have non-empty intersection and returns {@code false} otherwise.
     * @param a a chromosome interval.
     * @param b a chromosome interval.
     * @return {@code true} if the specified chromosome intervals
     * have non-empty intersection and returns {@code false} otherwise.
     */
    public static boolean overlap(ChromInterval a, ChromInterval b) {
        if (a.chromIndex() != b.chromIndex()) {
            return false;
        }
        else {
            return (a.start() <= b.inclEnd()) && (b.start() <= a.inclEnd());
        }
    }

    /**
     * Returns the union of the specified overlapping chromosome intervals.
     * @param a a chromosome interval.
     * @param b a chromosome interval.
     * @return the union of the specified overlapping chromosome intervals.
     * @throws IllegalArgumentException if
     * {@code ChromInterval.overlap(a, b)==false}.
     */
    public static ChromInterval merge(ChromInterval a, ChromInterval b) {
        if (overlap(a, b)==false) {
            String s = "non-overlappng intervals: " + a + " " + b;
            throw new IllegalArgumentException(s);
        }
        int start = Math.min(a.start(), b.start());
        int end = Math.max(a.inclEnd(), b.inclEnd());
        return new ChromInterval(a.chrom(), start, end);
    }
}
