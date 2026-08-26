package io.hamlook.aetheria.features.mining.gold;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.OreMinedEvent;
import io.hamlook.aetheria.features.mining.OreBlock;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class GoldTracker {

    private static final Pattern COMPACT_PATTERN = Pattern.compile("§b§lCOMPACT!.*§aEnchanted Gold");
    private static final long MINING_ACTIVITY_WINDOW_MS = 20_000L;

    private static boolean listenerRegistered = false;
    private static long lastGoldMinedMs = 0;

    private static void ensureListenerRegistered() {
        if (listenerRegistered) return;
        ItemPickupLog log = ItemPickupLog.getInstance();
        if (log != null) {
            log.addRichItemChangeListener(GoldTracker::onItemChange);
            listenerRegistered = true;
        }
    }

    private static boolean isActive() {
        return ATHRConfig.feature != null && ATHRConfig.feature.mining.goldTracker.enabled && GoldStats.isGoldArea();
    }

    private static void onItemChange(String internalId, String displayName, int delta) {
        if (!isActive()) return;
        if (delta <= 0) return;
        if (!isTrackingId(internalId)) return;
        if (ATHRConfig.feature.mining.goldTracker.onlyTrackWhileMining && !isMiningGold()) return;

        GoldStats stats = GoldStats.getInstance();
        stats.updateActivity();

        if ("GOLD_INGOT".equals(internalId)) {
            stats.getData().ingotCount += delta;
        } else if ("ENCHANTED_GOLD".equals(internalId)) {
            stats.getData().enchantedCount += delta;
        }

        stats.save();
    }

    private static boolean isTrackingId(String id) {
        if (ATHRConfig.feature == null) return false;
        String mode = ATHRConfig.feature.mining.goldTracker.trackMode;
        return "Track Ingot".equals(mode) ? "GOLD_INGOT".equals(id) : "ENCHANTED_GOLD".equals(id);
    }

    public static boolean isMiningGold() {
        return System.currentTimeMillis() - lastGoldMinedMs < MINING_ACTIVITY_WINDOW_MS;
    }

    @SubscribeEvent
    public void onOreMined(OreMinedEvent event) {
        if (!isActive()) return;
        if (event.originalOre == OreBlock.GOLD_ORE || event.originalOre == OreBlock.PURE_GOLD) {
            lastGoldMinedMs = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isActive()) return;

        String raw = event.message.getFormattedText();

        Matcher m = COMPACT_PATTERN.matcher(raw);
        if (m.find()) {
            if (ATHRConfig.feature.mining.goldTracker.onlyTrackWhileMining && !isMiningGold()) return;

            GoldStats stats = GoldStats.getInstance();
            stats.updateActivity();
            stats.getData().compactCount++;
            stats.save();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isActive()) return;

        ensureListenerRegistered();
        GoldStats.getInstance().timerTick();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        lastGoldMinedMs = 0;
        GoldStats.getInstance().pauseTimer();
    }
}
