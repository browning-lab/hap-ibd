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

import beagleutil.ChromInterval;
import java.io.File;

/**
 * <p>Interface {@code GeneticMap} represents a genetic map for one or more
 * chromosomes.
 * </p>
 * <p>Instances of class {@code GeneticMap} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface GeneticMap {

    /**
     * Returns the base position corresponding to the specified genetic map
     * position. If the genetic position is not a map position then the base
     * position is estimated from the nearest genetic map positions using
     * linear interpolation.
     *
     * @param chrom the chromosome index
     * @param geneticPosition the genetic position on the chromosome
     * @return the base position corresponding to the specified genetic map
     * position
     * @throws IllegalArgumentException if the calculated base position
     * exceeds {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if this genetic map has no
     * map positions for the specified chromosome
     * @throws IndexOutOfBoundsException if
     * {@code chrom < 0 || chrom >= ChromIds.instance().size()}
     */
    int basePos(int chrom, double geneticPosition);

    /**
     * Returns the genetic map position of the specified marker. The
     * genetic map position is estimated using linear interpolation.
     *
     * @param marker a genetic marker
     * @return the genetic map position of the specified marker
     * @throws IllegalArgumentException if this genetic map has no
     * map positions for the specified chromosome
     * @throws NullPointerException if {@code marker == null}
     */
    double genPos(Marker marker);

    /**
     * Returns the genetic map position of the specified genome coordinate.
     * The genetic map position is estimated using linear interpolation.
     *
     * @param chrom the chromosome index
     * @param basePosition the base coordinate on the chromosome
     * @return the genetic map position of the specified genome coordinate
     * @throws IllegalArgumentException if this genetic map has no
     * map positions for the specified chromosome
     * @throws IndexOutOfBoundsException if
     * {@code chrom < 0 || chrom >= ChromIds.instance().size()}
     */
    double genPos(int chrom, int basePosition);

    /**
     * Returns a string representation of this genetic map. The exact details
     * of the representation are unspecified and subject to change.
     *
     * @return a string representation of this genetic map
     */
    @Override
    String toString();

    /**
     * Constructs and returns a genetic map from the specified data.
     * If the specified map file is {@code null}, the returned genetic map
     * will convert genome coordinates to genetic units by dividing by
     * 1,000,000.  If {@code (chromInt != null)} the genetic map will
     * be restricted to chromosome {@code chromInt.chrom()}.
     * @param file a PLINK-format genetic map file with cM units
     * @param chromInt a chromosome interval
     * @return a genetic map from the specified data.
     * @throws IllegalArgumentException if any map position is infinite
     * or {@code NaN}
     * @throws NumberFormatException if the base position on any line of the map
     * file is not a parsable integer
     * @throws NumberFormatException if the genetic map position on any
     * line of the map file is not a parsable double
     * @throws IllegalArgumentException if a non-empty line of the specified
     * genetic map file does not contain 4 fields
     * @throws IllegalArgumentException if the map positions on each
     * chromosome are not sorted in ascending order
     * @throws IllegalArgumentException if there are duplicate
     * base positions on a chromosome
     * @throws IllegalArgumentException if all base positions on a chromosome
     * have the same genetic map position
     */
    static GeneticMap geneticMap(File file, ChromInterval chromInt) {
        if (file==null) {
            double scaleFactor = 1e-6;
            return new PositionMap(scaleFactor);
        }
        else {
            if (chromInt==null) {
                return PlinkGenMap.fromPlinkMapFile(file);
            }
            else {
                return PlinkGenMap.fromPlinkMapFile(file, chromInt.chrom());
            }
        }
    }

    /**
     * Returns the an array of length {@code hapPairs.nMarkers()} whose
     * whose {@code j}-th element is the genetic map position
     * of the {@code j}-th marker.
     * @param markers the list of markers
     * @return an array of genetic map positions
     * @throws IllegalArgumentException if this genetic map does not contain a
     * map position for any specified marker
     * @throws NullPointerException if {@code markers == null}
     */
    default double[] genPos(Markers markers) {
        double[] genPos = new double[markers.nMarkers()];
        for (int j=0; j<genPos.length; ++j) {
            genPos[j] = this.genPos(markers.marker(j));
        }
        return genPos;
    }

    /**
     * Returns the an array of length {@code hapPairs.nMarkers()} whose
     * whose {@code j}-th element for {@code j > 0} is the
     * probability of recombination between marker {@code j - 1}
     * and marker {@code j}.  The initial marker on a chromosome has
     * recombination probability 0.
     * Any inter-marker genetic distances less than {@code 1e-7} cM are
     * increased to {@code 1e-7} cM.
     * @param markers the list of markers
     * @param intensity the reciprocal of the expected length of the
     * longest IBD segment at a locus
     * @return an array of inter-marker recombination probabilities
     * @throws IllegalArgumentException if
     * {@code intensity <= 0.0 || Double.isFinite(intensity)==false}
     * @throws IllegalArgumentException if this genetic map does not contain a
     * map position for any specified marker
     * @throws NullPointerException if {@code markers == null}
     */
    default float[] pRecomb(Markers markers, double intensity) {
        if (intensity <= 0.0 || Double.isFinite(intensity)==false) {
            throw new IllegalArgumentException(String.valueOf(intensity));
        }
        double MIN_CM_DIST = 1e-7;
        int chrom = markers.marker(0).chromIndex();
        float[] pRecomb = new float[markers.nMarkers()];
        double c = -intensity;
        double lastGenPos = this.genPos(chrom, markers.marker(0).pos());
        pRecomb[0] = 0f;
        for (int j=1; j<pRecomb.length; ++j) {
            Marker m = markers.marker(j);
            double genPos = this.genPos(markers.marker(j));
            if (m.chromIndex()!= chrom) {
                pRecomb[j] = 0;
            }
            else {
                double genDist = Math.max(Math.abs(genPos - lastGenPos), MIN_CM_DIST);
                pRecomb[j] = (float) -Math.expm1(c*genDist);
            }
            lastGenPos = genPos;
        }
        return pRecomb;
    }
}
