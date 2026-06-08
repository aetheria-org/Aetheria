package io.hamlook.aetheria.core.features.network;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class NetworkConfig {

    @Expose
    @ConfigOption(name = "Offline Mode", desc = "Disables all network calls. Most mod features will not work.")
    @ConfigEditorBoolean
    public boolean offlineMode = false;

    @Expose
    @ConfigOption(name = "Disable Telemetry", desc = "Stops the mod from sending your username, mod list, and version on server join. Used for player counts and crash reports.")
    @ConfigEditorBoolean
    public boolean disableTelemetry = false;

    @Expose
    @ConfigOption(name = "Hide Mod List in Telemetry", desc = "Omits your installed mod list from telemetry. Username and mod version are still sent for player counts. Has no effect if Disable Telemetry is on.")
    @ConfigEditorBoolean
    public boolean disableModListInTelemetry = false;

    @Expose
    @ConfigOption(name = "Disable API Calls", desc = "Disables capes, profile viewer, profile parser, sync command, and any other feature that communicates with the mod API.")
    @ConfigEditorBoolean
    public boolean disableApiCalls = false;

    @Expose
    @ConfigOption(name = "Disable GitHub Calls", desc = "Disables fetching repo data from GitHub. Most mod features including overlays, timers, and version checks will stop working.")
    @ConfigEditorBoolean
    public boolean disableGithubCalls = false;

    @Expose
    public boolean hasSeenPrivacyNotice = false;

    @Expose
    @ConfigOption(name = "Privacy Notice", desc = "Open the privacy notice to review how your data is handled")
    @ConfigEditorButton(runnableId = "openPrivacyNotice", buttonText = "Open")
    public boolean openPrivacyNoticeButton = false;
}