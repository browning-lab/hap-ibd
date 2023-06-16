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

import blbutil.Const;
import blbutil.FileUtil;
import blbutil.Utilities;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Locale;

/**
 * <p>Class {@code HapIbdMain} contains the main() method entry point
 * for the hap-ibd program.</p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class HapIbdMain {

    private static final String EXECUTABLE = "hap-ibd.jar";
    private static final String PROGRAM = EXECUTABLE + "  [ version 1.0, 15Jun23.92f ]";
    private static final String COPYRIGHT = "Copyright (C) 2019-2023 Brian L. Browning";

    /**
     * The java command to run this version of the program.
     */
    static final String COMMAND = "java -jar " + EXECUTABLE;

    private static final String HELP_MESSAGE = "Enter \"" + COMMAND
            + "\" to print a list of command line arguments";

    private static final DecimalFormat DF1 = new DecimalFormat("0.0");
    private static final DecimalFormat DF3 = new DecimalFormat("0.000");

    private HapIbdMain() {
        // private constructor to prevent instantiation
    }

    /**
     * Entry point to the HapIBD program.  See the {@code hapibd.HapIbdPar}
     * class for details of the program arguments.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	Locale.setDefault(Locale.US);
        if (args.length==0 || args[0].toLowerCase().startsWith("help")) {
            System.out.println(PROGRAM);
            System.out.println();
            System.out.println(HapIbdPar.usage());
            System.exit(0);
        }
        HapIbdPar par = new HapIbdPar(args);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                String.valueOf(par.nthreads()));
        checkOutputPrefix(par);

        try (PrintWriter log = FileUtil.printWriter(new File(par.out() + ".log"))) {
            long t0 = System.nanoTime();
            Utilities.duoPrintln(log, startInfo(par));
            long[] stats = PbwtIbdDriver.detectIbd(par);
            long nSamples = stats[0];
            long nMarkers = stats[1];
            Utilities.duoPrint(log, statistics(nSamples, nMarkers));
            Utilities.duoPrintln(log, endInfo(t0));
        }
    }

    private static void checkOutputPrefix(HapIbdPar par) {
        File outPrefix = new File(par.out());
        if (outPrefix.isDirectory()) {
            String s = "ERROR: \"out\" parameter cannot be a directory: \""
                    + par.out() + "\"";
            Utilities.exit(HapIbdPar.usage() + s);
        }
        checkOutputFilename(par, ".hbd.gz");
        checkOutputFilename(par, ".ibd.gz");
        checkOutputFilename(par, ".log");
    }

    private static void checkOutputFilename(HapIbdPar par, String outSuffix) {
        File file = new File(par.out() + outSuffix);
        if (file.equals(par.gt())
                || file.equals(par.map())
                || file.equals(par.excludesamples())) {
            String s = "ERROR: Output file equals input file: " + file;
            Utilities.exit(HapIbdPar.usage() + s);
        }
    }

    private static String startInfo(HapIbdPar par) {
        StringBuilder sb = new StringBuilder(300);
        sb.append(COPYRIGHT);
        sb.append(Const.nl);
        sb.append(HELP_MESSAGE);
        sb.append(Const.nl);
        sb.append(Const.nl);
        sb.append("Program            :  ");
        sb.append(PROGRAM);
        sb.append(Const.nl);
        sb.append("Start Time         :  ");
        sb.append(Utilities.timeStamp());
        sb.append(Const.nl);
        sb.append("Max Memory         :  ");
        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory != Long.MAX_VALUE) {
            long maxMb = maxMemory / (1024*1024);
            sb.append(maxMb);
            sb.append(" MB");
        }
        else {
            sb.append("[no limit])");
        }
        sb.append(Const.nl);
        sb.append(Const.nl);
        sb.append(parameters(par));
        return sb.toString();
    }

    private static String parameters(HapIbdPar par) {
        StringBuilder sb = new StringBuilder(150);
        sb.append("Parameters");
        sb.append(Const.nl);
        sb.append("  gt               :  ");
        sb.append(par.gt());
        sb.append(Const.nl);
        sb.append("  map              :  ");
        sb.append(par.map());
        sb.append(Const.nl);
        sb.append("  out              :  ");
        sb.append(par.out());
        if (par.excludesamples()!=null) {
            sb.append(Const.nl);
            sb.append("  excludesamples   :  ");
            sb.append(par.excludesamples());
        }
        sb.append(Const.nl);
        sb.append("  min-seed         :  ");
        sb.append(par.min_seed());
        sb.append(Const.nl);
        sb.append("  max-gap          :  ");
        sb.append(par.max_gap());
        sb.append(Const.nl);
        sb.append("  min-extend       :  ");
        sb.append(par.min_extend());
        sb.append(Const.nl);
        sb.append("  min-output       :  ");
        sb.append(par.min_output());
        sb.append(Const.nl);
        sb.append("  min-markers      :  ");
        sb.append(par.min_markers());
        sb.append(Const.nl);
        sb.append("  min-mac          :  ");
        sb.append(par.min_mac());
        sb.append(Const.nl);
        sb.append("  nthreads         :  ");
        sb.append(par.nthreads());
        sb.append(Const.nl);
        return sb.toString();
    }

    private static String statistics(long nSamples, long nMarkers) {
        StringBuilder sb = new StringBuilder(300);
        long nHbdSegs = PbwtIbd.nHbdSegs();
        long nIbdSegs = PbwtIbd.nIbdSegs();
        double nHbdSegsPerSample = (double) nHbdSegs /  nSamples;
        double nIbdSegsPerSample = (double) nIbdSegs /  nSamples;
        sb.append("Statistics");
        sb.append(Const.nl);
        sb.append("  samples          :  ");
        sb.append(nSamples);
        sb.append(Const.nl);
        sb.append("  markers          :  ");
        sb.append(nMarkers);
        sb.append(Const.nl);
        sb.append("  IBD segments     :  ");
        sb.append(nIbdSegs);
        sb.append(Const.nl);
        sb.append("  IBD segs/sample  :  ");
        sb.append( DF1.format(nIbdSegsPerSample) );
        sb.append(Const.nl);
        sb.append("  HBD segments     :  ");
        sb.append(nHbdSegs);
        sb.append(Const.nl);
        sb.append("  HBD segs/sample  :  ");
        sb.append( DF3.format(nHbdSegsPerSample) );
        sb.append(Const.nl);
        return sb.toString();
    }

    private static String endInfo(long startNanoTime) {
        StringBuilder sb = new StringBuilder(300);
        long elapsedNanoTime = System.nanoTime() - startNanoTime;
        sb.append(Const.nl);
        sb.append("Wallclock Time:    :  ");
        sb.append(Utilities.elapsedNanos(elapsedNanoTime));
        sb.append(Const.nl);
        sb.append("End Time           :  ");
        sb.append(Utilities.timeStamp());
        return sb.toString();
    }
}
