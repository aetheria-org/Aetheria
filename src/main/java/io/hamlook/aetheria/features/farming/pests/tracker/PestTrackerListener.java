package io.hamlook.aetheria.features.farming.pests.tracker;

import io.hamlook.aetheria.features.farming.data.Crop;
import io.hamlook.aetheria.features.farming.pests.PestStats;
import io.hamlook.aetheria.features.farming.data.PestType;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class PestTrackerListener {

    private static final Pattern PEST_KILL = Pattern.compile("You received (\\d+)x (.+?) for killing (?:a|an) (.+?)!");
    private final Minecraft mc = Minecraft.getMinecraft();

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        PestStats.getInstance().onClientLogin();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        PestStats.getInstance().onClientLogout();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PestStats.getInstance().timerTick();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        PestStats.getInstance().onWorldUnload();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        PestStats stats = PestStats.getInstance();
        if (!stats.isTracking()) return;
        if (!ChatUtils.isFromServer(event)) return;
        if (mc.thePlayer == null) return;

        String msg = ColorUtils.stripColor(ChatUtils.clean(event));
        if (ChatUtils.isPlayerChat(msg)) return;

        Matcher m = PEST_KILL.matcher(msg);
        if (!m.find()) return;

        long qty = parseLong(m.group(1));
        String dropName = m.group(2).trim();
        Crop crop = Crop.findByDropName(dropName);
        String itemId = crop != null ? Crop.findByChatName(dropName) : null;
        if (itemId == null && crop != null) itemId = crop.rawId;
        PestType type = PestType.fromChatName(m.group(3).trim());
        if (type == null) return;
        stats.recordPestKill(type, qty, itemId);
    }
}