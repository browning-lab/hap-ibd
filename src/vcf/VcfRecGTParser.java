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

/**
 * <p>Class {@code VcfRecGTParser} parses VCF records and extracts the GT format
 * field.
 * </p>
 * <p>Instances of class {@code VcfRecGTParser} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class VcfRecGTParser {

    private final VcfHeader vcfHeader;
    private final Samples samples;
    private final String vcfRec;
    private final Marker marker;
    private final int nAlleles;
    private final int nSamples;
    private final int ninthTabPos;

    /**
     * Constructs a new {@code VcfRecGTParser} object from the specified VCF
     * record.
     * @param vcfHeader the VCF meta-information lines and header line
     * @param vcfRec the VCF record
     * @throws IllegalArgumentException if {@code vcfHeader.nSamples() == 0}
     * @throws IllegalArgumentException if a format error is detected in the
     * {@code vcfRecord}
     * @throws NullPointerException if
     * {@code vcfHeader == null || vcfRec == null}
     */
    public VcfRecGTParser(VcfHeader vcfHeader, String vcfRec) {
        if (vcfHeader.nSamples()==0) {
            throw new IllegalArgumentException("nSamples==0");
        }
        this.vcfHeader = vcfHeader;
        this.samples = vcfHeader.samples();
        this.vcfRec = vcfRec;
        this.marker = new BasicMarker(vcfRec);
        this.nAlleles = marker.nAlleles();
        this.nSamples = vcfHeader.nSamples();
        this.ninthTabPos = ninthTabPos(vcfRec);
    }

    static int ninthTabPos(String vcfRec) {
        int pos = -1;
        for (int j=0; j<9; ++j) {
            pos = vcfRec.indexOf(Const.tab, pos + 1);
            if (pos == -1) {
                throw new IllegalArgumentException(
                        "VCF record format error: " + vcfRec);
            }
        }
        return pos;
    }

    /**
     * Returns the VCF meta-information lines and header line for the backing
     * VCF record
     * @return the VCF meta-information lines and header line
     */
    public VcfHeader vcfHeader() {
        return vcfHeader;
    }

    /**
     * Returns the backing VCF record.
     * @return the backing VCF record
     */
    public String vcfRecord() {
        return vcfRec;
    }

    /**
     * Returns the marker.
     * @return the marker
     */
    public Marker marker() {
        return marker;
    }

    /**
     * Returns {@code this.marker().nAlleles()}.
     * @return the number of alleles
     */
    public int nAlleles() {
        return nAlleles;
    }

    /**
     * Returns the list of samples.
     * @return the list of samples
     */
    public Samples samples() {
        return vcfHeader.samples();
    }

    /**
     * Returns the number of samples.
     * @return the number of samples
     */
    public int nSamples() {
        return nSamples;
    }

    /**
     * Stores the genotypes genotypes in the specified long arrays.  The
     * contract for this method is undefined if any bit in an array is set
     * when the method is invoked, or if any array does not have
     * sufficient length for storing the record data.
     * @param allele1 a long array in which the first allele for each
     * sample is stored
     * @param allele2 a long array set in which the second allele for each
     * sample is stored
     * @param isMissing1 a long array whose {@code k}-th bit will be set
     * if the first allele of the {@code k}-th sample is missing
     * @param isMissing2 a long array set whose {@code k}-th bit will be set
     * if the second allele of the {@code k}-th sample is missing
     * @param isPhased a along array whose {@code k}-th bit will be set
     * if the phased allele separator is present in the {@code k}-th sample
     * @throws IllegalArgumentException if a format error is detected in the
     * VCF record
     * @throws NullPointerException if any parameter is {@code null}
     */
    public void storeAlleles(long[] allele1, long[] allele2,
            long[] isMissing1, long[] isMissing2, long[] isPhased) {
        int bitsPerLong = 6;
        int bitsPerAllele = bitsPerAllele(marker);
        int pos = ninthTabPos;
        int unfilt = -1;
        for (int s=0; s<nSamples; ++s) {
            if (pos == -1) {
                throwFieldCountError(vcfHeader, vcfRec);
            }
            int nextUnfiltered = vcfHeader.unfilteredSampleIndex(s);
            while (++unfilt < nextUnfiltered) {
                pos = vcfRec.indexOf(Const.tab, pos + 1);
                if (pos == -1) {
                    throwFieldCountError(vcfHeader, vcfRec);
                }
            }
            int alStart = pos+1;
            int alEnd1 = alEnd1(vcfRec, alStart);
            if (alStart==alEnd1) {
                throwIllegalArgException("missing data", s, false);
            }
            int alEnd2 = alEnd2(vcfRec, alEnd1);
            boolean isDiploid = alEnd1!=alEnd2;
            if (isDiploid!=samples.isDiploid(s)) {
                throwIllegalArgException("inconsistent number of alleles", s, false);
            }
            int a1 = parseAllele(alStart, alEnd1);
            int a2 =  alEnd1==alEnd2 ? a1 : parseAllele(alEnd1 + 1, alEnd2);
            if (isDiploid==false || vcfRec.charAt(alEnd1)==Const.phasedSep) {
                isPhased[s >> bitsPerLong] |= (1L << s);
            }
            if (a1 == -1) {
                isMissing1[s >> bitsPerLong] |= (1L << s);
            }
            else {
                storeAllele(allele1, s, bitsPerAllele, a1);
            }
            if (a2 == -1) {
                isMissing2[s >> bitsPerLong] |= (1L << s);
            }
            else {
                storeAllele(allele2, s, bitsPerAllele, a2);
            }
            pos = vcfRec.indexOf(Const.tab, alEnd2);
        }
    }

    private static int bitsPerAllele(Marker marker) {
        int nAllelesM1 = marker.nAlleles() - 1;
        int nStorageBits = Integer.SIZE - Integer.numberOfLeadingZeros(nAllelesM1);
        return nStorageBits;
    }

    private static void storeAllele(long[] al, int sample, int bitsPerAllele,
            int allele) {
        int bitsPerLong = 6;
        int index = sample*bitsPerAllele;
        int mask = 1;
        for (int k=0; k<bitsPerAllele; ++k) {
            if ((allele & mask)==mask) {
                al[index >> bitsPerLong] |= (1L << index);
            }
            ++index;
            mask <<= 1;
        }
    }

    /**
     * Returns the list of phased alleles in the backing VCF record.
     * @return the list of phased alleles in the backing VCF record
     * @throws IllegalArgumentException if the VCF record contains an
     * unphased or missing genotype
     * @throws IllegalArgumentException if a format error is detected in the
     * VCF record
     */
    private int[] phasedAlleles() {
        int[] alleles = new int[2*nSamples];
        int pos = ninthTabPos;
        int unfilt = -1;
        for (int s=0, hap=0; s<nSamples; ++s) {
            if (pos == -1) {
                throwFieldCountError(vcfHeader, vcfRec);
            }
            int nextUnfiltered = vcfHeader.unfilteredSampleIndex(s);
            while (++unfilt < nextUnfiltered) {
                pos = vcfRec.indexOf(Const.tab, pos + 1);
                if (pos == -1) {
                    throwFieldCountError(vcfHeader, vcfRec);
                }
            }
            int alStart = pos+1;
            int alEnd1 = alEnd1(vcfRec, alStart);
            if (alStart==alEnd1) {
                throwIllegalArgException("missing data", s, true);
            }
            int alEnd2 = alEnd2(vcfRec, alEnd1);
            boolean isDiploid = alEnd1!=alEnd2;
            int a1 = parseAllele(alStart, alEnd1);
            int a2 =  alEnd1==alEnd2 ? a1 : parseAllele(alEnd1 + 1, alEnd2);
            if (isDiploid!=samples.isDiploid(s)) {
                throwIllegalArgException("inconsistent number of alleles", s, true);
            }
            if ((isDiploid && vcfRec.charAt(alEnd1)!=Const.phasedSep)
                    || (a1==-1) || (a2==-1)) {
                throwIllegalArgException("unphased or missing genotype", s, true);
            }
            alleles[hap++] = a1;
            alleles[hap++] = a2;
            pos = vcfRec.indexOf(Const.tab, alEnd2);
        }
        return alleles;
    }

    /* returns exclusive end */
    private static int alEnd1(String rec, int start) {
        if (start==rec.length()) {
            throwGTFormatError(rec, rec.length());
        }
        int index = start;
        while (index < rec.length()) {
            char c = rec.charAt(index);
            if (c == Const.unphasedSep || c == Const.phasedSep
                    || c == Const.tab || c == Const.colon) {
                return index;
            }
            ++index;
        }
        return index;
    }

    /* returns exclusive end */
    private static int alEnd2(String rec, int start) {
        int index = start;
        while (index < rec.length()) {
            char c = rec.charAt(index);
            if (c == Const.colon || c == Const.tab) {
                return index;
            }
            ++index;
        }
        return index;
    }

    private int throwIllegalArgException(String msg, int sample, boolean isRef) {
        String err = "ERROR: " + msg
                + (isRef ? " for reference sample " : " for sample ")
                + vcfHeader.samples().id(sample)
                + " at marker [" + marker + "]";
        throw new IllegalArgumentException(err);
    }

    private int parseAllele(int start, int end) {
        if (start==end) {
            String s = "ERROR: Missing sample allele: " + vcfRec;
            throw new IllegalArgumentException(s);
        }
        int al;
        if (start + 1 == end) {
            char c = vcfRec.charAt(start);
            if (c=='.') {
                return -1;
            }
            else {
                al = (c - '0');
            }
        }
        else {
            al = Integer.parseInt(vcfRec.substring(start, end));
        }
        if (al < 0 || al >= nAlleles) {
            String strAllele = vcfRec.substring(start, end);
            String s = "ERROR: invalid allele [" + strAllele + "]: "
                    + Const.nl + vcfRec;
            throw new IllegalArgumentException(s);
        }
        return al;
    }

    private static void throwGTFormatError(String rec, int index) {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("ERROR: genotype is missing allele separator:");
        sb.append(Const.nl);
        sb.append(rec.substring(0, index));
        sb.append(Const.nl);
        sb.append("Exiting Program");
        sb.append(Const.nl);
        throw new IllegalArgumentException(sb.toString());
    }

    private static void throwFieldCountError(VcfHeader vcfHeader, String vcfRec) {
        String src = vcfHeader.src();
        String[] fields = StringUtil.getFields(vcfRec, Const.tab);
        StringBuilder sb = new StringBuilder(1000);
        sb.append("ERROR: CF header line has ");
        sb.append(vcfHeader.nHeaderFields());
        sb.append(" fields, but data line has ");
        sb.append(fields.length);
        sb.append(" fields");
        sb.append(Const.nl);
        sb.append("File source: ");
        sb.append(src);
        sb.append(Const.nl);
        sb.append(Arrays.toString(fields));
        sb.append(Const.nl);
        throw new IllegalArgumentException(sb.toString());
    }

    /**
     * Returns an array of length {@code this.nAlleles()} whose
     * {@code k}-th element is the list of haplotype indices carrying
     * the {@code k}-th allele if {@code k} is a non-major allele,
     * and whose {@code k}-th element is {@code null} if {@code k} is
     * the major allele.  If there is more than one allele with maximal count,
     * the allele with maximal count having the smallest index is defined to
     * be the major allele.
     * @return the indices of the haplotypes carrying each non-major allele
     * @throws IllegalArgumentException if a format error is detected in
     * the specified VCF record or if the specified VCF header is
     * inconsistent with the specified VCF header.
     *
     * @throws NullPointerException if {@code vcfRec == null || rec == null}
     */
    public int[][] nonMajRefIndices() {
        int[] alleles = phasedAlleles();
        int[] alCnts = new int[nAlleles];
        for (int a : alleles) {
            ++alCnts[a];
        }
        int majAl = 0;
        for (int j=1; j<nAlleles; ++j) {
            if (alCnts[j]>alCnts[majAl]) {
                majAl = j;
            }
        }
        int[][] nonMajIndices = new int[nAlleles][];
        for (int al=0; al<nAlleles; ++al) {
            nonMajIndices[al] = al==majAl ? null : new int[alCnts[al]];
        }
        Arrays.fill(alCnts, 0);
        for (int j=0; j<alleles.length; ++j) {
            int al = alleles[j];
            if (al!=majAl) {
                nonMajIndices[al][alCnts[al]++] = j;
            }
        }
        return nonMajIndices;
    }
}
