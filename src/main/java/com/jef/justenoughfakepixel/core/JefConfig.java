package com.jef.justenoughfakepixel.core;

import com.jef.justenoughfakepixel.core.config.command.JefCommand;
import com.jef.justenoughfakepixel.core.config.editors.GuiPositionEditor;
import com.jef.justenoughfakepixel.core.config.gui.GuiScreenElementWrapper;
import com.jef.justenoughfakepixel.core.config.gui.config.ConfigEditor;
import com.jef.justenoughfakepixel.features.diana.GuiDianaOverlayEditor;
import com.jef.justenoughfakepixel.features.dungeons.DungeonStats;
import com.jef.justenoughfakepixel.features.dungeons.overlays.DungeonBreakerOverlay;
import com.jef.justenoughfakepixel.features.dungeons.rooms.DungeonRoomOverlay;
import com.jef.justenoughfakepixel.features.fishing.trophy.TrophyFishOverlay;
import com.jef.justenoughfakepixel.features.mining.fetchur.FetchurOverlay;
import com.jef.justenoughfakepixel.features.mining.powder.PowderOverlay;
import com.jef.justenoughfakepixel.features.mining.powder.PowderStats;
import com.jef.justenoughfakepixel.features.misc.ItemPickupLog;
import com.jef.justenoughfakepixel.features.misc.PerformanceHUD;
import com.jef.justenoughfakepixel.features.misc.SearchBar;
import com.jef.justenoughfakepixel.features.misc.pet.CurrentPetOverlay;
import com.jef.justenoughfakepixel.features.qol.overlays.ItemAbilityTimerOverlay;
import com.jef.justenoughfakepixel.features.qol.overlays.ItemCooldownOverlay;
import com.jef.justenoughfakepixel.features.qol.overlays.ItemInvincibilityOverlay;
import com.jef.justenoughfakepixel.features.scoreboard.CustomScoreboard;
import com.jef.justenoughfakepixel.features.waypoints.WaypointGroupGui;
import com.jef.justenoughfakepixel.repo.JefRepo;
import com.jef.justenoughfakepixel.repo.RepoHandler;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JefConfig {

    public static final KeyBinding openGuiKey = new KeyBinding("Open JEF GUI", Keyboard.KEY_P, "JustEnoughFakepixel");
    public static Config feature;
    public static File configDirectory = new File("config/JustEnoughFakepixel");
    public static GuiScreen screenToOpen = null;
    private static File configFile;
    private static int screenTicks = 0;
    private static boolean waypointManagerKeyWasDown = false;
    private static boolean powderToggleKeyWasDown = false;
    private static boolean registered = false;

    private static boolean isKeyOrMouseDown(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) return false;
        if (keyCode < 0) return Mouse.isButtonDown(keyCode + 100);
        return Keyboard.isKeyDown(keyCode);
    }

    public static void register() {
        if (registered) return;
        init();
        MinecraftForge.EVENT_BUS.register(new JefConfig());
        ClientRegistry.registerKeyBinding(openGuiKey);
        ClientCommandHandler.instance.registerCommand(new JefCommand());
        registered = true;
    }

    public static void init() {
        if (!configDirectory.exists()) configDirectory.mkdirs();
        configFile = new File(configDirectory, "config.json");
        loadConfig();
    }

    private static void loadConfig() {
        if (configFile.exists()) {
            // Uses shared loadSafe for consistent corruption handling
            feature = JefStorageManager.loadSafe(configFile, Config.class, JefGsonBuilder.GSON_STRICT);
        }
        if (feature == null) {
            feature = new Config();
            saveConfig();
        }
    }

    public static void saveConfig() {
        // Uses shared saveAtomic — .tmp → verify → atomic rename, same as every other storage
        JefStorageManager.saveAtomic(configFile, feature, JefGsonBuilder.GSON_STRICT);
    }

    public static void reloadRepo() {
        RepoHandler.refresh(JefRepo.KEY_TIMERS);
        RepoHandler.refresh(JefRepo.KEY_PLAYERSIZES);
        RepoHandler.refresh(JefRepo.KEY_UPDATE);
        RepoHandler.refresh(JefRepo.KEY_TAGS);
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
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonOverlay.statsPos, stats::getOverlayWidth, stats::getOverlayHeight, () -> stats.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonOverlay.statsScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonRoomOverlayEditor() {
        if (feature == null) return;
        DungeonRoomOverlay overlay = DungeonRoomOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openHudEditor() {
        if (feature == null) return;
        PerformanceHUD hud = PerformanceHUD.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.performanceHudConfig.hudPos, hud::getOverlayWidth, hud::getOverlayHeight, () -> hud.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.misc.performanceHudConfig.hudScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openFetchurEditor() {
        if (feature == null) return;
        FetchurOverlay fetchur = FetchurOverlay.getInstance();
        screenToOpen = new GuiPositionEditor(feature.mining.fetchur.fetchurOverlayPos, fetchur::getOverlayWidth, fetchur::getOverlayHeight, () -> fetchur.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.mining.fetchur.fetchurOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDianaOverlayEditor() {
        if (feature == null) return;
        screenToOpen = new GuiDianaOverlayEditor(Minecraft.getMinecraft().currentScreen, JefConfig::saveConfig);
    }

    public static void openScoreboardEditor() {
        if (feature == null) return;
        CustomScoreboard sb = CustomScoreboard.getInstance();
        screenToOpen = new GuiPositionEditor(feature.scoreboard.position, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.scoreboard.scale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openSearchBarEditor() {
        if (feature == null) return;
        SearchBar sb = SearchBar.getInstance();
        screenToOpen = new GuiPositionEditor(feature.misc.searchBarConfig.searchBarPos, sb::getOverlayWidth, sb::getOverlayHeight, () -> sb.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openCurrentPetEditor() {
        if (feature == null) return;
        CurrentPetOverlay overlay = CurrentPetOverlay.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.currentPet.currentPetPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.misc.currentPet.currentPetScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemPickupLogEditor() {
        if (feature == null) return;
        ItemPickupLog overlay = ItemPickupLog.getInstance();
        if (overlay == null) return;
        overlay.render(true);
        screenToOpen = new GuiPositionEditor(feature.misc.itemPickupLogConfig.itemPickupLogPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.misc.itemPickupLogConfig.itemPickupLogScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemCooldownEditor() {
        if (feature == null) return;
        ItemCooldownOverlay overlay = ItemCooldownOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.itemCooldown.itemCooldownPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.qol.itemCooldown.itemCooldownScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemAbilityTimerEditor() {
        if (feature == null) return;
        ItemAbilityTimerOverlay overlay = ItemAbilityTimerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.abilityTimer.itemAbilityTimerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.qol.abilityTimer.itemAbilityTimerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openItemInvincibilityEditor() {
        if (feature == null) return;
        ItemInvincibilityOverlay overlay = ItemInvincibilityOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.qol.invincibility.itemInvincibilityPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.qol.invincibility.itemInvincibilityScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openPowderEditor() {
        if (feature == null) return;
        PowderOverlay overlay = PowderOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.mining.powderTrackerConfig.powderOverlayPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.mining.powderTrackerConfig.powderOverlayScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openDungeonBreakerEditor() {
        if (feature == null) return;
        DungeonBreakerOverlay overlay = DungeonBreakerOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.dungeons.dungeonBreaker.dungeonBreakerPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.dungeons.dungeonBreaker.dungeonBreakerScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void openInvButtonEditor() {
        screenToOpen = new com.jef.justenoughfakepixel.features.misc.invbuttons.GuiInvButtonEditor();
    }

    public static void openTrophyFishEditor() {
        if (feature == null) return;
        TrophyFishOverlay overlay = TrophyFishOverlay.getInstance();
        if (overlay == null) return;
        screenToOpen = new GuiPositionEditor(feature.fishing.trophyFish.trophyFishPos, overlay::getOverlayWidth, overlay::getOverlayHeight, () -> overlay.render(true), JefConfig::saveConfig, JefConfig::saveConfig).withOverlayScale(feature.fishing.trophyFish.trophyFishScale).withParent(Minecraft.getMinecraft().currentScreen);
    }

    public static void resetPowderTracker() {
        PowderStats.getInstance().reset();
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

        if (openGuiKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) openGui();

        boolean managerKeyDown = feature != null && isKeyOrMouseDown(feature.waypoints.waypointManagerKey);
        if (managerKeyDown && !waypointManagerKeyWasDown && Minecraft.getMinecraft().currentScreen == null)
            openWaypointGroupGui();
        waypointManagerKeyWasDown = managerKeyDown;

        if (feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey) && !powderToggleKeyWasDown && Minecraft.getMinecraft().currentScreen == null) {
            PowderStats.getInstance().toggleTracking();
        }

        powderToggleKeyWasDown = feature != null && isKeyOrMouseDown(feature.mining.powderTrackerConfig.powderToggleKey);
    }
}