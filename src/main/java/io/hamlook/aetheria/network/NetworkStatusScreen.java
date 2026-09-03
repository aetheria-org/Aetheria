package io.hamlook.aetheria.network;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.Gui;

import java.util.*;

public class NetworkStatusScreen extends NetworkNoticeScreen {

    private static final int PAD = 14;
    private static final int ROW_H = 52;
    private static final int ROW_GAP = 8;
    private static final int BTN_W = 110;
    private static final int BTN_H = 28;
    private static final int SETTINGS_W = 100;
    private static final int DONE_W = 130;

    private final List<NetworkStatusInfo.GateInfo> sections = new ArrayList<>();
    private final Map<NetworkStatusInfo.Gate, Boolean> read = new EnumMap<>(NetworkStatusInfo.Gate.class);

    public NetworkStatusScreen() {
        refreshSections();
    }

    private void refreshSections() {
        sections.clear();
        sections.addAll(NetworkStatusInfo.blockedGates());
    }

    private boolean allRead() {
        for (NetworkStatusInfo.GateInfo info : sections) {
            if (!Boolean.TRUE.equals(read.get(info.gate))) return false;
        }
        return true;
    }

    private int rowY(int index) {
        return py + 46 + index * (ROW_H + ROW_GAP);
    }

    private int rowW() {
        return PANEL_W - PAD * 2;
    }

    private int buttonX() {
        return px + PANEL_W - PAD - BTN_W;
    }

    private int buttonY(int index) {
        return rowY(index) + (ROW_H - BTN_H) / 2;
    }

    private boolean inButton(int mx, int my, int index) {
        return inBox(mx, my, buttonX(), buttonY(index), BTN_W, BTN_H);
    }

    private boolean inRow(int mx, int my, int index) {
        return inBox(mx, my, px + PAD, rowY(index), rowW(), ROW_H);
    }

    @Override
    protected void drawPanelBackground() {
        NineSliceUtils.draw(Resources.storageBackground(1), px, py, PANEL_W, PANEL_H, 6, 18);
    }

    @Override
    protected void onKeyTyped(char typedChar, int keyCode) {
    }

    @Override
    protected void drawPageContent(int mouseX, int mouseY, int slide) {
        drawCenteredString(fontRendererObj, "§lAetheria — Network Status", px + PANEL_W / 2, py + 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "§7Some features are disabled by your network settings.", px + PANEL_W / 2, py + 22, 0x888888);
        Gui.drawRect(px + 20, py + 34, px + PANEL_W - 20, py + 35, 0x33FFFFFF);

        for (int i = 0; i < sections.size(); i++) {
            drawSection(mouseX, mouseY, sections.get(i), i);
        }
    }

    private void drawSection(int mouseX, int mouseY, NetworkStatusInfo.GateInfo info, int index) {
        int x = px + PAD;
        int y = rowY(index);
        int w = rowW();
        int btnX = buttonX();

        if (inRow(mouseX, mouseY, index)) {
            read.put(info.gate, true);
            if (!inButton(mouseX, mouseY, index)) Gui.drawRect(x, y, x + w, y + ROW_H, 0x14FFFFFF);
        }

        String line1;
        String line2;
        if (info.gate == NetworkStatusInfo.Gate.OFFLINE) {
            line1 = "§c" + NetworkStatusInfo.whyText(info.gate);
            line2 = "§7All network features are disabled.";
        } else {
            line1 = "§c" + info.headline + " + " + info.countOther() + " other features won't work";
            line2 = "§7while " + info.settingName + " are off.";
        }

        fontRendererObj.drawString(trimToWidth(line1, btnX - x - 16), x + 8, y + 6, 0xFFFFFF, true);

        String hint = "§7Hover ▸ list";
        int hintX = btnX - fontRendererObj.getStringWidth(hint) - 8;
        fontRendererObj.drawString(trimToWidth(line2, hintX - x - 16), x + 8, y + 20, 0xCCCCCC, true);
        fontRendererObj.drawString(hint, hintX, y + 20, 0x888888, true);

        drawButton(btnX, buttonY(index), BTN_W, BTN_H, "§aTurn on " + NetworkStatusInfo.enableLabel(info.gate), 0x55FF55, inButton(mouseX, mouseY, index));
    }

    private String trimToWidth(String text, int maxW) {
        if (fontRendererObj.getStringWidth(text) <= maxW) return text;
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() > 0 && fontRendererObj.getStringWidth(sb.toString()) > maxW) {
            sb.deleteCharAt(sb.length() - 1);
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '§') sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void drawButton(int x, int y, int w, int h, String label, int color, boolean hovered) {
        RenderUtils.drawFloatingRectDark(x, y, w, h, false);
        if (hovered) Gui.drawRect(x, y, x + w, y + h, 0x18FFFFFF);
        drawCenteredString(fontRendererObj, label, x + w / 2, y + (h - fontRendererObj.FONT_HEIGHT) / 2, color);
    }

    @Override
    protected void drawTooltips(int mouseX, int mouseY) {
        for (int i = 0; i < sections.size(); i++) {
            NetworkStatusInfo.GateInfo info = sections.get(i);
            if (inRow(mouseX, mouseY, i) && !inButton(mouseX, mouseY, i)) {
                TextRenderUtils.drawHoveringText(Arrays.asList(info.affected), mouseX, mouseY, fontRendererObj);
                return;
            }
        }
    }

    @Override
    protected void drawNavigation(int mouseX, int mouseY) {
        int ny = navY();

        boolean settingsHover = inBox(mouseX, mouseY, px + NAV_PAD, ny, SETTINGS_W, NAV_H);
        drawButton(px + NAV_PAD, ny, SETTINGS_W, NAV_H, "§bOpen Settings", settingsHover ? 0x00C8C8 : 0xAAAAAA, settingsHover);

        int doneX = px + PANEL_W - NAV_PAD - DONE_W;
        if (!allRead()) {
            drawButton(doneX, ny, DONE_W, NAV_H, "§7Done", 0x666666, false);
            drawCenteredString(fontRendererObj, "§7Hover every section to enable.", px + PANEL_W / 2, ny + NAV_H + 4, 0x555555);
        } else {
            boolean doneHover = inBox(mouseX, mouseY, doneX, ny, DONE_W, NAV_H);
            drawButton(doneX, ny, DONE_W, NAV_H, "§aDone §8— Don't show again", doneHover ? 0x55FF55 : 0xAAAAAA, doneHover);
        }
    }

    @Override
    protected boolean handleClick(int mouseX, int mouseY, int mouseButton) {
        for (int i = 0; i < sections.size(); i++) {
            if (inButton(mouseX, mouseY, i)) {
                turnOn(sections.get(i).gate);
                return true;
            }
        }

        if (inBox(mouseX, mouseY, px + NAV_PAD, navY(), SETTINGS_W, NAV_H)) {
            ATHRConfig.openCategory("Network & Privacy");
            return true;
        }

        int doneX = px + PANEL_W - NAV_PAD - DONE_W;
        if (allRead() && inBox(mouseX, mouseY, doneX, navY(), DONE_W, NAV_H)) {
            if (ATHRConfig.feature != null) {
                ATHRConfig.feature.network.networkStatusAckMask = NetworkStatusInfo.ackMaskFor(NetworkStatusInfo.currentMask());
                ATHRConfig.saveConfig();
            }
            MinecraftCompat.getMinecraft().displayGuiScreen(null);
            return true;
        }

        return false;
    }

    private void turnOn(NetworkStatusInfo.Gate gate) {
        NetworkGuard.enableGate(gate);
        read.remove(gate);
        refreshSections();
        if (sections.isEmpty()) {
            MinecraftCompat.getMinecraft().displayGuiScreen(null);
        }
    }
}