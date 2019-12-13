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

import blbutil.Const;
import blbutil.Filter;
import blbutil.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Class {@code VcfHeader} represents the Variant Call Format (VCF)
 * meta-information lines and the Variant Call Format header line
 * that precede the first Variant Call Format record.
 * </p>
 * <p>Instances of class {@code VcfHeader} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class VcfHeader  {

    private static final String SHORT_HEADER_PREFIX= "#CHROM" + Const.tab + "POS"
            + Const.tab + "ID" + Const.tab + "REF" + Const.tab + "ALT"
            + Const.tab + "QUAL" + Const.tab + "FILTER" + Const.tab + "INFO";

    /**
     * A string equal to the first nine tab-delimited fields of a VCF header
     * line that contains sample data.
     */
    public static final String HEADER_PREFIX =
            SHORT_HEADER_PREFIX + Const.tab + "FORMAT";

    private static final int sampleOffset = 9;

    private final String src;
    private final VcfMetaInfo[] metaInfoLines;
    private final int nHeaderFields;
    private final int[] includedIndices;
    private final Samples samples;

    /**
     * Returns a boolean array whose {@code k}-th value is {@code true}
     * if the FORMAT field for the {@code k}-th sample in a VCF record
     * contains an allele separator character and returns {@code false}
     * otherwise.
     * @param vcfRec a VCF record
     * @return  a boolean array whose {@code k}-th value is {@code true}
     * if the FORMAT field for the {@code k}-th sample does not contain an
     * allele separator character

     * @throws NullPointerException if {@code vcfHeader == null || rec == null}
     */
    static boolean[] isDiploid(String vcfRec) {
        List<Boolean> list = new ArrayList<>();
        int start = VcfRecGTParser.ninthTabPos(vcfRec) + 1;
        boolean noAlleleSep = true;
        for (int j=start, n=vcfRec.length(); j<n; ++j)  {
            char c = vcfRec.charAt(j);
            if (c==Const.tab) {
                list.add(noAlleleSep==false);
                noAlleleSep = true;
            }
            else if (c==Const.unphasedSep || c==Const.phasedSep) {
                noAlleleSep = false;
            }
        }
        list.add(noAlleleSep==false);
        boolean[] isDiploid = new boolean[list.size()];
        for (int j=0; j<isDiploid.length; ++j) {
            isDiploid[j] = list.get(j);
        }
        return isDiploid;
    }

    /**
     * Returns a VCF header object for the specified VCF meta information lines
     * and header line. The header line must be the last line in the
     * specified {@code lines} array.
     * @param src a string describing the source of the VCF file
     * @param lines the VCF meta-information and header lines
     * @param isDiploid a boolean array whose {@code k}-th value is {@code true}
     * if the {@code k}-th sample is diploid, and is {@code false} if the
     * {@code k}-th sample is haploid
     * @throws IllegalArgumentException if a format error is encountered
     * in a meta-information line or header lines}
     * @throws NullPointerException if
     * {@code src==null || lines == null || isDiploid == nulle}
     */
    public VcfHeader(String src, String[] lines, boolean[] isDiploid) {
        this(src, lines, isDiploid, Filter.acceptAllFilter());
    }

    /**
     * Returns a VCF header object for the specified VCF meta information lines
     * and header line. The header line must be the last line in the
     * specified {@code lines} array.
     * @param src a string describing the source of the VCF file
     * @param lines the VCF meta-information and header lines
     * @param isDiploid a boolean array whose {@code k}-th value is {@code true}
     * if the {@code k}-th sample is diploid, and is {@code false} if the
     * {@code k}-th sample is haploid
     * @param sampleFilter a sample filter
     * @throws IllegalArgumentException if a format error is encountered
     * in a meta-information line or header lines}
     * @throws NullPointerException if
     * {@code src==null || lines == null || isDiploid == null}
     */
    public VcfHeader(String src, String[] lines, boolean[] isDiploid,
            Filter<String> sampleFilter) {
        if (src==null) {
            throw new NullPointerException(String.class.toString());
        }
        if (sampleFilter==null) {
            sampleFilter = Filter.acceptAllFilter();
        }
        int headerIndex = lines.length-1;
        this.src = src;
        this.metaInfoLines = new VcfMetaInfo[headerIndex];
        for (int j=0; j<headerIndex; ++j) {
            this.metaInfoLines[j] = new VcfMetaInfo(lines[j]);
        }
        checkHeaderLine(lines[headerIndex], src);
        String[] headerFields = StringUtil.getFields(lines[headerIndex], Const.tab);
        this.nHeaderFields = headerFields.length;
        this.includedIndices = includedIndices(headerFields, sampleFilter);
        this.samples = samples(headerFields, isDiploid, includedIndices);
    }

    private static void checkHeaderLine(String line, String src) {
        if (line == null || line.startsWith("#")==false) {
            String s = "Missing line (#CHROM ...) after meta-information lines"
                    + Const.nl + "File source: " + src
                    + Const.nl + line;
            throw new IllegalArgumentException(s);
        }
        if (line.startsWith(HEADER_PREFIX) == false) {
            if (line.equals(SHORT_HEADER_PREFIX)==false) {
                String s = "Missing header line (file source: " + src + ")"
                        + Const.nl + "The first line after the initial meta-information lines"
                        + Const.nl + "does not begin with: "
                        + Const.nl + HEADER_PREFIX
                        + Const.nl + line
                        + Const.nl + "The data fields in the header line must be tab-separated.";
                throw new IllegalArgumentException(s);
            }
        }
    }

    private static int[] includedIndices(String[] headerFields,
            Filter<String> sampleFilter) {
        int nUnfilteredSamples = Math.max(headerFields.length - sampleOffset, 0);
        int[] includedIndices = new int[nUnfilteredSamples];
        int index = 0;
        for (int j=0; j<nUnfilteredSamples; ++j) {
            if (sampleFilter.accept(headerFields[sampleOffset + j])) {
                includedIndices[index++] = j;
            }
        }
        if (index < includedIndices.length) {
            includedIndices = Arrays.copyOf(includedIndices, index);
        }
        return includedIndices;
    }

    private Samples samples(String[] headerFields, boolean[] isDiploid,
            int[] includedIndices) {
        String[] ids = new String[includedIndices.length];
        boolean[] restrictedIsDiploid = new boolean[includedIndices.length];
        for (int j=0; j<ids.length; ++j) {
            ids[j] = headerFields[sampleOffset + includedIndices[j]];
            restrictedIsDiploid[j] = isDiploid[includedIndices[j]];
        }
        return Samples.fromIds(ids, restrictedIsDiploid);
    }

    /**
     * Returns the source from which data are read.  The string representation
     * of the source is undefined and subject to change.
     * @return the source from which data are read
     */
    public String src() {
        return src;
    }

    /**
     * Returns the number of VCF meta-information lines. VCF meta-information
     * lines are lines that precede the VCF header line. A VCF meta-information
     * line must begin with "##".
     *
     * @return the number of VCF meta-information lines
     */
     public int nMetaInfoLines() {
         return metaInfoLines.length;
     }

    /**
      * Returns the specified VCF meta-information line.

      * @param index a VCF meta-information line index
      * @return the specified VCF meta-information line
      *
      * @throws IndexOutOfBoundsException if
      * {@code index < 0 || index >= this.nMetaInfoLines()}
      */
     public VcfMetaInfo metaInfoLine(int index) {
         return metaInfoLines[index];
     }

     /**
      * Returns the number of fields in the VCF header line before sample
      * exclusions.
      * @return the number of fields in the VCF header line before sample
      * exclusions
      */
     public int nHeaderFields() {
         return nHeaderFields;
     }

     /**
      * Returns the number of samples before sample exclusions.
      * @return the number of samples before sample exclusions
      */
     public int nUnfilteredSamples() {
         return Math.max(0, nHeaderFields - sampleOffset);
     }

     /**
      * Returns the index of the specified sample in the original
      * list of samples before sample exclusions.
      * @param sample a sample index
      * @return the index of the specified sample in the original
      * list of samples before sample exclusions
      * @throws IndexOutOfBoundsException if
      * {@code sample < 0 || sample >= this.nSamples()}
      */
     public int unfilteredSampleIndex(int sample) {
         return includedIndices[sample];
     }

     /**
      * Returns the number of samples after sample exclusions.
      * @return the number of samples after sample exclusions
      */
     public int nSamples() {
         return samples.nSamples();
     }

    /**
     * Return the list of samples after sample exclusions.
     * @return the list of samples after sample exclusions
     */
    public Samples samples() {
        return samples;
    }

    /**
     * Returns {@code this.sample().ids()}.
     * @return {@code this.sample().ids()}
     */
    public String[] sampleIds() {
        return samples.ids();
    }

    /**
     * Returns the VCF meta-information lines and the VCF header line after
     * applying sample exclusions.
     * @return the VCF meta-information lines and the VCF header line after
     * applying sample exclusions.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(400);
        for (int j=0; j<metaInfoLines.length; ++j) {
            sb.append(metaInfoLines[j]);
            sb.append(Const.nl);
        }
        String[] sampleIds = samples.ids();
        sb.append(HEADER_PREFIX);
        for (String id : sampleIds) {
            sb.append(Const.tab);
            sb.append(id);
        }
        sb.append(Const.nl);
        return sb.toString();
    }
}
