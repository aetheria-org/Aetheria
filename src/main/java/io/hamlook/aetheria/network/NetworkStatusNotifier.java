package io.hamlook.aetheria.network;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.WebSocketClient;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.features.diana.party.ui.DPartyGUI;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@RegisterEvents
public class NetworkStatusNotifier {

    private static boolean pendingShow = false;
    private static boolean ackedThisLaunch = false;
    private static int tickCounter = 0;

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (ATHRConfig.feature != null && !ackedThisLaunch && NetworkStatusInfo.shouldShow(ATHRConfig.feature.network.networkStatusAckMask)) {
            pendingShow = true;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || ATHRConfig.feature == null) return;

        tickCounter++;
        if (tickCounter % 40 == 0) {
            if (ATHRConfig.feature.network.smartSocketLifecycle && WebSocketClient.isConnected && Aetheria.webSocketClient != null && System.currentTimeMillis() - WebSocketClient.lastActivityMs > WebSocketClient.IDLE_TIMEOUT_MS && !(mc.currentScreen instanceof ChatUI) && !(mc.currentScreen instanceof DPartyGUI)) {
                Aetheria.webSocketClient.close(1000, "Idle timeout");
            }
        }

        if (pendingShow) {
            if (mc.currentScreen == null && SkyblockData.isOnSkyblock()) {
                pendingShow = false;
                ackedThisLaunch = true;
                ATHRConfig.screenToOpen = new NetworkStatusScreen();
            }
        }
    }
}