package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class GuiBridge {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        ASMGuiOpenEvent asmEvent = new ASMGuiOpenEvent(event.gui);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
        event.gui = asmEvent.gui;
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        ASMMouseEvent asmEvent = new ASMMouseEvent(event.gui);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        ASMKeyEvent asmEvent = new ASMKeyEvent(event.gui);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        new ASMGuiDrawEvent(event.gui, event.mouseX, event.mouseY).post();
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        ASMGuiDrawPreEvent asmEvent = new ASMGuiDrawPreEvent(event.gui, event.mouseX, event.mouseY);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBackgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent event) {
        new ASMGuiBackgroundDrawEvent(event.gui).post();
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        new ASMGuiInitEvent(event.gui, event.buttonList).post();
    }

    @SubscribeEvent
    public void onGuiInitPre(GuiScreenEvent.InitGuiEvent.Pre event) {
        new ASMGuiInitPreEvent(event.gui, event.buttonList).post();
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        ASMActionPerformedEvent asmEvent = new ASMActionPerformedEvent(event.gui, event.button);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMousePost(GuiScreenEvent.MouseInputEvent.Post event) {
        new ASMGuiMousePostEvent().post();
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        new ASMTooltipEvent(event.itemStack, event.toolTip).post();
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        new ASMEntityJoinWorldEvent(event.entity).post();
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        new ASMPlayerInteractEvent(event.action.ordinal(), event.pos).post();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        new ASMLivingDeathEvent(event.entity).post();
    }
}
