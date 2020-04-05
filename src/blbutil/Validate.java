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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class Validate contains static methods for validating command line
 * arguments.
 *
 * @author Brian L. Browning {@code <browning@uw.edu>}
 */
public final class Validate {

    private Validate() {
        // private constructor to prevent instantiation
    }

    /**
     * Returns a map with one (key, value) pair for each element
     * of the specified array.  Each element of the specified {@code String[]}
     * array must contain the specified delimiter character.
     * For each array element {@code s}, the key is
     * {@code  s.substring(0, s.indexOf(sep))}
     * and the value is {@code s.substring(s.indexOf(sep) + 1)}.
     *
     * @param args a string array
     * @param delim the delimiter character separating a key and value
     * @return a map with one (key, value) pair for each element
     * of the specified array
     *
     * @throws IllegalArgumentException if the specified delimiter character is
     * not found in any string element in the specified {@code String[]} array
     * @throws IllegalArgumentException if the specified delimiter
     * is the first or last character of each string element in the specified
     * {@code String[]} array
     * @throws IllegalArgumentException if any two elements of the
     * specified string array have the same key
     * @throws NullPointerException if {@code args == null} or if
     * {@code args[j] == null} for any {@code j} satisfying
     * {@code (0 <= j && j <= args.length)}
     */
    public static Map<String, String> argsToMap(String[] args, char delim) {
        Map<String, String> argMap=new HashMap<>();
        for (String arg : args) {
            int index=arg.indexOf(delim);
            if (index!=-1) {
                if (index == 0) {
                    String s = "missing key in key-value pair: " + arg;
                    throw new IllegalArgumentException(s);
                }
                if (index==(arg.length()-1)) {
                    String s = "missing value in key-value pair: " + arg;
                    throw new IllegalArgumentException(s);
                }
                String key = arg.substring(0, index);
                String value = arg.substring(index+1);
                if (argMap.containsKey(key)) {
                    String s = "duplicate arguments: " + key;
                    throw new IllegalArgumentException(s);
                }
                argMap.put(key, value);
            } else {
                String s = "missing delimiter character (" + delim + "): "
                        + arg;
                throw new IllegalArgumentException(s);
            }
        }
        return argMap;
    }

    /**
     * Checks whether the specified map of key-value pairs is empty.
     * If the map is non-empty, the method will print an error message
     * and terminate the Java virtual machine.
     *
     * @param argsMap a map of key-value pairs
     * @throws NullPointerException if {@code argsMap == null}
     */
    public static void confirmEmptyMap(Map<String, String> argsMap) {
        Set<String> keySet = argsMap.keySet();
        if (keySet.isEmpty()==false) {
            StringBuilder sb = new StringBuilder(50);
            sb.append("Error: unrecognized parameter");
            sb.append(keySet.size()==1 ? ":" : "s:");
            for (String key : keySet) {
                String value = argsMap.get(key);
                sb.append(' ');
                sb.append(key);
                sb.append('=');
                sb.append(value);
            }
            Utilities.exit(sb.toString());
        }
    }

    /**
     * Returns a {@code File} object corresponding to the specified filename or
     * {@code null} if {@code filename == null}
     *
     * @param filename a filename
     * @return a file corresponding to the specified filename, or {@code null}
     * if {@code filename == null}
     *
     * @throws IllegalArgumentException if {@code filename.isEmpty() == true}
     * @throws IllegalArgumentException if {@code filename != null} and the
     * specified file does not exist or is a directory
     */
    public static File getFile(String filename) {
        if (filename==null) {
            return null;
        }
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("filename is empty string");
        }
        else {
            File file = new File(filename);
            if (file.exists()==false) {
                String s = "File does not exist: " + file;
                throw new IllegalArgumentException(s);
            }
            if (file.isDirectory()) {
                String s = "File is a directory: " + file;
                throw new IllegalArgumentException(s);
            }
            return file;
        }
    }

    /**
     * Removes the specified key from the specified map, and returns the
     * integer value corresponding to the specified key.
     *
     * @param key the key
     * @param map a map of key-value pairs
     * @param isRequired {@code true} if the specified key
     * is required to be in the specified map, and {@code false} otherwise
     * @param defaultValue the value that will be returned if
     *   {@code (isRequired == false && map.get(key) == null)}
     * @param min the minimum valid integer value
     * @param max the maximum valid integer value
     *
     * @return the integer value corresponding to the specified key
     *
     * @throws IllegalArgumentException if {@code min > max}
     * @throws IllegalArgumentException if
     * {@code defaultValue < min || defaultValue > max}
     * @throws IllegalArgumentException if
     * {@code isRequired == true && map.get(key) == null}
     * @throws IllegalArgumentException if
     * {@code map.get(key) != null
     *  && (Integer.parseInt(map.get(key)) < min
     *       || Integer.parseInt(map.get(key)) > max)}
     * @throws NumberFormatException if {@code map.get(key) != null}
     * and {@code map.get(key)} is not a parsable {@code int}
     * @throws NullPointerException if {@code key == null || map == null}
     */
    public static int intArg(String key, Map<String, String> map,
            boolean isRequired, int defaultValue, int min, int max) {
        checkIntValue(key, defaultValue, min, max);
        String value = map.remove(key);
        if (value==null) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
            else {
                return defaultValue;
            }
        }
        else {
            return parseInt(key, value, min, max);
        }
    }

    /**
     * Removes the specified key from the specified map, and returns the
     * long value corresponding to the specified key.
     *
     * @param key the key
     * @param map a map of key-value pairs
     * @param isRequired {@code true} if the specified key
     * is required to be in the specified map, and {@code false} otherwise
     * @param defaultValue the value that will be returned if
     *   {@code (isRequired == false && map.get(key) == null)}
     * @param min the minimum valid long value
     * @param max the maximum valid long value
     *
     * @return the long value corresponding to the specified key
     *
     * @throws IllegalArgumentException if {@code min > max}
     * @throws IllegalArgumentException if
     * {@code defaultValue < min || defaultValue > max}
     * @throws IllegalArgumentException if
     * {@code isRequired == true && map.get(key) == null}
     * @throws IllegalArgumentException if
     * {@code map.get(key) != null
     *  && (Long.parseLong(map.get(key)) < min
     *       || Long.parseLong(map.get(key)) > max)}
     * @throws NumberFormatException if {@code map.get(key) != null}
     * and {@code map.get(key)} is not a parsable {@code long}
     * @throws NullPointerException if {@code key == null || map == null}
     */
    public static long longArg(String key, Map<String, String> map,
            boolean isRequired, long defaultValue, long min, long max) {
        checkLongValue(key, defaultValue, min, max);
        String value = map.remove(key);
        if (value==null) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
            else {
                return defaultValue;
            }
        }
        else {
            return parseLong(key, value, min, max);
        }
    }

    /**
     * Removes the specified key from the specified map, and returns the
     * float value corresponding to the specified key.
     *
     * @param key the key
     * @param map a map of key-value pairs
     * @param isRequired {@code true} if the specified key
     * is required to be in the specified map, and {@code false} otherwise
     * @param defaultValue the value that will be returned if
     *   {@code (isRequired == false && map.get(key) == null)}
     * @param min the minimum valid float value
     * @param max the maximum valid float value
     *
     * @return the float value corresponding to the specified key
     *
     * @throws IllegalArgumentException if {@code min > max}
     * @throws IllegalArgumentException if
     * {@code defaultValue < min || defaultValue > max
     *        || Float.isNan(defaultValue)==true}
     * @throws IllegalArgumentException if
     * {@code isRequired == true && map.get(key) == null}
     * @throws IllegalArgumentException if
     * {@code map.get(key) != null
     *  && (Float.parseFloat(map.get(key)) < min
     *       || Float.parseFloat(map.get(key)) > max
     *       || Float.isNaN(map.get(key))}
     * @throws NumberFormatException if {@code map.get(key) != null}
     * and {@code map.get(key)} is not a parsablbe {@code float}
     * @throws NullPointerException if {@code key == null || map == null}
     */
    public static float floatArg(String key, Map<String, String> map,
            boolean isRequired, float defaultValue, float min, float max) {
        checkFloatValue(key, defaultValue, min, max);
        String value = map.remove(key);
        if (value==null) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
            else {
                return defaultValue;
            }
        }
        else {
            return parseFloat(key, value, min, max);
        }
    }

    /**
     * Removes the specified key from the specified map, and returns the
     * double value corresponding to the specified key.
     *
     * @param key the key
     * @param map a map of key-value pairs
     * @param isRequired {@code true} if the specified key
     * is required to be in the specified map, and {@code false} otherwise
     * @param defaultValue the value that will be returned if
     *   {@code (isRequired == false && map.get(key) == null)}
     * @param min the minimum valid double value
     * @param max the maximum valid double value
     *
     * @return the double value corresponding to the specified key
     *
     * @throws IllegalArgumentException if {@code min > max}
     * @throws IllegalArgumentException if
     * {@code defaultValue < min || defaultValue > max
     *        || Double.isNan(defaultValue)==true}
     * @throws IllegalArgumentException if
     * {@code isRequired == true && map.get(key) == null}
     * @throws IllegalArgumentException if
     * {@code map.get(key) != null
     *  && (Double.parseDouble(map.get(key)) < min
     *       || Double.parseDouble(map.get(key)) > max
     *       || Double.isNaN(map.get(key))}
     * @throws NumberFormatException if {@code map.get(key) != null}
     * and {@code map.get(key)} is not a parsable {@code double}
     * @throws NullPointerException if {@code key == null || map == null}
     */
    public static double doubleArg(String key, Map<String, String> map,
            boolean isRequired, double defaultValue, double min, double max) {
        checkDoubleValue(key, defaultValue, min, max);
        String value = map.remove(key);
        if (value==null) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
            else {
                return defaultValue;
            }
        }
        else {
            return parseDouble(key, value, min, max);
        }
    }

    /**
     * Removes the specified key from the specified map, and returns the
     * boolean value corresponding to the specified key.  If the value
     * is {@code v}, then {@code true} is returned if
     * {@code (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("t"))}
     * and {@code false} is returned if
     * {@code (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("f"))}.
     *
     * @param key the key
     * @param map a map of key-value pairs
     * @param isRequired {@code true} if the specified key
     * is required to be in the specified map, and {@code false} otherwise
     * @param defaultValue the value that will be returned if
     *   {@code (isRequired == false && map.get(key) == null)}
     *
     * @return the boolean value corresponding to the specified key
     *
     * @throws IllegalArgumentException if
     * {@code isRequired == true && map.get(key) == null}
     * @throws IllegalArgumentException if the value
     * {@code (v = map.get(key)) != null &&
     * false == (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("t")
     *   || v.equalsIgnoreCase("false") || v.equalsIgnoreCase("f"))
     * }
     * @throws NullPointerException if {@code key == null || map == null}
     */
    public static boolean booleanArg(String key, Map<String, String> map,
            boolean isRequired, boolean defaultValue) {
        String value = map.remove(key);
        if (value==null) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
            else {
                return defaultValue;
            }
        }
        else {
            return parseBoolean(value);
        }
    }

    /**
     * Removes the specified key from the specified map, and returns the
     * string value corresponding to the specified key.  The value is permitted
     * to be {@code null}
     *
     * @param key the key
     * @param map a map of key-value pairs
     * @param isRequired {@code true} if the specified key
     * is required to be in the specified map, and {@code false} otherwise
     * @param defaultValue the value that will be returned if
     *   {@code (isRequired == false && map.get(key) == null)}
     * @param possibleValues an array of valid string values or {@code null} if
     * the valid values are {@code null} and all non-empty strings.
     *
     * @return the string value corresponding to the specified key
     *
     * @throws IllegalArgumentException if
     * {@code isRequired == true && map.get(key) == null}
     * @throws IllegalArgumentException if
     * {@code possibleValues != null} and {@code defaultValue} does not
     * equal any element of the {@code possibleValues} array
     * @throws IllegalArgumentException if
     * {@code possibleValues != null} and {@code map.get(key)} does not
     * equal any element of the {@code possibleValues} array
     * @throws NullPointerException if {@code key == null || map == null}
     */
    public static String stringArg(String key, Map<String, String> map,
            boolean isRequired, String defaultValue, String[] possibleValues) {
        checkStringValue(key, defaultValue, possibleValues);
        String value = map.remove(key);
        if (value==null) {
            if (isRequired) {
                String s = "missing " + key + " argument";
                throw new IllegalArgumentException(s);
            }
            else {
                return defaultValue;
            }
        }
        checkStringValue(key, value, possibleValues);
        return value;
    }

    private static int parseInt(String key, String toParse, int min, int max) {
        try {
            int i = Integer.parseInt(toParse);
            checkIntValue(key, i, min, max);
            return i;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static long parseLong(String key, String toParse, long min, long max) {
        try {
            long l = Long.parseLong(toParse);
            checkLongValue(key, l, min, max);
            return l;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static float parseFloat(String key, String toParse, float min,
            float max) {
        try {
            float f = Float.parseFloat(toParse);
            checkFloatValue(key, f, min, max);
            return f;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static double parseDouble(String key, String toParse, double min,
            double max) {
        try {
            double d = Double.parseDouble(toParse);
            checkDoubleValue(key, d, min, max);
            return d;
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(toParse + " is not a number");
        }
    }

    private static boolean parseBoolean(String s) {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t")) {
            return true;
        }
        else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("f")) {
            return false;
        }
        else {
            String msg = s + " is not \"true\" or \"false\"";
            throw new IllegalArgumentException(msg);
        }
    }

    private static void checkIntValue(String key, int value, int min, int max) {
        String s = null;
        if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        else if (value < min) {
            s = "value=" + value + " < " + min;
        }
        else if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkLongValue(String key, long value, long min,
            long max) {
        String s = null;
        if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        else if (value < min) {
            s = "value=" + value + " < " + min;
        }
        else if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkFloatValue(String key, float value, float min,
            float max) {
        String s = null;
        if (Float.isNaN(value)) {
            s = "value=" + value;
        }
        else if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        else if (value < min) {
            s = "value=" + value + " < " + min;
        }
        else if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkDoubleValue(String key, double value, double min,
            double max) {
        String s = null;
        if (Double.isNaN(value)) {
            s = "value=" + value;
        }
        else if (min > max) {
            s = "min=" + min + " > max=" + max;
        }
        else if (value < min) {
            s = "value=" + value + " < " + min;
        }
        else if (value > max) {
            s = "value=" + value + " > " + max;
        }
        if (s != null) {
            String prefix = "Error in \"" + key + "\" argument: ";
            throw new IllegalArgumentException(prefix + s);
        }
    }

    private static void checkStringValue(String key, String value,
            String[] possibleValues) {
        if (possibleValues != null) {
            boolean foundMatch = false;
            for (int j=0; j<possibleValues.length && foundMatch==false; ++j) {
                String s = possibleValues[j];
                foundMatch = (s==null) ? value==null : s.equalsIgnoreCase(value);
            }
            if (foundMatch==false) {
                String s = "Error in \"" + key + "\" argument: \"" + value
                        + "\" is not in " + Arrays.toString(possibleValues);
                throw new IllegalArgumentException(s);
            }
        }
    }
}
