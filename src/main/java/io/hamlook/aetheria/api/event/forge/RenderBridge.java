package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.events.ASMBlockHighlightEvent;
import io.hamlook.aetheria.events.ASMRenderOverlayEvent;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import java.util.logging.Level;
//#if FORGE
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//$$ import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import io.hamlook.aetheria.utils.render.WorldRenderUtils;
//#endif

@RegisterEvents
public class RenderBridge {

    //$$ private static long lastHudErrorMs = 0;
    //$$ private static int suppressedHudErrors = 0;
    //$$
    //$$ private static void logHudError(Throwable t) {
    //$$     long now = System.currentTimeMillis();
    //$$     suppressedHudErrors++;
    //$$     if (now - lastHudErrorMs < 5000) return;
    //$$     Aetheria.logger.log(Level.SEVERE, "[ATHR] HUD overlay dispatch failed x" + suppressedHudErrors, t);
    //$$     lastHudErrorMs = now;
    //$$     suppressedHudErrors = 0;
    //$$ }

    //#if FORGE
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        new ASMRenderWorldEvent(event.partialTicks).post();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        new ASMRenderOverlayEvent(
            new DrawContext(),
            GuiScreenUtils.getScaledWindowWidth(),
            GuiScreenUtils.getScaledWindowHeight(),
            event.partialTicks, 0
        ).post();
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        ASMBlockHighlightEvent asmEvent = new ASMBlockHighlightEvent(event.partialTicks, event.target);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }
    //#else
    //$$ static {
    //$$     WorldRenderEvents.LAST.register(context -> {
    //$$         WorldRenderUtils.setWorldContext(context.matrixStack(), context.consumers());
    //$$         try {
    //$$             new ASMRenderWorldEvent(context.tickCounter().getTickDelta(false)).post();
    //$$         } finally {
    //$$             WorldRenderUtils.clearWorldContext();
    //$$         }
    //$$     });
    //$$     HudRenderCallback.EVENT.register((a, b) -> {
    //$$         MinecraftClient client = MinecraftClient.getInstance();
    //#if MC < 1.21
    //$$         new ASMRenderOverlayEvent(
    //$$             new DrawContext(),
    //$$             client.getWindow().getScaledWidth(),
    //$$             client.getWindow().getScaledHeight(),
    //$$             b, 0).post();
//#else
    //$$         a.draw(() -> {
    //$$             try {
    //$$                 new ASMRenderOverlayEvent(
    //$$                     a,
    //$$                     GuiScreenUtils.getScaledWindowWidth(),
    //$$                     GuiScreenUtils.getScaledWindowHeight(),
    //$$                     b.getTickDelta(false), 0).post();
    //$$             } catch (Throwable t) {
    //$$                 logHudError(t);
    //$$             }
    //$$         });
    //#endif
    //$$     });
    //$$     WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> {
    //$$         WorldRenderUtils.setWorldContext(context.matrixStack(), context.consumers());
    //$$         try {
    //$$             ASMBlockHighlightEvent asmEvent = new ASMBlockHighlightEvent(
    //$$                 context.tickCounter().getTickDelta(false), hit);
    //$$             asmEvent.post();
    //$$             return !asmEvent.isCancelled();
    //$$         } finally {
    //$$             WorldRenderUtils.clearWorldContext();
    //$$         }
    //$$     });
    //$$ }
    //#endif
}
