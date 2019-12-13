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
package vcf;

import blbutil.Filter;
import blbutil.Utilities;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code FilterUtil} contains static methods for constructing
 * marker filters.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class FilterUtil {

    private FilterUtil() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns a filter that excludes markers that have an identifier
     * or genome coordinates that matches a line of the specified file,
     * or returns {@code null} if the {@code excludeMarkersFile} parameter is
     * {@code null}. Genome coordinates must be in "CHROM:POS" format.
     * @param excludeMarkersFile a file that contains an identifier
     * or genome coordinate of one excluded marker on each line
     * @return a filter that excludes markers that have an identifier
     * or genome coordinates that matches a line of the specified file,
     * or {@code null} if the {@code excludeMarkersFile} parameter is
     * {@code null}
     *
     * @throws IllegalArgumentException if the specified file does not exist
     * @throws IllegalArgumentException if the specified file is a directory
     * @throws IllegalArgumentException if any line of the specified
     * file contains two non-white-space characters separated by one or
     * more white-space characters
     */
    public static Filter<Marker> markerFilter(File excludeMarkersFile) {
        Set<String> excludeIds;
        if (excludeMarkersFile==null) {
            return Filter.acceptAllFilter();
        }
        else {
            excludeIds = Utilities.idSet(excludeMarkersFile);
            return idFilter(excludeIds);
        }
    }

    /**
     * Returns a filter that excludes samples that have an identifier
     * that matches a line of the specified file, or returns {@code null} if
     * the {@code excludeSamplesFile} parameter is {@code null}
     * @param excludeSamplesFile a file which contains an identifier
     * of one excluded sample on each line
     * @return a filter that excludes samples that have an identifier
     * that matches a line of the specified file, or {@code null} if
     * the {@code excludeSamplesFile} parameter is {@code null}
     *
     * @throws IllegalArgumentException if the specified file does not exist
     * @throws IllegalArgumentException if the specified file is a directory
     * @throws IllegalArgumentException if any line of the specified
     * file contains two non-white-space characters separated by one or
     * more white-space characters
     */
    public static Filter<String> sampleFilter(File excludeSamplesFile) {
        if (excludeSamplesFile==null) {
            return Filter.acceptAllFilter();
        }
        else {
           Set<String> exclude = Utilities.idSet(excludeSamplesFile);
           return Filter.excludeFilter(exclude);
        }
    }

    /**
     * Returns {@code true} if the specified marker has an identifier
     * is in the specified set, or if ("marker.chrom()" + ":" + "marker.pos()")
     * is in the specified set, and returns {@code false} otherwise.
     * @param marker a marker
     * @param set a set of marker identifiers and chromosome positions in
     * "CHROM:POS" format
     * @return {@code true} if the specified marker has an identifier
     * is in the specified set or if ("marker.chrom()" + ":" + "marker.pos()")
     * is in the specified set
     * @throws NullPointerException if {@code marker == null || set == null}
     */
    public static boolean markerIsInSet(Marker marker, Set<String> set) {
        for (int j=0, n=marker.nIds(); j<n; ++j) {
            if (set.contains(marker.id(j))) {
                return true;
            }
        }
        String posId = marker.chrom() + ':' + marker.pos();
        return set.contains(posId);
    }

    /**
     * Returns a filter that accepts all markers which do not have an
     * identifier or chromomsome position present in the specified
     * collection.
     * A marker is excluded if {@code exclude.contains(marker.id(j)) == true}
     * for any {@code 0 <= j < marker.nIds()} or if
     * {@code exclude.contains(marker.chrom() + ":" + marker.pos()) == true}.
     * @param exclude a collection of marker identifiers and chromosome
     * positions in "CHROM:POS" format
     * @return a filter that accepts all markers which do not have an
     * identifier or chromomsome position present in the specified
     * collection
     * @throws NullPointerException if {@code exclude == null}
     */
    public static Filter<Marker> idFilter(Collection<String> exclude) {
        final Set<String> excludeSet = new HashSet<>(exclude);
        if (excludeSet.isEmpty()) {
            return Marker -> true;
        }
        else {
            return (Marker marker) -> !markerIsInSet(marker, excludeSet);
        }
    }
}
