package vcf;

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
     * @throws UnsupportedOperationException Not implemented for VcfGeneticMap
     */
    @Override
    public int basePos(int chrom, double geneticPosition) {
        // VcfGeneticMap cannot go from genetic to physical position
        // At this time, this method is not used
        throw new UnsupportedOperationException("VcfGeneticMap cannot map from genetic to physical position");
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
     * @throws UnsupportedOperationException Not implemented for VcfGeneticMap
     */
    @Override
    public double genPos(int chrom, int basePosition) {
        throw new UnsupportedOperationException("VcfMap cannot interpolate positions.");
    }
}
