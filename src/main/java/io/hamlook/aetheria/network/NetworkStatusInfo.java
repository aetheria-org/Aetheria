package io.hamlook.aetheria.network;

import io.hamlook.aetheria.core.ATHRConfig;

import java.util.*;

public final class NetworkStatusInfo {

    private static final EnumMap<Gate, LinkedHashSet<String>> DEPENDENCIES = new EnumMap<>(Gate.class);
    /**
     * Only features without a user-facing entry point are seeded; everything else registers
     * itself dynamically via {@link NetworkGuard#requiresApi}/{@link NetworkGuard#requiresGithub}.
     * (The old seed also listed "Global Chat", "Diana Party", "Profile Viewer", "Profile Parser",
     * "/sync" — those are now registered at runtime, and "Diana Party" vs "Diana Parties" were
     * distinct strings, so the popup showed both.)
     */
    private static final String[] API_SEED = {"Capes", "Perks", "Price Info", "Secret Reports"};
    private static final String[] GITHUB_SEED = {"Secret Routes", "Player Sizes", "Enchant Data", "Timers", "Emoji Data", "Item Data", "Version/Cape data"};

    static {
        for (String feature : API_SEED) registerDependency(Gate.API, feature);
        for (String feature : GITHUB_SEED) registerDependency(Gate.GITHUB, feature);
    }

    private NetworkStatusInfo() {
    }

    /**
     * Declares that {@code feature} depends on {@code gate}. Called from
     * {@link NetworkGuard#requiresApi} and {@link NetworkGuard#requiresGithub}; the per-gate
     * sets feed the network-status popup's affected lists. Main thread only; a dedup add,
     * so repeated calls are O(1).
     */
    public static void registerDependency(Gate gate, String feature) {
        if (feature == null || feature.isEmpty()) return;
        DEPENDENCIES.computeIfAbsent(gate, g -> new LinkedHashSet<>()).add(feature);
    }

    /**
     * Snapshot of every feature registered under {@code gate} (seed + runtime registrations),
     * in registration order. Fresh array each call; callers cache it (see {@link #blockedGates}).
     */
    public static String[] affectedFeatures(Gate gate) {
        Set<String> set = DEPENDENCIES.get(gate);
        return set == null ? new String[0] : set.toArray(new String[0]);
    }

    /**
     * The gate that is actually blocking, given a feature's declared gate: Offline Mode
     * dominates every other gate, so the Enable button fixes the root cause.
     */
    public static Gate rootGateFor(Gate declared) {
        if (ATHRConfig.feature != null && ATHRConfig.feature.network.offlineMode) return Gate.OFFLINE;
        return declared;
    }

    private static String settingName(Gate gate) {
        switch (gate) {
            case API:
                return "API Calls";
            case GITHUB:
                return "GitHub Calls";
            default:
                return "Offline Mode";
        }
    }

    public static String enableLabel(Gate gate) {
        return gate == Gate.OFFLINE ? "Networking" : settingName(gate);
    }

    public static String whyText(Gate gate) {
        if (gate == Gate.OFFLINE) return "Offline Mode is on.";
        return settingName(gate) + " are off.";
    }

    public static String gateId(Gate gate) {
        switch (gate) {
            case API:
                return "api";
            case GITHUB:
                return "github";
            default:
                return "offline";
        }
    }

    public static Gate gateFromId(String id) {
        if ("github".equalsIgnoreCase(id)) return Gate.GITHUB;
        if ("offline".equalsIgnoreCase(id)) return Gate.OFFLINE;
        return Gate.API;
    }

    public static int currentMask() {
        if (ATHRConfig.feature == null) return 0;
        int mask = 0;
        if (!NetworkGuard.apiAllowed()) mask |= Gate.API.bit;
        if (!NetworkGuard.githubAllowed()) mask |= Gate.GITHUB.bit;
        if (ATHRConfig.feature.network.offlineMode) mask |= Gate.OFFLINE.bit;
        return mask;
    }

    public static boolean shouldShow(int ackMask) {
        int mask = currentMask();
        return mask != 0 && (ackMask == 0 || (mask & ~ackMask) != 0);
    }

    public static int ackMaskFor(int mask) {
        if ((mask & Gate.OFFLINE.bit) != 0) return Gate.OFFLINE.bit;
        int ack = 0;
        if ((mask & Gate.API.bit) != 0) ack |= Gate.API.bit;
        if ((mask & Gate.GITHUB.bit) != 0) ack |= Gate.GITHUB.bit;
        return ack;
    }

    public static List<GateInfo> blockedGates() {
        int mask = currentMask();
        List<GateInfo> gates = new ArrayList<>();
        if ((mask & Gate.OFFLINE.bit) != 0) {
            gates.add(new GateInfo(Gate.OFFLINE, "Offline Mode", "Offline Mode", combinedFeatures()));
            return gates;
        }
        if ((mask & Gate.API.bit) != 0)
            gates.add(new GateInfo(Gate.API, "API Calls", "Global Chat", affectedFeatures(Gate.API)));
        if ((mask & Gate.GITHUB.bit) != 0)
            gates.add(new GateInfo(Gate.GITHUB, "GitHub Calls", "Secret Routes", affectedFeatures(Gate.GITHUB)));
        return gates;
    }

    private static String[] combinedFeatures() {
        Set<String> union = new LinkedHashSet<>();
        Set<String> api = DEPENDENCIES.get(Gate.API);
        Set<String> github = DEPENDENCIES.get(Gate.GITHUB);
        if (api != null) union.addAll(api);
        if (github != null) union.addAll(github);
        return union.toArray(new String[0]);
    }

    public enum Gate {
        API(1), GITHUB(2), OFFLINE(4);

        public final int bit;

        Gate(int bit) {
            this.bit = bit;
        }
    }

    public static final class GateInfo {
        public final Gate gate;
        public final String settingName;
        public final String headline;
        public final String[] affected;

        GateInfo(Gate gate, String settingName, String headline, String[] affected) {
            this.gate = gate;
            this.settingName = settingName;
            this.headline = headline;
            this.affected = affected;
        }

        public int countOther() {
            return affected.length - 1;
        }
    }
}