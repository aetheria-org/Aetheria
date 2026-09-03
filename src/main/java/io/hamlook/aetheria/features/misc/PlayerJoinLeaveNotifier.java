package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RegisterEvents
public class PlayerJoinLeaveNotifier {

    private static final int TICK_INTERVAL = 20;
    private static final String PREFIX = "§8[Aetheria] ";
    private final Set<String> notified = new HashSet<>();
    private int tickCounter = 0;

    private static String format(String template, String playerName) {
        return template.replace("&&", "§").replace("%s", playerName);
    }

    private boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.playerJoinLeave.enabled && !ATHRConfig.feature.misc.playerJoinLeave.playersList.trim().isEmpty();
    }

    private Set<String> watchList() {
        String raw = ATHRConfig.feature.misc.playerJoinLeave.playersList;
        if (raw == null || raw.trim().isEmpty()) return Collections.emptySet();
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(String::toLowerCase).collect(Collectors.toSet());
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if ((tickCounter = (tickCounter + 1) % TICK_INTERVAL) != 0) return;
        if (!isEnabled()) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null) return;

        String selfName = mc.thePlayer.getGameProfile().getName().toLowerCase();
        Set<String> watch = watchList();

        Set<String> lobby = new HashSet<>();
        for (NetworkPlayerInfo info : mc.thePlayer.sendQueue.getPlayerInfoMap()) {
            String name = info.getGameProfile().getName().toLowerCase();
            if (!name.equals(selfName)) lobby.add(name);
        }

        for (NetworkPlayerInfo info : mc.thePlayer.sendQueue.getPlayerInfoMap()) {
            String lower = info.getGameProfile().getName().toLowerCase();
            if (watch.contains(lower) && lobby.contains(lower) && notified.add(lower)) {
                String display = info.getGameProfile().getName();
                ChatUtils.sendMessage(PREFIX + format(ATHRConfig.feature.misc.playerJoinLeave.joinMessage, display));
            }
        }

        Set<String> left = new HashSet<>(notified);
        left.retainAll(watch);
        left.removeAll(lobby);
        for (String name : left) {
            notified.remove(name);
            ChatUtils.sendMessage(PREFIX + format(ATHRConfig.feature.misc.playerJoinLeave.leaveMessage, name));
        }
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        notified.clear();
    }
}
