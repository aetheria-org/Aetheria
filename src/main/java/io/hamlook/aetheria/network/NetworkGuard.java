package io.hamlook.aetheria.network;

import io.hamlook.aetheria.core.ATHRConfig;

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
}