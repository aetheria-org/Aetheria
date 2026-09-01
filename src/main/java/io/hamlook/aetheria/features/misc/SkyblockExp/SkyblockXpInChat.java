package io.hamlook.aetheria.features.misc.SkyblockExp;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.misc.Misc;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraft.client.Minecraft;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;

@RegisterEvents
public class SkyblockXpInChat {

    private static final String PREFIX = "§b[ASM] §r";

    private static int pendingLevel = -1;
    private static int pendingXp = -1;
    private static long pendingTime = 0L;

    private static String getLevelColor(int level) {
        if (level >= 480) return "§4";
        if (level >= 440) return "§c";
        if (level >= 400) return "§6";
        if (level >= 360) return "§5";
        if (level >= 320) return "§d";
        if (level >= 280) return "§9";
        if (level >= 240) return "§3";
        if (level >= 200) return "§b";
        if (level >= 160) return "§2";
        if (level >= 120) return "§a";
        if (level >= 80) return "§e";
        if (level >= 40) return "§f";
        return "§7";
    }

    private static String formatLevel(int level) {
        return "§8[" + getLevelColor(level) + level + "§8]";
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.misc.skyblockXpInChat) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (!SkyblockData.isOnSkyblock()) return;
        if (Minecraft.getMinecraft().thePlayer.ticksExisted % 10 != 0) return;

        int currentXp = TablistParser.getSbCurrentXp();
        int maxXp = TablistParser.getSbMaxXp();
        int level = TablistParser.getSbLevel();

        if (currentXp == 0 && maxXp == 0) return;

        String profile = SkyblockData.getCurrentProfile();

        if (profile.isEmpty()) {
            if (pendingLevel == -1) {
                pendingLevel = level;
                pendingXp = currentXp;
                pendingTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - pendingTime > 5000) {
                pendingLevel = -1;
                pendingXp = -1;
                pendingTime = 0L;
            }
            return;
        }

        if (pendingLevel != -1 && System.currentTimeMillis() - pendingTime <= 5000) {
            Misc.SbProfileData fresh = new Misc.SbProfileData();
            fresh.lastSbLevel = pendingLevel;
            fresh.lastSbXp = pendingXp;
            ATHRConfig.feature.misc.sbProfileData.put(SkyblockData.getEnvironmentKey() + ":" + profile, fresh);
            ATHRConfig.markConfigDirty();
            pendingLevel = -1;
            pendingXp = -1;
            pendingTime = 0L;
            return;
        }
        pendingLevel = -1;
        pendingXp = -1;
        pendingTime = 0L;

        Misc.SbProfileData data = ATHRConfig.feature.misc.sbProfileData.computeIfAbsent(SkyblockData.getEnvironmentKey() + ":" + profile, k -> new Misc.SbProfileData());

        if (data.lastSbLevel == -1 || data.lastSbXp == -1) {
            data.lastSbLevel = level;
            data.lastSbXp = currentXp;
            ATHRConfig.markConfigDirty();
            return;
        }

        if (level != data.lastSbLevel) {
            int totalDiff = (level * 100 + currentXp) - (data.lastSbLevel * 100 + data.lastSbXp);
            String xpPart = "§7(§b" + currentXp + "§3/§b" + maxXp + "§7)";
            if (totalDiff > 0) {
                ChatUtils.sendMessage(PREFIX + "§6§lLEVEL UP! " + formatLevel(data.lastSbLevel) + " §e→ " + formatLevel(level) + " §a+" + totalDiff + " XP " + xpPart);
            } else {
                ChatUtils.sendMessage(PREFIX + "§c§lLEVEL DOWN! " + formatLevel(data.lastSbLevel) + " §e→ " + formatLevel(level) + " §c" + totalDiff + " XP " + xpPart);
            }
        } else if (currentXp != data.lastSbXp) {
            String xpPart = "§7(§b" + currentXp + "§3/§b" + maxXp + "§7)";
            int diff = currentXp - data.lastSbXp;
            if (diff > 0) {
                ChatUtils.sendMessage(PREFIX + "§a+" + diff + " XP " + xpPart);
            } else {
                ChatUtils.sendMessage(PREFIX + "§c" + diff + " XP " + xpPart);
            }
        }

        data.lastSbLevel = level;
        data.lastSbXp = currentXp;
        ATHRConfig.markConfigDirty();
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        pendingLevel = -1;
        pendingXp = -1;
        pendingTime = 0L;
    }
}
