package io.hamlook.aetheria.api.event.forge;

import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.DrawContext;
//#if FORGE
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
//$$ import net.fabricmc.fabric.api.event.player.UseItemCallback;
//$$ import net.minecraft.client.gui.Element;
//$$ import net.minecraft.client.gui.screen.Screen;
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
//#endif

@RegisterEvents
public class GuiBridge {

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

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        new ASMLivingDeathEvent(event.entity).post();
    }
    //#else
    //$$ private static final Map<Screen, List<Widget>> athr$lastButtons = new IdentityHashMap<>();
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
    //$$         new ASMPlayerInteractEvent(3, entity.getBlockPos()).post();
    //$$         return ActionResult.PASS;
    //$$     });
    //$$     ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
    //$$         List<String> plain = new ArrayList<>(lines.size());
    //$$         for (int i = 0; i < lines.size(); i++) plain.add(lines.get(i).getString());
    //$$         new ASMTooltipEvent(stack, plain).post();
    //$$     });
    //$$ }
    //$$
    //$$ private static List<Widget> athr$takeButtons(Screen screen) {
    //$$     List<Widget> known = athr$lastButtons.get(screen);
    //$$     return known != null ? known : new ArrayList<>();
    //$$ }
    //$$
    //$$ private static void athr$wireScreen(Screen screen) {
    //$$     List<Element> children = screen.children();
    //$$     List<Widget> buttons = new ArrayList<>();
    //$$     for (int i = 0; i < children.size(); i++) {
    //$$         Element child = children.get(i);
    //$$         if (child instanceof ClickableWidget) buttons.add((Widget) child);
    //$$     }
    //$$     athr$lastButtons.put(screen, buttons);
    //$$     new ASMGuiInitEvent(screen, buttons).post();
    //$$     ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
    //$$         ASMMouseEvent mouseEvent = new ASMMouseEvent(s);
    //$$         mouseEvent.post();
    //$$         return !mouseEvent.isCancelled();
    //$$     });
    //$$     ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
    //$$         if (button != 0) return true;
    //$$         Widget target = null;
    //$$         List<Element> kids = s.children();
    //$$         for (int i = kids.size() - 1; i >= 0; i--) {
    //$$             Element child = kids.get(i);
    //$$             if (child instanceof ClickableWidget) {
    //$$                 ClickableWidget widget = (ClickableWidget) child;
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
    //$$         new ASMGuiDrawPreEvent(context, s, mouseX, mouseY).post();
    //#endif
    //$$     });
    //$$     ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
    //#if MC < 1.21
    //$$         new ASMGuiDrawEvent(new DrawContext(), s, mouseX, mouseY).post());
    //#else
    //$$         new ASMGuiDrawEvent(context, s, mouseX, mouseY).post());
    //#endif
    //$$ }
    //#endif
}
