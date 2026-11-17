package io.hamlook.aetheria.features.misc.invbuttons;


import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMMouseEvent;
import io.hamlook.aetheria.events.GuiContainerRenderBeforeTooltipEvent;
import io.hamlook.aetheria.features.farming.visitors.VisitorPanelBase;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextureCompat;
import io.hamlook.aetheria.utils.render.HighlightUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen;
//#endif
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import io.hamlook.aetheria.utils.render.TextRenderUtils;
import java.util.Collections;

@RegisterEvents
public class InvButtonRenderer {

    private static final ResourceLocation EDITOR_TEX = Resources.INV_EDITOR_TEX;

    private InventoryButton hovered = null;
    private long hoveredSince = 0L;

    private static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.invButtons.enableInvButtons;
    }

    private static boolean isGuiEditor() {
        return MinecraftCompat.getCurrentScreen() instanceof GuiInvButtonEditor;
    }

    private static int btnX(InventoryButton btn, int gl, int gw) {
        return gl + btn.x + (btn.anchorRight ? gw : 0);
    }

    private static int btnY(InventoryButton btn, int gt, int gh) {
        return gt + btn.y + (btn.anchorBottom ? gh : 0);
    }

    //#if MC < 1.21
    private static boolean isVisible(InventoryButton btn, GuiContainer gui) {
    //#else
    //$$ private static boolean isVisible(InventoryButton btn, HandledScreen<?> gui) {
    //#endif
        return btn.isActive() && (!btn.playerInvOnly || gui instanceof GuiInventory);
    }

    //#if MC < 1.21
    private static boolean isTerminalMenu(GuiContainer gui) {
    //#else
    //$$ private static boolean isTerminalMenu(HandledScreen<?> gui) {
    //#endif
        String name = ContainerUtils.getContainerName(gui);
        if (name == null) return false;
        return name.equals("Click in order!") ||
               name.startsWith("Select all the") ||
               name.startsWith("What starts with") ||
               name.contains("Complete the maze!");
    }

    //#if MC < 1.21
    private static InventoryButton hitTest(int mx, int my, int gl, int gt, int gw, int gh, GuiContainer gui) {
    //#else
    //$$ private static InventoryButton hitTest(int mx, int my, int gl, int gt, int gw, int gh, HandledScreen<?> gui) {
    //#endif
        if (io.hamlook.aetheria.features.farming.visitors.VisitorPanelBase.isOverPanel(mx, my)) return null;
        for (InventoryButton btn : InventoryButtonStorage.getInstance().getButtons()) {
            if (!isVisible(btn, gui)) continue;
            int bx = btnX(btn, gl, gw);
            int by = btnY(btn, gt, gh);
            if (mx >= bx && mx <= bx + 18 && my >= by && my <= by + 18) return btn;
        }
        return null;
    }

    @HandleEvent
    public void onRenderButtons(GuiContainerRenderBeforeTooltipEvent event) {
        if (!isEnabled() || isGuiEditor()) return;
        if (StorageManager.isOverlayActive()) return;

        //#if MC < 1.21
        GuiContainer gui = event.gui;
        //#else
        //$$ HandledScreen<?> gui = event.gui;
        //#endif
        if (ATHRConfig.feature.misc.invButtons.disableInTerminals && isTerminalMenu(gui)) return;
        int gl = InventoryCompat.getGuiLeft(gui), gt = InventoryCompat.getGuiTop(gui), gw = InventoryCompat.getXSize(gui), gh = InventoryCompat.getYSize(gui);
        int mx = event.mouseX, my = event.mouseY;

        GlStateManagerCompat.pushMatrix();
        if (event.fromForeground) {
            GlStateManagerCompat.translate(-gl, -gt, 50);
        }
        for (InventoryButton btn : InventoryButtonStorage.getInstance().getButtons()) {
            if (!isVisible(btn, gui)) continue;
            int bx = btnX(btn, gl, gw);
            int by = btnY(btn, gt, gh);
            if (VisitorPanelBase.isRectOverPanel(bx, by, 18, 18)) continue;

            GlStateManagerCompat.color(1, 1, 1, 1f);
            GlStateManagerCompat.enableDepth();
            GlStateManagerCompat.enableAlpha();
            TextureCompat.drawBoundTextured(EDITOR_TEX, bx, by, 18, 18, btn.backgroundIndex * 18 / 256f, 18 / 256f, (btn.backgroundIndex * 18 + 18) / 256f, 36 / 256f);
            if (btn.icon != null && !btn.icon.trim().isEmpty()) {
                GlStateManagerCompat.enableDepth();
                InvButtonIconRenderer.renderIcon(btn.icon, bx + 1, by + 1);
            }
        }
        GlStateManagerCompat.popMatrix();

        InventoryButton newHovered = hitTest(mx, my, gl, gt, gw, gh, gui);
        long now = System.currentTimeMillis();
        if (newHovered != hovered) {
            hovered = newHovered;
            hoveredSince = now;
        }

        if (hovered == null) return;

        int bx = btnX(hovered, gl, gw);
        int by = btnY(hovered, gt, gh);
        GlStateManagerCompat.pushMatrix();
        if (event.fromForeground) {
            GlStateManagerCompat.translate(-gl, -gt, 60);
        }
        HighlightUtils.renderButtonHighlight(bx, by);
        GlStateManagerCompat.popMatrix();

        int delay = ATHRConfig.feature != null ? ATHRConfig.feature.misc.invButtons.invButtonTooltipDelay : 600;
        if (now - hoveredSince >= delay) {
            String cmd = hovered.command.trim();
            if (!cmd.startsWith("/")) cmd = "/" + cmd;
            GlStateManagerCompat.pushMatrix();
            if (event.fromForeground) {
                GlStateManagerCompat.translate(-gl, -gt, 0);
            }
            TextRenderUtils.drawHoveringText(Collections.singletonList("§7" + cmd), mx, my, 400, MinecraftCompat.getFontRenderer());
            GlStateManagerCompat.popMatrix();
        }

        GlStateManagerCompat.disableAlpha();
    }

    @HandleEvent
    public void onMouseInput(ASMMouseEvent event) {
        if (!isEnabled() || isGuiEditor()) return;
        if (StorageManager.isOverlayActive()) return;
        if (event.button < 0) return;
        if (!(event.gui instanceof GuiContainer)) return;

        //#if MC < 1.21
        GuiContainer gui = (GuiContainer) event.gui;
        //#else
        //$$ HandledScreen<?> gui = (HandledScreen<?>) event.gui;
        //#endif
        if (ATHRConfig.feature.misc.invButtons.disableInTerminals && isTerminalMenu(gui)) return;
        int gl = InventoryCompat.getGuiLeft(gui), gt = InventoryCompat.getGuiTop(gui), gw = InventoryCompat.getXSize(gui), gh = InventoryCompat.getYSize(gui);
        int mx = event.mouseX, my = event.mouseY;

        InventoryButton btn = hitTest(mx, my, gl, gt, gw, gh, gui);
        if (btn == null) return;

        if (InventoryCompat.getCursorStack(MinecraftCompat.getLocalPlayer()) != null) {
            event.cancel();
            return;
        }

        int clickType = ATHRConfig.feature != null ? ATHRConfig.feature.misc.invButtons.invButtonClickType : 0;
        boolean fire = (clickType == 0) == event.buttonState;
        if (fire) {
            String cmd = btn.command.trim();
            if (!cmd.startsWith("/")) cmd = "/" + cmd;
            ChatUtils.sendChatCommand(cmd);
        }
    }
}