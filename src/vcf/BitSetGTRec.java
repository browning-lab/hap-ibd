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

import java.util.stream.IntStream;

/**
 * <p>Class {@code BitSetGT} represents genotype emission
 * probabilities for a list of samples at a single marker.
 * The genotype emission probabilities are determined by the called
 * genotypes for the samples.
 * </p>
 * <p>Instances of class {@code BitSetGT} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class BitSetGTRec implements GTRec {

    private static final long BITS_PER_LONG = 6;

    private final int bitsPerAllele;
    private final Marker marker;
    private final Samples samples;
    private final boolean isRefData;

    private final long[][] alleles;
    private final long[] isMissing;
    private final long[] isPhased;

    /**
     * Constructs a new {@code BitSetGT} instance representing
     * the specified VCF record's GT format field data.
     *
     * @param vcfHeader meta-information lines and header line for the
     * specified VCF record.
     * @param vcfRecord a VCF record corresponding to the specified
     * {@code vcfHeader} object
     *
     * @throws IllegalArgumentException if a format error is detected
     * in the VCF record
     * @throws IllegalArgumentException if {@code rec.nSamples() == 0}
     * @throws IllegalArgumentException if the header line
     * or VCF record does not have a "GT" format field
     * @throws NullPointerException if
     * {@code vcfHeader == null || vcfRecord == null}
     */
    public BitSetGTRec(VcfHeader vcfHeader, String vcfRecord) {
        VcfRecGTParser gtp = new VcfRecGTParser(vcfHeader, vcfRecord);
        int nSamples = vcfHeader.nSamples();

        this.bitsPerAllele = bitsPerAllele(gtp.marker());
        this.marker = gtp.marker();
        this.samples = vcfHeader.samples();

        int nSampleWords = nLongWords(nSamples);
        this.alleles = new long[2][nLongWords(nSamples*bitsPerAllele)];
        this.isMissing = new long[nSampleWords];
        this.isPhased = new long[nSampleWords];
        gtp.storeAlleles(alleles[0], alleles[1], isMissing, isMissing, isPhased);
        this.isRefData = isRef(vcfHeader.nSamples(), isPhased, isMissing);
    }

    private static int nLongWords(int nBits) {
        int nLongWords = nBits >> BITS_PER_LONG;
        int mask = (1 << BITS_PER_LONG) - 1;
        if ((nBits & mask)!=0) {
            ++nLongWords;
        }
        return nLongWords;
    }

    private static boolean isRef(int nSamples, long[] isPhased,
            long[] isMissing) {
        if (Long.bitCount(isPhased[0])<Math.min(nSamples, Long.SIZE)) {
            return false;
        }
        else {
            int sumPhased = 0;
            int sumMissing = 0;
            for (int j=0; j<isPhased.length; ++j) {
                sumPhased += Long.bitCount(isPhased[j]);
                sumMissing += Long.bitCount(isMissing[j]);
            }
            return sumMissing==0 && sumPhased==nSamples;
        }
    }

    private static int bitsPerAllele(Marker marker) {
        int nAllelesM1 = marker.nAlleles() - 1;
        int nStorageBits = Integer.SIZE - Integer.numberOfLeadingZeros(nAllelesM1);
        return nStorageBits;
    }

    @Override
    public int nSamples() {
        return samples.nSamples();
    }

    @Override
    public Samples samples() {
        return samples;
    }

    @Override
    public int size() {
        return 2*samples.nSamples();
    }

    @Override
    public Marker marker() {
        return marker;
    }

    @Override
    public boolean isPhased() {
        return isRefData;
    }

    @Override
    public boolean isGTData() {
        return true;
    }

    @Override
    public boolean isPhased(int sample) {
        if (sample >= samples.nSamples()) {
            throw new IndexOutOfBoundsException(String.valueOf(sample));
        }
        return (isPhased[sample>>BITS_PER_LONG] & (1L<<sample)) != 0;
    }

    @Override
    public int allele1(int sample) {
        if (sample >= samples.nSamples()) {
            throw new IndexOutOfBoundsException(String.valueOf(sample));
        }
        boolean missing = (isMissing[sample>>BITS_PER_LONG] & (1L<<sample))!=0;
        return missing ? -1 : allele(alleles[0], sample);
    }

    @Override
    public int allele2(int sample) {
        if (sample >= samples.nSamples()) {
            throw new IndexOutOfBoundsException(String.valueOf(sample));
        }
        boolean missing = (isMissing[sample>>BITS_PER_LONG] & (1L<<sample))!=0;
        return missing ? -1 : allele(alleles[1], sample);
    }

    @Override
    public int get(int hap) {
        if (hap < 0 || hap >= this.size()) {
            throw new IllegalArgumentException(String.valueOf(hap));
        }
        int sample = hap>>1;
        boolean missing = (isMissing[sample>>BITS_PER_LONG] & (1L<<sample))!=0;
        return missing ? -1 : allele(alleles[hap & 0b1], sample);
    }

    private int allele(long[] bits, int sample) {
        int start = bitsPerAllele*sample;
        int end = start + bitsPerAllele;
        int allele = 0;
        int mask = 1;
        for (int j=start; j<end; ++j) {
            if ((bits[j >> BITS_PER_LONG] & (1L << j))!=0) {
                allele += mask;
            }
            mask <<= 1;
        }
        return allele;
    }

    @Override
    public float gl(int sample, int a1, int a2) {
        if (a1 < 0 || a1 >= marker.nAlleles())  {
            String s = "invalid alleles: (" + a1 + "): " + marker;
            throw new IllegalArgumentException(s);
        }
        if (a2 < 0 || a2 >= marker.nAlleles()) {
            String s = "invalid alleles: (" + a2 + "): " + marker;
            throw new IllegalArgumentException(s);
        }
        int obsA1 = allele1(sample);
        int obsA2 = allele2(sample);
        boolean consistent = (obsA1==-1 || obsA1==a1) && (obsA2==-1 || obsA2==a2);
        if (consistent==false && isPhased(sample)==false) {
            consistent = (obsA1==-1 || obsA1==a2) && (obsA2==-1 || obsA2==a1);
        }
        return consistent ? 1.0f : 0.0f;
    }

    @Override
    public int[] alleles() {
        return IntStream.range(0, size())
                .map(h -> get(h))
                .toArray();
    }

    @Override
    public int nAlleles() {
        return this.marker().nAlleles();
    }

    /**
     * Returns the data represented by {@code this} as a VCF
     * record with a GT format field. The returned VCF record
     * will have missing QUAL and INFO fields, will have "PASS"
     * in the filter field, and will have a GT format field.
     * @return the data represented by {@code this} as a VCF
     * record with a GT format field
     */
    @Override
    public String toString() {
        return GTRec.toVcfRec(this);
    }

//    /**
//     * Constructs a new {@code LowMemGT} instance representing
//     * the specified VCF record's GT format field data.
//     *
//     * @param rec a VCF file record.
//     * @param fam parent-offspring relationships.
//     * @param usePhase {@code true} if phase information in the specified
//     * VCF file record will be used, and {@code false} if phase
//     * information in the specified VCF file record will be ignored.
//     *
//     * @throws IllegalArgumentException if
//     * {@code rec.nSamples()==0|| rec.samples().equals(fam.samples())==false}.
//     * @throws IllegalArgumentException if the VCF record does not have a
//     * GT format field.
//     * @throws NullPointerException if {@code rec==null || fam==null}.
//     */
//    public BitSetGT(VcfRecord rec, NuclearFamilies fam, boolean usePhase) {
//        this(rec);
//        if (rec.samples().equals(fam.samples())==false) {
//            throw new IllegalArgumentException("inconsistent samples");
//        }
//        setBits(rec, usePhase, bitsPerAllele, allele1, allele2, isMissing1,
//                isMissing2, isPhased);
//        removeMendelianInconsistencies(rec, fam, isPhased, isMissing1,
//                isMissing2);
//    }
//
//    private BitSetGT(VcfRecord rec) {
//        int nSamples = rec.nSamples();
//        if (nSamples==0) {
//            String s = "missing sample data: " + rec;
//            throw new IllegalArgumentException(s);
//        }
//        if (rec.hasFormat(GT_FORMAT)==false) {
//            String s = "missing GT FORMAT: " + rec;
//            throw new IllegalArgumentException(s);
//        }
//        this.bitsPerAllele = bitsPerAllele(rec.marker());
//
//        this.samples = rec.samples();
//        this.marker = rec.marker();
//        this.isRefData = rec.isPhased();
//
//        int nSampleWords = nLongWords(nSamples);
//        int nAlleleWords = nLongWords(nSamples*bitsPerAllele);
//        this.allele1 = new long[nAlleleWords];
//        this.allele2 = new long[nAlleleWords];
//        this.alleles = new long[2*nAlleleWords];
//        this.isMissing = new long[nSampleWords];
//        this.isPhased = new long[nSampleWords];
//    }

//    /*
//     * Sets phase to unknown for all parent-offspring relationships, and sets
//     * all genotypes in a duo or trio genotypes to missing if a Mendelian
//     * inconsistency is found.
//     */
//    private static void removeMendelianInconsistencies(VcfRecord rec,
//            NuclearFamilies fam, BitSet isPhased, BitSet isMissing1,
//            BitSet isMissing2) {
//        for (int j=0, n=fam.nDuos(); j<n; ++j) {
//            int p = fam.duoParent(j);
//            int o = fam.duoOffspring(j);
//            isPhased.clear(p);
//            isPhased.clear(o);
//            if (duoIsConsistent(rec, p, o) == false) {
//                logDuoInconsistency(rec, p, o);
//                isMissing1.set(p);
//                isMissing2.set(p);
//                isMissing1.set(o);
//                isMissing2.set(o);
//            }
//        }
//        for (int j=0, n=fam.nTrios(); j<n; ++j) {
//            int f = fam.trioFather(j);
//            int m = fam.trioMother(j);
//            int o = fam.trioOffspring(j);
//            isPhased.clear(f);
//            isPhased.clear(m);
//            isPhased.clear(o);
//            if (trioIsConsistent(rec, f, m, o) == false) {
//                logTrioInconsistency(rec, f, m, o);
//                isMissing1.set(f);
//                isMissing2.set(f);
//                isMissing1.set(m);
//                isMissing2.set(m);
//                isMissing1.set(o);
//                isMissing2.set(o);
//            }
//        }
//    }
//
//    private static boolean duoIsConsistent(VcfRecord rec, int parent,
//            int offspring) {
//        int p1 = rec.gt(parent, 0);
//        int p2 = rec.gt(parent, 1);
//        int o1 = rec.gt(offspring, 0);
//        int o2 = rec.gt(offspring, 1);
//        boolean alleleMissing = (p1<0 || p2<0 || o1<0 || o2<0);
//        return (alleleMissing || p1==o1 || p1==o2 || p2==o1 || p2==o2);
//    }
//
//    private static boolean trioIsConsistent(VcfRecord rec, int father,
//            int mother, int offspring) {
//        int f1 = rec.gt(father, 0);
//        int f2 = rec.gt(father, 1);
//        int m1 = rec.gt(mother, 0);
//        int m2 = rec.gt(mother, 1);
//        int o1 = rec.gt(offspring, 0);
//        int o2 = rec.gt(offspring, 1);
//        boolean fo1 = (o1<0 || f1<0 || f2<0 || o1==f1 || o1==f2);
//        boolean mo2 = (o2<0 || m1<0 || m2<0 || o2==m1 || o2==m2);
//        if (fo1 && mo2) {
//            return true;
//        }
//        else {
//            boolean fo2 = (o2<0 || f1<0 || f2<0 || o2==f1 || o2==f2);
//            boolean mo1 = (o1<0 || m1<0 || m2<0 || o1==m1 || o1==m2);
//            return (fo2 && mo1);
//        }
//    }
//
//    private static void logDuoInconsistency(VcfRecord rec, int parent,
//            int offspring) {
//        StringBuilder sb = new StringBuilder(80);
//        sb.append("WARNING: Inconsistent duo genotype set to missing");
//        sb.append(Const.tab);
//        sb.append(rec.marker());
//        sb.append(Const.colon);
//        sb.append(rec.samples().id(parent));
//        sb.append(Const.tab);
//        sb.append(rec.samples().id(offspring));
//        main.Logger.getInstance().println(sb.toString());
//    }
//
//    private static void logTrioInconsistency(VcfRecord rec, int father,
//            int mother, int offspring) {
//        StringBuilder sb = new StringBuilder(80);
//        sb.append("WARNING: Inconsistent trio genotype set to missing");
//        sb.append(Const.tab);
//        sb.append(rec.marker());
//        sb.append(Const.tab);
//        sb.append(rec.samples().id(father));
//        sb.append(Const.tab);
//        sb.append(rec.samples().id(mother));
//        sb.append(Const.tab);
//        sb.append(rec.samples().id(offspring));
//        main.Logger.getInstance().println(sb.toString());
//    }
}
