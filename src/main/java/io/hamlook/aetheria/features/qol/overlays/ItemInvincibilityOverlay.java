package io.hamlook.aetheria.features.qol.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.qol.timers.ItemInvincibilityTimers;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.overlay.TimerOverlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.List;

@RegisterEvents
public class ItemInvincibilityOverlay extends TimerOverlay {

    @Getter
    private static ItemInvincibilityOverlay instance;

    public ItemInvincibilityOverlay() {
        super();
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.qol.invincibility.itemInvincibilityPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.qol.invincibility.itemInvincibilityScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.qol.invincibility.itemInvincibilityBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.qol.invincibility.itemInvincibilityCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.invincibility.itemInvincibilityOverlay;
    }

    @Override
    protected String getHeaderText() {
        return "§b§lInvincibility";
    }

    @Override
    protected List<String> getActiveTimers() {
        return ItemInvincibilityTimers.getActiveTimers();
    }

    @Override
    protected ItemStack findItemStack(String id) {
        return ItemInvincibilityTimers.findItemStack(id);
    }

    @Override
    protected long getRemainingMs(String id) {
        return ItemInvincibilityTimers.getRemainingMs(id);
    }

    @Override
    protected boolean shouldShowWhenEmpty() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.invincibility.itemInvincibilityShowWhenEmpty;
    }

    @Override
    protected String getPreviewItemName() {
        return "§5Bonzo's Mask";
    }
}