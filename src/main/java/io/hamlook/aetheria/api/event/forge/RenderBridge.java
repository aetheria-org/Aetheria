package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMBlockHighlightEvent;
import io.hamlook.aetheria.events.ASMRenderOverlayEvent;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class RenderBridge {

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        new ASMRenderWorldEvent(event.partialTicks).post();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        new ASMRenderOverlayEvent(sr, event.partialTicks, event.type.ordinal()).post();
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        ASMBlockHighlightEvent asmEvent = new ASMBlockHighlightEvent(event.partialTicks, event.target);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }
}
