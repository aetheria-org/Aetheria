package io.hamlook.aetheria.features.diana;

import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class DianaTracker {

    private static final Pattern BORROW_DIG = Pattern.compile("You dug out a Griffin Borrow! \\(([1-4])/4\\)");
    private static final Pattern MOB_SPAWN = Pattern.compile("Uh oh! You dug out (.+)");
    private static final Pattern RARE_STICK = Pattern.compile("RARE DROP! Daedalus Stick");
    private static final Pattern RARE_RELIC = Pattern.compile("RARE DROP! Minos Relic");
    private static final Pattern RARE_CHIMERA = Pattern.compile("RARE DROP! Chimera [IVX]+");
    private static final Pattern RARE_SHELMET = Pattern.compile("RARE DROP! Dwarf Turtle Shelmet");
    private static final Pattern RARE_REMEDIES = Pattern.compile("RARE DROP! Antique Remedies");
    private static final Pattern RARE_PLUSHIE = Pattern.compile("RARE DROP! Crochet Tiger Plushie");
    private static final Pattern DROP_FEATHER = Pattern.compile("RARE DROP! You dug out a Griffin Feather");
    private static final Pattern DROP_SOUVENIR = Pattern.compile("RARE DROP! You dug out a Washed-up Souvenir");
    private static final Pattern DROP_CROWN = Pattern.compile("RARE DROP! You dug out a Crown of Greed");
    private static final Pattern DROP_COINS = Pattern.compile("RARE DROP! You dug out ([\\d,]+) Coins");
    private static final Pattern GRIFFIN_DOUBLED = Pattern.compile("Your Griffin doubled your rewards?!");
    private static final Pattern LOOT_SHARE = Pattern.compile("^LOOT SHARE You received loot for assisting");
    private final Minecraft mc = Minecraft.getMinecraft();
    private volatile boolean pendingDouble = false;

    private static boolean isInHub() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.HUB;
    }

    public static String getBorrowsMessage() {
        DianaStats s = DianaStats.getInstance();
        DianaData d = s.getData();
        return String.format("B:%d BPH:%.1f PT:%s Sess:%s", d.totalBorrows, s.getBph(), DianaStats.formatTime(d.activeTimeMs), DianaStats.formatTime(s.getSessionTimeMs()));
    }

    public static String getInqMessage() {
        DianaStats s = DianaStats.getInstance();
        DianaData d = s.getData();
        double pct = s.getInqChance();
        return String.format("Inqs:%d(%s) SL:%d LS:%d", d.totalInqs, pct >= 0 ? String.format("%.2f%%", pct) : "?", d.mobsSinceInq, d.totalInqsLootshared);
    }

    public static String getMobsMessage() {
        DianaStats s = DianaStats.getInstance();
        DianaData d = s.getData();
        return String.format("Mobs:%d Inq:%d Mino:%d Champ:%d Gaia:%d Hunter:%d Lynx:%d", d.totalMobs, d.totalInqs, d.totalMinotaurs, d.totalChamps, d.totalGaiaConstructs, d.totalMinosHunters, d.totalSiameseLynxes);
    }

    public static String getChimMessage() {
        DianaData d = DianaStats.getInstance().getData();
        return String.format("Chim:%d ISL:%d LS:%d", d.totalChimeras, d.inqsSinceChimera, d.totalInqsLootshared);
    }

    public static String getStickMessage() {
        DianaData d = DianaStats.getInstance().getData();
        return String.format("Sticks:%d MSL:%d", d.totalSticks, d.minotaursSinceStick);
    }

    public static String getRelicMessage() {
        DianaData d = DianaStats.getInstance().getData();
        return String.format("Relics:%d CSL:%d", d.totalRelics, d.champsSinceRelic);
    }

    public static String getLootMessage() {
        DianaData d = DianaStats.getInstance().getData();
        return String.format("F:%d Sh:%d Re:%d Pl:%d St:%d Rl:%d Ch:%d So:%d Cr:%d G:%s", d.griffinFeathers, d.dwarfTurtleShelmets, d.antiqueRemedies, d.crochetTigerPlushies, d.totalSticks, d.totalRelics, d.totalChimeras, d.souvenirs, d.crownsOfGreed, Utils.shortNumberFormat(d.totalCoins, 0));
    }

    public static String getTimeMessage() {
        DianaStats s = DianaStats.getInstance();
        return String.format("Sess:%s Total:%s", DianaStats.formatTime(s.getSessionTimeMs()), DianaStats.formatTime(s.getData().activeTimeMs));
    }

    public static String getHelpMessage() {
        return "§6[Diana] §e!burrows !inq !mobs !chim !stick !relic !loot !time\n" + "§7SL=since last ISL=inqs since last MSL=minos since last CSL=champs since last";
    }

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        DianaStats.getInstance().onClientLogin();
        pendingDouble = false;
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        DianaStats.getInstance().onClientLogout();
        pendingDouble = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DianaStats.getInstance().timerTick();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (mc.thePlayer == null) return;

        String msg = ChatUtils.clean(event);

        if (ChatUtils.isPartyMessage(msg) || ChatUtils.isPlayerMessage(msg) || ChatUtils.isMsgReceived(msg) || ChatUtils.isMsgSent(msg) || ChatUtils.isDonateMessage(msg)) {
            return;
        }
        DianaStats stats = DianaStats.getInstance();

        if (stats.isTracking() && stats.isDianaMayor()) {
            if (LOOT_SHARE.matcher(msg).find()) {
                stats.onLootshare();
                if (LootshareDetect.wasInqKilledByOther()) {
                    stats.getData().totalInqsLootshared++;
                    LootshareDetect.clearInqDisappear();
                }
            }
            handleBorrowDrops(msg, stats);
            handleRareMobDrops(msg, stats);
            handleMobSpawnHP(msg);
            handleBorrowDig(msg, stats);
            handleMobSpawn(msg, stats);
        }
    }

    private void handleBorrowDig(String msg, DianaStats stats) {
        if (!BORROW_DIG.matcher(msg).find()) return;
        stats.updateActivity();
        stats.getData().totalBorrows++;
        stats.save();
    }

    private void handleMobSpawnHP(String msg) {
        Matcher m = MOB_SPAWN.matcher(msg);
        if (!m.find()) return;
        switch (m.group(1).trim()) {
            case "Minotaur":
            case "Minos Champion":
            case "Minos Hunter":
            case "Gaia Construct":
            case "Siamese Lynxes":
                LootshareDetect.onNonInqMobDug();
        }
    }

    private void handleMobSpawn(String msg, DianaStats stats) {
        Matcher m = MOB_SPAWN.matcher(msg);
        if (!m.find()) return;

        stats.updateActivity();
        DianaData d = stats.getData();
        d.totalMobs++;

        switch (m.group(1).trim()) {
            case "Minos Inquisitor":
                d.mobsSinceInq = 0;
                d.inqsSinceChimera++;
                d.totalInqs++;
                break;
            case "Minotaur":
                d.mobsSinceInq++;
                d.minotaursSinceStick++;
                d.totalMinotaurs++;
                LootshareDetect.onNonInqMobDug();
                break;
            case "Minos Champion":
                d.mobsSinceInq++;
                d.champsSinceRelic++;
                d.totalChamps++;
                LootshareDetect.onNonInqMobDug();
                break;
            case "Gaia Construct":
                d.mobsSinceInq++;
                d.totalGaiaConstructs++;
                LootshareDetect.onNonInqMobDug();
                break;
            case "Minos Hunter":
                d.mobsSinceInq++;
                d.totalMinosHunters++;
                LootshareDetect.onNonInqMobDug();
                break;
            case "Siamese Lynxes":
                d.mobsSinceInq++;
                d.totalSiameseLynxes++;
                LootshareDetect.onNonInqMobDug();
                break;
            default:
                d.mobsSinceInq++;
        }
        stats.save();
    }

    private void handleRareMobDrops(String msg, DianaStats stats) {
        DianaData d = stats.getData();
        boolean changed = false;
        if (RARE_STICK.matcher(msg).find()) {
            stats.updateActivity();
            d.minotaursSinceStick = 0;
            d.totalSticks++;
            changed = true;
        }
        if (RARE_RELIC.matcher(msg).find()) {
            stats.updateActivity();
            d.champsSinceRelic = 0;
            d.totalRelics++;
            changed = true;
        }
        if (RARE_CHIMERA.matcher(msg).find()) {
            stats.updateActivity();
            d.inqsSinceChimera = 0;
            d.totalChimeras++;
            changed = true;
        }
        if (RARE_SHELMET.matcher(msg).find()) {
            stats.updateActivity();
            d.dwarfTurtleShelmets++;
            changed = true;
        }
        if (RARE_REMEDIES.matcher(msg).find()) {
            stats.updateActivity();
            d.antiqueRemedies++;
            changed = true;
        }
        if (RARE_PLUSHIE.matcher(msg).find()) {
            stats.updateActivity();
            d.crochetTigerPlushies++;
            changed = true;
        }
        if (changed) stats.save();
    }

    private void handleBorrowDrops(String msg, DianaStats stats) {
        DianaData d = stats.getData();

        if (GRIFFIN_DOUBLED.matcher(msg).find()) {
            stats.updateActivity();
            if (stats.lastDropType != null) applyDoubledReward(stats);
            else pendingDouble = true;
            return;
        }

        if (DROP_FEATHER.matcher(msg).find()) {
            stats.updateActivity();
            d.griffinFeathers++;
            recordDrop(stats, "feather", 1L);
            if (pendingDouble) {
                d.griffinFeathers++;
                consumePending(stats);
            }
            stats.save();
        } else if (DROP_SOUVENIR.matcher(msg).find()) {
            stats.updateActivity();
            d.souvenirs++;
            recordDrop(stats, "souvenir", 1L);
            if (pendingDouble) {
                d.souvenirs++;
                consumePending(stats);
            }
            stats.save();
        } else if (DROP_CROWN.matcher(msg).find()) {
            stats.updateActivity();
            d.crownsOfGreed++;
            recordDrop(stats, "crown", 1L);
            if (pendingDouble) {
                d.crownsOfGreed++;
                consumePending(stats);
            }
            stats.save();
        } else {
            Matcher coins = DROP_COINS.matcher(msg);
            if (coins.find()) {
                stats.updateActivity();
                long amount = parseLong(coins.group(1));
                d.totalCoins += amount;
                recordDrop(stats, "coins", amount);
                if (pendingDouble) {
                    d.totalCoins += amount;
                    consumePending(stats);
                }
                stats.save();
            }
        }
    }

    private void recordDrop(DianaStats stats, String type, long amount) {
        stats.lastDropType = type;
        stats.lastDropAmount = amount;
        stats.lastDropMs = System.currentTimeMillis();
    }

    private void consumePending(DianaStats stats) {
        pendingDouble = false;
        stats.lastDropType = null;
    }

    private void applyDoubledReward(DianaStats stats) {
        DianaData d = stats.getData();
        switch (stats.lastDropType) {
            case "feather":
                d.griffinFeathers++;
                break;
            case "souvenir":
                d.souvenirs++;
                break;
            case "crown":
                d.crownsOfGreed++;
                break;
            case "coins":
                d.totalCoins += stats.lastDropAmount;
                break;
        }
        stats.lastDropType = null;
        stats.save();
    }

    private long parseLong(String s) {
        try {
            return Long.parseLong(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}