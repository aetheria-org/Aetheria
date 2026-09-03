package io.hamlook.aetheria.network;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sends the blocked-feature chat message (with {@code [Enable ...]} and {@code [Hide]} buttons)
 * when {@link NetworkGuard#requiresApi} or {@link NetworkGuard#requiresGithub} hits a closed gate.
 *
 * <p>Suppression rules, checked in order in {@link #showBlocked}: not in a world (no player),
 * feature already hidden via {@code [Hide]} (persistent {@code dismissedFeatureGateMessages}
 * set in config), or within the per-feature in-memory cooldown. The cooldown stops spam on
 * repeated attempts while keeping the Enable button available.</p>
 *
 * <p>Tokens are URL-safe base64 of the UTF-8 feature name — no spaces or special characters,
 * so they fit inside a chat {@code RUN_COMMAND} even for names like {@code "/sync"}.</p>
 */
public final class BlockedFeatureMessenger {

    private static final long COOLDOWN_MS = 10_000L;
    private static final Map<String, Long> lastSentAt = new HashMap<>();

    private BlockedFeatureMessenger() {
    }

    /**
     * Builds and sends the blocked-feature message. {@code root} is the actual blocking gate
     * (see {@link NetworkStatusInfo#rootGateFor}), so the Enable button fixes the root cause.
     */
    public static void showBlocked(String feature, NetworkStatusInfo.Gate root) {
        if (feature == null || feature.isEmpty()) return;
        if (MinecraftCompat.getMinecraft().thePlayer == null) return;
        if (dismissed(feature)) return;

        long now = System.currentTimeMillis();
        Long last = lastSentAt.get(feature);
        if (last != null && now - last < COOLDOWN_MS) return;
        lastSentAt.put(feature, now);

        IChatComponent rootComp = TextCompat.createText("§c" + feature + " §7is disabled because §c" + NetworkStatusInfo.whyText(root) + " ");

        IChatComponent enable = TextCompat.createText("§a[Enable " + NetworkStatusInfo.enableLabel(root) + "§r]");
        TextCompat.setClickRunCommand(TextCompat.getChatStyle(enable), "/athrnet enable " + NetworkStatusInfo.gateId(root));
        TextCompat.setHoverShowText(TextCompat.getChatStyle(enable), "§7Turn on " + NetworkStatusInfo.enableLabel(root) + " to use " + feature);
        TextCompat.appendSibling(rootComp, enable);

        IChatComponent hide = TextCompat.createText(" §7[§8Hide§7]");
        TextCompat.setClickRunCommand(TextCompat.getChatStyle(hide), "/athrnet hide " + tokenFor(feature));
        TextCompat.setHoverShowText(TextCompat.getChatStyle(hide), "§7Never show this message again");
        TextCompat.appendSibling(rootComp, hide);

        MinecraftCompat.getMinecraft().thePlayer.addChatMessage(rootComp);
    }

    private static boolean dismissed(String feature) {
        if (feature == null || ATHRConfig.feature == null) return false;
        return ATHRConfig.feature.network.dismissedFeatureGateMessages.contains(feature);
    }

    /**
     * Adds/removes {@code feature} from the persistent hidden set and saves the config.
     */
    public static void setDismissed(String feature, boolean dismissed) {
        if (feature == null || feature.isEmpty() || ATHRConfig.feature == null) return;
        Set<String> set = ATHRConfig.feature.network.dismissedFeatureGateMessages;
        if (dismissed) set.add(feature);
        else set.remove(feature);
        ATHRConfig.saveConfig();
    }

    /**
     * URL-safe base64 token for a feature name, for use inside a chat command.
     */
    public static String tokenFor(String feature) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(feature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Inverse of {@link #tokenFor}; returns null on malformed input.
     */
    public static String featureFromToken(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}