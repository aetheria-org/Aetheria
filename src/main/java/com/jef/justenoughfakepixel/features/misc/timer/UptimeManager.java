package com.jef.justenoughfakepixel.features.misc.timer;

import lombok.Getter;


public class UptimeManager {

    private static final UptimeManager INSTANCE = new UptimeManager();

    private long endTimeMs = -1;

    private long pausedRemainingMs = -1;

    @Getter
    private long totalDurationMs = 0;

    private boolean fired = false;

    private UptimeManager() {
    }

    public static UptimeManager getInstance() {
        return INSTANCE;
    }


    public boolean isRunning() {
        return endTimeMs > 0 && pausedRemainingMs < 0;
    }

    public boolean isActive() {
        return endTimeMs > 0 || pausedRemainingMs >= 0;
    }

    public boolean isPaused() {
        return pausedRemainingMs >= 0;
    }


    public void start(long durationMs) {
        this.totalDurationMs = durationMs;
        this.endTimeMs = System.currentTimeMillis() + durationMs;
        this.pausedRemainingMs = -1;
        this.fired = false;
    }

    public void cancel() {
        endTimeMs = -1;
        pausedRemainingMs = -1;
        fired = false;
    }


    public void pause() {
        if (!isRunning()) return;
        pausedRemainingMs = getRemainingMs();
        endTimeMs = -1;
    }


    public void resume() {
        if (!isPaused()) return;
        endTimeMs = System.currentTimeMillis() + pausedRemainingMs;
        pausedRemainingMs = -1;
    }


    public void addTime(long ms) {
        if (isPaused()) {
            pausedRemainingMs = Math.max(0, pausedRemainingMs + ms);
        } else if (isRunning()) {
            endTimeMs += ms;
            if (endTimeMs <= System.currentTimeMillis()) endTimeMs = System.currentTimeMillis();
        }
    }


    public long getRemainingMs() {
        if (isPaused()) return pausedRemainingMs;
        if (endTimeMs < 0) return 0;
        return Math.max(0, endTimeMs - System.currentTimeMillis());
    }


    public boolean pollExpired() {
        if (!isActive() || isPaused() || fired) return false;
        if (getRemainingMs() == 0) {
            fired = true;
            return true;
        }
        return false;
    }
}