package vcf;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class VcfGeneticMap implements GeneticMap {
    /**
     * Returns the base position corresponding to the specified genetic map
     * position. If the genetic position is not a map position then the base
     * position is estimated from the nearest genetic map positions using
     * linear interpolation.
     *
     * @param chrom           the chromosome index
     * @param geneticPosition the genetic position on the chromosome
     * @return the base position corresponding to the specified genetic map
     * position
     * @throws IllegalArgumentException  if the calculated base position
     *                                   exceeds {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException  if this genetic map has no
     *                                   map positions for the specified chromosome
     * @throws IndexOutOfBoundsException if
     *                                   {@code chrom < 0 || chrom >= ChromIds.instance().size()}
     */
    @Override
    public int basePos(int chrom, double geneticPosition) {
        // VcfGeneticMap cannot go from genetic to physical position
        // At this time, this method is not used
        throw new NotImplementedException();
    }

    /**
     * Returns the genetic map position of the specified marker. The
     * genetic map position is estimated using linear interpolation.
     *
     * @param marker a genetic marker
     * @return the genetic map position of the specified marker
     * @throws IllegalArgumentException if this genetic map has no
     *                                  map positions for the specified chromosome
     * @throws NullPointerException     if {@code marker == null}
     */
    @Override
    public double genPos(Marker marker) {
        return marker.geneticPos();
    }

    /**
     * Returns the genetic map position of the specified genome coordinate.
     * The genetic map position is estimated using linear interpolation.
     *
     * @param chrom        the chromosome index
     * @param basePosition the base coordinate on the chromosome
     * @return the genetic map position of the specified genome coordinate
     * @throws IllegalArgumentException  if this genetic map has no
     *                                   map positions for the specified chromosome
     * @throws IndexOutOfBoundsException if
     *                                   {@code chrom < 0 || chrom >= ChromIds.instance().size()}
     */
    @Override
    public double genPos(int chrom, int basePosition) {
        throw new NotImplementedException();
    }
}
