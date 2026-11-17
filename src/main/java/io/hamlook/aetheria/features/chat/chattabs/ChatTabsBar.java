package io.hamlook.aetheria.features.chat.chattabs;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.ChatTabsConfig;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChatTabsBar {

    public static final int BOX_HEIGHT = 12;
    public static final int BAR_TOP_OFFSET = 27;
    static final int X0 = 2;
    static final int UNDERLINE_ROW = 11;
    static final int PAD = 4;
    private static final int GAP = 2;

    private ChatTabsBar() {
    }

    public static boolean isVisible() {
        if (config() == null) return false;
        for (ChatTabType tab : ChatTabType.values()) {
            if (tab != ChatTabType.ALL && ChatTabs.enabled(tab)) return true;
        }
        return false;
    }

    public static Layout computeLayout(int screenWidth, int screenHeight) {
        return layout(screenWidth, screenHeight, null, -1);
    }

    public static Layout dragLayout(int screenWidth, int screenHeight, ChatTabType held, int mouseX) {
        if (held == null) return computeLayout(screenWidth, screenHeight);
        FontRenderer fr = MinecraftCompat.getFontRenderer();
        int heldWidth = tabWidth(held, fr);
        Layout others = layout(screenWidth, screenHeight, held, -1);
        int center = heldTabX(heldWidth, mouseX, screenWidth) + heldWidth / 2;
        int dropIndex = insertionIndex(center, others.entries);
        return layout(screenWidth, screenHeight, held, dropIndex);
    }

    public static void commitDrop(ChatTabType held, int mouseX, int screenWidth, int screenHeight) {
        ChatTabsConfig cfg = config();
        if (cfg == null || held == null) return;

        Layout others = layout(screenWidth, screenHeight, held, -1);
        int heldWidth = tabWidth(held, MinecraftCompat.getFontRenderer());
        int center = heldTabX(heldWidth, mouseX, screenWidth) + heldWidth / 2;
        int dropIndex = insertionIndex(center, others.entries);

        List<ChatTabType> merged = new ArrayList<>();
        for (ChatTabType tab : orderedTabs()) {
            if (tab == held) continue;
            if (ChatTabs.hideEmptyTabs()
                    && tab != ChatTabType.ALL
                    && ChatTabs.hasNoMessages(tab)) continue;
            if (ChatTabs.enabled(tab)) merged.add(tab);
        }
        merged.add(Math.min(Math.max(dropIndex, 0), merged.size()), held);

        Set<ChatTabType> ordered = new HashSet<>(merged);
        List<String> newOrder = new ArrayList<>(ChatTabType.values().length);
        int next = 0;
        for (ChatTabType tab : orderedTabs()) {
            if (ordered.contains(tab)) {
                newOrder.add(merged.get(next++).name());
            } else {
                newOrder.add(tab.name());
            }
        }
        cfg.tabOrder = newOrder;
        ATHRConfig.saveConfig();
    }

    public static List<ChatTabType> orderedTabs() {
        List<ChatTabType> result = new ArrayList<>();
        ChatTabsConfig cfg = config();
        if (cfg != null && cfg.tabOrder != null) {
            Set<String> seen = new HashSet<>();
            for (String name : cfg.tabOrder) {
                if (name == null) continue;
                String key = name.trim().toUpperCase(Locale.ROOT);
                if (!seen.add(key)) continue;
                try {
                    result.add(ChatTabType.valueOf(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        for (ChatTabType tab : ChatTabType.values()) {
            if (result.contains(tab)) continue;
            result.add(tab);
        }
        return result;
    }

    public static int insertionIndex(int heldCenterX, List<Entry> others) {
        int passed = 0;
        for (Entry entry : others) {
            if (heldCenterX >= entry.x + entry.w / 2) passed++;
        }
        return Math.min(passed, others.size());
    }

    public static int heldTabX(int heldWidth, int mouseX, int screenWidth) {
        int x = mouseX - heldWidth / 2;
        int maxX = Math.max(X0, screenWidth - 2 - heldWidth);
        return Math.min(Math.max(x, X0), maxX);
    }

    public static ChatTabsConfig config() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.chat.chatTabs;
    }

    static int tabWidth(ChatTabType tab, FontRenderer fr) {
        return DrawContextUtils.getStringWidth(fr, tab.label) + 2 * PAD + badgeExtraWidth(tab, fr);
    }

    static int badgeExtraWidth(ChatTabType tab, FontRenderer fr) {
        if (tab == ChatTabType.ALL || !ChatTabs.unreadEnabled()) return 0;
        String text = ChatTabs.badgeText(tab);
        if (text == null) return 0;
        if (ChatTabs.dotBadges()) return 6;
        return Math.max(DrawContextUtils.getStringWidth(fr, text) + 4, 10) + 2;
    }

    private static Layout layout(int screenWidth, int screenHeight, ChatTabType held, int dropIndex) {
        ChatTabsConfig cfg = config();
        int y = screenHeight - BAR_TOP_OFFSET;
        List<ChatTabType> tabs = new ArrayList<>();
        if (cfg == null) return new Layout(new ArrayList<>(), y, -1, 0);

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        boolean hideEmpty = ChatTabs.hideEmptyTabs();
        for (ChatTabType tab : orderedTabs()) {
            if (tab == held) continue;
            if (hideEmpty && tab != ChatTabType.ALL && ChatTabs.hasNoMessages(tab)) continue;
            if (ChatTabs.enabled(tab)) tabs.add(tab);
        }

        List<Integer> widths = new ArrayList<>(tabs.size());
        int contentWidth = 0;
        for (ChatTabType tab : tabs) {
            int width = tabWidth(tab, fr);
            widths.add(width);
            contentWidth += width;
        }

        int insert = -1;
        int ghostWidth = 0;
        if (held == null) {
            int limit = Math.max(0, screenWidth - 4);
            while (tabs.size() > 1 && contentWidth + GAP * (tabs.size() - 1) > limit) {
                contentWidth -= widths.remove(widths.size() - 1);
                tabs.remove(tabs.size() - 1);
            }
        } else {
            ghostWidth = tabWidth(held, fr);
            contentWidth += ghostWidth;
            insert = Math.min(Math.max(dropIndex, 0), tabs.size());
        }

        int slots = tabs.size() + (insert >= 0 ? 1 : 0);
        int totalWidth = contentWidth + GAP * Math.max(0, slots - 1);
        int x = startX(cfg, screenWidth, totalWidth);

        List<Entry> entries = new ArrayList<>(tabs.size());
        int ghostX = -1;
        int otherIndex = 0;
        for (int slot = 0; slot < slots; slot++) {
            if (insert >= 0 && slot == insert) {
                ghostX = x;
                x += ghostWidth + GAP;
                continue;
            }
            entries.add(new Entry(tabs.get(otherIndex), x, widths.get(otherIndex)));
            x += widths.get(otherIndex) + GAP;
            otherIndex++;
        }
        return new Layout(entries, y, ghostX, ghostWidth);
    }

    private static int startX(ChatTabsConfig cfg, int screenWidth, int totalWidth) {
        switch (clampAlignment(cfg.barAlignment)) {
            case 1:
                return Math.max(X0, (screenWidth - totalWidth) / 2);
            case 2:
                return Math.max(X0, screenWidth - 2 - totalWidth);
            default:
                return X0;
        }
    }

    private static int clampAlignment(int alignment) {
        return alignment < 0 || alignment > 2 ? 0 : alignment;
    }

    public static final class Layout {
        public final List<Entry> entries;
        public final int y;
        public final int ghostX;
        public final int ghostWidth;

        Layout(List<Entry> entries, int y, int ghostX, int ghostWidth) {
            this.entries = entries;
            this.y = y;
            this.ghostX = ghostX;
            this.ghostWidth = ghostWidth;
        }
    }

    public static final class Entry {
        public final ChatTabType tab;
        public final int x;
        public final int w;

        Entry(ChatTabType tab, int x, int w) {
            this.tab = tab;
            this.x = x;
            this.w = w;
        }
    }
}
