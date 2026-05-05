package com.jef.justenoughfakepixel.utils;

public class ColorUtils {

    private ColorUtils() {}

    public static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "");
    }
}