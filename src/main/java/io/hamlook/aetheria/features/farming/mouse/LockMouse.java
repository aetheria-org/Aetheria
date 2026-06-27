package io.hamlook.aetheria.features.farming.mouse;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class LockMouse {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String PREFIX = EnumChatFormatting.GREEN + "[ASM] " + EnumChatFormatting.RESET;

    private boolean keyWasDown = false;

    public static boolean isLocked() {
        return ATHRConfig.feature != null && ATHRConfig.feature.farming.lockMouseConfig.lockMouse;
    }

    public static void setLocked(boolean locked) {
        if (ATHRConfig.feature == null) return;
        ATHRConfig.feature.farming.lockMouseConfig.lockMouse = locked;
        ATHRConfig.saveConfig();
        ChatUtils.sendMessage(PREFIX + (locked ? EnumChatFormatting.GREEN + "Mouse locked." : EnumChatFormatting.RED + "Mouse unlocked."));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ATHRConfig.feature == null || mc.thePlayer == null || mc.currentScreen != null) return;

        boolean keyDown = KeybindHelper.isKeyDown(ATHRConfig.feature.farming.lockMouseConfig.lockMouseKey);
        if (keyDown && !keyWasDown) setLocked(!isLocked());
        keyWasDown = keyDown;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (ATHRConfig.feature == null || !isLocked() || mc.currentScreen != null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        int iconSize = 16;
        float iconX = (w - iconSize) / 2f;
        float iconY = (h - iconSize) / 2f;

        mc.getTextureManager().bindTexture(Resources.LOCK_CURSOR);
        Utils.drawTexturedRect(iconX, iconY, iconSize, iconSize);

        if (ATHRConfig.feature.farming.lockMouseConfig.showUnlockHint) {
            String hint = "Use /lockyp to unlock mouse";
            float textX = (w - mc.fontRendererObj.getStringWidth(hint)) / 2f;
            mc.fontRendererObj.drawStringWithShadow(hint, textX, iconY + iconSize + 4, 0xFFFFFF);
        }
    }
}