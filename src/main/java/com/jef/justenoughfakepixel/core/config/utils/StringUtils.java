package com.jef.justenoughfakepixel.core.config.utils;

import com.jef.justenoughfakepixel.utils.ColorUtils;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;

public final class StringUtils {

    private StringUtils() {
    }

    public static String cleanColour(String in) {
        return ColorUtils.stripColor(in);
    }

    public static String joinStrings(String[] arr, int start) {
        if (start >= arr.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    public static String joinRange(String[] arr, int startInclusive, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (i > startInclusive) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    public static String formatNumber(double number) {
        String[] suffixes = {"", "k", "m", "b", "t", "q"};
        int index = 0;

        double value = Math.abs(number);
        while (value >= 1000 && index < suffixes.length - 1) {
            value /= 1000;
            index++;
        }

        DecimalFormat format = new DecimalFormat("#.##");
        String formatted = format.format(value) + suffixes[index];

        return number < 0 ? "-" + formatted : formatted;
    }

    public static String clean(String s) {
        return s.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ').trim();
    }


    public static <T> Map<String, T> subMapWithKeysThatAreSuffixes(String prefix, NavigableMap<String, T> map) {
        return "".equals(prefix) ? map : map.subMap(prefix, true, nextString(prefix), false);
    }

    private static String nextString(String input) {
        return input.substring(0, input.length() - 1) + (char) (input.charAt(input.length() - 1) + 1);
    }
}