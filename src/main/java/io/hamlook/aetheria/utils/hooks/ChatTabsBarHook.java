package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.features.chat.ChatTabsConfig;
import io.hamlook.aetheria.features.chat.chattabs.ChatTabType;
import io.hamlook.aetheria.features.chat.chattabs.ChatTabs;
import io.hamlook.aetheria.features.chat.chattabs.ChatTabsBar;
import io.hamlook.aetheria.features.chat.chattabs.ChatTabsBarRenderer;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MouseCompat;

public final class ChatTabsBarHook {

    private static final int DRAG_SLOP = 3;

    private static ChatTabType pressTab;
    private static int pressX;
    private static int pressY;
    private static boolean dragging;

    private static ChatTabType hoverTab;
    private static long hoverSinceMs;
    private static boolean previewUnlocked;

    private ChatTabsBarHook() {
    }

    public static void draw(int mouseX, int mouseY) {
        pollRelease(mouseX, mouseY);
        ChatTabsConfig cfg = ChatTabsBar.config();
        ChatTabsBar.Layout layout = ChatTabsBar.computeLayout(
                GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight());
        updatePreview(mouseX, mouseY, layout, cfg);
        ChatTabsBarRenderer.draw(mouseX, mouseY, layout);
    }

    public static boolean onClick(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || !ChatTabsBar.isVisible()) return false;

        int screenWidth = GuiScreenUtils.getScaledWindowWidth();
        int screenHeight = GuiScreenUtils.getScaledWindowHeight();
        ChatTabsBar.Layout layout = ChatTabsBar.computeLayout(screenWidth, screenHeight);
        if (mouseY < layout.y || mouseY >= layout.y + ChatTabsBar.BOX_HEIGHT) return false;

        for (ChatTabsBar.Entry entry : layout.entries) {
            if (mouseX >= entry.x && mouseX < entry.x + entry.w) {
                pressTab = entry.tab;
                pressX = mouseX;
                pressY = mouseY;
                dragging = false;
                clearPreview();
                return true;
            }
        }
        return false;
    }

    public static ChatTabType draggedTab() {
        return dragging ? pressTab : null;
    }

    public static boolean dragActive() {
        return pressTab != null;
    }

    public static void cancelDrag() {
        pressTab = null;
        dragging = false;
        clearPreview();
    }

    public static void enforceActiveTabFitsOverflow() {
        if (!ChatTabsBar.isVisible()) return;

        ChatTabType current = ChatTabs.activeTab();
        if (current == ChatTabType.ALL) return;

        int screenWidth = GuiScreenUtils.getScaledWindowWidth();
        int screenHeight = GuiScreenUtils.getScaledWindowHeight();
        ChatTabsBar.Layout layout = ChatTabsBar.computeLayout(screenWidth, screenHeight);
        for (ChatTabsBar.Entry entry : layout.entries) {
            if (entry.tab == current) return;
        }
        ChatTabs.activate(ChatTabType.ALL);
    }

    private static void updatePreview(int mouseX, int mouseY, ChatTabsBar.Layout layout, ChatTabsConfig cfg) {
        if (cfg == null || !cfg.hoverPreview.enabled || pressTab != null) {
            clearPreview();
            return;
        }
        boolean inBar = !layout.entries.isEmpty()
                && mouseY >= layout.y && mouseY < layout.y + ChatTabsBar.BOX_HEIGHT
                && mouseX >= layout.entries.get(0).x
                && mouseX < layout.entries.get(layout.entries.size() - 1).x
                        + layout.entries.get(layout.entries.size() - 1).w;
        if (!inBar) {
            clearPreview();
            return;
        }
        dropHiddenPreview(layout);

        ChatTabType hover = hoverTab(layout, mouseX);
        long now = System.currentTimeMillis();
        if (hover != null && hover != hoverTab) {
            hoverTab = hover;
            hoverSinceMs = now;
        }
        if (hover != null && (previewUnlocked || now - hoverSinceMs >= cfg.hoverPreview.delayMs)) {
            ChatTabs.preview(hover);
            previewUnlocked = true;
        }
    }

    private static ChatTabType hoverTab(ChatTabsBar.Layout layout, int mouseX) {
        for (ChatTabsBar.Entry entry : layout.entries) {
            if (mouseX >= entry.x && mouseX < entry.x + entry.w) return entry.tab;
        }
        return null;
    }

    private static void dropHiddenPreview(ChatTabsBar.Layout layout) {
        ChatTabType preview = ChatTabs.previewTab();
        if (preview == null) return;
        if (!layout.entries.isEmpty()) {
            for (ChatTabsBar.Entry entry : layout.entries) {
                if (entry.tab == preview) return;
            }
        }
        ChatTabs.preview(null);
    }

    private static void clearPreview() {
        ChatTabs.preview(null);
        hoverTab = null;
        hoverSinceMs = 0;
        previewUnlocked = false;
    }

    public static void pollRelease(int mouseX, int mouseY) {
        if (pressTab == null) return;
        if (MouseCompat.isButtonDown(0)) {
            if (!dragging) {
                int dx = mouseX - pressX;
                int dy = mouseY - pressY;
                if (dx * dx + dy * dy >= DRAG_SLOP * DRAG_SLOP) {
                    dragging = true;
                }
            }
            return;
        }
        ChatTabType released = pressTab;
        boolean wasDragging = dragging;
        pressTab = null;
        dragging = false;
        if (!wasDragging) {
            ChatTabs.activate(released);
            return;
        }
        int screenWidth = GuiScreenUtils.getScaledWindowWidth();
        int screenHeight = GuiScreenUtils.getScaledWindowHeight();
        ChatTabsBar.commitDrop(released, mouseX, screenWidth, screenHeight);
    }
}
