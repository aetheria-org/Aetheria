package io.hamlook.aetheria.core;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.config.gui.config.ConfigAnnotations.Category;
import io.hamlook.aetheria.core.features.about.About;
import io.hamlook.aetheria.core.features.chat.ChatConfig;
import io.hamlook.aetheria.core.features.cosmetics.Cosmetics;
import io.hamlook.aetheria.core.features.debug.Debug;
import io.hamlook.aetheria.core.features.diana.Diana;
import io.hamlook.aetheria.core.features.dungeons.Dungeons;
import io.hamlook.aetheria.core.features.fishing.Fishing;
import io.hamlook.aetheria.core.features.misc.Misc;
import io.hamlook.aetheria.core.features.mining.Mining;
import io.hamlook.aetheria.core.features.overlays.Overlays;
import io.hamlook.aetheria.core.features.qol.Qol;
import io.hamlook.aetheria.core.features.waypoints.Waypoints;
import io.hamlook.aetheria.core.features.farming.Farming;
import io.hamlook.aetheria.core.features.scoreboard.Scoreboard;
import io.hamlook.aetheria.core.features.storage.Storage;
import io.hamlook.aetheria.features.capes.CapeManager;

import java.awt.*;
import java.net.URI;

public class Config {

    @Expose
    @Category(name = "About", desc = "Links, credits & used software")
    public final About about = new About();

    @Expose
    @Category(name = "Quality of life", desc = "QOL features")
    public final Qol qol = new Qol();

    @Expose
    @Category(name = "Scoreboard", desc = "Custom scoreboard panel")
    public final Scoreboard scoreboard = new Scoreboard();

    @Expose
    @Category(name = "Chat Utils", desc = "Chat compacting, timestamps, chat heads, copy & visual tweaks")
    public final ChatConfig chat = new ChatConfig();

    @Expose
    @Category(name = "Misc", desc = "Misc features")
    public final Misc misc = new Misc();

    @Expose
    @Category(name = "Storage", desc = "Storage Overlay features")
    public final Storage storage = new Storage();

    @Expose
    @Category(name = "Cosmetics", desc = "Capes and Cosmetics Feature")
    public final Cosmetics cosmetics = new Cosmetics();

    @Expose
    @Category(name = "Waypoints", desc = "Waypoints config & GUI")
    public final Waypoints waypoints = new Waypoints();

    @Expose
    @Category(name = "Mining", desc = "Mining features")
    public final Mining mining = new Mining();

    @Expose
    @Category(name = "Diana", desc = "Diana event tracking & overlays")
    public final Diana diana = new Diana();

    @Expose
    @Category(name = "Dungeons", desc = "Dungeon features")
    public final Dungeons dungeons = new Dungeons();

    @Expose
    @Category(name = "Farming", desc = "Farming features")
    public final Farming farming = new Farming();

    @Expose
    @Category(name = "Fishing", desc = "Fishing features")
    public final Fishing fishing = new Fishing();

    @Expose
    @Category(name = "Overlays", desc = "Various Ovarlay features")
    public final Overlays overlays = new Overlays();

    @Expose
    @Category(name = "Debug", desc = "Debug tools")
    public final Debug debug = new Debug();

    private static void openUrl(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) {}
    }

    public void executeRunnable(String runnableId) {
        switch (runnableId) {
            case "reloadRepo": ATHRConfig.reloadRepo(); break;
            case "openScoreboardEditor": ATHRConfig.openScoreboardEditor(); break;
            case "openWaypointGroupGui": ATHRConfig.openWaypointGroupGui(); break;
            case "openStatsEditor": ATHRConfig.openStatsEditor(); break;
            case "openHudEditor": ATHRConfig.openHudEditor(); break;
            case "openFetchurEditor": ATHRConfig.openFetchurEditor(); break;
            case "openDianaOverlayEditor": ATHRConfig.openDianaOverlayEditor(); break;
            case "openSearchBarEditor": ATHRConfig.openSearchBarEditor(); break;
            case "openCurrentPetEditor": ATHRConfig.openCurrentPetEditor(); break;
            case "openItemPickupLogEditor": ATHRConfig.openItemPickupLogEditor(); break;
            case "openItemCooldownEditor": ATHRConfig.openItemCooldownEditor(); break;
            case "openPowderEditor": ATHRConfig.openPowderEditor(); break;
            case "openInvButtonEditor": ATHRConfig.openInvButtonEditor(); break;
            case "resetPowderTracker": ATHRConfig.resetPowderTracker(); break;
            case "openPristineEditor": ATHRConfig.openPristineEditor(); break;
            case "resetPristineTracker": ATHRConfig.resetPristineTracker(); break;
            case "openDungeonBreakerEditor": ATHRConfig.openDungeonBreakerEditor(); break;
            case "openTrophyFishEditor": ATHRConfig.openTrophyFishEditor(); break;
            case "openDungeonRoomOverlayEditor": ATHRConfig.openDungeonRoomOverlayEditor(); break;
            case "openItemInvincibilityEditor": ATHRConfig.openItemInvincibilityEditor(); break;
            case "openItemAbilityTimerEditor": ATHRConfig.openItemAbilityTimerEditor(); break;
            case "openBpsEditor": ATHRConfig.openBpsEditor(); break;
            case "openUptimeEditor": ATHRConfig.openUptimeEditor(); break;
            case "chatFiltersGUI": ATHRConfig.openChatFilterUI(); break;
            case "reloadCapes": CapeManager.reload(); break;
            case "openWebsite": openUrl("https://aetheria.github.io"); break;
            case "openDiscord": openUrl("https://discord.gg/tdMFbmhFTb"); break;
            case "openGithub": openUrl("https://github.com/aetheria/aetheria"); break;
            case "openLicenseForge": openUrl("https://github.com/MinecraftForge/MinecraftForge"); break;
            case "openLicenseMixin": openUrl("https://github.com/SpongePowered/Mixin/"); break;
            case "openLicenseMoulConfig": openUrl("https://github.com/NotEnoughUpdates/MoulConfig"); break;
            case "openLicenseLombok": openUrl("https://projectlombok.org/"); break;
            case "openLicenseReflections": openUrl("https://github.com/ronmamo/reflections"); break;
            case "openLicenseJavassist": openUrl("https://github.com/jboss-javassist/javassist"); break;
            case "openLicenseJbAnnotations": openUrl("https://github.com/JetBrains/java-annotations"); break;
        }
    }
}
