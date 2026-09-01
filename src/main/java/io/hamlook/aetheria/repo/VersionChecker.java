package io.hamlook.aetheria.repo;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.data.UpdateData;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;

@RegisterEvents
public class VersionChecker {

    private static final UpdateData FALLBACK = new UpdateData();
    private boolean notified = false;

    // Returns true if latest is a higher semver than current.
    public static boolean isNewer(String current, String latest) {
        if (latest == null) return false;
        String[] c = current.replaceAll("[^0-9.]", "").split("\\.");
        String[] l = latest.replaceAll("[^0-9.]", "").split("\\.");
        int len = Math.max(c.length, l.length);
        for (int i = 0; i < len; i++) {
            int cv = i < c.length ? parseSafe(c[i]) : 0;
            int lv = i < l.length ? parseSafe(l[i]) : 0;
            if (lv > cv) return true;
            if (lv < cv) return false;
        }
        return false;
    }

    private static int parseSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        if (notified) return;
        if (!"Welcome to Fakepixel SkyBlock!".equals(event.message.getUnformattedText())) return;
        notified = true;

        UpdateData data = RepoHandler.get(ATHRRepo.KEY_UPDATE, UpdateData.class, FALLBACK);
        if (!isNewer(Aetheria.VERSION, data.version)) return;

        String msg = EnumChatFormatting.GREEN + "[ATHR] " + EnumChatFormatting.YELLOW + "Update available: " + EnumChatFormatting.WHITE + data.version + EnumChatFormatting.GRAY + " (you have " + Aetheria.VERSION + ")" + (data.updateMsg != null && !data.updateMsg.isEmpty() ? "\n" + EnumChatFormatting.AQUA + data.updateMsg : "") + (data.updateUrl != null && !data.updateUrl.isEmpty() ? "\n" + EnumChatFormatting.GOLD + data.updateUrl : "");

        ChatUtils.sendMultilineMessage(msg);
    }
}