package io.hamlook.aetheria.core;

import io.hamlook.aetheria.command.Command;
import io.hamlook.aetheria.core.moulconfig.editors.GuiPositionEditor;
import io.hamlook.aetheria.core.moulconfig.gui.GuiScreenElementWrapper;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigEditor;
import io.hamlook.aetheria.features.chat.chatfilters.ui.ChatFilterGUI;
import io.hamlook.aetheria.features.chat.globalchat.ui.NotificationOverlay;
import io.hamlook.aetheria.features.diana.DianaStats;
import io.hamlook.aetheria.features.diana.GuiDianaOverlayEditor;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonBreakerOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.reward.RewardAnalyzerOverlay;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomOverlay;
import io.hamlook.aetheria.features.events.EventNotifierOverlay;
import io.hamlook.aetheria.features.farming.BPSOverlay;
import io.hamlook.aetheria.features.farming.farmingtracker.FarmingTracker;
import io.hamlook.aetheria.features.farming.farmingtracker.FarmingTrackerOverlay;
import io.hamlook.aetheria.features.farming.organicmatter.OrganicMatterTracker;
import io.hamlook.aetheria.features.farming.organicmatter.OrganicMatterTrackerOverlay;
import io.hamlook.aetheria.features.farming.pests.PestStats;
import io.hamlook.aetheria.features.farming.pests.overlay.PestFinderOverlay;
import io.hamlook.aetheria.features.farming.pests.overlay.PestTrackerOverlay;
import io.hamlook.aetheria.features.fishing.trophy.TrophyFishOverlay;
import io.hamlook.aetheria.features.mining.fetchur.FetchurOverlay;
import io.hamlook.aetheria.features.mining.powder.PowderOverlay;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.features.misc.PerformanceHUD;
import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.features.misc.killcombo.KillComboOverlay;
import io.hamlook.aetheria.features.misc.killcombo.KillComboTracker;
import io.hamlook.aetheria.features.misc.pet.CurrentPetOverlay;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostOverlay;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostStats;
import io.hamlook.aetheria.features.misc.timer.UptimeOverlay;
import io.hamlook.aetheria.features.farming.sensitivityreducer.PitchYawOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemAbilityTimerOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemCooldownOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemInvincibilityOverlay;
import io.hamlook.aetheria.features.scoreboard.CustomScoreboard;
import io.hamlook.aetheria.features.waypoints.WaypointGroupGui;
import io.hamlook.aetheria.features.qol.raredroptracker.RareDropTrackerGUI;
import io.hamlook.aetheria.features.qol.raredroptracker.RareDropTrackerOverlay;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.features.mining.pristine.PristineOverlay;
import io.hamlook.aetheria.features.mining.pristine.PristineStats;
import io.hamlook.aetheria.features.misc.invbuttons.GuiInvButtonEditor;
import io.hamlook.aetheria.network.PrivacyNoticeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ATHRConfig {

    public static final KeyBinding openGuiKey = new KeyBinding("Open ATHR GUI", Keyboard.KEY_RMENU, "aetheria");
    public static Config feature;
    public static File configDirectory = new File("config/Aetheria");
    public static GuiScreen screenToOpen = null;
    private static File configFile;
    private static int screenTicks = 0;
    private static boolean waypointManagerKeyWasDown = false;
    private static boolean rareDropTrackerGuiKeyWasDown = false;
    private static boolean powderToggleKeyWasDown = false;
    private static boolean pristineToggleKeyWasDown = false;
    private static boolean ghostToggleKeyWasDown = false;
    private static boolean ghostResetKeyWasDown = false;
    private static boolean registered = false;
    private static boolean configLoaded = false;
    private static boolean configDirty = false;
    private static long lastSaveRequestMs = 0L;
    static boolean previousSessionClean = true;
    private static boolean shutdownHookRegistered = false;
    private static boolean configRetriedOnce = false;
    private static long lastFlushAttemptMs = 0L;
    private static File cleanShutdownMarker;
    private static final long CONFIG_SAVE_DEBOUNCE_MS = 2000L;
    private static final long CONFIG_SAVE_MAX_LATENCY_MS = 10000L;

    private static boolean isKeyOrMouseDown(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) return false;
        if (keyCode < 0) return Mouse.isButtonDown(keyCode + 100);
        return Keyboard.isKeyDown(keyCode);
    }

    public static void register() {
        if (registered) return;
        init();
        MinecraftForge.EVENT_BUS.register(new ATHRConfig());
        ClientRegistry.registerKeyBinding(openGuiKey);
        ClientCommandHandler.instance.registerCommand(new Command());
        registered = true;
    }

    public static void init() {
        if (configLoaded) return;
        if (!configDirectory.exists()){
            File oldConfigFolder = new File("config/JustEnoughFakepixel");
            if(oldConfigFolder.exists()){
                oldConfigFolder.renameTo(configDirectory);
            }else {
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
        if (configFile.exists()) {
            feature = StorageManager.loadSafe(configFile, Config.class, GsonBuilder.GSON_STRICT);
        }
        if (feature == null) {
            feature = tryRestoreFromCorruptedBackups();
            if (feature == null) {
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
            } else {
                System.err.println("[ATHR] Recovered config settings from a corrupted backup.");
            }
        }
    }

    private static Config tryRestoreFromCorruptedBackups() {
        if (configFile == null) return null;
        File parent = configFile.getParentFile();
        if (parent == null) return null;
        File[] backups = parent.listFiles((dir, name) ->
                name.startsWith(configFile.getName() + ".") && name.endsWith(".corrupted"));
        if (backups == null || backups.length == 0) return null;
        Arrays.sort(backups, (a, b) -> b.getName().compareTo(a.getName()));
        for (File backup : backups) {
            try (Reader r = new BufferedReader(new InputStreamReader(Files.newInputStream(backup.toPath()), StandardCharsets.UTF_8))) {
                Config restored = GsonBuilder.GSON_STRICT.fromJson(r, Config.class);
                if (restored != null) {
                    System.err.println("[ATHR] Restoring config from " + backup.getName());
                    return restored;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static boolean saveConfig() {
        if (feature == null || configFile == null) return false;
        boolean ok = StorageManager.saveAtomic(configFile, feature, GsonBuilder.GSON_STRICT);
        if (ok) configDirty = false;
        return ok;
    }

    public static void markConfigDirty() {
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

    public static void openGui() {
        screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(feature));
    }
    public static void openCategory(String categoryName) {
        screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(feature, categoryName));
    }

    public static void openSearch(String search) {
        screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(feature, null, search));
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
        screenToOpen = new GuiPositionEditor(feature.qol.rareDropTracker.overlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.rareDropTracker.overlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openNotificationsOverlayEditor() {
        if (feature == null) return;
        NotificationOverlay overlay = NotificationOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.network.globalChatConfig.notificationsPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openStatsEditor() {
        if (feature == null) return;
        DungeonStats stats = DungeonStats.getInstance();
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonOverlay.statsPos, stats::getOverlayWidth, stats::getOverlayHeight, () -> stats.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonOverlay.statsScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonRoomOverlayEditor() {
        if (feature == null) return;
        DungeonRoomOverlay overlay = DungeonRoomOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonAnalyzerOverlayEditor() {
        if (feature == null) return;
        RewardAnalyzerOverlay overlay = RewardAnalyzerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.priceEstimator.analyzerPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.priceEstimator.overlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openHudEditor() {
        if (feature == null) return;
        PerformanceHUD hud = PerformanceHUD.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.performanceHudConfig.hudPos, hud::getOverlayWidth, hud::getOverlayHeight, () -> hud.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.performanceHudConfig.hudScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openFetchurEditor() {
        if (feature == null) return;
        FetchurOverlay fetchur = FetchurOverlay.getInstance();
        screenToOpen = new GuiPositionEditor(feature.mining.fetchur.fetchurOverlayPos, fetchur::getOverlayWidth, fetchur::getOverlayHeight, () -> fetchur.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.fetchur.fetchurOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDianaOverlayEditor() {
        if (feature == null) return;
        screenToOpen = new GuiDianaOverlayEditor(Minecraft.getMinecraft().currentScreen, ATHRConfig::markConfigDirty);
    }

    public static void openScoreboardEditor() {
        if (feature == null) return;
        CustomScoreboard sb = CustomScoreboard.getInstance();
        screenToOpen = new GuiPositionEditor(feature.scoreboard.position, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.scoreboard.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openSearchBarEditor() {
        if (feature == null) return;
        SearchBar sb = SearchBar.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.searchBarConfig.searchBarPos, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openCurrentPetEditor() {
        if (feature == null) return;
        CurrentPetOverlay overlay = CurrentPetOverlay.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.currentPet.currentPetPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.currentPet.currentPetScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemPickupLogEditor() {
        if (feature == null) return;
        ItemPickupLog overlay = ItemPickupLog.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.itemPickupLogConfig.itemPickupLogPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.itemPickupLogConfig.itemPickupLogScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemCooldownEditor() {
        if (feature == null) return;
        ItemCooldownOverlay overlay = ItemCooldownOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.itemCooldown.itemCooldownPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.itemCooldown.itemCooldownScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemAbilityTimerEditor() {
        if (feature == null) return;
        ItemAbilityTimerOverlay overlay = ItemAbilityTimerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.abilityTimer.itemAbilityTimerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.abilityTimer.itemAbilityTimerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemInvincibilityEditor() {
        if (feature == null) return;
        ItemInvincibilityOverlay overlay = ItemInvincibilityOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.invincibility.itemInvincibilityPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.qol.invincibility.itemInvincibilityScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openPowderEditor() {
        if (feature == null) return;
        PowderOverlay overlay = PowderOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.powderTrackerConfig.powderOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.powderTrackerConfig.powderOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonBreakerEditor() {
        if (feature == null) return;
        DungeonBreakerOverlay overlay = DungeonBreakerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonBreaker.dungeonBreakerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonBreaker.dungeonBreakerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonMapEditor(){
        if (feature == null) return;
        DungeonMapOverlay overlay = DungeonMapOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonMapConfig.dungeonMapPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonMapConfig.appearance.scale).withParent(Minecraft.getMinecraft().currentScreen);

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
        screenToOpen = new GuiPositionEditor(feature.fishing.trophyFish.trophyFishPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.fishing.trophyFish.trophyFishScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openBpsEditor() {
        if (feature == null) return;
        BPSOverlay overlay = BPSOverlay.getInstance();
        assert overlay != null;
        screenToOpen = new GuiPositionEditor(feature.farming.bps.bpsPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.bps.bpsScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPowderTracker() {
        PowderStats.getInstance().reset();
    }

    public static void openFarmingTrackerEditor() {
        if (feature == null) return;
        FarmingTrackerOverlay overlay = FarmingTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.farmingTracker.farmingTrackerPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.farmingTracker.farmingTrackerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetFarmingTracker() {
        FarmingTracker.reset();
    }

    public static void openOrganicMatterTrackerEditor() {
        if (feature == null) return;
        OrganicMatterTrackerOverlay overlay = OrganicMatterTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.organicMatterTracker.organicMatterTrackerPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.organicMatterTracker.organicMatterTrackerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetOrganicMatterTracker() {
        OrganicMatterTracker.reset();
    }

    public static void openPestEditor() {
        if (feature == null) return;
        PestTrackerOverlay overlay = PestTrackerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.pests.pestTracker.pestOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.pests.pestTracker.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openPestFinderEditor() {
        if (feature == null) return;
        PestFinderOverlay overlay = PestFinderOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.pests.pestFinder.pestFinderPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.pests.pestFinder.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPestTracker() {
        PestStats.getInstance().reset();
    }

    public static void openVisitorOverlayEditor() {
        if (feature == null) return;
        io.hamlook.aetheria.features.farming.visitors.VisitorShoppingListOverlay overlay =
                io.hamlook.aetheria.features.farming.visitors.VisitorShoppingListOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.visitors.overlay.overlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.visitors.overlay.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openVisitorPanelEditor() {
        if (feature == null) return;
        io.hamlook.aetheria.features.farming.visitors.VisitorPanel panel =
                io.hamlook.aetheria.features.farming.visitors.VisitorPanel.getInstance();
        if (panel == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.visitors.panel.panelPos, panel::getLastWidth, panel::getLastHeight, panel::renderPreview, ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.visitors.panel.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetVisitorList() {
        io.hamlook.aetheria.features.farming.FarmingApi.clearVisitorData();
    }

    public static void openPristineEditor() {
        if (feature == null) return;
        PristineOverlay overlay = PristineOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.pristineTrackerConfig.pristineOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.mining.pristineTrackerConfig.pristineOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPristineTracker() {
        PristineStats.getInstance().reset();
    }


    public static void openKillComboEditor() {
        if (feature == null) return;
        KillComboOverlay overlay = KillComboOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.killCombo.killComboPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.killCombo.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openUptimeEditor() {
        if (feature == null) return;
        UptimeOverlay overlay = UptimeOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.uptimeConfig.uptimePos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.uptimeConfig.uptimeScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openPeltTrackerEditor() {
        if (feature == null) return;
        io.hamlook.aetheria.features.farming.trevor.PeltOverlay overlay = io.hamlook.aetheria.features.farming.trevor.PeltOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.trevor.peltTrackerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPeltTracker() {
        io.hamlook.aetheria.features.farming.trevor.PeltOverlay.reset();
    }

    public static void openPitchYawEditor() {
        if (feature == null) return;
        PitchYawOverlay overlay = PitchYawOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.farming.sensitivityReducer.pitchYawOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.farming.sensitivityReducer.pitchYawOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openGhostEditor() {
        if (feature == null) return;
        GhostOverlay overlay = GhostOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.ghostTrackerConfig.ghostOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.misc.ghostTrackerConfig.ghostScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openEventNotifierEditor() {
        if (feature == null) return;
        EventNotifierOverlay overlay = EventNotifierOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.eventNotification.overlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::markConfigDirty, ATHRConfig::saveConfig).withOverlayScale(feature.eventNotification.overlayScale).withParent(Minecraft.getMinecraft().currentScreen);
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

    public static void openChatFilterUI() {
        if (feature == null) return;
        screenToOpen = new ChatFilterGUI();
    }

    public static void openPrivacyNotice() {
        if (feature == null) return;
        Minecraft.getMinecraft().displayGuiScreen(new PrivacyNoticeScreen(Minecraft.getMinecraft().currentScreen));
    }



    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiMainMenu)) return;

        if (!ATHRConfig.feature.network.hasSeenPrivacyNotice) {
            event.gui = new PrivacyNoticeScreen(event.gui);
            return;
        }

        if (!ATHRConfig.feature.network.hasSeenSocketLifecycleNotice) {
            event.gui = new PrivacyNoticeScreen(event.gui, true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        flushConfigIfDirty();
        if (Minecraft.getMinecraft().thePlayer == null) return;

        if (screenToOpen != null) {
            screenTicks++;
            if (screenTicks == 5) {
                Minecraft.getMinecraft().displayGuiScreen(screenToOpen);
                screenTicks = 0;
                screenToOpen = null;
            }
        }

        if (openGuiKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) openOptionsGui();

        boolean managerKeyDown = feature != null && isKeyOrMouseDown(feature.waypoints.waypointManagerKey);
        if (managerKeyDown && !waypointManagerKeyWasDown && Minecraft.getMinecraft().currentScreen == null)
            openWaypointGroupGui();
        waypointManagerKeyWasDown = managerKeyDown;

        boolean rdtKeyDown = feature != null && isKeyOrMouseDown(feature.qol.rareDropTracker.trackerGuiKey);
        if (rdtKeyDown && !rareDropTrackerGuiKeyWasDown && Minecraft.getMinecraft().currentScreen == null)
            openRareDropTrackerGui();
        rareDropTrackerGuiKeyWasDown = rdtKeyDown;

        if (feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey) && !powderToggleKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            PowderStats.getInstance().toggleTracking();
        }

        powderToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.mining.pristineTrackerConfig.pristineToggleKey) && !pristineToggleKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            PristineStats.getInstance().toggleTracking();
        }

        pristineToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.pristineTrackerConfig.pristineToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostToggleKey) && !ghostToggleKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            GhostStats.getInstance().toggleTracking();
        }
        ghostToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostResetKey) && !ghostResetKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            resetGhostTracker();
        }
        ghostResetKeyWasDown = feature != null && isKeyOrMouseDown(feature.misc.ghostTrackerConfig.ghostResetKey);
    }
}