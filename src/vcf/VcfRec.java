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
import blbutil.StringUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * <p>Class {@code VcfRecord} represents a VCF record.
 * </p>
 * <p>Instances of class {@code VcfRecord} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class VcfRec implements GTRec {

    /**
     * The VCF FORMAT code for log-scaled genotype likelihood data: "GL".
     */
    public static final String GL_FORMAT = "GL";

    /**
     * The VCF FORMAT code for phred-scaled genotype likelihood data: "PL".
     */
    public static final String PL_FORMAT = "PL";

    private static final int sampleOffset = 9;

    private final VcfHeader vcfHeader;
    private final String vcfRecord;
    private final int[] delimiters;
    private final Marker marker;

    private final String[] formatFields;
    private final Map<String, Integer> formatMap;

    private final GTRec gtEm;
    private final float[] gls;

    /**
     * Returns the VCF genotype index for the specified pair of alleles.
     * @param a1 the first allele
     * @param a2 the second allele
     * @return the VCF genotype index for the specified pair of alleles
     * @throws IllegalArgumentException if {@code a1 < 0 || a2 < 0}
     */
    public static int gtIndex(int a1, int a2) {
        if (a1 < 0) {
            throw new IllegalArgumentException("a1<0: " + a1);
        }
        if (a2 < 0) {
            throw new IllegalArgumentException("a2<0: " + a2);
        } else if (a1 < a2) {
            return (a2 * (a2 + 1)) / 2 + a1;
        } else {
            return (a1 * (a1 + 1)) / 2 + a2;
        }
    }

    private VcfRec(VcfHeader vcfHeader, String vcfRecord, boolean useGT,
            boolean useGL, float maxLR) {
        this.vcfHeader = vcfHeader;
        this.vcfRecord = vcfRecord;
        this.delimiters = delimiters(vcfHeader, vcfRecord);
        this.marker = new BasicMarker(vcfRecord);
        this.formatFields = formats(format());
        this.formatMap = formatToIndexMap(vcfHeader, vcfRecord, formatFields);
        boolean storeGT = useGT && formatMap.containsKey("GT");
        boolean storeGL = useGL &&
                (formatMap.containsKey("PL") || formatMap.containsKey("GL"));
        if (storeGT==false && storeGL==false) {
            String s = "Missing required data: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        this.gtEm = storeGT ? new BitSetGTRec(vcfHeader, vcfRecord) : null;
        this.gls = storeGL ? likelihoodsFromGL(maxLR) : null;
    }

    /**
     * Constructs and returns a new {@code VcfRecord} instance from a
     * VCF record and its GT format subfield data
     *
     * @param vcfHeader meta-information lines and header line for the
     * specified VCF record.
     * @param vcfRecord a VCF record with a GL format field corresponding to
     * the specified {@code vcfHeader} object
     * @return a new {@code VcfRecord} instance
     *
     * @throws IllegalArgumentException if the VCF record does not have a
     * GT format field
     * @throws IllegalArgumentException if a VCF record format error is
     * detected
     * @throws IllegalArgumentException if there are not
     * {@code vcfHeader.nHeaderFields()} tab-delimited fields in the
     * specified VCF record
     * @throws NullPointerException if
     * {@code vcfHeader == null || vcfRecord == null}
     */
    public static VcfRec fromGT(VcfHeader vcfHeader, String vcfRecord) {
        boolean useGT = true;
        boolean useGL = false;
        float maxLR = Float.NaN;
        return new VcfRec(vcfHeader, vcfRecord, useGT, useGL, maxLR);
    }

    /**
     * Constructs and returns a new {@code VcfRecord} instance from a
     * VCF record and its GL or PL format subfield data. If both
     * GL and PL format subfields are present, the GL format field will be used.
     * If the maximum normalized genotype likelihood is 1.0 for a sample,
     * then any other genotype likelihood for the sample that is less than
     * {@code lrThreshold} is set to 0.
     *
     * @param vcfHeader meta-information lines and header line for the
     * specified VCF record
     * @param vcfRecord a VCF record with a GL format field corresponding to
     * the specified {@code vcfHeader} object
     * @param maxLR the maximum likelihood ratio
     * @return a new {@code VcfRecord} instance
     *
     * @throws IllegalArgumentException if the VCF record does not have a
     * GL format field
     * @throws IllegalArgumentException if a VCF record format error is
     * detected
     * @throws IllegalArgumentException if there are not
     * {@code vcfHeader.nHeaderFields()} tab-delimited fields in the
     * specified VCF record
     * @throws NullPointerException if
     * {@code vcfHeader == null || vcfRecord == null}
     */
    public static VcfRec fromGL(VcfHeader vcfHeader, String vcfRecord,
            float maxLR) {
        boolean useGT = false;
        boolean useGL = true;
        return new VcfRec(vcfHeader, vcfRecord, useGT, useGL, maxLR);
    }

    /**
     * Constructs and returns a new {@code VcfRecord} instance from a VCF
     * record and its GT, GL, and PL format subfield data.
     * If the GT format subfield is present and non-missing, the
     * GT format subfield is used to determine genotype likelihoods.  Otherwise
     * the GL or PL format subfield is used to determine genotype likelihoods.
     * If both the GL and PL format subfields are present, only the GL format
     * subfield will be used.  If the maximum normalized genotype likelihood
     * is 1.0 for a sample, then any other genotype likelihood for the sample
     * that is less than {@code lrThreshold} is set to 0.
     *
     * @param vcfHeader meta-information lines and header line for the
     * specified VCF record
     * @param vcfRecord a VCF record with a GT, a GL or a PL format field
     * corresponding to the specified {@code vcfHeader} object
     * @param maxLR the maximum likelihood ratio
     * @return a new {@code VcfRecord}
     *
     * @throws IllegalArgumentException if the VCF record does not have a
     * GT, GL, or PL format field
     * @throws IllegalArgumentException if a VCF record format error is
     * detected
     * @throws IllegalArgumentException if there are not
     * {@code vcfHeader.nHeaderFields()} tab-delimited fields in the
     * specified VCF record
     * @throws NullPointerException if
     * {@code vcfHeader == null || vcfRecord == null}
     */
    public static VcfRec fromGTGL(VcfHeader vcfHeader, String vcfRecord,
            float maxLR) {
        boolean useGT = true;
        boolean useGL = true;
        return new VcfRec(vcfHeader, vcfRecord, useGT, useGL, maxLR);
    }

    private static int[] delimiters(VcfHeader vcfHeader, String vcfRecord) {
        int nFields = vcfHeader.nHeaderFields();
        int[] delimiters = new int[nFields + 1];
        delimiters[0] = -1;
        for (int j=1; j<nFields; ++j) {
            delimiters[j] = vcfRecord.indexOf(Const.tab, delimiters[j-1] + 1);
            if (delimiters[j] == -1) {
                fieldCountError(vcfHeader, vcfRecord);
            }
        }
        if (vcfRecord.indexOf(Const.tab, delimiters[nFields-1] + 1) != -1) {
            fieldCountError(vcfHeader, vcfRecord);
        }
        delimiters[nFields] = vcfRecord.length();
        return delimiters;
    }

    private static void fieldCountError(VcfHeader vcfHeader, String vcfRecord) {
        String src = "File source: " + vcfHeader.src();
        String[] fields = StringUtil.getFields(vcfRecord, Const.tab);
        String s = "VCF header line has " + vcfHeader.nHeaderFields()
                + " fields, but data line has " + fields.length + " fields"
                + Const.nl + "File source:" + src
                + Const.nl + Arrays.toString(fields);
        throw new IllegalArgumentException(s);
    }

    /**
     * Return {@code true} if all characters in the specified
     * string are letters or digits and returns {@code false} otherwise.
     * @param s a string.
     * @return {@code true} if all characters in the specified
     * string are letters or digits and returns {@code false} otherwise.
     */
    private static boolean isAlphanumeric(String s) {
        for (int j=0, n=s.length(); j<n; ++j) {
            if (Character.isLetterOrDigit(s.charAt(j))==false) {
                return false;
            }
        }
        return true;
    }

    private String[] formats(String formats) {
        if (formats.equals(Const.MISSING_DATA_STRING) || formats.isEmpty()) {
            String s = "missing format field: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        String[] fields =  StringUtil.getFields(formats, Const.colon);
        for (String f : fields) {
            if (f.isEmpty()) {
                String s = "missing format in format subfield list: " + vcfRecord;
                throw new IllegalArgumentException(s);
            }
            //  Commented-out alpha-numeric check to avoid throwing an
            //    exception when the FORMAT subfield code is not alphanumeric.
//            if (isAlphanumeric(f)==false) {
//                 String s = "format subfield must be alphanumeric (" + f + "): "
//                         + vcfRecord;
//                 throw new IllegalArgumentException(s);
//            }
        }
        return fields;
    }

    private static Map<String, Integer> formatToIndexMap(VcfHeader vcfHeader,
            String vcfRecord, String[] formatFields) {
        if (vcfHeader.nSamples()==0) {
            return Collections.emptyMap();
        }
        Map<String, Integer> map = new HashMap<>(formatFields.length);
        for (int j=0; j<formatFields.length; ++j) {
            map.put(formatFields[j], j);
        }
        if (map.containsKey("GT") && map.get("GT")!=0) {
            String s = "GT format is not first format: " + vcfRecord;
            throw new IllegalArgumentException(s);
        }
        return map;
    }

    /* returns exclusive end */
    private int formatSubfieldEnd(int start) {
        while (start < vcfRecord.length()) {
            char c = vcfRecord.charAt(start);
            if (c == Const.colon || c == Const.tab) {
                return start;
            }
            ++start;
        }
        return start;
    }

    private float[] likelihoodsFromGL(float maxLR) {
        float minLR = 1f/maxLR;
        int nGt = this.marker.nGenotypes();
        String[] dataGL = hasFormat(GL_FORMAT) ? formatData(GL_FORMAT) : null;
        String[] dataPL = hasFormat(PL_FORMAT) ? formatData(PL_FORMAT) : null;
        double[] doubleLike = new double[nGt];
        float[] floatLike = new float[nSamples()*nGt];
        int floatLikeIndex = 0;
        for (int s=0, n=nSamples(); s<n; ++s) {
            Arrays.fill(doubleLike, 0.0);
            if (dataGL != null) {
                String[] fields = getGL(GL_FORMAT, dataGL, s, nGt);
                for (int k=0; k<nGt; ++k) {
                    doubleLike[k] = GL2Like(fields[k]);
                }
            }
            else if (dataPL != null) {
                String[] fields = getGL(PL_FORMAT, dataPL, s, nGt);
                for (int k=0; k<nGt; ++k) {
                    doubleLike[k] = PL2Like(fields[k]);
                }
            }
            rescaleToMax1(doubleLike);
            for (int gt=0; gt<nGt; ++gt) {
                if (doubleLike[gt] >= minLR) {
                    floatLike[floatLikeIndex] = (float) doubleLike[gt];
                }
                ++floatLikeIndex;
            }
        }
        assert floatLikeIndex==floatLike.length;
        return floatLike;
    }

    private String[] getGL(String format, String[] sampleData,
            int sample, int nGt) {
        if (sampleData[sample].equals(Const.MISSING_DATA_STRING)) {
            String[] fields = new String[nGt];
            Arrays.fill(fields, "0");
            return fields;
        }
        else {
            String[] subfields = StringUtil.getFields(sampleData[sample],
                    Const.comma);
            if (subfields.length!=nGt) {
                String s = "unexpected number of " + format + " subfields: "
                        + sampleData(sample, format) + Const.nl
                        + vcfRecord;
                throw new IllegalArgumentException(s);
            }
            for (String subfield : subfields) {
                if (subfield.equals(Const.MISSING_DATA_STRING)) {
                    String s = "missing subfield in " + format + " field: "
                        + sampleData(sample, format) + Const.nl
                        + vcfRecord;
                    throw new IllegalArgumentException(s);
                }
            }
            return subfields;
        }
    }

    private static double GL2Like(String gl) {
        return Math.pow(10.0, Double.parseDouble(gl));
    }

    private static double PL2Like(String pl) {
        return Math.pow(10.0, -Integer.parseInt(pl)/10.0);
    }

    private static void rescaleToMax1(double[] like) {
        double max = max(like);
        if (max == 0.0f) {
            Arrays.fill(like, 1.0);
        }
        else {
            for (int j=0; j<like.length; ++j) {
                like[j] /= max;
            }
        }
    }

    /* returns max{double[] like, double 0.0} */
    private static double max(double[] like) {
        double max = 0.0;
        for (int k=0; k<like.length; ++k) {
            if (like[k] > max) {
                max = like[k];
            }
        }
        return max;
    }

    /**
     * Returns the QUAL field.
     * @return the QUAL field
     */
    public String qual() {
        return vcfRecord.substring(delimiters[5] + 1, delimiters[6]);
    }

    /**
     * Returns the FILTER field.
     * @return the FILTER field
     */
    public String filter() {
        return vcfRecord.substring(delimiters[6] + 1, delimiters[7]);
    }

    /**
     * Returns the INFO field.
     * @return the INFO field
     */
    public String info() {
        return vcfRecord.substring(delimiters[7] + 1, delimiters[8]);
    }

    /**
     * Returns the FORMAT field.  Returns the empty string ("") if the FORMAT
     * field is missing.
     * @return the FORMAT field
     */
    public String format() {
        if (delimiters.length > 9) {
            return vcfRecord.substring(delimiters[8] + 1, delimiters[9]);
        }
        else {
            return "";
        }
    }

    /**
     * Returns the number of FORMAT subfields.
     * @return the number of FORMAT subfields
     */
    public int nFormatSubfields() {
        return formatFields.length;
    }

    /**
     * Returns the specified FORMAT subfield.
     * @param subfieldIndex a FORMAT subfield index
     * @return the specified FORMAT subfield
     *
     * @throws IndexOutOfBoundsException if
     * {@code subfieldIndex < 0 || subfieldIndex >= this.nFormatSubfields()}
     */
    public String formatSubfield(int subfieldIndex) {
        if (formatFields==null) {
            throw new IllegalArgumentException("No format exists");
        }
        return formatFields[subfieldIndex];
    }

    /**
     * Returns {@code true} if the specified FORMAT subfield is
     * present, and returns {@code false} otherwise.
     * @param formatCode a FORMAT subfield code
     * @return {@code true} if the specified FORMAT subfield is
     * present
     */
    public boolean hasFormat(String formatCode) {
        return formatMap.get(formatCode)!=null;
    }

    /**
     * Returns the index of the specified FORMAT subfield if the
     * specified subfield is defined for this VCF record, and returns -1
     * otherwise.
     * @param formatCode the format subfield code
     * @return the index of the specified FORMAT subfield if the
     * specified subfield is defined for this VCF record, and {@code -1}
     * otherwise
     */
    public int formatIndex(String formatCode) {
        Integer index = formatMap.get(formatCode);
        return (index==null) ? -1 : index;
    }

    /**
     * Returns the data for the specified sample.
     * @param sample a sample index
     * @return the data for the specified sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    public String sampleData(int sample) {
        int index = vcfHeader.unfilteredSampleIndex(sample);
        return vcfRecord.substring(delimiters[index + sampleOffset] + 1,
                delimiters[index + sampleOffset + 1]);
    }

    /**
     * Returns the specified data for the specified sample.
     * @param sample a sample index
     * @param formatCode a FORMAT subfield code
     * @return the specified data for the specified sample
     *
     * @throws IllegalArgumentException if
     * {@code this.hasFormat(formatCode)==false}
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    public String sampleData(int sample, String formatCode) {
        Integer formatIndex = formatMap.get(formatCode);
        if (formatIndex==null) {
            String s = "missing format data: " + formatCode;
            throw new IllegalArgumentException(s);
        }
        return VcfRec.this.sampleData(sample, formatIndex);
    }

    /**
     * Returns the specified data for the specified sample.
     * @param sample a sample index
     * @param subfieldIndex a FORMAT subfield index
     * @return the specified data for the specified sample
     *
     * @throws IndexOutOfBoundsException if
     * {@code field < 0 || field >= this.nFormatSubfields()}
     * @throws IndexOutOfBoundsException if
     * {@code sample < 0 || sample >= this.nSamples()}
     */
    public String sampleData(int sample, int subfieldIndex) {
        if (subfieldIndex < 0 || subfieldIndex >= formatFields.length) {
            throw new IndexOutOfBoundsException(String.valueOf(subfieldIndex));
        }
        int index = sampleOffset + vcfHeader.unfilteredSampleIndex(sample);
        int start = delimiters[index] + 1;
        for (int j = 0; j < subfieldIndex; ++j) {
            int end = formatSubfieldEnd(start);
            if (end==vcfRecord.length() || vcfRecord.charAt(end)==Const.tab) {
                return ".";
            }
            else {
                start = end + 1;
            }
        }
        int end = formatSubfieldEnd(start);
        if (end==start) {
            return ".";
        }
        else {
            return vcfRecord.substring(start, end);
        }
    }

    /**
     * Returns an array of length {@code this.nSamples()}
     * containing the specified FORMAT subfield data for each sample.  The
     * {@code k}-th element of the array is the specified FORMAT subfield data
     * for the {@code k}-th sample.
     * @param formatCode a format subfield code
     * @return an array of length {@code this.nSamples()}
     * containing the specified FORMAT subfield data for each sample
     *
     * @throws IllegalArgumentException if
     * {@code this.hasFormat(formatCode) == false}
     */
    public String[] formatData(String formatCode) {
        Integer formatIndex = formatMap.get(formatCode);
        if (formatIndex==null) {
            String s = "missing format data: " + formatCode;
            throw new IllegalArgumentException(s);
        }
        String[] sa = new String[vcfHeader.nSamples()];
        for (int j=0; j<sa.length; ++j) {
            sa[j] = sampleData(j, formatIndex);
        }
        return sa;
    }

    @Override
    public Samples samples() {
        return vcfHeader.samples();
    }


    @Override
    public int nSamples() {
        return vcfHeader.nSamples();
    }

    /**
     * Returns the VCF meta-information lines and the VCF header line.
     * @return the VCF meta-information lines and the VCF header line
     */
    public VcfHeader vcfHeader() {
        return vcfHeader;
    }

    @Override
    public Marker marker() {
        return marker;
    }

    @Override
    public int allele1(int sample) {
        return gtEm == null ? -1 : gtEm.allele1(sample);
    }

    @Override
    public int allele2(int sample) {
        return gtEm == null ? -1 : gtEm.allele2(sample);
    }

    @Override
    public int get(int hap) {
        return gtEm == null ? -1 : gtEm.get(hap);
    }

    @Override
    public int[] alleles() {
        if (gtEm==null) {
            return IntStream.range(0, size())
                    .map(h -> -1)
                    .toArray();
        }
        else {
            return gtEm.alleles();
        }
    }

    @Override
    public boolean isPhased(int sample) {
        return gtEm == null ? false : gtEm.isPhased(sample);
    }

    @Override
    public boolean isPhased() {
        return  gtEm == null ? false : gtEm.isPhased();
    }

    @Override
    public boolean isGTData() {
        return gls==null;
    }

    @Override
    public float gl(int sample, int allele1, int allele2) {
        if (gtEm==null
                || (gls!=null
                && (gtEm.allele1(sample) == -1 || gtEm.allele2(sample) == -1))) {
            int n = marker.nAlleles();
            if (allele1 < 0 || allele2 < 0 || allele1 >= n || allele2 >= n) {
                String s = allele1 + " " + allele2 + " " + n;
                throw new ArrayIndexOutOfBoundsException(s);
            }
            int gtIndex = VcfRec.gtIndex(allele1, allele2);
            return gls[(sample*marker.nGenotypes()) + gtIndex];
        }
        else {
            return gtEm.gl(sample, allele1, allele2);
        }
    }

    @Override
    public int nAlleles() {
        return this.marker().nAlleles();
    }

    @Override
    public int size() {
        return 2*vcfHeader.nSamples();
    }

    /**
     * Returns the VCF record.
     * @return the VCF record
     */
    @Override
    public String toString() {
        return vcfRecord;
    }
}
