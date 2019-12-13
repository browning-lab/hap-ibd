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

import blbutil.DoubleArray;
import blbutil.FloatArray;

/**
 * <p>Class {@code MarkerRecombMap} represents genetic map positions and
 * inter-marker haplotype switch probabilities for a sequence of
 * genomic loci.
 * </p>
 * <p>Instances of class {@code MarkerRecombMap} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class MarkerMap {

    private final DoubleArray genPos;
    private final FloatArray genDist;

    /**
     * Returns a new {@code MarkerMap} instance constructed
     * from the specified data
     * @param genMap the genetic map
     * @param markers a list of markers
     * @return a returns new {@code MarkerMap} instance
     * @throws IllegalArgumentException if
     * {@code markers.marker(0).chromIndex()
     *     != markers.marker(markers.nMarkers()-1).chromIndex()}
     * @throws NullPointerException if
     * {@code genMap == null || markers == null}
     */
    public static MarkerMap create(GeneticMap genMap, Markers markers) {
        if (markers.marker(0).chromIndex()
                != markers.marker(markers.nMarkers()-1).chromIndex()) {
            throw new IllegalArgumentException("inconsistent data");
        }
        return new MarkerMap(genMap, markers);
    }

    private MarkerMap(GeneticMap genMap, Markers markers) {
        this(genMap.genPos(markers));
    }

    private MarkerMap(double[] gPos) {
        this.genPos = new DoubleArray(gPos);
        this.genDist = genDist(gPos);
    }

    /**
     * Return a marker map restricted to the specified markers
     * @param indices a list of distinct marker indices in increasing order
     * @return a marker map restricted to the specified markers
     * @throws IndexOutOfBoundsException if there exists {@code j} such that
     * {@code (0 <= j && j < indices.length)} such that
     * {@code (indices[j] < 0 || indices[j] >= this.nMarkers())}
     * @throws IllegalArgumentException if there exists {@code j} such that
     * {@code (1 <= j && j < indices.length)} such that
     * {@code (indices[j] <= indice[j - 1])}
     * @throws NullPointerException if {@code indices == null}
     */
    public MarkerMap restrict(int[] indices) {
        double[] gPos = new double[indices.length];
        gPos[0] = genPos.get(indices[0]);
        for (int j=1; j<indices.length; ++j) {
            if (indices[j] <= indices[j-1]) {
                throw new IllegalArgumentException(String.valueOf(indices[j]));
            }
            gPos[j] = genPos.get(indices[j]);
        }
        return new MarkerMap(gPos);
    }

    private static FloatArray genDist(double[] genPos) {
        float minCmDist = 1e-7f;
        float[] da = new float[genPos.length];
        for (int j=1; j<da.length; ++j) {
            da[j] = (float) (genPos[j] - genPos[j-1]);
            if (da[j] < minCmDist) {
                da[j] = minCmDist;
            }
        }
        return new FloatArray(da);
    }

    /**
     * Returns a {@code DoubleArray} of size {@code this.markers().nMarkers()}
     * whose {@code k}-th element is the genetic map position of the
     * {@code k}-th marker.
     * @return the array of genetic map positions
     */
    public DoubleArray genPos() {
        return genPos;
    }

    /**
     * Return a {@code FloatArray} of size {@code this.markers().nMarkers()}
     * whose {@code k}-th element is the genetic distance between the
     * {@code k}-th target marker and the previous marker, or {@code 0.0}
     * if {@code (k == 0)}.
     * @return a {@code FloatArray} of size {@code this.nTargMarkers()}
     * whose {@code k}-th element is the genetic distance between the
     * {@code k}-th target marker and the previous marker,
     */
    public FloatArray genDist() {
        return genDist;
    }

    /**
     * Returns a map of marker index to the probability of recombination
     * in the interval between the marker and the preceding marker.
     * @param recombIntensity the factor multiplied by genetic distance to
     * obtain the probability of transitioning to a random HMM state.
     * @return a map of marker index to the probability of recombination
     * in the interval between the marker and the preceding marker
     * @throws IllegalArgumentException if
     * {@code intensity <= 0.0 || Float.isFinite(intensity)==false}
     */
    public FloatArray pRecomb(float recombIntensity) {
        if (recombIntensity <= 0.0 || Float.isFinite(recombIntensity)==false) {
            throw new IllegalArgumentException(String.valueOf(recombIntensity));
        }
        double c = -recombIntensity;
        float[] pRecomb = new float[genDist.size()];
        for (int j=1; j<pRecomb.length; ++j) {
            pRecomb[j] = (float) -Math.expm1(c*genDist.get(j));
        }
        return new FloatArray(pRecomb);
    }
}
