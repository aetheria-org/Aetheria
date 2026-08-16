package io.hamlook.aetheria.network;

import io.hamlook.aetheria.core.ATHRConfig;

/**
 * Network gate checks for Aetheria's privacy/network settings.
 *
 * <p>Call {@link #requiresApi(String)} or {@link #requiresGithub(String)} at a feature's
 * <b>user-facing entry point</b>:</p>
 *
 * <pre>{@code
 * if (!NetworkGuard.requiresApi("Global Chat")) return;
 * }</pre>
 *
 * <p>The call does three things:</p>
 * <ol>
 *   <li>Declares "feature depends on this gate" — {@link NetworkStatusInfo#registerDependency},
 *       which feeds the network-status popup's affected-feature lists.</li>
 *   <li>Returns whether the gate is currently open (true = proceed).</li>
 *   <li>When closed, sends the player a chat message with {@code [Enable ...]} and
 *       {@code [Hide]} buttons via {@link BlockedFeatureMessenger}.</li>
 * </ol>
 *
 * <p>Use the silent {@code apiAllowed()}/{@code githubAllowed()}/{@code networkingEnabled()}/
 * {@code telemetryAllowed()}/{@code modListInTelemetryAllowed()} checks for background fetches
 * that must not message the player, and gate whitelist/version checks with nothing at all —
 * those must always work.</p>
 */
public class NetworkGuard {

    private NetworkGuard() {
    }

    // Returns false if all networking is disabled
    public static boolean networkingEnabled() {
        if (ATHRConfig.feature == null) return false;
        return !ATHRConfig.feature.network.offlineMode;
    }

    // Telemetry: username, mod list, version sent on server join
    public static boolean telemetryAllowed() {
        if (!networkingEnabled()) return false;
        return !ATHRConfig.feature.network.disableTelemetry;
    }

    // Mod list specifically within telemetry
    public static boolean modListInTelemetryAllowed() {
        if (!telemetryAllowed()) return false;
        return !ATHRConfig.feature.network.disableModListInTelemetry;
    }

    // API calls: capes, profile viewer, supabase, profile parser
    public static boolean apiAllowed() {
        if (!networkingEnabled()) return false;
        return !ATHRConfig.feature.network.disableApiCalls;
    }

    // GitHub calls: repo data used by most mod features
    public static boolean githubAllowed() {
        if (!networkingEnabled()) return false;
        return !ATHRConfig.feature.network.disableGithubCalls;
    }

    /**
     * Declares that {@code feature} requires API Calls and returns whether they're enabled.
     * Usage: {@code if (!NetworkGuard.requiresApi("Global Chat")) return;}
     * When disabled, messages the player with Enable/Hide buttons (see class javadoc).
     */
    public static boolean requiresApi(String feature) {
        return requires(NetworkStatusInfo.Gate.API, feature);
    }

    /**
     * Declares that {@code feature} requires GitHub Calls and returns whether they're enabled.
     * Usage: {@code if (!NetworkGuard.requiresGithub("Secret Routes")) return;}
     * When disabled, messages the player with Enable/Hide buttons (see class javadoc).
     */
    public static boolean requiresGithub(String feature) {
        return requires(NetworkStatusInfo.Gate.GITHUB, feature);
    }

    /**
     * Flips the config flag for {@code gate} off (enables the gate) and saves the config.
     * Shared by the popup's "Turn on" button and the {@code /athrnet enable} command.
     */
    public static void enableGate(NetworkStatusInfo.Gate gate) {
        if (ATHRConfig.feature == null) return;
        switch (gate) {
            case API:
                ATHRConfig.feature.network.disableApiCalls = false;
                break;
            case GITHUB:
                ATHRConfig.feature.network.disableGithubCalls = false;
                break;
            default:
                ATHRConfig.feature.network.offlineMode = false;
        }
        ATHRConfig.saveConfig();
    }

    private static boolean requires(NetworkStatusInfo.Gate declared, String feature) {
        NetworkStatusInfo.registerDependency(declared, feature);
        if (allowed(declared)) return true;
        BlockedFeatureMessenger.showBlocked(feature, NetworkStatusInfo.rootGateFor(declared));
        return false;
    }

    private static boolean allowed(NetworkStatusInfo.Gate gate) {
        switch (gate) {
            case API: return apiAllowed();
            case GITHUB: return githubAllowed();
            default: return networkingEnabled();
        }
    }
}