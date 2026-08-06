package io.hamlook.aetheria.utils;

import java.util.TimeZone;

/** Small time helpers shared by client API calls. */
public final class TimeUtils {

    private TimeUtils() {
    }

    /** Current local UTC offset in minutes (e.g. +330 for IST, -300 for EST). */
    public static int getLocalOffsetMinutes() {
        return TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000;
    }
}