package io.hamlook.aetheria.features.qol.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.qol.timers.ItemCooldowns;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.overlay.TimerOverlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.List;

@RegisterEvents
public class ItemCooldownOverlay extends TimerOverlay {

    @Getter
    private static ItemCooldownOverlay instance;

    public ItemCooldownOverlay() {
        super();
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.qol.itemCooldown.itemCooldownPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.qol.itemCooldown.itemCooldownScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.qol.itemCooldown.itemCooldownBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.qol.itemCooldown.itemCooldownCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.itemCooldown.itemCooldownOverlay;
    }

    @Override
    protected String getHeaderText() {
        return "§b§lCooldowns";
    }

    @Override
    protected List<String> getActiveTimers() {
        return ItemCooldowns.getActiveCooldowns();
    }

    @Override
    protected ItemStack findItemStack(String id) {
        return ItemCooldowns.findItemStack(id);
    }

    @Override
    protected long getRemainingMs(String id) {
        return ItemCooldowns.getRemainingMs(id);
    }

    @Override
    protected boolean shouldShowWhenEmpty() {
        return ATHRConfig.feature != null && ATHRConfig.feature.qol.itemCooldown.itemCooldownShowWhenEmpty;
    }

    @Override
    protected String getPreviewItemName() {
        return "§5Example Item";
    }
}