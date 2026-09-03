package io.hamlook.aetheria.features.qol.raredroptracker;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMGuiDrawEvent;
import io.hamlook.aetheria.events.ASMGuiMousePostEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Handles the "click anywhere within 5 seconds" prompt fired after a tracked
 * Rare Drop Tracker item with a bound command is picked up. The click-to-open
 * mechanism (dim overlay + hint text, listen for the next left click, run the
 * command) mirrors the pattern NotEnoughFakepixel uses for its Maddox batphone
 * "click anywhere on-screen to open Maddox" prompt.
 */
@RegisterEvents
public class RareDropCommandTrigger {

    private static final long WINDOW_MS = 5000L;

    private static String pendingCommand = null;
    private static long promptStartTime = 0L;
    private static boolean messageSent = false;

    private RareDropCommandTrigger() {
    }

    /**
     * Arms the trigger: the next left click anywhere (while any GUI screen is
     * open) within {@link #WINDOW_MS} will run the given command.
     */
    public static void arm(String command) {
        pendingCommand = command;
        promptStartTime = System.currentTimeMillis();
        messageSent = false;
    }

    private static boolean isActive() {
        return pendingCommand == null || System.currentTimeMillis() - promptStartTime >= WINDOW_MS;
    }

    @HandleEvent
    public void onDraw(ASMGuiDrawEvent event) {
        if (isActive()) {
            pendingCommand = null;
            return;
        }

        if (!messageSent) {
            ChatUtils.sendMessage("§d§lClick anywhere with-in 5 seconds to open the command you set for this drop!");
            messageSent = true;
        }

        Minecraft mc = MinecraftCompat.getMinecraft();
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();

        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x38000000);

        String msg = "§dClick anywhere to run your drop command!";
        int w = mc.fontRendererObj.getStringWidth(msg);
        mc.fontRendererObj.drawStringWithShadow(msg, (sr.getScaledWidth() - w) / 2f, sr.getScaledHeight() / 2f, -1);
    }

    @HandleEvent
    public void onMouseInputPost(ASMGuiMousePostEvent event) {
        if (isActive()) return;
        if (!MouseCompat.getEventButtonState() || MouseCompat.getEventButton() != 0) return;

        String command = pendingCommand;
        pendingCommand = null;
        ChatUtils.sendChatCommand(command);
    }
}
