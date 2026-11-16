package io.hamlook.aetheria.core;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.features.about.About;
import io.hamlook.aetheria.core.features.chat.ChatConfig;
import io.hamlook.aetheria.core.features.chat.PlayerButtonsConfig;
import io.hamlook.aetheria.core.features.cosmetics.Cosmetics;
import io.hamlook.aetheria.core.features.debug.Debug;
import io.hamlook.aetheria.core.features.diana.Diana;
import io.hamlook.aetheria.core.features.dungeons.Dungeons;
import io.hamlook.aetheria.core.features.farming.Farming;
import io.hamlook.aetheria.core.features.fishing.Fishing;
import io.hamlook.aetheria.core.features.mining.Mining;
import io.hamlook.aetheria.core.features.misc.Misc;
import io.hamlook.aetheria.core.features.network.NetworkConfig;
import io.hamlook.aetheria.core.features.overlays.Overlays;
import io.hamlook.aetheria.core.features.qol.Qol;
import io.hamlook.aetheria.core.features.scoreboard.Scoreboard;
import io.hamlook.aetheria.core.features.storage.Storage;
import io.hamlook.aetheria.core.features.waypoints.Waypoints;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.Category;
import io.hamlook.aetheria.features.capes.CapeManager;
import io.hamlook.aetheria.features.dungeons.rooms.secret.SecretChime;
import io.hamlook.aetheria.repo.RepoLinks;
import io.hamlook.aetheria.utils.UrlOpener;

public class Config {

    @Expose
    public int keybindMigrationVersion = 0;

    @Expose
    public int configVersion = 0;

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
    @Category(name = "Misc", desc = "Misc features")
    public final Misc misc = new Misc();

    @Expose
    @Category(name = "Storage", desc = "Storage Overlay features")
    public final Storage storage = new Storage();

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
    @Category(name = "Network & Privacy", desc = "Manage network calls")
    public final NetworkConfig network = new NetworkConfig();

    @Expose
    @Category(name = "Chat", desc = "Chat compacting, timestamps, chat heads, copy & visual tweaks")
    public final ChatConfig chat = new ChatConfig();

    @Expose
    @Category(name = "Overlays", desc = "Various Overlay features")
    public final Overlays overlays = new Overlays();

    @Expose
    @Category(name = "Cosmetics", desc = "Capes and Cosmetics Feature")
    public final Cosmetics cosmetics = new Cosmetics();

    @Expose
    @Category(name = "Waypoints", desc = "Waypoints config & GUI")
    public final Waypoints waypoints = new Waypoints();

    @Expose
    @Category(name = "Debug", desc = "Debug tools")
    public final Debug debug = new Debug();

    private static void openUrl(String url) {
        if (UrlOpener.open(url)) return;
        Aetheria.logger.warning("Config: could not open URL " + url);
    }

    public void executeRunnable(String runnableId) {
        switch (runnableId) {
            case "reloadRepo": ATHRConfig.reloadRepo(); break;
            case "openScoreboardEditor": ATHRConfig.openScoreboardEditor(); break;
            case "openWaypointGroupGui": ATHRConfig.openWaypointGroupGui(); break;
            case "openRareDropTrackerGui": ATHRConfig.openRareDropTrackerGui(); break;
            case "openRareDropTrackerOverlayEditor": ATHRConfig.openRareDropTrackerOverlayEditor(); break;
            case "openNotificationsOverlayEditor": ATHRConfig.openNotificationsOverlayEditor(); break;
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
            case "openGoldEditor": ATHRConfig.openGoldEditor(); break;
            case "resetGoldTracker": ATHRConfig.resetGoldTracker(); break;
            case "openFarmingTrackerEditor": ATHRConfig.openFarmingTrackerEditor(); break;
            case "resetFarmingTracker": ATHRConfig.resetFarmingTracker(); break;
            case "openOrganicMatterTrackerEditor": ATHRConfig.openOrganicMatterTrackerEditor(); break;
            case "resetOrganicMatterTracker": ATHRConfig.resetOrganicMatterTracker(); break;
            case "openPestEditor": ATHRConfig.openPestEditor(); break;
            case "resetPestTracker": ATHRConfig.resetPestTracker(); break;
            case "openPestFinderEditor": ATHRConfig.openPestFinderEditor(); break;
            case "openGardenPlotWarpGridEditor": ATHRConfig.openGardenPlotWarpGridEditor(); break;
            case "openVisitorOverlayEditor": ATHRConfig.openVisitorOverlayEditor(); break;
            case "openVisitorPanelEditor": ATHRConfig.openVisitorPanelEditor(); break;
            case "resetVisitorList": ATHRConfig.resetVisitorList(); break;
            case "openPristineEditor": ATHRConfig.openPristineEditor(); break;
            case "resetPristineTracker": ATHRConfig.resetPristineTracker(); break;
            case "openDungeonBreakerEditor": ATHRConfig.openDungeonBreakerEditor(); break;
            case "testSecretChime": SecretChime.playTest(); break;
            case "editDungeonMapPos": ATHRConfig.openDungeonMapEditor(); break;
            case "openTrophyFishEditor": ATHRConfig.openTrophyFishEditor(); break;
            case "openDungeonRoomOverlayEditor": ATHRConfig.openDungeonRoomOverlayEditor(); break;
            case "editAnalyzerOverlay": ATHRConfig.openDungeonAnalyzerOverlayEditor(); break;
            case "openItemInvincibilityEditor": ATHRConfig.openItemInvincibilityEditor(); break;
            case "openItemAbilityTimerEditor": ATHRConfig.openItemAbilityTimerEditor(); break;
            case "openBpsEditor": ATHRConfig.openBpsEditor(); break;
            case "openUptimeEditor": ATHRConfig.openUptimeEditor(); break;
            case "openPeltTrackerEditor": ATHRConfig.openPeltTrackerEditor(); break;
            case "resetPeltTracker": ATHRConfig.resetPeltTracker(); break;
            case "openPitchYawEditor": ATHRConfig.openPitchYawEditor(); break;
            case "openKillComboEditor": ATHRConfig.openKillComboEditor(); break;
            case "openGhostEditor": ATHRConfig.openGhostEditor(); break;
            case "resetGhostTracker": ATHRConfig.resetGhostTracker(); break;
            case "resetKillCombo": ATHRConfig.resetKillCombo(); break;
            case "resetDianaTracker": ATHRConfig.resetDianaTracker(); break;
            case "resetItemAnimations": ATHRConfig.resetItemAnimations(); break;
            case "chatFiltersGUI": ATHRConfig.openChatFilterUI(); break;
            case "openPrivacyNotice": ATHRConfig.openPrivacyNotice(); break;
            case "reloadCapes": CapeManager.reload(); break;
            case "openWebsite": openUrl(RepoLinks.website()); break;
            case "openDiscord": openUrl(RepoLinks.discord()); break;
            case "openGithub": openUrl(RepoLinks.github()); break;
            case "openLicenseForge": openUrl("https://github.com/MinecraftForge/MinecraftForge"); break;
            case "openLicenseMixin": openUrl("https://github.com/SpongePowered/Mixin/"); break;
            case "openLicenseMoulConfig": openUrl("https://github.com/NotEnoughUpdates/MoulConfig"); break;
            case "openLicenseLombok": openUrl("https://projectlombok.org/"); break;
            case "openLicenseJbAnnotations": openUrl("https://github.com/JetBrains/java-annotations"); break;
            case "openLicenseFabricLoader": openUrl("https://github.com/FabricMC/fabric-loader"); break;
            case "openLicenseFabricApi": openUrl("https://github.com/FabricMC/fabric"); break;
            case "openLicenseFabricLanguageKotlin": openUrl("https://github.com/FabricMC/fabric-language-kotlin"); break;
            case "openLicenseMixinExtras": openUrl("https://github.com/LlamaLad7/MixinExtras"); break;
            case "openLicenseKotlin": openUrl("https://github.com/JetBrains/kotlin"); break;
            case "openLicenseGson": openUrl("https://github.com/google/gson"); break;
            case "openLicenseRsyntaxtextarea": openUrl("https://github.com/bobbylight/RSyntaxTextArea"); break;
            case "openLicenseJavaWebsocket": openUrl("https://github.com/TooTallNate/Java-WebSocket"); break;
            case "openLicenseSlf4j": openUrl("https://github.com/qos-ch/slf4j"); break;
            case "openLicenseImageioApng": openUrl("https://github.com/Tianscar/imageio-apng"); break;
            case "openLicenseWebp4j": openUrl("https://github.com/MrNanko/webp4j"); break;
            case "openModrinth": openUrl(RepoLinks.modrinth()); break;
            case "openSkyAtlas": openUrl(RepoLinks.skyatlas()); break;
            case "resetPlayerButtonsColors":
                ATHRConfig.feature.chat.playerButtons.backgroundColor = PlayerButtonsConfig.DEFAULT_BACKGROUND_COLOR;
                ATHRConfig.feature.chat.playerButtons.accentColor = PlayerButtonsConfig.DEFAULT_ACCENT_COLOR;
                ATHRConfig.saveConfig();
                break;
        }
    }
}