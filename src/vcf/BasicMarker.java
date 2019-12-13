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

import beagleutil.ChromIds;
import blbutil.Const;
import blbutil.StringUtil;
import blbutil.Utilities;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Class {@code BasicMarker} represents a genetic marker.
 * </p>
 * <p>Instances of class {@code BasicMarker} are immutable.
 * </p>
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class BasicMarker implements Marker {

    private static final String[] EMPTY_ID_ARRAY = new String[0];
    private static final Map<String, String[]> ALLELES_MAP
            = new HashMap<>(24);

    private final int chromIndex;
    private final int pos;
    private final String[] ids;
    private final String[] alleles;
    private final int nGenotypes;
    private final int end;

    /**
     * Constructs a new {@code BasicMarker} instance from the specified data.
     * The {@code end()} method of the new instance will return {@code -1}.
     * The JVM will exit with an error message if any marker identifier
     * in the specified{@code ids} array or if any allele identifier in the
     * specified {@code alleles} array does not conform to the VCF
     * specification.
     *
     * @param chrom a chromosome index
     * @param pos the marker position
     * @param ids a list of marker identifiers
     * @param alleles a list of alleles beginning with the reference allele
     *
     * @throws IllegalArgumentException if
     * {@code chrom < 0 || chrom >= ChromIds.instance().size()}
     * @throws NullPointerException if {@code ids == null} or if any element
     * of {@code ids} is {@code null}
     * @throws NullPointerException if {@code alleles == null} or if any element
     * of {@code alleles} is {@code null}
     */
    public BasicMarker(int chrom, int pos, String[] ids, String[] alleles) {
        this(chrom, pos, ids, alleles, -1);
    }

    /**
     * Constructs a new {@code BasicMarker} instance from the specified data.
     * The JVM will exit with an error message if any marker identifier
     * in the specified {@code ids} array does not conform to the VCF
     * specification, if any allele identifier in the specified {@code alleles}
     * array does not conform to the VCF specification, or if
     * {@code (end != -1 && end < pos)}.
     * @param chrom a chromosome index
     * @param pos the marker position
     * @param ids a list of marker identifiers
     * @param alleles a list of alleles beginning with the reference allele
     * @param end the INFO END field, or -1 if there is no INFO END field
     *
     * @throws IllegalArgumentException if
     * {@code chrom < 0 || chrom >= ChromIds.instance().size()}
     * @throws NullPointerException if {@code ids == null} or if any element
     * of {@code ids} is {@code null}
     * @throws NullPointerException if {@code alleles == null} or if any element
     * of {@code alleles} is {@code null}
     */
    public BasicMarker(int chrom, int pos, String[] ids, String[] alleles,
            int end) {
        if (chrom<0 || chrom>=ChromIds.instance().size()) {
            throw new IndexOutOfBoundsException(String.valueOf(chrom));
        }
        ids = removeMissingIds(chrom, pos, ids);
        checkAlleles(chrom, pos, alleles);
        checkEnd(chrom, pos, end);
        this.chromIndex = chrom;
        this.pos = pos;
        this.ids = ids.length == 0 ? EMPTY_ID_ARRAY : ids.clone();
        this.alleles = canonicalAlleles(alleles.clone());
        this.nGenotypes = (alleles.length*(alleles.length+1))/2;
        this.end = end;
    }

    private static String[] removeMissingIds(int chrom, int pos, String[] ids) {
        int index = 0;
        for (int j=0; j<ids.length; ++j) {
            String id = ids[j];
            for (int k=0, n=id.length(); k<n; ++k) {
                if (Character.isWhitespace(id.charAt(k))) {
                    String s = "ERROR: ID field contains white-space at "
                            + coordinate(chrom, pos) + " [" + id + "]";
                    Utilities.exit(s);
                }
            }
            if (id.length()>0 && id.equals(Const.MISSING_DATA_STRING)==false) {
                ids[index++] = id;
            }
        }
        return (index < ids.length) ? Arrays.copyOf(ids, index) : ids;
    }

    private static void checkAlleles(int chrom, int pos, String[] alleles) {
        if (alleles.length<1) {
            String s = "ERROR: missing REF allele at "
                    + coordinate(chrom, pos);
            throw new IllegalArgumentException(s);
        }
        Set<String> set = new HashSet<>(Arrays.asList(alleles));
        if (set.size() != alleles.length) {
            String s = "ERROR: duplicate allele at "
                    + coordinate(chrom, pos) + " " + Arrays.toString(alleles);
            Utilities.exit(s);
        }
        checkREF(chrom, pos, alleles[0]);
        for (int j=1; j<alleles.length; ++j) {
            checkAltAllele(chrom, pos, alleles[j]);
        }
    }

    private static void checkREF(int chrom, int pos, String ref) {
        if (ref.isEmpty()) {
            String s = "ERROR: missing REF field at " + coordinate(chrom, pos);
            Utilities.exit(s);
        }
        for  (int j=0, n=ref.length(); j<n; ++j) {
            char c = Character.toUpperCase(ref.charAt(j));
            if ((c=='A' || c=='C' || c=='G' || c=='T' || c=='N')==false) {
                String s = "ERROR: REF field is not a sequence"
                        + " of A, C, T, G, or N characters at "
                        + coordinate(chrom, pos) + " [" + ref + "]" ;
                Utilities.exit(s);
            }
        }
    }

    private static void checkAltAllele(int chrom, int pos, String alt) {
        int n = alt.length();
        if (n==1 && alt.charAt(0)=='*') {
            return;
        }
        if (n >= 2 && alt.charAt(0)=='<' && alt.charAt(n-1)=='>') {
            for (int j=1; j<n-1; ++j) {
                char c = alt.charAt(j);
                if (Character.isWhitespace(c) || c==Const.comma || c=='<'
                        || c=='>') {
                    String s = "ERROR: invalid ALT allele at "
                            + coordinate(chrom, pos) + " [" + alt + "]";
                    Utilities.exit(s);
                }
            }
        }
        else {
            for (int j=0; j<n; ++j) {
                char c = Character.toUpperCase(alt.charAt(j));
                if ((c=='A' || c=='C' || c=='G' || c=='T' || c=='N')==false) {
                    String s = "ERROR: invalid ALT allele at "
                            + coordinate(chrom, pos) + " [" + alt + "]";
                    Utilities.exit(s);
                }
            }
        }
    }

    /* Return specified String[] alleles if not a SNV */
    private static String[] canonicalAlleles(String[] alleles) {
        if (isSNV(alleles)) {
            String key = alleles[0];
            for (int j=1; j<alleles.length; ++j) {
                key += alleles[j];
            }
            String[] storedAlleles = ALLELES_MAP.get(key);
            if (storedAlleles!=null) {
                alleles = storedAlleles;
            }
            else {
                ALLELES_MAP.put(key, alleles.clone());
            }
        }
        return alleles;
    }

    private static boolean isSNV(String[] alleles) {
        for (String a : alleles) {
            if (a.length()!=1) {
                return false;
            }
        }
        return true;
    }

    private static void checkEnd(int chrom, int pos, int end) {
        if (end != -1 && end < pos) {
            String s = "ERROR: invalid INFO:END field at "
                    + coordinate(chrom, pos) + " [" + end + "]";
            Utilities.exit(s);
        }
    }

    /**
     * Constructs a new {@code BasicMarker} instance from the specified
     * string VCF record prefix.
     * @param vcfRecord a VCF record prefix
     * @throws IllegalArgumentException if the specified VCF record
     * prefix has fewer than 8 tab-delimited fields, or if a format
     * error is detected in the first 8 fields of the specified VCF record
     * @throws NullPointerException if {@code vcfRecord == null}
     */
    @SuppressWarnings("RedundantStringConstructorCall")
    public BasicMarker(String vcfRecord) {
        int minFieldCnt = 8;
        String[] fields = StringUtil.getFields(vcfRecord, Const.tab,
                minFieldCnt+1);
        if (fields.length < minFieldCnt) {
            String s = "VCF record does not contain at least "
                    + minFieldCnt + " tab-delimited fields: " + vcfRecord;
            Utilities.exit(s);
        }
        // Store minimal required data, not entire VCF record
        for (int j=0; j<5; ++j) {
            fields[j] = new String(fields[j]);
        }
        String infoField = fields[7];
        this.chromIndex = extractChrom(fields[0], vcfRecord);
        this.pos = extractPos(fields[1], vcfRecord);
        this.ids = extractIds(chromIndex, pos, fields[2]);
        this.alleles = extractAlleles(chromIndex, pos, fields[3], fields[4]);
        this.nGenotypes = alleles.length*(alleles.length+1)/2;
        this.end = extractEnd(chromIndex, pos, infoField);
    }

    private static int extractChrom(String chrom, String vcfRecord) {
        if (chrom.isEmpty() || chrom.equals(Const.MISSING_DATA_STRING)) {
            String s = "ERROR: missing CHROM field: " + Const.nl +
                    vcfRecord.substring(0,80);
            Utilities.exit(s);
        }
        for (int j=0, n=chrom.length(); j<n; ++j) {
            char c = chrom.charAt(j);
            if (c==Const.colon || Character.isWhitespace(c)) {
                String s = "invalid character in CHROM field ['" + c
                        + "']: " + Const.nl + vcfRecord.substring(0,80);
                Utilities.exit(s);
            }
        }
        return ChromIds.instance().getIndex(chrom);
    }

    private static int extractPos(String pos, String vcfRecord) {
        for (int j=0, n=pos.length(); j<n; ++j) {
            if (Character.isDigit(pos.charAt(j))==false) {
                String s = "ERROR: invalid POS field [" + pos + "]: "
                        + Const.nl + vcfRecord.substring(0,80);
                Utilities.exit(s);
            }
        }
        return Integer.parseInt(pos);
    }

    private static String[] extractIds(int chrom, int pos, String id) {
        if (id.isEmpty()) {
            String s = "ERROR: missing ID field at " + coordinate(chrom, pos);
            Utilities.exit(s);
        }
        if (id.equals(Const.MISSING_DATA_STRING)) {
            return EMPTY_ID_ARRAY;
        }
        String[] ids = StringUtil.getFields(id, Const.semicolon);
        removeMissingIds(chrom, pos, ids);
        return ids;
    }

    private static String[] extractAlleles(int chrom, int pos, String ref,
            String alt) {
        if (ref.isEmpty()) {
            String s = "ERROR: missing REF field at " + coordinate(chrom, pos);
            Utilities.exit(s);
        }
        if (alt.isEmpty()) {
            String s = "ERROR: missing ALT field: at " + coordinate(chrom, pos);
            Utilities.exit(s);
        }
        String[] altAlleles = EMPTY_ID_ARRAY;
        if (alt.equals(Const.MISSING_DATA_STRING)==false) {
            altAlleles = StringUtil.getFields(alt, Const.comma);
        }
        String[] alleles = new String[altAlleles.length + 1];
        alleles[0] = ref;
        System.arraycopy(altAlleles, 0, alleles, 1, altAlleles.length);
        checkAlleles(chrom, pos, alleles);
        return canonicalAlleles(alleles);
    }

    /*
     * Returns value of first END key in the specified INFO field, or
     * returns -1 if there is no END key in INFO field.
     */
    private static int extractEnd(int chrom, int pos, String info) {
        String[] fields = StringUtil.getFields(info, Const.semicolon);
        String key = "END=";
        int end = -1;
        for (String field : fields) {
            if (field.startsWith(key)) {
                String value = field.substring(4);
                for (int j=0, n=value.length(); j<n; ++j) {
                    char c = value.charAt(j);
                    if (Character.isDigit(c)==false) {
                        String s = "ERROR: invalid INFO:END field at "
                                + coordinate(chrom, pos) + " [" + key + value
                                + "]";
                        Utilities.exit(s);
                    }
                }
                end = Integer.parseInt(value);
                checkEnd(chrom, pos, end);
            }
        }
        return end;
    }

    /**
     * Constructs and returns a new marker obtained from the specified marker
     * by changing the marker's non-symbolic alleles to the alleles on the
     * opposite chromosome strand.
     * @param marker a marker
     * @return the equivalent marker on the opposite chromosome strand
     * @throws NullPointerException if {@code marker == null}
     */
    public static Marker flipStrand(Marker marker) {
        return new BasicMarker(marker);
    }

   /* Private constructor used by flipStrand(Marker) method */
    private BasicMarker(Marker markerOnReverseStrand) {
        Marker m = markerOnReverseStrand;
        this.chromIndex = m.chromIndex();
        this.pos = m.pos();
        this.ids = new String[m.nIds()];
        for (int j=0; j<this.ids.length; ++j) {
            this.ids[j] = m.id(j);
        }
        this.alleles = new String[m.nAlleles()];
        for (int j=0; j<this.alleles.length; ++j) {
            if (m.allele(j).charAt(0)!='<') {    // not a symbolic allele
                this.alleles[j] = flipStrand(m.allele(j));
            }
            else {
                this.alleles[j] = m.allele(j);
            }
        }
        this.nGenotypes = m.nGenotypes();
        this.end = m.end();
    }

    /**
     * Returns the string allele obtained by changing the specified allele
     * to the opposite chromosome strand
     * @param allele a string allele
     * @return the string allele obtained by changing the specified allele
     * to the opposite chromosome strand
     * @throws IllegalArgumentException if any character in the specified
     * string is not 'A', 'C', 'G', 'T', 'N', or '*'.
     * @throws NullPointerException if {@code allele == null}
     */
    public static String flipStrand(String allele) {
        char[] ca = new char[allele.length()];
        for (int j=0; j<ca.length; ++j) {
            ca[j] = flipBase(allele.charAt(j));
        }
        return new String(ca);
    }

    private static char flipBase(char c) {
        switch (c) {
            case 'A' : return 'T';
            case 'C' : return 'G';
            case 'G' : return 'C';
            case 'T' : return 'A';
            case 'N' : return 'N';
            case '*' : return '*';
            default: throw new IllegalArgumentException(String.valueOf(c));
        }
    }


    @Override
    public String chrom() {
        return ChromIds.instance().id(chromIndex);
    }

    @Override
    public int chromIndex() {
        return chromIndex;
    }

    @Override
    public int pos() {
        return pos;
    }

    @Override
    public int nIds() {
        return ids.length;
    }

    @Override
    public String id(int index) {
        return ids[index];
    }

    @Override
    public String id() {
        return ids.length>0 ? ids[0] : coordinate(chromIndex, pos);
    }

    @Override
    public int nAlleles() {
        return alleles.length;
    }

    @Override
    public String allele(int index) {
        return alleles[index];
    }

    @Override
    public String[] alleles() {
        return alleles.clone();
    }

    @Override
    public int nGenotypes() {
        return nGenotypes;
    }

    @Override
    public int end() {
        return end;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        sb.append(chrom());
        sb.append(Const.tab);
        sb.append(pos);
        if (ids.length==0) {
            sb.append(Const.tab);
            sb.append(Const.MISSING_DATA_CHAR);
        }
        else {
            for (int j=0; j<ids.length; ++j) {
                sb.append(j==0 ? Const.tab : Const.semicolon);
                sb.append(ids[j]);
            }
        }
        if (alleles.length==1) {
            sb.append(Const.tab);
            sb.append(alleles[0]);
            sb.append(Const.tab);
            sb.append(Const.MISSING_DATA_CHAR);
        }
        else {
            for (int j=0; j<alleles.length; ++j) {
                sb.append(j<2 ? Const.tab : Const.comma);
                sb.append(alleles[j]);
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + chromIndex;
        hash = 29 * hash + this.pos;
        for (int j=0; j<alleles.length; ++j) {
            hash = 29 * hash + alleles[j].hashCode();
        }
        hash = 29 * hash + end;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BasicMarker other = (BasicMarker) obj;
        if (this.chromIndex != other.chromIndex) {
            return false;
        }
        if (this.pos != other.pos) {
            return false;
        }
        if (!Arrays.equals(this.alleles, other.alleles)) {
            return false;
        }
        return this.end == other.end;
    }

    @Override
    public int compareTo(Marker other) {
        if (this.chromIndex != other.chromIndex()) {
            return (this.chromIndex < other.chromIndex()) ? -1 : 1;
        }
        if (this.pos != other.pos()) {
            return (this.pos < other.pos()) ? -1 : 1;
        }
        int n = Math.min(this.alleles.length, other.nAlleles());
        for (int j=0; j<n; ++j) {
            int cmp = this.alleles[j].compareTo(other.allele(j));
            if (cmp != 0) {
                return cmp;
            }
        }
        if (this.alleles.length != other.nAlleles()) {
            return (this.alleles.length < other.nAlleles()) ? -1 : 1;
        }
        if (this.end != other.end()) {
            return (this.end < other.end()) ? -1 : 1;
        }
        return 0;
    }

    private static String coordinate(int chrom, int pos) {
        return ChromIds.instance().id(chrom) + Const.colon + pos;
    }
}
