package io.hamlook.aetheria.utils.render;

import java.util.ArrayList;
import java.util.List;

public final class MiniMessageDetector {

    private MiniMessageDetector() {}

    public static class Segment {
        public final String text;
        public final int color;          // solid color, -1 if none
        public final int gradientStart;  // gradient start color, -1 if none
        public final int gradientEnd;    // gradient end color, -1 if none

        public Segment(String text, int color) {
            this.text = text;
            this.color = color;
            this.gradientStart = -1;
            this.gradientEnd = -1;
        }

        public Segment(String text, int gradientStart, int gradientEnd) {
            this.text = text;
            this.color = -1;
            this.gradientStart = gradientStart;
            this.gradientEnd = gradientEnd;
        }
    }

    public static List<Segment> parse(String input) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        int i = 0;
        int currentColor = -1;
        int gradientStart = -1;
        boolean inGradient = false;

        while (i < input.length()) {
            // <color:#rrggbb>
            if (input.startsWith("<color:", i)) {
                flushBuffer(segments, buffer, currentColor, gradientStart, inGradient);
                int gt = input.indexOf('>', i);
                String hex = input.substring(i + 7, gt);
                currentColor = parseHex(hex);
                gradientStart = -1;
                inGradient = false;
                i = gt + 1;
                continue;
            }
            // </color>  (with or without hex)
            if (input.startsWith("</color", i)) {
                flushBuffer(segments, buffer, currentColor, gradientStart, inGradient);
                currentColor = -1;
                int gt = input.indexOf('>', i);
                i = gt + 1;
                continue;
            }
            // <gradient:#rrggbb>
            if (input.startsWith("<gradient:", i)) {
                flushBuffer(segments, buffer, currentColor, gradientStart, inGradient);
                int gt = input.indexOf('>', i);
                String hex = input.substring(i + 10, gt);
                gradientStart = parseHex(hex);
                currentColor = -1;
                inGradient = true;
                i = gt + 1;
                continue;
            }
            // </gradient:#rrggbb>  (hex required)
            if (input.startsWith("</gradient:", i)) {
                int gt = input.indexOf('>', i);
                String hex = input.substring(i + 11, gt);
                int gradientEnd = parseHex(hex);
                // buffer holds the gradient text
                segments.add(new Segment(buffer.toString(), gradientStart, gradientEnd));
                buffer.setLength(0);
                gradientStart = -1;
                inGradient = false;
                i = gt + 1;
                continue;
            }

            // regular character
            buffer.append(input.charAt(i));
            i++;
        }

        // any trailing text
        flushBuffer(segments, buffer, currentColor, gradientStart, inGradient);
        return segments;
    }

    private static void flushBuffer(List<Segment> segments,
                                    StringBuilder buffer,
                                    int currentColor,
                                    int gradientStart,
                                    boolean inGradient) {
        if (buffer.length() == 0) return;
        if (inGradient && gradientStart != -1) {
            // gradient without explicit end tag – treat as solid start color
            segments.add(new Segment(buffer.toString(), gradientStart));
        } else if (currentColor != -1) {
            segments.add(new Segment(buffer.toString(), currentColor));
        } else {
            segments.add(new Segment(buffer.toString(), -1));
        }
        buffer.setLength(0);
    }

    private static int parseHex(String hex) {
        hex = hex.replace("#", "");
        if (hex.length() == 6) hex = "FF" + hex; // assume fully opaque if alpha omitted
        return (int) Long.parseLong(hex, 16);
    }
}