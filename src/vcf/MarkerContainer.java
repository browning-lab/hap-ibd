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

/**
 * Interface {@code MarkerContainer} represents an object that stores
 * a unique {@code vcf.Marker} instance.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public interface MarkerContainer {

    /**
     * Returns the marker.
     * @return the marker
     */
    Marker marker();

    /**
     * Returns the number of marker alleles.
     * @return the number of marker alleles.
     */
    int nAlleles();
}
