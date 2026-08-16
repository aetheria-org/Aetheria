package io.hamlook.aetheria.network;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

public class PrivacyNoticeScreen extends NetworkNoticeScreen {

    private static final int TOG_W = 160;
    private static final int TOG_H = 28;

    private static final Page[] PAGES = {new Page("Telemetry", "Sends your username and mod version when joining a server.", "Used for player counts and crash reports.\nNo gameplay data is collected.", "disableTelemetry"), new Page("Mod List in Telemetry", "Also includes your installed mod list alongside telemetry.", "Only sent when Telemetry is enabled above.\nUseful for crash triage and compatibility reports.", "disableModListInTelemetry"), new Page("API Calls", "Communicates with the mod API for capes, profile viewer,\nprofile parser, and the sync command.", "Disabling this will break those features entirely.", "disableApiCalls"), new Page("GitHub Calls", "Fetches repo data from GitHub used by overlays, timers,\nversion checks, and most other features.", "Disabling this will break the majority of the mod.", "disableGithubCalls"), new Page("Smart Connection", "Global Chat and Diana Party only connect while you're using them, and disconnect after 10 minutes of inactivity.", "Reduces background connections and network usage when you're not chatting. Off by default.", "smartSocketLifecycle", true),};

    private static final Page[] SOCKET_ONLY_PAGES = {PAGES[PAGES.length - 1]};
    private final GuiScreen parent;
    private final Page[] pages;
    private final boolean firstLaunch;
    private int page;
    public PrivacyNoticeScreen(GuiScreen parent) {
        this(parent, false);
    }

    public PrivacyNoticeScreen(GuiScreen parent, boolean socketOnly) {
        this.parent = parent;
        this.pages = socketOnly ? SOCKET_ONLY_PAGES : PAGES;
        this.firstLaunch = ATHRConfig.feature != null && (socketOnly ? !ATHRConfig.feature.network.hasSeenSocketLifecycleNotice : !ATHRConfig.feature.network.hasSeenPrivacyNotice);
        for (Page p : pages) p.touched = false;
    }

    private boolean getValue(Page p) {
        if (ATHRConfig.feature == null) return false;
        switch (p.field) {
            case "disableTelemetry":
                return ATHRConfig.feature.network.disableTelemetry;
            case "disableModListInTelemetry":
                return ATHRConfig.feature.network.disableModListInTelemetry;
            case "disableApiCalls":
                return ATHRConfig.feature.network.disableApiCalls;
            case "disableGithubCalls":
                return ATHRConfig.feature.network.disableGithubCalls;
            case "smartSocketLifecycle":
                return ATHRConfig.feature.network.smartSocketLifecycle;
            default:
                return false;
        }
    }

    private void setValue(Page p, boolean value) {
        if (ATHRConfig.feature == null) return;
        switch (p.field) {
            case "disableTelemetry":
                ATHRConfig.feature.network.disableTelemetry = value;
                break;
            case "disableModListInTelemetry":
                ATHRConfig.feature.network.disableModListInTelemetry = value;
                break;
            case "disableApiCalls":
                ATHRConfig.feature.network.disableApiCalls = value;
                break;
            case "disableGithubCalls":
                ATHRConfig.feature.network.disableGithubCalls = value;
                break;
            case "smartSocketLifecycle":
                ATHRConfig.feature.network.smartSocketLifecycle = value;
                break;
        }
    }

    private boolean isLastPage() {
        return page == pages.length - 1;
    }

    private int togX() {
        return px + PANEL_W / 2 - TOG_W / 2;
    }

    private int togY() {
        return py + PANEL_H / 2 + 14;
    }

    private int backX() {
        return px + NAV_PAD;
    }

    private int nextX() {
        return px + PANEL_W - NAV_PAD - NAV_W;
    }

    private int skipX() {
        return px + PANEL_W / 2 - NAV_W / 2;
    }

    private int skipY() {
        return navY();
    }

    @Override
    protected void drawPanelBackground() {
        RenderUtils.drawFloatingRectDark(px, py, PANEL_W, PANEL_H, false);
    }

    @Override
    protected void drawPageContent(int mouseX, int mouseY, int slide) {
        Page cur = pages[page];
        int cx = px + PANEL_W / 2 + slide;

        drawCenteredString(fontRendererObj, "§lAetheria — Network & Privacy", cx, py + 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "§7You can change these any time in Settings → Network.", cx, py + 22, 0x888888);
        Gui.drawRect(px + 20, py + 34, px + PANEL_W - 20, py + 35, 0x33FFFFFF);

        drawCenteredString(fontRendererObj, "§e" + cur.title, cx, py + 48, 0xFFFFFF);
        drawWrapped(cur.what, cx, py + 68, 0xCCCCCC);
        drawWrapped(cur.why, cx, py + 110, 0x888888);
        drawToggle(mouseX, mouseY, slide, cur);
    }

    private void drawToggle(int mouseX, int mouseY, int slide, Page cur) {
        boolean enabled = cur.isEnableToggle == getValue(cur);
        int tx = togX() + slide;
        int ty = togY();
        boolean hov = inBox(mouseX, mouseY, togX(), ty, TOG_W, TOG_H);

        RenderUtils.drawFloatingRectDark(tx, ty, TOG_W, TOG_H, false);
        Gui.drawRect(tx, ty, tx + TOG_W, ty + TOG_H, hov ? (enabled ? 0x2244FF44 : 0x22FF4444) : 0);
        Gui.drawRect(tx, ty, tx + 4, ty + TOG_H, enabled ? 0xFF33BB55 : 0xFFBB3333);
        drawCenteredString(fontRendererObj, enabled ? "§a✔  ENABLED" : "§c✗  DISABLED", tx + TOG_W / 2, ty + (TOG_H - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFF);
    }

    @Override
    protected void drawNavigation(int mouseX, int mouseY) {
        Page cur = pages[page];
        int ny = navY();

        drawCenteredString(fontRendererObj, "§7" + (page + 1) + " / " + pages.length, px + PANEL_W / 2, ny - 14, 0x666666);

        if (page > 0) {
            boolean hb = inBox(mouseX, mouseY, backX(), ny, NAV_W, NAV_H);
            drawNavBtn(backX(), ny, "§7◄ Back", hb ? 0x00C8C8 : 0xAAAAAA, hb);
        }

        boolean hn = inBox(mouseX, mouseY, nextX(), ny, NAV_W, NAV_H);
        String label;
        int color;
        if (isLastPage()) {
            label = "Confirm §a►";
            color = hn ? 0x55FF55 : 0xAAAAAA;
        } else if (firstLaunch && !cur.touched) {
            label = "Accept §7►";
            color = hn ? 0x00C8C8 : 0x888888;
        } else {
            label = "Next §7►";
            color = hn ? 0x00C8C8 : 0xAAAAAA;
        }
        drawNavBtn(nextX(), ny, label, color, hn);

        if (firstLaunch && !cur.touched) {
            drawCenteredString(fontRendererObj, "§8Continuing will keep this enabled", px + PANEL_W / 2, ny + NAV_H + 4, 0x555555);
        }

        if (firstLaunch) {
            boolean hs = inBox(mouseX, mouseY, skipX(), skipY(), NAV_W, NAV_H);
            drawNavBtn(skipX(), skipY(), "§7Skip", hs ? 0xAAAAAA : 0x666666, hs);
        }
    }

    @Override
    protected boolean handleClick(int mouseX, int mouseY, int mouseButton) {
        Page cur = pages[page];

        if (inBox(mouseX, mouseY, togX(), togY(), TOG_W, TOG_H)) {
            setValue(cur, !getValue(cur));
            cur.touched = true;
            return true;
        }

        int ny = navY();

        if (page > 0 && inBox(mouseX, mouseY, backX(), ny, NAV_W, NAV_H)) {
            navigateTo(page - 1, 1);
            return true;
        }

        if (inBox(mouseX, mouseY, nextX(), ny, NAV_W, NAV_H)) {
            if (firstLaunch && !cur.touched) setValue(cur, false);
            if (isLastPage()) confirm();
            else navigateTo(page + 1, -1);
            return true;
        }

        if (firstLaunch && inBox(mouseX, mouseY, skipX(), skipY(), NAV_W, NAV_H)) {
            confirm();
            return true;
        }

        return false;
    }

    private void navigateTo(int target, int dir) {
        page = target;
        animOffset = dir * PANEL_W * 0.35f;
    }

    private void confirm() {
        if (ATHRConfig.feature != null) {
            ATHRConfig.feature.network.hasSeenPrivacyNotice = true;
            ATHRConfig.feature.network.hasSeenSocketLifecycleNotice = true;
            ATHRConfig.saveConfig();
        }
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private static class Page {
        final String title, what, why, field;
        final boolean isEnableToggle;
        boolean touched;

        Page(String title, String what, String why, String field) {
            this(title, what, why, field, false);
        }

        Page(String title, String what, String why, String field, boolean isEnableToggle) {
            this.title = title;
            this.what = what;
            this.why = why;
            this.field = field;
            this.isEnableToggle = isEnableToggle;
        }
    }
}