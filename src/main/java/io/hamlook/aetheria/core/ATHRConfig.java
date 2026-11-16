package io.hamlook.aetheria.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.api.event.AetheriaEventBus;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.config.ConfigListDefaults;
import io.hamlook.aetheria.core.config.ConfigMigrations;
import io.hamlook.aetheria.core.moulconfig.editors.GuiPositionEditor;
import io.hamlook.aetheria.core.moulconfig.gui.GuiScreenElementWrapper;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigEditor;
import io.hamlook.aetheria.events.ASMGuiOpenEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.features.chat.chatfilters.ui.ChatFilterGUI;
import io.hamlook.aetheria.features.chat.globalchat.ui.NotificationOverlay;
import io.hamlook.aetheria.features.diana.DianaStats;
import io.hamlook.aetheria.features.diana.GuiDianaOverlayEditor;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonBreakerOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.reward.RewardAnalyzerOverlay;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomOverlay;
import io.hamlook.aetheria.features.farming.BPSOverlay;
import io.hamlook.aetheria.features.farming.farmingtracker.FarmingTracker;
import io.hamlook.aetheria.features.farming.farmingtracker.FarmingTrackerOverlay;
import io.hamlook.aetheria.features.farming.gardenplots.GardenPlotWarpGrid;
import io.hamlook.aetheria.features.farming.organicmatter.OrganicMatterTracker;
import io.hamlook.aetheria.features.farming.organicmatter.OrganicMatterTrackerOverlay;
import io.hamlook.aetheria.features.farming.pests.PestStats;
import io.hamlook.aetheria.features.farming.pests.overlay.PestFinderOverlay;
import io.hamlook.aetheria.features.farming.pests.overlay.PestTrackerOverlay;
import io.hamlook.aetheria.features.farming.sensitivityreducer.PitchYawOverlay;
import io.hamlook.aetheria.features.fishing.trophy.TrophyFishOverlay;
import io.hamlook.aetheria.features.mining.fetchur.FetchurOverlay;
import io.hamlook.aetheria.features.mining.gold.GoldStats;
import io.hamlook.aetheria.features.mining.gold.GoldTrackerOverlay;
import io.hamlook.aetheria.features.mining.powder.PowderOverlay;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.features.mining.pristine.PristineOverlay;
import io.hamlook.aetheria.features.mining.pristine.PristineStats;
import io.hamlook.aetheria.features.misc.PerformanceHUD;
import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostOverlay;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostStats;
import io.hamlook.aetheria.features.misc.invbuttons.GuiInvButtonEditor;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.features.misc.killcombo.KillComboOverlay;
import io.hamlook.aetheria.features.misc.killcombo.KillComboTracker;
import io.hamlook.aetheria.features.misc.pet.CurrentPetOverlay;
import io.hamlook.aetheria.features.misc.timer.UptimeOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemAbilityTimerOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemCooldownOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemInvincibilityOverlay;
import io.hamlook.aetheria.features.qol.raredroptracker.RareDropTrackerGUI;
import io.hamlook.aetheria.features.qol.raredroptracker.RareDropTrackerOverlay;
import io.hamlook.aetheria.core.features.scoreboard.Scoreboard;
import io.hamlook.aetheria.features.scoreboard.CustomScoreboard;
import io.hamlook.aetheria.features.waypoints.WaypointGroupGui;
import io.hamlook.aetheria.network.PrivacyNoticeScreen;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.KeybindMigration;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.KeybindCompat;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
//#if FORGE
import net.minecraftforge.common.MinecraftForge;
//#endif
//#if MC < 1.16
import org.lwjgl.input.Keyboard;
//#else
//$$ import org.lwjgl.glfw.GLFW;
//#endif

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;

public class ATHRConfig {

    //#if MC < 1.16
    public static final KeyBinding openGuiKey = KeybindCompat.createKeyBinding("Open ATHR GUI", Keyboard.KEY_RMENU, "aetheria");
    //#else
    //$$ public static final KeyBinding openGuiKey = KeybindCompat.createKeyBinding("Open ATHR GUI", GLFW.GLFW_KEY_RIGHT_ALT, "aetheria");
    //#endif
    public static Config feature;
    public static File configDirectory = new File("config/Aetheria");
    public static GuiScreen screenToOpen = null;
    private static Config sharedConfigEditorConfig;
    private static ConfigEditor sharedConfigEditor;
    private static File configFile;
    private static int screenTicks = 0;
    private static boolean waypointManagerKeyWasDown = false;
    private static boolean rareDropTrackerGuiKeyWasDown = false;
    private static boolean powderToggleKeyWasDown = false;
    private static boolean pristineToggleKeyWasDown = false;
    private static boolean ghostToggleKeyWasDown = false;
    private static boolean ghostResetKeyWasDown = false;
    private static boolean globalChatOpenKeyWasDown = false;
    private static boolean registered = false;
    private static boolean configLoaded = false;
    private static boolean configDirty = false;
    private static long lastSaveRequestMs = 0L;
    static boolean previousSessionClean = true;
    private static boolean shutdownHookRegistered = false;
    private static boolean configRetriedOnce = false;
    private static long lastFlushAttemptMs = 0L;
    private static File cleanShutdownMarker;
    private static volatile boolean configWritable = true;
    private static volatile boolean blockedWriteLogged = false;
    private static boolean lastParseIncomplete = false;
    private static int lastParseReached = -1;
    private static File quarantinedThisLoad = null;
    private static String pendingConfigNotice = null;
    private static final long CONFIG_SAVE_DEBOUNCE_MS = 2000L;
    private static final long CONFIG_SAVE_MAX_LATENCY_MS = 10000L;

    private static boolean isKeyOrMouseDown(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) return false;
        if (keyCode < 0) return MouseCompat.isButtonDown(keyCode + 100);
        return KeyboardCompat.isKeyDown(keyCode);
    }

    public static void register() {
        if (registered) return;
        init();
        //#if FORGE
        MinecraftForge.EVENT_BUS.register(new ATHRConfig());
        //#endif
        AetheriaEventBus.INSTANCE.register(new ATHRConfig());
        KeybindCompat.registerKeyBinding(openGuiKey);
        registered = true;
    }

    public static void init() {
        if (configLoaded) return;
        if (!configDirectory.exists()) {
            File oldConfigFolder = new File("config/JustEnoughFakepixel");
            if (oldConfigFolder.exists()) {
                oldConfigFolder.renameTo(configDirectory);
            } else {
                configDirectory.mkdirs();
            }
        }
        configFile = new File(configDirectory, "config.json");
        cleanShutdownMarker = new File(configDirectory, ".clean_shutdown");
        previousSessionClean = !cleanShutdownMarker.exists();
        writeCleanShutdownMarker();
        loadConfig();
        configLoaded = true;
        registerShutdownHook();
    }

    private static void loadConfig() {
        configWritable = true;
        blockedWriteLogged = false;
        quarantinedThisLoad = null;
        lastParseIncomplete = false;
        lastParseReached = -1;
        pendingConfigNotice = null;

        boolean migrated = false;
        boolean fresh = false;
        boolean incomplete = false;
        int loadedVersion = -1;

        if (configFile.exists()) {
            String raw = StorageManager.loadSafeRaw(configFile, true);
            if (raw != null) {
                JsonObject original = parseObject(raw);
                if (original == null) {
                    Aetheria.logger.severe("[ATHR] config.json is not a JSON object; quarantining it");
                    quarantinedThisLoad = StorageManager.backupCorrupted(configFile);
                } else {
                    int from = ConfigMigrations.readVersion(original);
                    if (from < ConfigMigrations.CURRENT) {
                        backupBeforeMigration(from);
                    }
                    JsonObject migratedRoot = ConfigMigrations.apply(original);
                    loadedVersion = ConfigMigrations.readVersion(migratedRoot);
                    migrated = loadedVersion > from;
                    incomplete = ConfigMigrations.isIncomplete(from, loadedVersion);
                    feature = deserialize(migratedRoot);
                    if (feature == null && migratedRoot != original) {
                        Aetheria.logger.severe("[ATHR] migrated config did not deserialize; retrying the unmigrated tree with writes disabled");
                        feature = deserialize(original);
                        if (feature != null) {
                            incomplete = true;
                            loadedVersion = from;
                        }
                    }
                    if (feature == null) {
                        quarantinedThisLoad = StorageManager.backupCorrupted(configFile);
                    }
                }
            }
        }

        if (feature == null) {
            feature = tryRestoreFromCorruptedBackups(quarantinedThisLoad);
            if (feature != null) {
                migrated = true;
                incomplete = lastParseIncomplete;
                if (lastParseReached >= 0) loadedVersion = lastParseReached;
                System.err.println("[ATHR] Recovered config settings from a corrupted backup.");
            } else {
                fresh = true;
                feature = new Config();
                if (configFile.exists()) {
                    System.err.println("[ATHR] config.json failed to load and no usable .corrupted backup was found; using defaults.");
                    if (previousSessionClean) {
                        System.err.println("[ATHR] Previous session shut down cleanly — the corruption is NOT crash-related (possible write bug or external process).");
                    } else {
                        System.err.println("[ATHR] Previous session did NOT shut down cleanly (crash/BSOD/force-kill) — config.json last modified "
                                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(configFile.lastModified()))
                                + " was likely corrupted by an interrupted write.");
                    }
                }
            }
        }

        if (fresh) {
            feature.configVersion = ConfigMigrations.CURRENT;
            feature.scoreboard.scoreboardLinesSeen = Scoreboard.maxLineId();
        }

        int effectiveVersion = loadedVersion >= 0 ? loadedVersion : lastParseReached;

        if (incomplete) {
            configWritable = false;
            Aetheria.logger.severe("[ATHR] config migration stopped at v" + effectiveVersion
                    + " (target v" + ConfigMigrations.CURRENT
                    + "); config writes are disabled this session and config.json is unchanged");
            CrashLog.report(configFile, "loading", "config migration incomplete at v" + effectiveVersion);
            pendingConfigNotice = "§c[ATHR] Config migration failed at v" + effectiveVersion
                    + ". Settings are not being saved this session. Backup: config.json.pre-v" + effectiveVersion;
        }

        boolean newer = effectiveVersion > ConfigMigrations.CURRENT;
        boolean reconciled = false;
        if (!incomplete && !newer) {
            try {
                reconciled = ConfigListDefaults.apply(feature);
            } catch (Exception e) {
                Aetheria.logger.log(Level.SEVERE, "[ATHR] scoreboard line reconcile failed", e);
            }
        }
        boolean keybinds = false;
        if (!incomplete && !newer) {
            try {
                keybinds = KeybindMigration.migrateIfNeeded(feature);
            } catch (Exception e) {
                Aetheria.logger.log(Level.SEVERE, "[ATHR] keybind migration failed", e);
            }
        }
        if ((migrated && !newer) || reconciled || keybinds) {
            saveConfig();
        }
    }

    private static JsonObject parseObject(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Config deserialize(JsonObject root) {
        try {
            Config loaded = GsonBuilder.GSON_STRICT.fromJson(root, Config.class);
            if (loaded == null) throw new IllegalStateException("deserialized to null");
            return loaded;
        } catch (Exception e) {
            Aetheria.logger.log(Level.SEVERE, "[ATHR] config tree could not be deserialized", e);
            CrashLog.report(configFile, "loading", "deserialization failed: " + e.getMessage());
            return null;
        }
    }

    private static void backupBeforeMigration(int from) {
        File backup = new File(configDirectory, "config.json.pre-v" + from);
        try {
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] could not write pre-migration backup: " + e.getMessage());
        }
    }

    private static Config tryRestoreFromCorruptedBackups(File skip) {
        if (configFile == null) return null;
        File parent = configFile.getParentFile();
        if (parent == null) return null;
        File[] backups = parent.listFiles((dir, name) ->
                name.startsWith(configFile.getName() + ".") && name.endsWith(".corrupted"));
        if (backups == null || backups.length == 0) return null;
        Arrays.sort(backups, (a, b) -> b.getName().compareTo(a.getName()));
        for (File backup : backups) {
            if (skip != null && backup.getName().equals(skip.getName())) continue;
            try {
                String json = new String(Files.readAllBytes(backup.toPath()), StandardCharsets.UTF_8);
                Config restored = parseConfig(json);
                if (restored != null) {
                    Aetheria.logger.warning("[ATHR] Restoring config from " + backup.getName()
                            + " — settings newer than this backup are not present");
                    return restored;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Config parseConfig(String json) {
        lastParseIncomplete = false;
        lastParseReached = -1;
        JsonObject original = parseObject(json);
        if (original == null) return null;
        int from = ConfigMigrations.readVersion(original);
        JsonObject migratedRoot = ConfigMigrations.apply(original);
        int reached = ConfigMigrations.readVersion(migratedRoot);
        lastParseReached = reached;
        Config loaded = deserialize(migratedRoot);
        if (loaded != null) {
            lastParseIncomplete = ConfigMigrations.isIncomplete(from, reached);
            return loaded;
        }
        if (migratedRoot != original) {
            loaded = deserialize(original);
            if (loaded != null) {
                lastParseIncomplete = true;
                return loaded;
            }
        }
        return null;
    }

    public static boolean saveConfig() {
        if (feature == null || configFile == null) return false;
        if (!configWritable) {
            configDirty = false;
            if (!blockedWriteLogged) {
                blockedWriteLogged = true;
                Aetheria.logger.severe("[ATHR] config write blocked: a migration failed this session. "
                        + "config.json is unchanged; restore config.json.pre-v* only if you accept losing newer edits.");
            }
            return false;
        }
        boolean ok = StorageManager.saveAtomic(configFile, feature, GsonBuilder.GSON_STRICT);
        if (ok) configDirty = false;
        return ok;
    }

    public static void markConfigDirty() {
        if (!configWritable) return;
        configDirty = true;
        lastSaveRequestMs = System.currentTimeMillis();
    }

    private static void flushConfigIfDirty() {
        if (!configDirty || feature == null || configFile == null) return;
        long now = System.currentTimeMillis();
        boolean debounced = now - lastSaveRequestMs >= CONFIG_SAVE_DEBOUNCE_MS;
        boolean overdue = lastFlushAttemptMs != 0 && now - lastFlushAttemptMs >= CONFIG_SAVE_MAX_LATENCY_MS;
        if (!debounced && !overdue) return;
        lastFlushAttemptMs = now;
        if (saveConfig()) {
            configRetriedOnce = false;
        } else if (configRetriedOnce) {
            configDirty = false;
            configRetriedOnce = false;
        } else {
            configRetriedOnce = true;
            lastSaveRequestMs = now;
        }
    }

    private static void writeCleanShutdownMarker() {
        try {
            Files.write(cleanShutdownMarker.toPath(), new byte[]{'1'});
        } catch (Exception ignored) {}
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) return;
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (configDirty) {
                    saveConfig();
                }
            } catch (Exception ignored) {}
            try {
                StorageManager.saveAll();
            } catch (Exception ignored) {}
            try {
                Files.deleteIfExists(cleanShutdownMarker.toPath());
            } catch (Exception ignored) {}
        }, "ATHR-Config-Shutdown"));
    }

    public static void reloadRepo() {
        RepoHandler.refresh(ATHRRepo.KEY_TIMERS);
        RepoHandler.refresh(ATHRRepo.KEY_PLAYERSIZES);
        RepoHandler.refresh(ATHRRepo.KEY_UPDATE);
        RepoHandler.refresh(ATHRRepo.KEY_TAGS);
    }

    private static ConfigEditor getConfigEditor() {
        if (feature == null || !feature.debug.rememberConfigMenuState) {
            sharedConfigEditor = null;
            return new ConfigEditor(feature);
        }
        if (sharedConfigEditorConfig != feature) {
            sharedConfigEditor = null;
            sharedConfigEditorConfig = feature;
        }
        if (sharedConfigEditor == null) {
            sharedConfigEditor = new ConfigEditor(feature);
        }
        return sharedConfigEditor;
    }

    public static ConfigEditor primedConfigEditor() {
        ConfigEditor editor = getConfigEditor();
        editor.resetForOpen();
        return editor;
    }

    private static GuiScreen configScreen(String category, String search) {
        ConfigEditor editor = primedConfigEditor();
        if (category != null) editor.focusCategory(category);
        if (search != null) editor.focusSearch(search);
        return new GuiScreenElementWrapper(editor);
    }

    public static void openGui() {
        screenToOpen = configScreen(null, null);
    }

    public static void openCategory(String categoryName) {
        screenToOpen = configScreen(categoryName, null);
    }

    public static void openSearch(String search) {
        screenToOpen = configScreen(null, search);
    }

    public static void openSubcategory(String categoryName, String... subcategoryFieldPath) {
        ConfigEditor editor = primedConfigEditor();
        editor.focusCategory(categoryName);
        editor.openSubcategory(categoryName, subcategoryFieldPath);
        screenToOpen = new GuiScreenElementWrapper(editor);
    }

    public static void openWaypointGroupGui() {
        screenToOpen = new GuiScreenElementWrapper(new WaypointGroupGui());
    }

    public static void openRareDropTrackerGui() {
        screenToOpen = new GuiScreenElementWrapper(new RareDropTrackerGUI());
    }

    public static void openRareDropTrackerOverlayEditor() {
        if (feature == null) return;
        RareDropTrackerOverlay overlay = RareDropTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.rareDropTracker.overlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.rareDropTracker.overlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openNotificationsOverlayEditor() {
        if (feature == null) return;
        NotificationOverlay overlay = NotificationOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.network.globalChatConfig.notificationsPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openStatsEditor() {
        if (feature == null) return;
        DungeonStats stats = DungeonStats.getInstance();
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonOverlay.statsPos, stats::getOverlayWidth, stats::getOverlayHeight, () -> stats.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonOverlay.statsScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openDungeonRoomOverlayEditor() {
        if (feature == null) return;
        DungeonRoomOverlay overlay = DungeonRoomOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openDungeonAnalyzerOverlayEditor() {
        if (feature == null) return;
        RewardAnalyzerOverlay overlay = RewardAnalyzerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.priceEstimator.analyzerPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.priceEstimator.overlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openHudEditor() {
        if (feature == null) return;
        PerformanceHUD hud = PerformanceHUD.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.performanceHudConfig.hudPos, hud::getOverlayWidth, hud::getOverlayHeight, () -> hud.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.performanceHudConfig.hudScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openFetchurEditor() {
        if (feature == null) return;
        FetchurOverlay fetchur = FetchurOverlay.getInstance();
        screenToOpen = new GuiPositionEditor(feature.mining.fetchur.fetchurOverlayPos, fetchur::getOverlayWidth, fetchur::getOverlayHeight, () -> fetchur.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.fetchur.fetchurOverlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openDianaOverlayEditor() {
        if (feature == null) return;
        screenToOpen = new GuiDianaOverlayEditor(MinecraftCompat.getCurrentScreen(), ATHRConfig::markConfigDirty);
    }

    public static void openScoreboardEditor() {
        if (feature == null) return;
        CustomScoreboard sb = CustomScoreboard.getInstance();
        screenToOpen = new GuiPositionEditor(feature.scoreboard.position, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.scoreboard.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openSearchBarEditor() {
        if (feature == null) return;
        SearchBar sb = SearchBar.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.searchBarConfig.searchBarPos, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openCurrentPetEditor() {
        if (feature == null) return;
        CurrentPetOverlay overlay = CurrentPetOverlay.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.currentPet.currentPetPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.currentPet.currentPetScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openItemPickupLogEditor() {
        if (feature == null) return;
        ItemPickupLog overlay = ItemPickupLog.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.itemPickupLogConfig.itemPickupLogPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.itemPickupLogConfig.itemPickupLogScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openItemCooldownEditor() {
        if (feature == null) return;
        ItemCooldownOverlay overlay = ItemCooldownOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.itemCooldown.itemCooldownPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.itemCooldown.itemCooldownScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openItemAbilityTimerEditor() {
        if (feature == null) return;
        ItemAbilityTimerOverlay overlay = ItemAbilityTimerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.abilityTimer.itemAbilityTimerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.abilityTimer.itemAbilityTimerScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openItemInvincibilityEditor() {
        if (feature == null) return;
        ItemInvincibilityOverlay overlay = ItemInvincibilityOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.invincibility.itemInvincibilityPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.invincibility.itemInvincibilityScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openPowderEditor() {
        if (feature == null) return;
        PowderOverlay overlay = PowderOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.powderTrackerConfig.powderOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.powderTrackerConfig.powderOverlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openDungeonBreakerEditor() {
        if (feature == null) return;
        DungeonBreakerOverlay overlay = DungeonBreakerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonBreaker.dungeonBreakerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonBreaker.dungeonBreakerScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openDungeonMapEditor() {
        if (feature == null) return;
        DungeonMapOverlay overlay = DungeonMapOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonMapConfig.dungeonMapPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonMapConfig.appearance.scale).withParent(MinecraftCompat.getCurrentScreen());

    }

    public static void openInvButtonEditor() {
        screenToOpen = new GuiInvButtonEditor();
    }

    public static void openOptionsGui() {
        screenToOpen = new OptionsMenu();
    }

    public static void openTrophyFishEditor() {
        if (feature == null) return;
        TrophyFishOverlay overlay = TrophyFishOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.fishing.trophyFish.trophyFishPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.fishing.trophyFish.trophyFishScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openBpsEditor() {
        if (feature == null) return;
        BPSOverlay overlay = BPSOverlay.getInstance();
        assert overlay != null;
        screenToOpen = new GuiPositionEditor(feature.farming.bps.bpsPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.bps.bpsScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetPowderTracker() {
        PowderStats.getInstance().reset();
    }

    public static void openGoldEditor() {
        if (feature == null) return;
        GoldTrackerOverlay overlay = GoldTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.goldTracker.goldOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.goldTracker.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetGoldTracker() {
        GoldStats.getInstance().reset();
        GoldStats.getInstance().save();
    }

    public static void openFarmingTrackerEditor() {
        if (feature == null) return;
        FarmingTrackerOverlay overlay = FarmingTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.farmingTracker.farmingTrackerPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.farmingTracker.farmingTrackerScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetFarmingTracker() {
        FarmingTracker.reset();
    }

    public static void openOrganicMatterTrackerEditor() {
        if (feature == null) return;
        OrganicMatterTrackerOverlay overlay = OrganicMatterTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.organicMatterTracker.organicMatterTrackerPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.organicMatterTracker.organicMatterTrackerScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetOrganicMatterTracker() {
        OrganicMatterTracker.reset();
    }

    public static void openPestEditor() {
        if (feature == null) return;
        PestTrackerOverlay overlay = PestTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.pests.pestTracker.pestOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.pests.pestTracker.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openPestFinderEditor() {
        if (feature == null) return;
        PestFinderOverlay overlay = PestFinderOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.pests.pestFinder.pestFinderPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.pests.pestFinder.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openGardenPlotWarpGridEditor() {
        if (feature == null) return;
        GardenPlotWarpGrid grid = GardenPlotWarpGrid.getInstance();
        screenToOpen = new GuiPositionEditor(feature.farming.gardenPlotWarpGrid.pos, grid::getOverlayWidth, grid::getOverlayHeight, () -> grid.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetPestTracker() {
        PestStats.getInstance().reset();
    }

    public static void openVisitorOverlayEditor() {
        if (feature == null) return;
        io.hamlook.aetheria.features.farming.visitors.VisitorShoppingListOverlay overlay =
                io.hamlook.aetheria.features.farming.visitors.VisitorShoppingListOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.visitors.overlay.overlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.visitors.overlay.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openVisitorPanelEditor() {
        if (feature == null) return;
        io.hamlook.aetheria.features.farming.visitors.VisitorPanel panel =
                io.hamlook.aetheria.features.farming.visitors.VisitorPanel.getInstance();
        if (panel == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.visitors.panel.panelPos, panel::getLastWidth, panel::getLastHeight, panel::renderPreview, ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.visitors.panel.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetVisitorList() {
        io.hamlook.aetheria.features.farming.FarmingApi.clearVisitorData();
    }

    public static void openPristineEditor() {
        if (feature == null) return;
        PristineOverlay overlay = PristineOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.pristineTrackerConfig.pristineOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.pristineTrackerConfig.pristineOverlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetPristineTracker() {
        PristineStats.getInstance().reset();
    }

    public static void openKillComboEditor() {
        if (feature == null) return;
        KillComboOverlay overlay = KillComboOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.killCombo.killComboPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.killCombo.scale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openUptimeEditor() {
        if (feature == null) return;
        UptimeOverlay overlay = UptimeOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.uptimeConfig.uptimePos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.uptimeConfig.uptimeScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openPeltTrackerEditor() {
        if (feature == null) return;
        io.hamlook.aetheria.features.farming.trevor.PeltOverlay overlay = io.hamlook.aetheria.features.farming.trevor.PeltOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.trevor.peltTrackerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetPeltTracker() {
        io.hamlook.aetheria.features.farming.trevor.PeltOverlay.reset();
    }

    public static void openPitchYawEditor() {
        if (feature == null) return;
        PitchYawOverlay overlay = PitchYawOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.sensitivityReducer.pitchYawOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.sensitivityReducer.pitchYawOverlayScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void openGhostEditor() {
        if (feature == null) return;
        GhostOverlay overlay = GhostOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.ghostTrackerConfig.ghostOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.ghostTrackerConfig.ghostScale).withParent(MinecraftCompat.getCurrentScreen());
    }

    public static void resetGhostTracker() {
        GhostStats.getInstance().reset();
        io.hamlook.aetheria.features.misc.ghosttracker.GhostStats.getInstance().reset();
    }

    public static void resetKillCombo() {
        KillComboTracker.getInstance().reset();
    }

    public static void resetDianaTracker() {
        DianaStats.getInstance().reset();
    }

    public static void resetItemAnimations() {
        if (feature == null) return;
        feature.cosmetics.itemAnimations.reset();
        saveConfig();
    }

    public static void openChatFilterUI() {
        if (feature == null) return;
        screenToOpen = new ChatFilterGUI();
    }

    public static void openPrivacyNotice() {
        if (feature == null) return;
        MinecraftCompat.getMinecraft().displayGuiScreen(new PrivacyNoticeScreen(MinecraftCompat.getCurrentScreen()));
    }

    @HandleEvent
    public void onGuiOpen(ASMGuiOpenEvent event) {
        if (!(event.gui instanceof GuiMainMenu)) return;

        if (!ATHRConfig.feature.network.hasSeenPrivacyNotice) {
            event.gui = new PrivacyNoticeScreen(event.gui);
            return;
        }

        if (!ATHRConfig.feature.network.hasSeenSocketLifecycleNotice) {
            event.gui = new PrivacyNoticeScreen(event.gui, true);
        }
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        if (pendingConfigNotice != null && MinecraftCompat.getLocalPlayer() != null) {
            ChatUtils.sendMessage(pendingConfigNotice);
            pendingConfigNotice = null;
        }
        flushConfigIfDirty();

        if (screenToOpen != null) {
            screenTicks++;
            if (screenTicks == 5) {
                MinecraftCompat.getMinecraft().displayGuiScreen(screenToOpen);
                screenTicks = 0;
                screenToOpen = null;
            }
        }

        if (MinecraftCompat.getLocalPlayer() == null) return;
        if (KeybindCompat.wasPressed(openGuiKey) && MinecraftCompat.getCurrentScreen() == null) openOptionsGui();

        boolean managerKeyDown = feature != null && isKeyOrMouseDown(feature.waypoints.waypointManagerKey);
        if (managerKeyDown && !waypointManagerKeyWasDown && MinecraftCompat.getCurrentScreen() == null)
            openWaypointGroupGui();
        waypointManagerKeyWasDown = managerKeyDown;

        boolean rdtKeyDown = feature != null && isKeyOrMouseDown(feature.qol.rareDropTracker.trackerGuiKey);
        if (rdtKeyDown && !rareDropTrackerGuiKeyWasDown && MinecraftCompat.getCurrentScreen() == null)
            openRareDropTrackerGui();
        rareDropTrackerGuiKeyWasDown = rdtKeyDown;

        if (feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey) && !powderToggleKeyWasDown && MinecraftCompat.getCurrentScreen() == null) {
            PowderStats.getInstance().toggleTracking();
        }

        powderToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.mining.pristineTrackerConfig.pristineToggleKey) && !pristineToggleKeyWasDown && MinecraftCompat.getCurrentScreen() == null) {
            PristineStats.getInstance().toggleTracking();
        }

        pristineToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.pristineTrackerConfig.pristineToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostToggleKey) && !ghostToggleKeyWasDown && MinecraftCompat.getCurrentScreen() == null) {
            GhostStats.getInstance().toggleTracking();
        }
        ghostToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostResetKey) && !ghostResetKeyWasDown && MinecraftCompat.getCurrentScreen() == null) {
            resetGhostTracker();
        }
        ghostResetKeyWasDown = feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostResetKey);

        boolean globalChatOpenKeyDown = feature != null && isKeyOrMouseDown(feature.network.globalChatConfig.openChatKey);
        if (globalChatOpenKeyDown && !globalChatOpenKeyWasDown && MinecraftCompat.getCurrentScreen() == null) {
            io.hamlook.aetheria.features.chat.globalchat.GChatCommand.tryOpen();
        }
        globalChatOpenKeyWasDown = globalChatOpenKeyDown;
    }
}