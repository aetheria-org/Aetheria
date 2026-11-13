package io.hamlook.aetheria.features.cosmetics.itemanimations;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.cosmetics.ItemAnimationsConfig;

public final class ItemAnimations {

    public static final float PIVOT_X = 0.56f;
    public static final float PIVOT_Y = -0.52f;
    public static final float PIVOT_Z = -0.72f;
    public static final float EQUIP_DROP = -0.6f;
    public static final float BASE_SCALE = 0.4f;
    public static final int VANILLA_SWING_LENGTH = 6;
    public static final float MIN_SWING_RANGE = 0.25f;
    public static final float MAX_SWING_RANGE = 2f;
    public static final float VANILLA_SWING_RANGE = 1f;

    private ItemAnimations() {}

    public static ItemAnimationsConfig config() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.cosmetics.itemAnimations;
    }

    public static float pivotX(float handSign, float offset) {
        return handSign * PIVOT_X * (1f + offset);
    }

    public static float pivotY(float offset) {
        return PIVOT_Y * (1f - offset);
    }

    public static float pivotZ(float offset) {
        return PIVOT_Z * (1f + offset);
    }

    public static float swingRange() {
        ItemAnimationsConfig config = config();
        if (config == null) return VANILLA_SWING_RANGE;
        return Math.min(Math.max(config.swingRange, MIN_SWING_RANGE), MAX_SWING_RANGE);
    }

    public static int swingLength(int vanillaLength, float speed, boolean ignoreHaste) {
        if (speed <= 0f) return 1;
        int base = ignoreHaste ? VANILLA_SWING_LENGTH : vanillaLength;
        return Math.max((int) (base / speed), 1);
    }

    public static float swingProgress(float progress, float speed) {
        if (speed <= 0f) return 0f;
        return progress < 0f ? 0f : Math.min(progress, 1f);
    }
}
