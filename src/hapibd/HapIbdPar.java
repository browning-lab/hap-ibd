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
package hapibd;

import beagleutil.ChromInterval;
import blbutil.Const;
import blbutil.Validate;
import java.io.File;
import java.util.Map;

/**
 * <p>Class {@code HapIbdPar} represents the command line parameters and default
 * parameters for the hap-ibd program.</p>
 *
 * <p>Class {@code HapIbdPar} is immutable.</p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class HapIbdPar {

    private final String[] args;

    // data input/output parameters
    private final File gt;
    private final File map;
    private final boolean useVcfMap;
    private final String out;
    private final File excludesamples;

    // algorithm parameters
    private final int min_mac;
    private final float min_seed;
    private final int max_gap;
    private final float min_extend;
    private final float min_output;
    private final int min_markers;
    private final int nthreads;

    private static final int DEF_MIN_MAC = 2;
    private static final float DEF_MIN_SEED = 2.0f;
    private static final float DEF_MIN_EXTEND = 1.0f;
    private static final int DEF_MAX_GAP = 1000;
    private static final float DEF_MIN_OUTPUT = 2.0f;
    private static final int DEF_MIN_MARKERS = 100;
    private static final int DEF_NTHREADS = Runtime.getRuntime().availableProcessors();

    /**
     * Constructs an {@code HapIbdPar} object that represents the
     * command line arguments for the HapIBD program.  See the
     * {@code usage()} method for a description of the command line
     * parameters.   The constructor exists with an error message if
     * a command line parameter name is not recognized.
     *
     * @param args the command line arguments
     * @throws IllegalArgumentException if the command line parameters
     * are incorrectly specified
    */
    public HapIbdPar(String[] args) {
        int IMAX = Integer.MAX_VALUE;
        float FMIN = Float.MIN_VALUE;
        float FMAX = Float.MAX_VALUE;
        this.args = args.clone();
        Map<String, String> argsMap = Validate.argsToMap(args, '=');

        // data input/output parameters
        gt = Validate.getFile(Validate.stringArg("gt", argsMap, true, null,
                null));
        useVcfMap = Validate.booleanArg("vcf-has-cm", argsMap, false, false);
        map = Validate.getFile(Validate.stringArg("map", argsMap, false, null, null));
        out = Validate.stringArg("out", argsMap, true, null, null);
        excludesamples = Validate.getFile(Validate.stringArg("excludesamples",
                argsMap, false, null, null));

        // algorithm parameters
        min_mac = Validate.intArg("min-mac", argsMap, false, DEF_MIN_MAC, 1, IMAX);
        min_seed = Validate.floatArg("min-seed", argsMap, false, DEF_MIN_SEED, FMIN, FMAX);
        max_gap = Validate.intArg("max-gap", argsMap, false, DEF_MAX_GAP, -1, IMAX);
        float minMinExtend = Math.min(min_seed, DEF_MIN_EXTEND);
        min_extend = Validate.floatArg("min-extend", argsMap, false, minMinExtend, FMIN, min_seed);
        min_output = Validate.floatArg("min-output", argsMap, false, DEF_MIN_OUTPUT, FMIN, FMAX);
        min_markers = Validate.intArg("min-markers", argsMap, false, DEF_MIN_MARKERS, 1, IMAX);
        nthreads = Validate.intArg("nthreads", argsMap, false, DEF_NTHREADS, 1, IMAX);

        Validate.confirmEmptyMap(argsMap);
    }

    /**
     * Returns the command line arguments.
     * @return the command line arguments
     */
    public String[] args() {
        return args.clone();
    }

    /**
     * Returns a string describing the command line arguments.
     * The format of the returned string is unspecified and subject to change.
     * @return a string describing the command line arguments.
     */
    public static String usage() {
        String nl = Const.nl;
        return "Syntax: " + HapIbdMain.COMMAND + " [arguments in format: parameter=value]" + nl
                + nl
                + "Data Parameters: " + nl
                + "  gt=<VCF file with GT field>                         (required)" + nl
                + "  map=<PLINK map file with cM units>                  (required)" + nl
                + "  out=<output file prefix>                            (required)" + nl
                + "  excludesamples=<excluded samples file>              (optional)" + nl + nl

                + "Algorithm Parameters: " + nl
                + "  min-seed=<min cM length of seed segment>            (default: " + DEF_MIN_SEED + ")" + nl
                + "  max-gap=<max base pairs in non-IBS gap>             (default: " + DEF_MAX_GAP + ")" + nl
                + "  min-extend=<min cM length of extension segment>     (default: min(" + DEF_MIN_EXTEND + ", min-seed))" + nl
                + "  min-output=<min cM length of output segment>        (default: " + DEF_MIN_OUTPUT + ")" + nl
                + "  min-markers=<min markers in seed segment>           (default: " + DEF_MIN_MARKERS + ")" + nl
                + "  min-mac=<minimum minor allele count filter>         (default: " + DEF_MIN_MAC + ")" + nl
                + "  nthreads=<number of computational threads>          (default: all CPU cores)" + nl;
    }

    private static ChromInterval parseChromInt(String chrom) {
        ChromInterval ci = ChromInterval.parse(chrom);
        if (chrom!=null && ci==null) {
            throw new IllegalArgumentException("Invalid chrom parameter: " + chrom);
        }
        return ci;
    }

    // data input/output parameters

    /**
     * Returns the gt parameter.
     * @return the gt parameter
     */
    public File gt() {
        return gt;
    }

    /**
     * Returns the map parameter.
     * @return the map parameter
     */
    public File map() {
        return map;
    }

    /**
     * Returns if we should get genetic position from the VCF
     * @return useVcfMap boolean
     */
    public boolean useVcfMap() {
        return useVcfMap;
    }

    /**
     * Returns the out parameter.
     * @return the out parameter
     */
    public String out() {
        return out;
    }

    /**
     * Returns the excludesamples parameter or {@code null}
     * if no excludesamples parameter was specified.
     *
     * @return the excludesamples parameter or {@code null}
     * if no excludesamples parameter was specified
     */
    public File excludesamples() {
        return excludesamples;
    }

    // algorithm parameters

    /**
     * Returns the min-mac parameter.
     * @return the min-mac parameter
     */
    public int min_mac() {
        return min_mac;
    }

    /**
     * Returns the min-seed parameter.
     * @return the min-seed parameter
     */
    public float min_seed() {
        return min_seed;
    }

    /**
     * Returns the min-markers parameter.
     * @return the min-markers parameter
     */
    public int min_markers() {
        return min_markers;
    }

    /**
     * Returns the max-gap parameter.
     * @return the max-gap parameter
     */
    public int max_gap() {
        return max_gap;
    }


    /**
     * Returns the min-ext parameter.
     * @return the min-ext parameter
     */
    public float min_extend() {
        return min_extend;
    }

    /**
     * Returns the min-output parameter.
     * @return the min-output parameter
     */
    public float min_output() {
        return min_output;
    }

    /**
     * Returns the nthreads parameter.
     * @return the nthreads parameter
     */
    public int nthreads() {
        return nthreads;
    }
}
