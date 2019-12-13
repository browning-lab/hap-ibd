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
 * Class {@code StringUtil} is a utility class with static methods
 * for counting and returning delimited fields in a string.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public class StringUtil {

    /* Private constructor to prevent instantiation */
    private StringUtil() {
    }

    /**
     * Returns the number of delimited fields in the specified
     * string.  Returns 0 if the specified string has length 0.
     *
     * @param s a string
     * @param delimiter a delimiter character
     * @return the number of delimited fields in the specified string
     * @throws NullPointerException if {@code s == null}
     */
    public static int countFields(String s, char delimiter) {
        int cnt = 0;
        for (int j=0, n=s.length(); j<n; ++j) {
            if (s.charAt(j)==delimiter) {
                ++cnt;
            }
        }
        return cnt + 1;
    }

    /**
     * Returns {@code Math.min(countFields(s, delimiter), max)}.
     *
     * @param s a string with 0 or more {@code delimiter} characters
     * @param delimiter the delimiter character
     * @param max the maximum value that can be returned
     *
     * @return {@code Math.min(countFields(s, delimiter), max)}
     *
     * @throws NullPointerException if {@code s == null}
     */
    public static int countFields(String s, char delimiter, int max) {
        int cnt = 0;
        int maxCnt = max - 1;
        for (int j=0, n=s.length(); j<n && cnt<maxCnt; ++j) {
            if (s.charAt(j)==delimiter) {
                ++cnt;
            }
        }
        return Math.min(cnt + 1, max);
    }

    /**
     * Returns an array obtained by splitting the specified string
     * around the specified delimiter.
     * The array returned by this method contains each substring of
     * the string that does not contain the delimiter and that
     * is preceded by the delimiter or the beginning of
     * the string and that is terminated by the delimiter or the end
     * of the string.  The substrings in the array are in
     * the order in which they occur in the specified string.
     * If there are no delimiters in the specified string then the method
     * return an array of length one, whose single element is the specified
     * string.
     *
     * @param s a string
     * @param delimiter a delimiter character
     *
     * @return the array of strings obtained by splitting the specified string
     * around the specified delimiter
     *
     * @throws NullPointerException if {@code s == null}
     */
    public static String[] getFields(String s, char delimiter) {
        String[] fields = new String[countFields(s, delimiter)];
        int start = 0;
        for (int j=0; j<fields.length; ++j)  {
            int end = s.indexOf(delimiter, start);
            fields[j] = end>=0 ? s.substring(start,end) : s.substring(start);
            start = end + 1;
        }
        return fields;
    }

    /**
     * Returns an array obtained by splitting the specified string
     * around the first {@code (limit - 1)} occurrences of the specified
     * delimiter.  If the string contains fewer than {@code (limit - 1)}
     * delimiter characters, the returned value will equal
     * {@code StringUtil.getFields(s, delimiter)}
     *
     * @param s a string
     * @param delimiter a delimiter character
     * @param limit the maximum length of the returned array
     *
     * @return an array obtained by splitting the specified string
     * around the specified delimiter
     *
     * @throws NullPointerException if {@code s == null}
     * @throws IllegalArgumentException if {@code limit < 2 }
     */
    public static String[] getFields(String s, char delimiter, int limit) {
        if (limit < 2) {
            throw new IllegalArgumentException("limit: " + limit);
        }
        String[] fields = new String[countFields(s, delimiter, limit)];
        if (fields.length > 0) {
            int start = 0;
            for (int j=0, n=fields.length-1; j<n; ++j)  {
                int end = s.indexOf(delimiter, start);
                fields[j] = s.substring(start, end);
                start = end + 1;
            }
            fields[fields.length - 1] = s.substring(start);
        }
        return fields;
    }

    /**
     * Returns the number of white-space delimited fields in the specified
     * string.  A field is a maximal set of consecutive characters that are not
     * white space characters.  White space is defined as any unicode
     * characters less than or equal to '&#92;u0020'.
     *
     * @param s a string
     * @return the number of white-space delimited fields in the specified
     * string
     * @throws NullPointerException if {@code s == null}
     */
    public static int countFields(String s) {
        int start = 0;
        int end = s.length();
        while (start<end && s.charAt(start)<=' ') {
            ++start;
        }
        while (end>start && s.charAt(end-1)<=' ') {
            --end;
        }
        int fieldCount = (start<end) ? 1 : 0;
        while (++start<end && s.charAt(start)>' ') {
        }
        while (start<end) {
            while (s.charAt(++start)<=' ') {
            }
            ++fieldCount;
            while (++start<end && s.charAt(start)>' ') {
            }
        }
        return fieldCount;
    }

    /**
     * Returns an array obtained by trimming white-space from the
     * beginning and end of the specified string, and splitting the resulting
     * string around white space.
     * White space is any maximal substring of unicode characters
     * less than or equal to '&#92;u0020'. White-space at the beginning and
     * end of the string is ignored.  The substrings in the returned array
     * are in the order in which they occur in this string.  If there is no
     * white-space in the specified string, the method returns an array
     * of length one whose single element is the trimmed string.  If the
     * specified string contains only white-space a string array
     * of length 0 is returned.
     *
     * @param s a string
     * @return the array of strings obtained by splitting the specified string
     * around white space
     *
     * @throws NullPointerException if {@code s == null}
     */
    public static String[] getFields(String s) {
        s = s.trim();
        int n = s.length();
        String[] fields = new String[countFields(s)];
        if (fields.length > 0) {
            int index = 0;
            int start = 0;
            int j = -1;
            while (++j<n && s.charAt(j)>' ')  {
            }
            fields[index++] = s.substring(start, j);
            while (j<n) {
                while (s.charAt(++j)<=' ') {
                }
                start = j;
                while (++j<n && s.charAt(j)>' ') {
                }
                fields[index++] = s.substring(start, j);
            }
            assert index==fields.length;
        }
        return fields;
    }

    /**
     * <p>Returns an array obtained by trimming white-space from the
     * beginning and end of the specified string, and splitting the resulting
     * string around the first {@code (limit-1)} white-space delimiters.
     * A white-space delimiter is any maximal substring of unicode characters
     * less than or equal to '&#92;u0020'.  If the trimemed string contains
     * fewer than {@code (limit - 1)} white space delimiters, the returned value
     * will equal {@code StringUtil.getFields(s)}.  The substrings in the
     * returned array are in the order in which they occur in this string.
     * If there are no white-space delimiters in the specified string, the
     * method returns an array of length one whose single element is the
     * trimmed string. If the specified string contains only white-space,
     * a string array of length 0 is returned.
     *</p>
     *
     * @param s a string
     * @param limit the maximum length of the returned array
     *
     * @return the array of strings obtained by splitting the specified string
     * around white space
     *
     * @throws NullPointerException if {@code s == null}
     * @throws IllegalArgumentException if {@code limit < 2}
     */
    public static String[] getFields(String s, int limit) {
        if (limit<2) {
            throw new IllegalArgumentException("limit: " + limit);
        }
        s = s.trim();
        int n = s.length();
        int j=-1;
        while (++j<n && s.charAt(j)>' ') {
        }
        int fieldCount = (j>0) ? 1 : 0;
        while (j<n && fieldCount<limit) {
            while (s.charAt(++j)<=' ') {
            }
            ++fieldCount;
            while (++j<n && s.charAt(j)>' ') {
            }
        }
        String[] fields = new String[fieldCount];
        if (fields.length>0) {
            int index = 0;
            int start = 0;
            j = -1;
            while (++j<n && s.charAt(j)>' ') {
            }
            fields[index++] = s.substring(start, j);
            while (j<n && index<limit) {
                while (s.charAt(++j)<=' ') {
                }
                start = j;
                while (++j<n && s.charAt(j)>' ') {
                }
                if (index < limit-1) {
                    fields[index++] = s.substring(start, j);
                }
                else {
                    fields[index++] = s.substring(start);
                }
            }
        }
        return fields;
    }
}
