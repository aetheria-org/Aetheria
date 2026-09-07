package io.hamlook.aetheria.features.mining.fetchur;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.scoreboard.CustomScoreboard;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

@RegisterEvents
public class FetchurOverlay extends Overlay {

    @Getter
    private static FetchurOverlay instance;

    public FetchurOverlay() {
        super(160, LINE_HEIGHT + PADDING * 2);
        instance = this;
    }

    @Override
    protected int getBaseWidth() {
        return 160;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.mining.fetchur.fetchurOverlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.mining.fetchur.fetchurOverlayScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.mining.fetchur.overlayBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.mining.fetchur.overlayCornerRadius;
    }

    @Override
    protected boolean extraGuard() {
        return SkyblockData.isOnSkyblock() && !CustomScoreboard.isActive();
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.mining.fetchur.showFetchurOverlay;
    }

    @Override
    protected boolean hideOnChat()   { return ATHRConfig.feature.mining.fetchur.hideOnChat; }
    @Override
    protected boolean hideOnTab()    { return ATHRConfig.feature.mining.fetchur.hideOnTab; }
    @Override
    protected boolean hideOnDebug()  { return ATHRConfig.feature.mining.fetchur.hideOnDebug; }

    @Override
    public List<String> getLines(boolean preview) {
        String item = preview ? "Yellow Stained Glass x20" : FetchurData.getTodaysItem();
        return Collections.singletonList(EnumChatFormatting.GOLD + "Fetchur: " + EnumChatFormatting.YELLOW + item);
    }
}