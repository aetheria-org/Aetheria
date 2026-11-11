package io.hamlook.aetheria.utils;


import lombok.Getter;
import lombok.Setter;

public final class LerpUtils {

    private static final float SIGMOID_STR = 8;
    private static final float SIGMOID_A = -1 / (sigmoid(-0.5f * SIGMOID_STR) - sigmoid(0.5f * SIGMOID_STR));
    private static final float SIGMOID_B = SIGMOID_A * sigmoid(-0.5f * SIGMOID_STR);

    private LerpUtils() {
    }

    public static float clampZeroOne(float f) {
        return Math.max(0, Math.min(1, f));
    }

    public static float sigmoid(float val) {
        return (float) (1 / (1 + Math.exp(-val)));
    }

    public static float sigmoidZeroOne(float f) {
        f = clampZeroOne(f);
        return SIGMOID_A * sigmoid(SIGMOID_STR * (f - 0.5f)) - SIGMOID_B;
    }

    public static float lerp(float a, float b, float amount) {
        return b + (a - b) * clampZeroOne(amount);
    }

    // LerpingInteger

    public static final class LerpingInteger {

        @Getter
        private int timeSpent;
        private long lastMillis;
        @Setter
        private int timeToReachTarget;
        private int targetValue;
        private int lerpValue;

        public LerpingInteger(int initialValue, int timeToReachTarget) {
            this.targetValue = this.lerpValue = initialValue;
            this.timeToReachTarget = timeToReachTarget;
        }

        public LerpingInteger(int initialValue) {
            this(initialValue, 200);
        }

        public void tick() {
            int lastTimeSpent = timeSpent;
            this.timeSpent += System.currentTimeMillis() - lastMillis;

            float lastDistPercent = lastTimeSpent / (float) timeToReachTarget;
            float distPercent = timeSpent / (float) timeToReachTarget;
            float fac = (1 - lastDistPercent) / lastDistPercent;

            int startValue = lerpValue - (int) ((targetValue - lerpValue) / fac);
            int dist = targetValue - startValue;
            if (dist == 0) return;

            int oldLerpValue = lerpValue;
            lerpValue = distPercent >= 1 ? targetValue : startValue + (int) (dist * distPercent);

            if (lerpValue == oldLerpValue) {
                timeSpent = lastTimeSpent;
            } else {
                this.lastMillis = System.currentTimeMillis();
            }
        }

        public void resetTimer() {
            timeSpent = 0;
            lastMillis = System.currentTimeMillis();
        }

        public int getValue() {
            return lerpValue;
        }

        public void setValue(int value) {
            this.targetValue = this.lerpValue = value;
        }

        public int getTarget() {
            return targetValue;
        }

        public void setTarget(int targetValue) {
            this.targetValue = targetValue;
        }
    }

    // LerpingFloat

    public static final class LerpingFloat {

        @Setter
        private int timeToReachTarget;
        private int timeSpent;
        private long lastMillis;
        private float targetValue;
        private float lerpValue;

        public LerpingFloat(float initialValue, int timeToReachTarget) {
            this.targetValue = this.lerpValue = initialValue;
            this.timeToReachTarget = timeToReachTarget;
        }

        public LerpingFloat(int initialValue) {
            this(initialValue, 200);
        }

        public void tick() {
            int lastTimeSpent = timeSpent;
            this.timeSpent += System.currentTimeMillis() - lastMillis;

            float lastDistPercent = lastTimeSpent / (float) timeToReachTarget;
            float distPercent = timeSpent / (float) timeToReachTarget;
            float fac = (1 - lastDistPercent) / lastDistPercent;

            float startValue = lerpValue - (targetValue - lerpValue) / fac;
            float dist = targetValue - startValue;
            if (dist == 0) return;

            float oldLerpValue = lerpValue;
            lerpValue = distPercent >= 1 ? targetValue : startValue + dist * distPercent;

            if (lerpValue == oldLerpValue) {
                timeSpent = lastTimeSpent;
            } else {
                this.lastMillis = System.currentTimeMillis();
            }
        }

        public void resetTimer() {
            timeSpent = 0;
            lastMillis = System.currentTimeMillis();
        }

        public float getValue() {
            return lerpValue;
        }

        public void setValue(float value) {
            this.targetValue = this.lerpValue = value;
        }

        public float getTarget() {
            return targetValue;
        }

        public void setTarget(float target) {
            this.targetValue = target;
        }
    }
}