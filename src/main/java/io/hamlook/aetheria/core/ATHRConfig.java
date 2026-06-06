package io.hamlook.aetheria.core;

import io.hamlook.aetheria.command.Command;
import io.hamlook.aetheria.core.moulconfig.editors.GuiPositionEditor;
import io.hamlook.aetheria.core.moulconfig.gui.GuiScreenElementWrapper;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigEditor;
import io.hamlook.aetheria.features.chat.chatfilters.ui.ChatFilterGUI;
import io.hamlook.aetheria.features.diana.GuiDianaOverlayEditor;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonBreakerOverlay;
import io.hamlook.aetheria.features.dungeons.overlays.map.DungeonMapOverlay;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomOverlay;
import io.hamlook.aetheria.features.farming.BPSOverlay;
import io.hamlook.aetheria.features.fishing.trophy.TrophyFishOverlay;
import io.hamlook.aetheria.features.mining.fetchur.FetchurOverlay;
import io.hamlook.aetheria.features.mining.powder.PowderOverlay;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.features.misc.ItemPickupLog;
import io.hamlook.aetheria.features.misc.PerformanceHUD;
import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.features.misc.pet.CurrentPetOverlay;
import io.hamlook.aetheria.features.misc.timer.UptimeOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemAbilityTimerOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemCooldownOverlay;
import io.hamlook.aetheria.features.qol.overlays.ItemInvincibilityOverlay;
import io.hamlook.aetheria.features.scoreboard.CustomScoreboard;
import io.hamlook.aetheria.features.waypoints.WaypointGroupGui;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.features.mining.pristine.PristineOverlay;
import io.hamlook.aetheria.features.mining.pristine.PristineStats;
import io.hamlook.aetheria.features.misc.invbuttons.GuiInvButtonEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;

public class ATHRConfig {

    public static final KeyBinding openGuiKey = new KeyBinding("Open ATHR GUI", Keyboard.KEY_RMENU, "aetheria");
    public static Config feature;
    public static File configDirectory = new File("config/Aetheria");
    public static GuiScreen screenToOpen = null;
    private static File configFile;
    private static int screenTicks = 0;
    private static boolean waypointManagerKeyWasDown = false;
    private static boolean powderToggleKeyWasDown = false;
    private static boolean pristineToggleKeyWasDown = false;
    private static boolean registered = false;

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
        if (!configDirectory.exists()){
            File oldConfigFolder = new File("config/JustEnoughFakepixel");
            if(oldConfigFolder.exists()){
                oldConfigFolder.renameTo(configDirectory);
            }else {
                configDirectory.mkdirs();
            }
        }
        configFile = new File(configDirectory, "config.json");
        loadConfig();
    }

    private static void loadConfig() {
        if (configFile.exists()) {
            // Uses shared loadSafe for consistent corruption handling
            feature = StorageManager.loadSafe(configFile, Config.class, GsonBuilder.GSON_STRICT);
        }
        if (feature == null) {
            feature = new Config();
            saveConfig();
        }
    }

    public static void saveConfig() {
        // Uses shared saveAtomic — .tmp → verify → atomic rename, same as every other storage
        StorageManager.saveAtomic(configFile, feature, GsonBuilder.GSON_STRICT);
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

    public static void openWaypointGroupGui() {
        screenToOpen = new GuiScreenElementWrapper(new WaypointGroupGui());
    }

    public static void openStatsEditor() {
        if (feature == null) return;
        DungeonStats stats = DungeonStats.getInstance();
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonOverlay.statsPos, stats::getOverlayWidth, stats::getOverlayHeight, () -> stats.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonOverlay.statsScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonRoomOverlayEditor() {
        if (feature == null) return;
        DungeonRoomOverlay overlay = DungeonRoomOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openHudEditor() {
        if (feature == null) return;
        PerformanceHUD hud = PerformanceHUD.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.performanceHudConfig.hudPos, hud::getOverlayWidth, hud::getOverlayHeight, () -> hud.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.misc.performanceHudConfig.hudScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openFetchurEditor() {
        if (feature == null) return;
        FetchurOverlay fetchur = FetchurOverlay.getInstance();
        screenToOpen = new GuiPositionEditor(feature.mining.fetchur.fetchurOverlayPos, fetchur::getOverlayWidth, fetchur::getOverlayHeight, () -> fetchur.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.mining.fetchur.fetchurOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDianaOverlayEditor() {
        if (feature == null) return;
        screenToOpen = new GuiDianaOverlayEditor(Minecraft.getMinecraft().currentScreen, ATHRConfig::saveConfig);
    }

    public static void openScoreboardEditor() {
        if (feature == null) return;
        CustomScoreboard sb = CustomScoreboard.getInstance();
        screenToOpen = new GuiPositionEditor(feature.scoreboard.position, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.scoreboard.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openSearchBarEditor() {
        if (feature == null) return;
        SearchBar sb = SearchBar.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.searchBarConfig.searchBarPos, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openCurrentPetEditor() {
        if (feature == null) return;
        CurrentPetOverlay overlay = CurrentPetOverlay.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.currentPet.currentPetPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.misc.currentPet.currentPetScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemPickupLogEditor() {
        if (feature == null) return;
        ItemPickupLog overlay = ItemPickupLog.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.itemPickupLogConfig.itemPickupLogPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.misc.itemPickupLogConfig.itemPickupLogScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemCooldownEditor() {
        if (feature == null) return;
        ItemCooldownOverlay overlay = ItemCooldownOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.itemCooldown.itemCooldownPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.qol.itemCooldown.itemCooldownScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemAbilityTimerEditor() {
        if (feature == null) return;
        ItemAbilityTimerOverlay overlay = ItemAbilityTimerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.abilityTimer.itemAbilityTimerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.qol.abilityTimer.itemAbilityTimerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemInvincibilityEditor() {
        if (feature == null) return;
        ItemInvincibilityOverlay overlay = ItemInvincibilityOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.invincibility.itemInvincibilityPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.qol.invincibility.itemInvincibilityScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openPowderEditor() {
        if (feature == null) return;
        PowderOverlay overlay = PowderOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.powderTrackerConfig.powderOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.mining.powderTrackerConfig.powderOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonBreakerEditor() {
        if (feature == null) return;
        DungeonBreakerOverlay overlay = DungeonBreakerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonBreaker.dungeonBreakerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonBreaker.dungeonBreakerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonMapEditor(){
        if (feature == null) return;
        DungeonMapOverlay overlay = DungeonMapOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonMapConfig.dungeonMapPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonMapConfig.scale).withParent(Minecraft.getMinecraft().currentScreen);

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
        screenToOpen = new GuiPositionEditor(feature.fishing.trophyFish.trophyFishPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.fishing.trophyFish.trophyFishScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openBpsEditor() {
        if (feature == null) return;
        BPSOverlay overlay = BPSOverlay.getInstance();
        assert overlay != null;
        screenToOpen = new GuiPositionEditor(feature.farming.bps.bpsPosition, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.farming.bps.bpsScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPowderTracker() {
        PowderStats.getInstance().reset();
    }

    public static void openPristineEditor() {
        if (feature == null) return;
        PristineOverlay overlay = PristineOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.pristineTrackerConfig.pristineOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.mining.pristineTrackerConfig.pristineOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPristineTracker() {
        PristineStats.getInstance().reset();
    }


    public static void openUptimeEditor() {
        if (feature == null) return;
        UptimeOverlay overlay = UptimeOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.misc.uptimeConfig.uptimePos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), ATHRConfig::saveConfig, ATHRConfig::saveConfig).withOverlayScale(feature.misc.uptimeConfig.uptimeScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openChatFilterUI() {
        if (feature == null) return;
        screenToOpen = new ChatFilterGUI();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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

        if (feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey) && !powderToggleKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            PowderStats.getInstance().toggleTracking();
        }

        powderToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey);

        if (feature != null && isKeyOrMouseDown(feature.mining.pristineTrackerConfig.pristineToggleKey) && !pristineToggleKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            PristineStats.getInstance().toggleTracking();
        }

        pristineToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.pristineTrackerConfig.pristineToggleKey);
    }
}