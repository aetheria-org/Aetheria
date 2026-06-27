package io.hamlook.aetheria.features.mining.pristine;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class PristineTracker {

    private static final Pattern PRISTINE_GEMSTONE = Pattern.compile("PRISTINE!.*?(Flawed|Fine|Flawless) (Ruby|Sapphire|Amber|Amethyst|Jade|Topaz|Jasper|Opal|Citrine|Aquamarine|Peridot|Onyx) Gemstone.*?x(\\d+)");


    private static boolean isActive() {
        PristineStats stats = PristineStats.getInstance();
        return ATHRConfig.feature == null || !ATHRConfig.feature.mining.pristineTrackerConfig.pristineTracker || !stats.isTrackingEnabled() || SkyblockData.getCurrentLocation() != SkyblockData.Location.CRYSTAL_HOLLOWS;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || isActive()) return;

        PristineStats stats = PristineStats.getInstance();
        stats.timerTick();
        if (stats.shouldAutoStop()) stats.toggleTracking();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (isActive()) return;
        String msg = ChatUtils.clean(event);
        if (ChatUtils.isPartyMessage(msg) || ChatUtils.isPlayerMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg))
            return;

        if (!msg.contains("PRISTINE!")) return;

        Matcher m = PRISTINE_GEMSTONE.matcher(msg);
        if (m.find()) {
            PristineStats stats = PristineStats.getInstance();
            PristineData data = stats.getData();
            stats.updateActivity();
            String quality = m.group(1);
            String gem = m.group(2);
            long amount = parseLong(m.group(3));
            String key = quality + "_" + gem;
            data.gemstones.put(key, data.gemstones.getOrDefault(key, 0L) + amount);
            data.totalProcs++;
            data.lastPristineMs = System.currentTimeMillis();
            stats.save();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        PristineStats.getInstance().pauseTimer();
    }
}
