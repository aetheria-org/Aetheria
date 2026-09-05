package io.hamlook.aetheria.network;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMServerJoinEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.features.diana.party.ui.DPartyGUI;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class NetworkStatusNotifier {

    private static boolean pendingShow = false;
    private static boolean ackedThisLaunch = false;
    private static int tickCounter = 0;

    @HandleEvent
    public void onServerJoin(ASMServerJoinEvent event) {
        if (ATHRConfig.feature != null && !ackedThisLaunch && NetworkStatusInfo.shouldShow(ATHRConfig.feature.network.networkStatusAckMask)) {
            pendingShow = true;
        }
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null || ATHRConfig.feature == null) return;

        tickCounter++;
        if (tickCounter % 40 == 0) {
            if (ATHRConfig.feature.network.smartSocketLifecycle && WebSocketClient.isConnected && Aetheria.webSocketClient != null && System.currentTimeMillis() - WebSocketClient.lastActivityMs > WebSocketClient.IDLE_TIMEOUT_MS && !(MinecraftCompat.getCurrentScreen() instanceof ChatUI) && !(MinecraftCompat.getCurrentScreen() instanceof DPartyGUI)) {
                Aetheria.webSocketClient.close(1000, "Idle timeout");
            }
        }

        if (pendingShow) {
            if (MinecraftCompat.getCurrentScreen() == null && SkyblockData.isOnSkyblock()) {
                pendingShow = false;
                ackedThisLaunch = true;
                ATHRConfig.screenToOpen = new NetworkStatusScreen();
            }
        }
    }
}