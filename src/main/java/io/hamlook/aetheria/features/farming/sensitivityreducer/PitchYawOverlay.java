package io.hamlook.aetheria.features.farming.sensitivityreducer;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.SensitivityReducerConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class PitchYawOverlay extends Overlay {

    @Getter
    private static PitchYawOverlay instance;

    public PitchYawOverlay() {
        super(90, 24);
        instance = this;
    }

    private static SensitivityReducerConfig config() {
        return ATHRConfig.feature.farming.sensitivityReducer;
    }

    @Override
    public Position getPosition() {
        return config().pitchYawOverlayPos;
    }

    @Override
    public float getScale() {
        return config().pitchYawOverlayScale;
    }

    @Override
    public int getBgColor() {
        return 0x64000000;
    }

    @Override
    public int getCornerRadius() {
        return 4;
    }

    @Override
    protected boolean isEnabled() {
        if (ATHRConfig.feature == null || !config().showPitchYawOverlay) return false;
        if (config().showOnlyWhileFarming && !FarmingApi.isCurrentlyFarming()) return false;
        return !config().showOnlyWhileHoldingFarmingTool || FarmingApi.isHoldingFarmingTool();
    }

    @Override
    protected boolean extraGuard() {
        return mc.thePlayer != null;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();

        if (preview) {
            lines.add("Pitch: 12.3456");
            lines.add("Yaw: -45.6789");
            return lines;
        }

        if (mc.thePlayer == null) return lines;

        lines.add(String.format("Pitch: %.4f", wrapDegrees(mc.thePlayer.rotationPitch)));
        lines.add(String.format("Yaw: %.4f", wrapDegrees(mc.thePlayer.rotationYaw)));
        return lines;
    }

    // Vanilla's rotationYaw/rotationPitch aren't bounded to -180..180 - they keep
    // accumulating the further you turn (e.g. spinning around repeatedly can push
    // yaw past 180, 360, 720...). Wrap the displayed value into (-180, 180] so it
    // flips sign once you cross the boundary, instead of just climbing forever.
    private static float wrapDegrees(float value) {
        float wrapped = value % 360f;
        if (wrapped >= 180f) wrapped -= 360f;
        else if (wrapped < -180f) wrapped += 360f;
        return wrapped;
    }

    // Two-tone per-line text: "Pitch"/"Yaw" labels use the configured color while the
    // numbers stay white.
    @Override
    protected void drawLine(String line, int x, int y) {
        int splitIdx = line.indexOf(':') + 1;
        String label = line.substring(0, splitIdx);
        String value = line.substring(splitIdx);
        int labelColor = ChromaColour.specialToChromaRGB(config().pitchYawLabelColor);
        mc.fontRendererObj.drawStringWithShadow(label, x, y, labelColor);
        mc.fontRendererObj.drawStringWithShadow(value, x + mc.fontRendererObj.getStringWidth(label), y, 0xFFFFFF);
    }
}
