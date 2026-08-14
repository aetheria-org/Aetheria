// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChromaColour {

    private static final int RADIX = 10;
    private static final int MIN_CHROMA_SECS = 1;
    private static final int MAX_CHROMA_SECS = 60;
    public static long startTime = -1;

    private static final ConcurrentHashMap<String, int[]> DECOMPOSED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> STATIC_ARGB = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, float[]> CHROMA_BASE = new ConcurrentHashMap<>();

    public static String special(int chromaSpeed, int alpha, int rgb) {
        return special(chromaSpeed, alpha, (rgb & 0xFF0000) >> 16, (rgb & 0x00FF00) >> 8, (rgb & 0x0000FF));
    }

    public static String special(int chromaSpeed, int alpha, int r, int g, int b) {
        return Integer.toString(chromaSpeed, RADIX) + ":" +
               Integer.toString(alpha, RADIX) + ":" +
               Integer.toString(r, RADIX) + ":" +
               Integer.toString(g, RADIX) + ":" +
               Integer.toString(b, RADIX);
    }

    private static int[] decompose(String csv) {
        int[] cached = DECOMPOSED.get(csv);
        if (cached != null) return cached;
        String[] split = csv.split(":");
        int[] arr = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            arr[i] = Integer.parseInt(split[split.length - 1 - i], RADIX);
        }
        DECOMPOSED.put(csv, arr);
        return arr;
    }

    public static int specialToSimpleRGB(String special) {
        int[] d = decompose(special);
        return (d[3] & 0xFF) << 24 | (d[2] & 0xFF) << 16 | (d[1] & 0xFF) << 8 | (d[0] & 0xFF);
    }

    public static int getSpeed(String special) {
        return decompose(special)[4];
    }

    public static float getSecondsForSpeed(int speed) {
        return (255 - speed) / 254f * (MAX_CHROMA_SECS - MIN_CHROMA_SECS) + MIN_CHROMA_SECS;
    }

    public static int specialToChromaRGB(String special) {
        if (startTime < 0) startTime = System.currentTimeMillis();
        Integer cached = STATIC_ARGB.get(special);
        if (cached != null) return cached;
        int[] d = decompose(special);
        int chr = d[4], a = d[3];
        if (chr == 0) {
            int argb = (a & 0xFF) << 24 | (d[2] & 0xFF) << 16 | (d[1] & 0xFF) << 8 | (d[0] & 0xFF);
            STATIC_ARGB.put(special, argb);
            return argb;
        }
        float[] base = CHROMA_BASE.get(special);
        if (base == null) {
            float[] hsv = Color.RGBtoHSB(d[2], d[1], d[0], null);
            base = new float[]{hsv[0], hsv[1], hsv[2]};
            CHROMA_BASE.put(special, base);
        }
        float seconds = getSecondsForSpeed(chr);
        float hue = base[0] + (System.currentTimeMillis() - startTime) / 1000f / seconds;
        hue %= 1;
        if (hue < 0) hue += 1;
        return (a & 0xFF) << 24 | (Color.HSBtoRGB(hue, base[1], base[2]) & 0x00FFFFFF);
    }

    public static int animatedRainbow(int speed, int alpha) {
        if (startTime < 0) startTime = System.currentTimeMillis();
        float seconds = getSecondsForSpeed(speed);
        float hue = (System.currentTimeMillis() - startTime) / 1000f / seconds;
        hue %= 1;
        if (hue < 0) hue += 1;
        return (alpha & 0xFF) << 24 | (Color.HSBtoRGB(hue, 1F, 1F) & 0x00FFFFFF);
    }

    public static int applyChromaShift(int argb, float x, float y, int mode, float size) {
        if (mode == 0) return argb;
        float shift = ((x + y) / Math.max(1F, size)) % 1F;
        int a = (argb >>> 24) & 255;
        int r = (argb >>> 16) & 255;
        int g = (argb >>> 8) & 255;
        int b = argb & 255;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        hsb[0] = (hsb[0] + shift) % 1F;
        return (a << 24) | (Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0x00FFFFFF);
    }

    public static int rotateHue(int argb, int degrees) {
        int a = (argb >> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        hsv[0] += degrees / 360f;
        hsv[0] %= 1;
        return (a & 0xFF) << 24 | (Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0x00FFFFFF);
    }
}