package io.hamlook.aetheria.features.mining.powder;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class PowderTracker {

    private static final Pattern CHEST_UNCOVERED = Pattern.compile("You uncovered a treasure chest!");
    private static final Pattern COMPACT = Pattern.compile("COMPACT! You found an? Enchanted Hard Stone!");
    private static final Pattern GEMSTONE_POWDER = Pattern.compile("Gemstone Powder x([\\d,]+)");
    private static final Pattern GEMSTONE_DROP = Pattern.compile("\\S (Rough|Flawed|Fine|Flawless) " + "(Ruby|Sapphire|Amber|Amethyst|Jade|Topaz|Jasper|Opal|Citrine|Aquamarine|Peridot|Onyx) " + "Gemstone x([\\d,]+)");
    private static final Pattern DIAMOND_ESSENCE = Pattern.compile("Diamond Essence x([\\d,]+)");
    private static final Pattern GOLD_ESSENCE = Pattern.compile("Gold Essence x([\\d,]+)");
    private static final Pattern OIL_BARREL = Pattern.compile("Oil Barrel x([\\d,]+)");
    private static final Pattern ASCENSION_ROPE = Pattern.compile("Ascension Rope x([\\d,]+)");
    private static final Pattern WISHING_COMPASS = Pattern.compile("Wishing Compass x([\\d,]+)");
    private static final Pattern JUNGLE_HEART = Pattern.compile("Jungle Heart x([\\d,]+)");
    private static final Pattern GOBLIN_EGG = Pattern.compile("§9Goblin Egg §r§8x([\\d,]+)");
    private static final Pattern GREEN_GOBLIN_EGG = Pattern.compile("§aGreen Goblin Egg §r§8x([\\d,]+)");
    private static final Pattern RED_GOBLIN_EGG = Pattern.compile("§cRed Goblin Egg §r§8x([\\d,]+)");
    private static final Pattern YELLOW_GOBLIN_EGG = Pattern.compile("§eYellow Goblin Egg §r§8x([\\d,]+)");
    private static final Pattern BLUE_GOBLIN_EGG = Pattern.compile("§3Blue Goblin Egg §r§8x([\\d,]+)");

    private static final long SYNC_WINDOW_MS = 2000;

    private static boolean listenerRegistered = false;

    private static long pendingGemstoneDelta = 0;
    private static long pendingDeltaTime = 0;
    private static long lastGemstoneChatTime = 0;

    private static void ensureListenerRegistered() {
        if (listenerRegistered) return;

        ItemPickupLog itemLog = ItemPickupLog.getInstance();
        if (itemLog != null) {
            itemLog.addItemChangeListener(PowderTracker::onItemChange);
        }

        TablistParser.setGemstonePowderChangeListener(PowderTracker::onGemstonePowderChange);

        listenerRegistered = true;
    }

    private static void onGemstonePowderChange(long oldValue, long newValue) {
        if (newValue <= oldValue) return;

        long delta = newValue - oldValue;
        long now = System.currentTimeMillis();

        if (now - lastGemstoneChatTime <= SYNC_WINDOW_MS) {
            if (isActive()) return;
            PowderStats stats = PowderStats.getInstance();
            stats.updateActivity();
            stats.getData().gemstonePowder += delta;
            stats.save();
            lastGemstoneChatTime = 0;
        } else {
            pendingGemstoneDelta = delta;
            pendingDeltaTime = now;
        }
    }

    private static void onItemChange(String displayName, int delta) {
        if (isActive()) return;
        if (!displayName.contains("§aEnchanted Hard Stone")) return;
        if (delta <= 0) return;

        PowderStats stats = PowderStats.getInstance();
        stats.updateActivity();
        stats.getData().hardStone += delta;
        stats.save();
    }

    public static boolean isDoublePowder() {
        return TablistParser.isEventActive("2x Powder");
    }

    public static String getDoublePowderTimeLeft() {
        if (!isDoublePowder()) return null;
        return TablistParser.getActiveEventTimeLeft();
    }

    public static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.mining.powderTrackerConfig.powderTracker && PowderStats.getInstance().isTrackingEnabled();
    }

    private static boolean isActive() {
        return !isEnabled() || SkyblockData.getCurrentLocation() != SkyblockData.Location.CRYSTAL_HOLLOWS;
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
        if (event.phase != TickEvent.Phase.END) return;
        if (isActive()) return;

        ensureListenerRegistered();

        PowderStats.getInstance().timerTick();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (isActive()) return;

        String msg = ChatUtils.clean(event);

        if (ChatUtils.isPartyMessage(msg) || ChatUtils.isPlayerMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg))
            return;

        PowderStats stats = PowderStats.getInstance();
        PowderData data = stats.getData();

        if (CHEST_UNCOVERED.matcher(msg).find()) {
            stats.updateActivity();
            data.totalChestsPicked++;
            stats.save();
            return;
        }

        if (COMPACT.matcher(msg).find()) {
            stats.updateActivity();
            data.hardStoneCompacted++;
            stats.save();
            return;
        }

        Matcher m;

        m = GEMSTONE_POWDER.matcher(msg);
        if (m.find()) {
            long now = System.currentTimeMillis();

            if (pendingGemstoneDelta > 0 && now - pendingDeltaTime <= SYNC_WINDOW_MS) {
                stats.updateActivity();
                data.gemstonePowder += pendingGemstoneDelta;
                stats.save();
                pendingGemstoneDelta = 0;
                pendingDeltaTime = 0;
            } else {
                lastGemstoneChatTime = now;
            }
            return;
        }

        m = GEMSTONE_DROP.matcher(msg);
        if (m.find() && !msg.contains("PRISTINE")) {
            stats.updateActivity();
            String key = PowderStats.gemKey(m.group(1), m.group(2));
            data.gemstones.put(key, data.gemstones.getOrDefault(key, 0L) + parseLong(m.group(3)));
            stats.save();
            return;
        }

        m = DIAMOND_ESSENCE.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.diamondEssence += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = GOLD_ESSENCE.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.goldEssence += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = OIL_BARREL.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.oilBarrels += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = ASCENSION_ROPE.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.ascensionRopes += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = WISHING_COMPASS.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.wishingCompasses += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = JUNGLE_HEART.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.jungleHearts += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = GOBLIN_EGG.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.goblinEgg += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = GREEN_GOBLIN_EGG.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.greenGoblinEgg += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = RED_GOBLIN_EGG.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.redGoblinEgg += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = YELLOW_GOBLIN_EGG.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.yellowGoblinEgg += parseLong(m.group(1));
            stats.save();
            return;
        }

        m = BLUE_GOBLIN_EGG.matcher(msg);
        if (m.find()) {
            stats.updateActivity();
            data.blueGoblinEgg += parseLong(m.group(1));
            stats.save();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        pendingGemstoneDelta = 0;
        pendingDeltaTime = 0;
        lastGemstoneChatTime = 0;

        PowderStats.getInstance().pauseTimer();
    }
}