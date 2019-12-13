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
package blbutil;

/**
 * Class {@code Const} provides public static final fields with
 * string and character constants.
 *
 * @author Brian L Browning
 */
public class Const {

    private Const() {
        // private constructor to prevent instantiation.
    }

    /**
     * The system-dependent string representing a new line-line:
     * {@code System.getProperty("line.separator")}
     */
    public static final String nl = System.getProperty("line.separator");

    /**
     * The VCF missing-data symbol as a string: {@code "."}
     */
    public static final String MISSING_DATA_STRING = ".";

    /**
     * The VCF missing-data symbol as a character: {@code '.'}
     */
    public static final char MISSING_DATA_CHAR = '.';

    /**
     * The colon character: {@code ':'}
     */
    public static final char colon = ':';

    /**
     * The hyphen character: {@code '-'}
     */
    public static final char hyphen = '-';

    /**
     * The tab character: {@code '\t'}
     */
    public static final char tab = '\t';

    /**
     * The semicolon character: {@code ';'}
     */
    public static final char semicolon = ';';

    /**
     * The comma character: {@code ','}
     */
    public static final char comma = ',';

    /**
     * The phased allele separator: {@code '|'}
     */
    public static final char phasedSep = '|';

    /**
     *  The unphased allele separator: {@code '/'}
     */
    public static final char unphasedSep = '/';

    /**
     * The value 1,000,000,000
     */
    public static final int giga = 1000000000;

    /**
     * The value 1,000,000
     */
    public static final int mega = 1000000;
}
