package io.hamlook.aetheria.utils.overlay;

import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMRenderOverlayEvent;

public abstract class SimpleOverlay {

    public abstract boolean shouldRender();

    public abstract void render(ScaledResolution sr);

    @HandleEvent
    public final void onRenderOverlay(ASMRenderOverlayEvent event) {
        if (event.type != 0) return;
        if (ATHRConfig.feature == null) return;
        boolean shouldHide = (hideOnChat() && OverlayUtils.isChatOpen())
            || (hideOnTab() && OverlayUtils.isTabHeld())
            || (hideOnDebug() && OverlayUtils.isDebugActive())
            || OverlayUtils.isStorageActive();
        if (shouldHide) return;
        if (!shouldRender()) return;
        render(event.resolution);
    }

    protected boolean hideOnChat()   { return true; }
    protected boolean hideOnTab()    { return true; }
    protected boolean hideOnDebug()  { return true; }
}
