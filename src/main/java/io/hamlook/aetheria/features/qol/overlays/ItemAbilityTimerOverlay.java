package io.hamlook.aetheria.features.qol.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.qol.timers.ItemAbilityTimers;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.overlay.TimerOverlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.List;

@RegisterEvents
public class ItemAbilityTimerOverlay extends TimerOverlay {

    @Getter
    private static ItemAbilityTimerOverlay instance;

    public ItemAbilityTimerOverlay() {
        super();
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.qol.abilityTimer.itemAbilityTimerPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.qol.abilityTimer.itemAbilityTimerScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.qol.abilityTimer.itemAbilityTimerBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.qol.abilityTimer.itemAbilityTimerCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.abilityTimer.itemAbilityTimerOverlay;
    }

    @Override
    protected String getHeaderText() {
        return "§b§lAbility Timers";
    }

    @Override
    protected List<String> getActiveTimers() {
        return ItemAbilityTimers.getActiveTimers();
    }

    @Override
    protected ItemStack findItemStack(String id) {
        return ItemAbilityTimers.findItemStack(id);
    }

    @Override
    protected long getRemainingMs(String id) {
        return ItemAbilityTimers.getRemainingMs(id);
    }

    @Override
    protected boolean shouldShowWhenEmpty() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.abilityTimer.itemAbilityTimerShowWhenEmpty;
    }

    @Override
    protected String getPreviewItemName() {
        return "§6Fire Veil";
    }
}