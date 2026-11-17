package io.hamlook.aetheria.features.mining;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.mining.BalConfig;
import io.hamlook.aetheria.events.*;
import io.hamlook.aetheria.features.mining.bal.BalAlertOverlay;
import io.hamlook.aetheria.features.misc.PerformanceHUD;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RegisterEvents
public class MiningApi {

    private static final Set<String> ALLOWED_SOUND_NAMES = new HashSet<>(Arrays.asList(
        "dig.glass", "dig.stone", "dig.gravel", "dig.cloth", "random.orb"));
    private static final long CLICK_EXPIRE_MS = 10000;
    private static final long MINED_EXPIRE_MS = 5000;
    private static final long INIT_SOUND_TIMEOUT_MS = 200;
    private static final long CLICK_WINDOW_MS = 1000;
    private static final double MAX_BREAK_DISTANCE = 7.0;
    private static final long BAL_DEATH_DELAY_MS = 5000L;
    private static final long BAL_WARN_30_MS = 30000L;
    private static final long BAL_WARN_10_MS = 10000L;
    private static final String BAL_DEATH_TEXT = "The boss looks weak and tired and retreats into the lava";
    private static final String BAL_SPAWN_TEXT = "The ground starts to shake and the lava bubbles";

    private static final ConcurrentHashMap<BlockPos, Long> recentClickedBlocks = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<MinedBlock> surroundingMinedBlocks = new ConcurrentLinkedQueue<>();

    private static BlockPos lastClickedPos = null;
    private static long lastClicked = 0;
    private static BlockPos lastLoggedClickPos = null;

    private static long balDeathAtMs = 0;
    private static long balRespawnAtMs = 0;
    private static boolean balAlertFired = false;
    private static boolean balWarn30Fired = false;
    private static boolean balWarn10Fired = false;

    private static long lastInitSound = 0;
    private static BlockPos initBlockPos = null;
    private static boolean waitingForInitSound = true;
    private static boolean waitingForEffMinerSound = false;
    private static boolean waitingForEffMinerBlock = false;
    private static boolean ignoreInit = false;

    private static final long RATE_LIMIT_MS = 2000;
    private static final Map<String, Long> spamCooldown = new ConcurrentHashMap<>();

    @Getter
    private static Set<OreBlock> currentAreaOreBlocks = EnumSet.noneOf(OreBlock.class);

    private static class MinedBlock {
        final OreBlock ore;
        boolean confirmed;
        final long time;

        MinedBlock(OreBlock ore, boolean confirmed) {
            this.ore = ore;
            this.confirmed = confirmed;
            this.time = System.currentTimeMillis();
        }
    }

    @HandleEvent
    public void onBlockClick(BlockClickEvent event) {
        if (!isMiningArea()) { debugLog("BlockClick: not mining area"); return; }
        if (OreBlock.getByStateOrNull(event.getBlockState()) == null) { debugLogRateLimited("bc:" + event.pos, "BlockClick: unknown block at " + event.pos); return; }
        long now = System.currentTimeMillis();
        recentClickedBlocks.put(event.pos, now);
        lastClickedPos = event.pos;
        lastClicked = now;
        if (!event.pos.equals(lastLoggedClickPos)) {
            debugLog("BlockClick at " + event.pos);
            lastLoggedClickPos = event.pos;
        }
    }

    @HandleEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (!isMiningArea()) return;

        if (ATHRConfig.feature != null && ATHRConfig.feature.debug.enableDebug && ALLOWED_SOUND_NAMES.contains(event.soundName)) {
            String msg = buildSoundDebugMessage(event);
            if ("random.orb".equals(event.soundName)) {
                debugLogRateLimited("orb:sound", msg);
            } else {
                debugLog(msg);
            }
        }

        if (!ALLOWED_SOUND_NAMES.contains(event.soundName)) return;

        if (waitingForInitSound) {
            if (!"random.orb".equals(event.soundName)) {
                BlockPos pos = event.getBlockPos();
                if (!recentClickedBlocks.containsKey(pos)) { debugLog("Init reject dig: pos " + pos + " not in recentClicked"); return; }
                waitingForInitSound = false;
                waitingForEffMinerBlock = true;
                initBlockPos = pos;
                lastInitSound = System.currentTimeMillis();
                debugLog("Init sound (dig) at " + pos + " pitch=" + event.pitch);
            } else {
                if (System.currentTimeMillis() - lastClicked > CLICK_WINDOW_MS) { debugLogRateLimited("orb:window", "Init reject orb: outside click window (" + (System.currentTimeMillis() - lastClicked) + "ms > " + CLICK_WINDOW_MS + ")"); return; }
                if (lastClickedPos == null) { debugLogRateLimited("orb:null", "Init reject orb: lastClickedPos is null"); return; }
                IBlockState state = MinecraftCompat.getLocalWorld().getBlockState(lastClickedPos);
                OreBlock ore = OreBlock.getByStateOrNull(state);
                if (ore == null) { debugLogRateLimited("orb:unknown:" + lastClickedPos, "Init reject orb: unknown ore at " + lastClickedPos + " state=" + state); return; }
                if (ore.hasInitSound) { debugLogRateLimited("orb:hasInit:" + ore.name(), "Init reject orb: " + ore.name() + " hasInitSound=true"); return; }
                ignoreInit = true;
                waitingForInitSound = false;
                waitingForEffMinerBlock = true;
                lastInitSound = System.currentTimeMillis();
                debugLog("Init sound (orb) at " + lastClickedPos + " ore=" + ore.name());
            }
        }

        if (waitingForEffMinerSound) {
            MinedBlock last = null;
            for (MinedBlock b : surroundingMinedBlocks) {
                last = b;
            }
            if (last != null && !last.confirmed) {
                waitingForEffMinerSound = false;
                last.confirmed = true;
                waitingForEffMinerBlock = true;
                debugLog("EffMiner confirmed " + last.ore.name());
            }
        }
    }

    @HandleEvent
    public void onBlockChange(ServerBlockChangeEvent event) {
        if (!isMiningArea()) { debugLog("S22: not mining area"); return; }

        IBlockState oldState = event.getOldState();
        IBlockState newState = event.newState;
        Block oldBlock = oldState.getBlock();
        Block newBlock = newState.getBlock();

        if (oldState.equals(newState)) return;
        if (BlockCompat.isAir(oldBlock) || oldBlock == Blocks.bedrock) { debugLogRateLimited("s22:ab:" + event.pos, "S22: old block is air/bedrock at " + event.pos); return; }
        if (!BlockCompat.isAir(newBlock) && newBlock != Blocks.bedrock && !OreBlock.isTitaniumBlock(newState)) { debugLogRateLimited("s22:nt:" + event.pos, "S22: new block not air/titanium at " + event.pos); return; }

        BlockPos pos = event.pos;

        EntityPlayer player = MinecraftCompat.getLocalPlayer();
        if (player == null) return;
        double dist = EntityCompat.getDistanceToPos(player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (dist > MAX_BREAK_DISTANCE) { debugLogRateLimited("s22:far:" + pos, "S22: too far (" + String.format("%.1f", dist) + ") at " + pos); return; }

        if (System.currentTimeMillis() - lastInitSound > getBlockChangeWindow()) { debugLogRateLimited("s22:win:" + pos, "S22: outside window at " + pos); return; }

        OreBlock ore = OreBlock.getByStateOrNull(oldState);
        if (ore == null) { debugLogRateLimited("s22:unk:" + pos, "S22: unknown ore at " + pos); return; }

        if (pos.equals(initBlockPos)) {
            surroundingMinedBlocks.add(new MinedBlock(ore, true));
            debugLog("Confirmed block " + ore.name() + " at " + pos);
            runEvent();
            return;
        }

        if (waitingForEffMinerBlock && (!ignoreInit || !ore.hasInitSound)) {
            waitingForEffMinerBlock = false;
            surroundingMinedBlocks.add(new MinedBlock(ore, false));
            waitingForEffMinerSound = true;
            debugLog("EffMiner block " + ore.name() + " at " + pos);
        }
    }

    private static void runEvent() {
        boolean ignoreFilter = ignoreInit;
        resetOreEvent();

        if (surroundingMinedBlocks.isEmpty()) return;

        MinedBlock original = null;
        for (MinedBlock b : surroundingMinedBlocks) {
            if (b.confirmed) {
                original = b;
                break;
            }
        }
        if (original == null) {
            surroundingMinedBlocks.clear();
            recentClickedBlocks.clear();
            return;
        }

        Map<OreBlock, Integer> extraBlocks = new HashMap<>();
        for (MinedBlock b : surroundingMinedBlocks) {
            if (ignoreFilter ? b.ore == original.ore : b.confirmed) {
                extraBlocks.merge(b.ore, 1, Integer::sum);
            }
        }

        new OreMinedEvent(original.ore, extraBlocks).post();
        debugLog("OreMinedEvent fired: " + original.ore.name()
            + (extraBlocks.size() > 1 ? " extras=" + extraBlocks : ""));

        surroundingMinedBlocks.clear();
        lastClickedPos = null;
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;

        long now = System.currentTimeMillis();
        tickBal(now);

        if (!isMiningArea()) return;

        recentClickedBlocks.entrySet().removeIf(e -> now - e.getValue() > CLICK_EXPIRE_MS);
        surroundingMinedBlocks.removeIf(b -> now - b.time > MINED_EXPIRE_MS);
        cleanupSpamCooldown(now);

        if (!waitingForInitSound && now - lastInitSound > INIT_SOUND_TIMEOUT_MS) {
            if (ignoreInit) {
                runEvent();
            } else {
                debugLog("Init timeout (ignoreInit=false) -- reset");
                resetOreEvent();
            }
        }

        updateCurrentAreaOreBlocks();
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        recentClickedBlocks.clear();
        surroundingMinedBlocks.clear();
        lastClickedPos = null;
        lastLoggedClickPos = null;
        resetOreEvent();
        clearBalTimer();
        currentAreaOreBlocks = EnumSet.noneOf(OreBlock.class);
        debugLog("State reset on world unload");
    }

    @HandleEvent
    public void onDebugReport(DebugReportEvent event) {
        event.title("MiningApi");
        event.addData("Area: " + (isMiningArea() ? SkyblockData.getCurrentLocation() : "none"));
        event.addData("recentClickedBlocks: " + recentClickedBlocks.size());
        event.addData("surroundingMinedBlocks: " + surroundingMinedBlocks.size());
        event.addData("waitingForInitSound: " + waitingForInitSound);
        event.addData("waitingForEffMinerBlock: " + waitingForEffMinerBlock);
        event.addData("waitingForEffMinerSound: " + waitingForEffMinerSound);
        event.addData("ignoreInit: " + ignoreInit);
        event.addData("lastInitSound: " + (System.currentTimeMillis() - lastInitSound) + "ms ago");
        event.addData("currentAreaOreBlocks: " + currentAreaOreBlocks.size());
        event.addData("Bal respawn: " + (hasBalTimer() ? formatBalClock(getBalRemainingMs()) : "none"));
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        BalConfig cfg = balConfig();
        if (cfg == null || !cfg.enabled) return;
        if (!ChatUtils.isFromServer(event)) return;
        if (SkyblockData.getCurrentLocation() != SkyblockData.Location.CRYSTAL_HOLLOWS) return;
        if (event.parsed() != null) return;

        String message = event.plain();

        if (message.contains(BAL_DEATH_TEXT)) {
            balDeathAtMs = System.currentTimeMillis() + BAL_DEATH_DELAY_MS;
            return;
        }
        if (cfg.clearOnSpawn && message.contains(BAL_SPAWN_TEXT)) {
            clearBalTimer();
        }
    }

    private static void tickBal(long now) {
        BalConfig cfg = balConfig();
        if (cfg == null || !cfg.enabled) return;
        if (MinecraftCompat.getLocalWorld() == null || MinecraftCompat.getLocalPlayer() == null) return;
        if (SkyblockData.getCurrentLocation() != SkyblockData.Location.CRYSTAL_HOLLOWS) return;

        if (balDeathAtMs != 0) {
            if (now < balDeathAtMs) return;
            balDeathAtMs = 0;
            balRespawnAtMs = now + cfg.respawnMinutes * 60_000L;
            balAlertFired = false;
            balWarn30Fired = false;
            balWarn10Fired = false;
            ChatUtils.sendMessage("§b[ASM] §eBal defeated! §7Respawns in §f" + formatBalClock(balRespawnAtMs - now));
            return;
        }

        if (balRespawnAtMs == 0) return;

        long remaining = balRespawnAtMs - now;
        if (remaining <= 0) {
            clearBalTimer();
            return;
        }

        if (!balAlertFired && remaining <= cfg.alertBeforeSeconds * 1000L) {
            balAlertFired = true;
            BalAlertOverlay.trigger("§c§lBAL RESPAWN! §r§f" + formatBalClock(remaining), cfg);
            if (cfg.chatMessage) {
                ChatUtils.sendMessage("§b[ASM] §eBal respawns in §f" + formatBalClock(remaining) + "§e!");
            }
        }
        if (!balWarn30Fired && remaining <= BAL_WARN_30_MS) {
            balWarn30Fired = true;
            ChatUtils.sendMessage("§b[ASM] §eBal respawns in §c30s§e!");
            BalAlertOverlay.playAlertSound(cfg.alertSound);
        }
        if (!balWarn10Fired && remaining <= BAL_WARN_10_MS) {
            balWarn10Fired = true;
            ChatUtils.sendMessage("§b[ASM] §eBal respawns in §c10s§e!");
            BalAlertOverlay.playAlertSound(cfg.alertSound);
        }
    }

    public static boolean hasBalTimer() {
        return balRespawnAtMs != 0;
    }

    public static long getBalRemainingMs() {
        if (balRespawnAtMs == 0) return 0;
        return Math.max(0L, balRespawnAtMs - System.currentTimeMillis());
    }

    public static String formatBalClock(long ms) {
        long totalSeconds = Math.max(0L, ms) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private static void clearBalTimer() {
        balDeathAtMs = 0;
        balRespawnAtMs = 0;
        balAlertFired = false;
        balWarn30Fired = false;
        balWarn10Fired = false;
    }

    private static BalConfig balConfig() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.mining.bal;
    }

    private static void resetOreEvent() {
        lastInitSound = 0;
        waitingForInitSound = true;
        ignoreInit = false;
        initBlockPos = null;
        waitingForEffMinerSound = false;
        waitingForEffMinerBlock = false;
    }

    private static void updateCurrentAreaOreBlocks() {
        if (!isMiningArea()) return;
        Set<OreBlock> updated = EnumSet.noneOf(OreBlock.class);
        for (OreBlock ore : OreBlock.values()) {
            if (ore.checkArea.get()) updated.add(ore);
        }
        currentAreaOreBlocks = updated;
    }

    public static boolean isMiningArea() {
        SkyblockData.Location loc = SkyblockData.getCurrentLocation();
        return loc == SkyblockData.Location.DWARVEN
            || loc == SkyblockData.Location.CRYSTAL_HOLLOWS
            || loc == SkyblockData.Location.CRIMSON_ISLE
            || loc == SkyblockData.Location.THE_END
            || loc == SkyblockData.Location.SPIDERS_DEN;
    }

    private static long getBlockChangeWindow() {
        double ping = PerformanceHUD.getPingMs();
        if (ping < 0) return 1000;
        return Math.max(200, (long) (ping * 2) + 100);
    }

    private static String buildSoundDebugMessage(PlaySoundEvent event) {
        BlockPos bp = event.getBlockPos();
        EntityPlayer player = MinecraftCompat.getLocalPlayer();
        double dist = player != null
            ? EntityCompat.getDistanceToPos(player, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5)
            : -1;
        return "[MiningApi/Sound] " + event.soundName
            + " pitch=" + event.pitch
            + " vol=" + event.volume
            + " pos=" + bp
            + " dist=" + String.format("%.1f", dist)
            + (waitingForInitSound ? " [WAITING_INIT]" : "")
            + (waitingForEffMinerBlock ? " [WAITING_EFF_BLOCK]" : "")
            + (waitingForEffMinerSound ? " [WAITING_EFF_SOUND]" : "");
    }

    private static void debugLog(String msg) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.debug.enableDebug) {
            Aetheria.logger.info("[MiningApi] " + msg);
        }
    }

    private static void debugLogRateLimited(String key, String msg) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.debug.enableDebug) return;
        long now = System.currentTimeMillis();
        Long last = spamCooldown.get(key);
        if (last != null && now - last < RATE_LIMIT_MS) return;
        spamCooldown.put(key, now);
        Aetheria.logger.info("[MiningApi] " + msg);
    }

    private static void cleanupSpamCooldown(long now) {
        if (spamCooldown.size() > 200) {
            spamCooldown.entrySet().removeIf(e -> now - e.getValue() > RATE_LIMIT_MS * 3);
        }
    }
}