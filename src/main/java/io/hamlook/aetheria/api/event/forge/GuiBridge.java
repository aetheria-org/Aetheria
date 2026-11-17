package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.DrawContext;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
//#if FORGE
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#else
//$$ import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
//$$ import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
//$$ import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//$$ import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
//$$ import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
//$$ import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
//$$ import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
//$$ import net.fabricmc.fabric.api.event.player.UseBlockCallback;
//$$ import net.fabricmc.fabric.api.event.player.UseEntityCallback;
//$$ import net.fabricmc.fabric.api.event.player.UseItemCallback;
//$$ import net.minecraft.client.gui.Element;
//$$ import net.minecraft.client.gui.screen.Screen;
//$$ import net.minecraft.client.gui.widget.ButtonWidget;
//$$ import net.minecraft.client.gui.widget.ClickableWidget;
//$$ import net.minecraft.client.gui.widget.Widget;
//$$ import net.minecraft.entity.Entity.RemovalReason;
//$$ import net.minecraft.util.ActionResult;
//$$ import net.minecraft.util.TypedActionResult;
//#endif

//#if FORGE
//#else
//$$ import java.util.ArrayList;
//$$ import java.util.IdentityHashMap;
//$$ import java.util.List;
//$$ import java.util.Map;
//$$ import java.util.logging.Level;
//#endif

@RegisterEvents
public class GuiBridge {

    //$$ private static long lastGuiErrorMs = 0;
    //$$ private static int suppressedGuiErrors = 0;
    //$$
    //$$ private static void logGuiError(Throwable t) {
    //$$     long now = System.currentTimeMillis();
    //$$     suppressedGuiErrors++;
    //$$     if (now - lastGuiErrorMs < 5000) return;
    //$$     Aetheria.logger.log(Level.SEVERE, "[ATHR] GUI render dispatch failed x" + suppressedGuiErrors, t);
    //$$     lastGuiErrorMs = now;
    //$$     suppressedGuiErrors = 0;
    //$$ }

    //#if FORGE
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
        new ASMGuiDrawEvent(new DrawContext(), event.gui, event.mouseX, event.mouseY).post();
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        ASMGuiDrawPreEvent asmEvent = new ASMGuiDrawPreEvent(new DrawContext(), event.gui, event.mouseX, event.mouseY);
        asmEvent.post();
        if (asmEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBackgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent event) {
        new ASMGuiBackgroundDrawEvent(new DrawContext(), event.gui).post();
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

    // 1.8.9 has no entity interact event, every use entity packet is a click on one
    @HandleEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof C02PacketUseEntity)) return;
        if (MinecraftCompat.getLocalWorld() == null) return;
        Entity entity = ((C02PacketUseEntity) event.packet).getEntityFromWorld(MinecraftCompat.getLocalWorld());
        if (entity == null) return;
        new ASMPlayerInteractEvent(ASMPlayerInteractEvent.INTERACT_ENTITY, entity.getPosition(), entity).post();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        new ASMLivingDeathEvent(event.entity).post();
    }
    //#else
    //$$ private static final Map<Screen, List<ButtonWidget>> athr$lastButtons = new IdentityHashMap<>();
    //$$
    //$$ static {
    //$$     ScreenEvents.BEFORE_INIT.register((client, screen, width, height) ->
    //$$         new ASMGuiInitPreEvent(screen, athr$takeButtons(screen)).post());
    //$$     ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> athr$wireScreen(screen));
    //$$     ClientEntityEvents.ENTITY_LOAD.register((entity, world) ->
    //$$         new ASMEntityJoinWorldEvent(entity).post());
    //$$     ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
    //$$         if (entity.getRemovalReason() == RemovalReason.KILLED) {
    //$$             new ASMLivingDeathEvent(entity).post();
    //$$         }
    //$$     });
    //$$     UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
    //$$         new ASMPlayerInteractEvent(1, hit.getBlockPos()).post();
    //$$         return ActionResult.PASS;
    //$$     });
    //$$     UseItemCallback.EVENT.register((player, world, hand) -> {
    //$$         new ASMPlayerInteractEvent(0, player.getBlockPos()).post();
    //$$         return TypedActionResult.pass(player.getStackInHand(hand));
    //$$     });
    //$$     AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
    //$$         new ASMPlayerInteractEvent(2, pos).post();
    //$$         return ActionResult.PASS;
    //$$     });
    //$$     AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
    //$$         new ASMPlayerInteractEvent(ASMPlayerInteractEvent.INTERACT_ENTITY, entity.getBlockPos(), entity).post();
    //$$         return ActionResult.PASS;
    //$$     });
    //$$     UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
    //$$         new ASMPlayerInteractEvent(ASMPlayerInteractEvent.INTERACT_ENTITY, entity.getBlockPos(), entity).post();
    //$$         return ActionResult.PASS;
    //$$     });
    //$$     ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
    //$$         List<String> coded = new ArrayList<>(lines.size());
    //$$         for (int i = 0; i < lines.size(); i++) coded.add(TextCompat.getFormattedTextWithResets(lines.get(i)));
    //$$         List<String> plain = new ArrayList<>(coded);
    //$$         new ASMTooltipEvent(stack, plain).post();
    //$$         for (int i = 0; i < lines.size() && i < plain.size(); i++) {
    //$$             if (plain.get(i).equals(coded.get(i))) continue;
    //$$             lines.set(i, TextCompat.fromFormatted(plain.get(i)));
    //$$         }
    //$$         for (int i = plain.size(); i < lines.size();) lines.remove(plain.size());
    //$$         for (int i = lines.size(); i < plain.size(); i++) lines.add(TextCompat.fromFormatted(plain.get(i)));
    //$$     });
    //$$ }
    //$$
    //$$ private static List<ButtonWidget> athr$takeButtons(Screen screen) {
    //$$     List<ButtonWidget> known = athr$lastButtons.get(screen);
    //$$     return known != null ? known : new ArrayList<>();
    //$$ }
    //$$
    //$$ private static void athr$wireScreen(Screen screen) {
    //$$     List<ButtonWidget> buttons = new ArrayList<>();
    //$$     for (Element child : screen.children()) {
    //$$         if (child instanceof ButtonWidget) buttons.add((ButtonWidget) child);
    //$$     }
    //$$     athr$lastButtons.put(screen, buttons);
    //$$     new ASMGuiInitEvent(screen, buttons).post();
    //$$     ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
    //$$         ASMMouseEvent mouseEvent = new ASMMouseEvent(s, button, true, 0, (int) mouseX, (int) mouseY);
    //$$         mouseEvent.post();
    //$$         return !mouseEvent.isCancelled();
    //$$     });
    //$$     ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
    //$$         ASMMouseEvent mouseEvent = new ASMMouseEvent(s, -1, false, (int) (verticalAmount * 120), (int) mouseX, (int) mouseY);
    //$$         mouseEvent.post();
    //$$         return !mouseEvent.isCancelled();
    //$$     });
    //$$     ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
    //$$         if (button != 0) return true;
    //$$         ButtonWidget target = null;
    //$$         List<? extends Element> kids = s.children();
    //$$         for (int i = kids.size() - 1; i >= 0; i--) {
    //$$             Element child = kids.get(i);
    //$$             if (child instanceof ButtonWidget) {
    //$$                 ButtonWidget widget = (ButtonWidget) child;
    //$$                 if (widget.isMouseOver(mouseX, mouseY)) {
    //$$                     target = widget;
    //$$                     break;
    //$$                 }
    //$$             }
    //$$         }
    //$$         if (target == null) return true;
    //$$         ASMActionPerformedEvent actionEvent = new ASMActionPerformedEvent(s, target);
    //$$         actionEvent.post();
    //$$         return !actionEvent.isCancelled();
    //$$     });
    //$$     ScreenMouseEvents.afterMouseClick(screen).register((s, mouseX, mouseY, button) ->
    //$$         new ASMGuiMousePostEvent().post());
    //$$     ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) -> {
    //$$         while (KeyboardCompat.next()) {
    //$$         }
    //$$         ASMKeyEvent keyEvent = new ASMKeyEvent(s);
    //$$         keyEvent.post();
    //$$         return !keyEvent.isCancelled();
    //$$     });
    //$$     ScreenEvents.beforeRender(screen).register((s, context, mouseX, mouseY, tickDelta) -> {
    //#if MC < 1.21
    //$$         new ASMGuiBackgroundDrawEvent(new DrawContext(), s).post();
    //$$         new ASMGuiDrawPreEvent(new DrawContext(), s, mouseX, mouseY).post();
    //#else
    //$$         new ASMGuiBackgroundDrawEvent(context, s).post();
    //$$         context.draw(() -> {
    //$$             try {
    //$$                 new ASMGuiDrawPreEvent(context, s, mouseX, mouseY).post();
    //$$             } catch (Throwable t) {
    //$$                 logGuiError(t);
    //$$             }
    //$$         });
    //#endif
    //$$     });
    //$$     ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
    //#if MC < 1.21
    //$$         new ASMGuiDrawEvent(new DrawContext(), s, mouseX, mouseY).post());
    //#else
    //$$         context.draw(() -> {
    //$$             try {
    //$$         context.draw(() -> {
    //$$             try {
    //$$                 new ASMGuiDrawEvent(context, s, mouseX, mouseY).post();
    //$$             } catch (Throwable t) {
    //$$                 logGuiError(t);
    //$$             }
    //$$         });
    //$$             } catch (Throwable t) {
    //$$                 logGuiError(t);
    //$$             }
    //$$         }));
    //#endif
    //$$ }
    //#endif
}
