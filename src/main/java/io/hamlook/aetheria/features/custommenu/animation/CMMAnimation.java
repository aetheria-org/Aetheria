package io.hamlook.aetheria.features.custommenu.animation;

public class CMMAnimation {
    public boolean enabled = true;
    public AnimationType type = AnimationType.NONE;
    public int durationMs = 250;
    public int delayMs = 0;
    public int repeat = 0;
    public boolean reverse = false;
    public AnimationCurve customCurve = new AnimationCurve();

    public float progress(long elapsedMs) {
        if (!enabled || type == AnimationType.NONE) return 1f;
        if (elapsedMs <= delayMs) return 0f;
        float raw = Math.max(0f, Math.min(1f, (elapsedMs - delayMs) / (float) Math.max(1, durationMs)));
        float value;
        switch (type) {
            case EASE_IN: value = raw * raw; break;
            case EASE_OUT: value = 1f - (1f - raw) * (1f - raw); break;
            case EASE_IN_OUT: value = raw < .5f ? 2f * raw * raw : 1f - (float) Math.pow(-2f * raw + 2f, 2f) / 2f; break;
            case CUSTOM: value = customCurve == null ? raw : customCurve.sample(raw); break;
            default: value = raw;
        }
        return reverse ? 1f - value : value;
    }
}
