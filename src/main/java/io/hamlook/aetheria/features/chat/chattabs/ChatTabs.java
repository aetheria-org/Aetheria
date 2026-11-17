package io.hamlook.aetheria.features.chat.chattabs;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.ChatTabsConfig;
import io.hamlook.aetheria.utils.chat.ChatUtils.MessageType;
import io.hamlook.aetheria.utils.compat.ChatHudCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

import java.util.EnumMap;
import java.util.Map;

public final class ChatTabs {

    private static volatile ChatTabType active = ChatTabType.ALL;
    private static volatile ChatTabType previewOverride;
    private static int lastSignature = -1;
    private static final Map<ChatTabType, Integer> unread = new EnumMap<>(ChatTabType.class);

    private ChatTabs() {
    }

    private static boolean masterEnabled() {
        return ATHRConfig.feature != null
                && ATHRConfig.feature.chat != null
                && ATHRConfig.feature.chat.chatTabs != null
                && ATHRConfig.feature.chat.chatTabs.enabled;
    }

    public static boolean enabled(ChatTabType tab) {
        if (tab == null || tab == ChatTabType.ALL) return true;
        if (!masterEnabled()) return false;
        ChatTabsConfig cfg = ATHRConfig.feature.chat.chatTabs;
        switch (tab) {
            case PARTY:  return cfg.partyEnabled;
            case PLAYER: return cfg.playerEnabled;
            case MSG:    return cfg.msgEnabled;
            case GUILD:  return cfg.guildEnabled;
            case COOP:   return cfg.coopEnabled;
            default:     return true;
        }
    }

    private static ChatTabType allFallback(ChatTabType tab) {
        if (tab == null) return ChatTabType.ALL;
        return enabled(tab) ? tab : ChatTabType.ALL;
    }

    public static ChatTabType activeTab() {
        ChatTabType current = active;
        if (!enabled(current)) {
            active = ChatTabType.ALL;
            return ChatTabType.ALL;
        }
        return current;
    }

    public static ChatTabType previewTab() {
        return previewOverride;
    }

    public static ChatTabType effectiveTab() {
        ChatTabType preview = previewOverride;
        if (preview != null) {
            if (enabled(preview)) return preview;
            previewOverride = null;
        }
        return activeTab();
    }

    public static void preview(ChatTabType tab) {
        ChatTabType resolved = tab != null && enabled(tab) ? tab : null;
        if (previewOverride == resolved) return;
        previewOverride = resolved;
        refreshView();
    }

    public static void activate(ChatTabType tab) {
        if (tab == null) return;
        previewOverride = null;
        active = allFallback(tab);
        unread.remove(active);
        refreshView();
    }

    public static void noteLineArrived(MessageType type) {
        ChatTabType owner = ChatTabType.owner(type);
        if (owner == ChatTabType.ALL) return;
        if (!enabled(owner)) return;
        if (owner == effectiveTab()) return;
        unread.put(owner, unread.getOrDefault(owner, 0) + 1);
    }

    public static int unread(ChatTabType tab) {
        Integer count = unread.get(tab);
        return count == null ? 0 : count;
    }

    public static String badgeText(ChatTabType tab) {
        int count = unread(tab);
        if (count <= 0) return null;
        return count > 99 ? "99+" : Integer.toString(count);
    }

    public static boolean unreadEnabled() {
        ChatTabsConfig.UnreadBadge cfg = unreadBadge();
        return cfg != null && cfg.enabled;
    }

    public static boolean dotBadges() {
        ChatTabsConfig.UnreadBadge cfg = unreadBadge();
        return cfg != null && cfg.badgeStyle == 1;
    }

    public static void pruneDisabledUnread() {
        unread.keySet().removeIf(tab -> tab != ChatTabType.ALL && !enabled(tab));
    }

    private static ChatTabsConfig.UnreadBadge unreadBadge() {
        return ATHRConfig.feature != null
                && ATHRConfig.feature.chat != null
                && ATHRConfig.feature.chat.chatTabs != null
                ? ATHRConfig.feature.chat.chatTabs.unreadBadge
                : null;
    }

    public static void refreshView() {
        int signature = viewSignature();
        if (signature == lastSignature) return;
        if (MinecraftCompat.getMinecraft() == null) return;
        lastSignature = signature;
        ChatHudCompat.rebuildChatView();
    }

    private static int viewSignature() {
        ChatTabType current = effectiveTab();
        int signature = current.ordinal();
        ChatTabsConfig cfg = ATHRConfig.feature == null || ATHRConfig.feature.chat == null
                ? null
                : ATHRConfig.feature.chat.chatTabs;
        if (cfg == null) return signature;
        signature |= (cfg.enabled ? 1 : 0) << 3;
        signature |= (cfg.partyEnabled ? 1 : 0) << 4;
        signature |= (cfg.playerEnabled ? 1 : 0) << 5;
        signature |= (cfg.msgEnabled ? 1 : 0) << 6;
        signature |= (cfg.guildEnabled ? 1 : 0) << 7;
        signature |= (cfg.coopEnabled ? 1 : 0) << 8;
        return signature;
    }

    public static boolean lineMatchesTab(MessageType type, ChatTabType tab) {
        if (tab == null || tab == ChatTabType.ALL) return true;
        if (!enabled(tab)) return false;
        return ChatTabType.owner(type) == tab;
    }
}
