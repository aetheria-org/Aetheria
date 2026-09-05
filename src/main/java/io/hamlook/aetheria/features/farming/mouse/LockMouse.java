package io.hamlook.aetheria.features.farming.mouse;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.overlay.SimpleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class LockMouse extends SimpleOverlay {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();
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

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ATHRConfig.feature == null || MinecraftCompat.getLocalPlayer() == null || MinecraftCompat.getCurrentScreen() != null) return;

        boolean keyDown = KeybindHelper.isKeyDown(ATHRConfig.feature.farming.lockMouseConfig.lockMouseKey);
        if (keyDown && !keyWasDown) setLocked(!isLocked());
        keyWasDown = keyDown;
    }

    @Override
    public boolean shouldRender() {
        return isLocked() && MinecraftCompat.getCurrentScreen() == null;
    }

    @Override
    public void render(ScaledResolution sr) {
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();

        int iconSize = 16;
        float iconX = (w - iconSize) / 2f;
        float iconY = (h - iconSize) / 2f;

        mc.getTextureManager().bindTexture(Resources.LOCK_CURSOR);
        Utils.drawTexturedRect(iconX, iconY, iconSize, iconSize);

        if (ATHRConfig.feature.farming.lockMouseConfig.showUnlockHint) {
            String hint = "Use /lockyp to unlock mouse";
            float textX = (w - MinecraftCompat.getFontRenderer().getStringWidth(hint)) / 2f;
            MinecraftCompat.getFontRenderer().drawStringWithShadow(hint, textX, iconY + iconSize + 4, 0xFFFFFF);
        }
    }
}
