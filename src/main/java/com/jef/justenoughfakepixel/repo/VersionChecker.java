package com.jef.justenoughfakepixel.repo;

import com.jef.justenoughfakepixel.JefMod;
import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.repo.data.UpdateData;
import com.jef.justenoughfakepixel.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (notified) return;
        if (!"Welcome to Fakepixel SkyBlock!".equals(event.message.getUnformattedText())) return;
        notified = true;

        UpdateData data = RepoHandler.get(JefRepo.KEY_UPDATE, UpdateData.class, FALLBACK);
        if (!isNewer(JefMod.VERSION, data.version)) return;

        String msg = EnumChatFormatting.GREEN + "[JEF] " + EnumChatFormatting.YELLOW + "Update available: " + EnumChatFormatting.WHITE + data.version + EnumChatFormatting.GRAY + " (you have " + JefMod.VERSION + ")" + (data.updateMsg != null && !data.updateMsg.isEmpty() ? "\n" + EnumChatFormatting.AQUA + data.updateMsg : "") + (data.updateUrl != null && !data.updateUrl.isEmpty() ? "\n" + EnumChatFormatting.GOLD + data.updateUrl : "");

        ChatUtils.sendMultilineMessage(msg);
    }
}