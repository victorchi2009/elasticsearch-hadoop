/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.hadoop.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Utility class around Strings. Used to remove dependency on other libraries that might (or not) be available at runtime.
 */
public abstract class StringUtils {

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final String EMPTY = "";

    public static boolean hasLength(CharSequence sequence) {
        return (sequence != null && sequence.length() > 0);
    }

    public static boolean hasText(CharSequence sequence) {
        if (!hasLength(sequence)) {
            return false;
        }
        int length = sequence.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(sequence.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static List<String> tokenize(String string) {
        return tokenize(string, ",");
    }

    public static List<String> tokenize(String string, String delimiters) {
        return tokenize(string, delimiters, true, true);
    }

    public static List<String> tokenize(String string, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if (string == null) {
            return Collections.emptyList();
        }
        StringTokenizer st = new StringTokenizer(string, delimiters);
        List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    public static String concatenate(Collection<?> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return EMPTY;
        }
        if (delimiter == null) {
            delimiter = EMPTY;
        }
        StringBuilder sb = new StringBuilder();

        for (Object object : list) {
            sb.append(object.toString());
            sb.append(delimiter);
        }

        sb.setLength(sb.length() - delimiter.length());
        return sb.toString();
    }

    public static String concatenate(Object[] array, String delimiter) {
        if (array == null || array.length == 0) {
            return EMPTY;
        }
        if (delimiter == null) {
            delimiter = EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static String deleteWhitespace(CharSequence sequence) {
        if (!hasLength(sequence)) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder(sequence.length());
        for (int i = 0; i < sequence.length(); i++) {
            char currentChar = sequence.charAt(i);
            if (!Character.isWhitespace(currentChar)) {
                sb.append(currentChar);
            }
        }
        // return the initial String if no whitespace is found
        return (sb.length() == sequence.length() ? sequence.toString() : sb.toString());
    }

    public static String asUTFString(byte[] content) {
        return asUTFString(content, content.length);
    }

    public static String asUTFString(byte[] content, int length) {
        return (content == null || length == 0 ? EMPTY : new String(content, 0, length, UTF_8));
    }

    public static byte[] toUTF(String string) {
        return string.getBytes(UTF_8);
    }

    // Based on "Algorithms on Strings, Trees and Sequences by Dan Gusfield".
    // returns -1 if the two strings are within the given threshold of each other, -1 otherwise
    public static int levenshteinDistance(CharSequence one, CharSequence another, int threshold) {
        int n = one.length();
        int m = another.length();

        // if one string is empty, the edit distance is necessarily the length of the other
        if (n == 0) {
            return m <= threshold ? m : -1;
        } else if (m == 0) {
            return n <= threshold ? n : -1;
        }

        if (n > m) {
            // swap the two strings to consume less memory
            final CharSequence tmp = one;
            one = another;
            another = tmp;
            n = m;
            m = another.length();
        }

        int p[] = new int[n + 1]; // 'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; // placeholder to assist in swapping p and d

        // fill in starting table values
        final int boundary = Math.min(n, threshold) + 1;
        for (int i = 0; i < boundary; i++) {
            p[i] = i;
        }

        // these fills ensure that the value above the rightmost entry of our
        // stripe will be ignored in following loop iterations
        Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
        Arrays.fill(d, Integer.MAX_VALUE);

        for (int j = 1; j <= m; j++) {
            final char t_j = another.charAt(j - 1);
            d[0] = j;

            // compute stripe indices, constrain to array size
            final int min = Math.max(1, j - threshold);
            final int max = (j > Integer.MAX_VALUE - threshold) ? n : Math.min(n, j + threshold);

            // the stripe may lead off of the table if s and t are of different sizes
            if (min > max) {
                return -1;
            }

            // ignore entry left of leftmost
            if (min > 1) {
                d[min - 1] = Integer.MAX_VALUE;
            }

            // iterates through [min, max] in s
            for (int i = min; i <= max; i++) {
                if (one.charAt(i - 1) == t_j) {
                    // diagonally left and up
                    d[i] = p[i - 1];
                } else {
                    // 1 + minimum of cell to the left, to the top, diagonally left and up
                    d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
                }
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // if p[n] is greater than the threshold, there's no guarantee on it being the correct
        // distance
        if (p[n] <= threshold) {
            return p[n];
        }
        return -1;
    }

    public static List<String> findSimiliar(CharSequence match, Collection<String> potential) {
        List<String> list = new ArrayList<String>(3);

        // 1 switches or 1 extra char
        int maxDistance = 2;

        for (String string : potential) {
            int dist = levenshteinDistance(match, string, maxDistance);
            if (dist >= 0) {
                if (dist < maxDistance) {
                    maxDistance = dist;
                    list.clear();
                    list.add(string);
                }
                else if (dist == maxDistance) {
                    list.add(string);
                }
            }
        }

        return list;
    }

    public static String sanitizeResource(String resource) {
        String res = resource.trim();
        if (res.startsWith("/")) {
            res = res.substring(1);
        }
        if (res.endsWith("/")) {
            res = res.substring(0, res.length() - 1);
        }
        return res;
    }
}