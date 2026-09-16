package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.ASMBlockHighlightEvent;
import io.hamlook.aetheria.events.ASMRenderOverlayEvent;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
//#if FORGE
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//$$ import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//$$ import net.minecraft.client.MinecraftClient;
//#endif

@RegisterEvents
public class RenderBridge {

    //#if FORGE
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        new ASMRenderWorldEvent(event.partialTicks).post();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        new ASMRenderOverlayEvent(
            new DrawContext(),
            GuiScreenUtils.getScaledWindowWidth(),
            GuiScreenUtils.getScaledWindowHeight(),
            event.partialTicks, event.type.ordinal()
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
    //$$     WorldRenderEvents.LAST.register(context ->
    //$$         new ASMRenderWorldEvent(context.tickCounter().getTickDelta(false)).post());
    //$$     HudRenderCallback.EVENT.register((a, b) -> {
    //$$         MinecraftClient client = MinecraftClient.getInstance();
    //#if MC < 1.21
    //$$         new ASMRenderOverlayEvent(
    //$$             new DrawContext(),
    //$$             client.getWindow().getScaledWidth(),
    //$$             client.getWindow().getScaledHeight(),
    //$$             b, 0).post();
    //#else
    //$$         new ASMRenderOverlayEvent(
    //$$             a,
    //$$             client.getWindow().getScaledWidth(),
    //$$             client.getWindow().getScaledHeight(),
    //$$             b.getTickDelta(false), 0).post();
    //#endif
    //$$     });
    //$$     WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> {
    //$$         ASMBlockHighlightEvent asmEvent = new ASMBlockHighlightEvent(
    //$$             context.tickCounter().getTickDelta(false), hit);
    //$$         asmEvent.post();
    //$$         return !asmEvent.isCancelled();
    //$$     });
    //$$ }
    //#endif
}
