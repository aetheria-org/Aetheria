
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPageContent(int mouseX, int mouseY, int slide) {
        Page cur = PAGES[page];
        int cx = px + PANEL_W / 2 + slide;

        drawCenteredString(fontRendererObj, "§lAetheria — Network & Privacy", cx, py + 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "§7You can change these any time in Settings → Network.", cx, py + 22, 0x888888);
        Gui.drawRect(px + 20, py + 34, px + PANEL_W - 20, py + 35, 0x33FFFFFF);

        drawCenteredString(fontRendererObj, "§e" + cur.title, cx, py + 48, 0xFFFFFF);
        drawWrapped(cur.what, cx, py + 68,  PANEL_W - 60, 0xCCCCCC);
        drawWrapped(cur.why,  cx, py + 110, PANEL_W - 60, 0x888888);
        drawToggle(mouseX, mouseY, slide, cur);
    }

    private void drawToggle(int mouseX, int mouseY, int slide, Page cur) {
        boolean disabled = getValue(cur);
        int tx = togX() + slide;
        int ty = togY();
        boolean hov = inBox(mouseX, mouseY, togX(), ty, TOG_W, TOG_H);

        RenderUtils.drawFloatingRectDark(tx, ty, TOG_W, TOG_H, false);
        Gui.drawRect(tx, ty, tx + TOG_W, ty + TOG_H, hov ? (disabled ? 0x22FF4444 : 0x2244FF44) : 0);
        Gui.drawRect(tx, ty, tx + 4, ty + TOG_H, disabled ? 0xFFBB3333 : 0xFF33BB55);
        drawCenteredString(fontRendererObj, disabled ? "§c✗  DISABLED" : "§a✔  ENABLED",
                tx + TOG_W / 2, ty + (TOG_H - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFF);
    }

    private void drawNavigation(int mouseX, int mouseY) {
        Page cur = PAGES[page];
        int ny = navY();

        drawCenteredString(fontRendererObj, "§7" + (page + 1) + " / " + PAGES.length,
                px + PANEL_W / 2, ny - 14, 0x666666);

        if (page > 0) {
            boolean hb = inBox(mouseX, mouseY, backX(), ny, NAV_W, NAV_H);
            drawNavBtn(backX(), ny, "§7◄ Back", hb ? 0x00C8C8 : 0xAAAAAA, hb);
        }

        boolean hn = inBox(mouseX, mouseY, nextX(), ny, NAV_W, NAV_H);
        String label;
        int color;
        if (isLastPage()) {
            label = "Confirm §a►"; color = hn ? 0x55FF55 : 0xAAAAAA;
        } else if (firstLaunch && !cur.touched) {
            label = "Accept §7►"; color = hn ? 0x00C8C8 : 0x888888;
        } else {
            label = "Next §7►"; color = hn ? 0x00C8C8 : 0xAAAAAA;
        }
        drawNavBtn(nextX(), ny, label, color, hn);

        if (firstLaunch && !cur.touched) {
            drawCenteredString(fontRendererObj, "§8Continuing will keep this enabled",
                    px + PANEL_W / 2, ny + NAV_H + 4, 0x555555);
        }

        if (firstLaunch) {
            boolean hs = inBox(mouseX, mouseY, skipX(), skipY(), NAV_W, NAV_H);
            drawNavBtn(skipX(), skipY(), "§7Skip", hs ? 0xAAAAAA : 0x666666, hs);
        }
    }

    private void drawNavBtn(int x, int y, String label, int color, boolean hovered) {
        RenderUtils.drawFloatingRectDark(x, y, NAV_W, NAV_H, false);
        if (hovered) Gui.drawRect(x, y, x + NAV_W, y + NAV_H, 0x18FFFFFF);
        drawCenteredString(fontRendererObj, label, x + NAV_W / 2, y + (NAV_H - fontRendererObj.FONT_HEIGHT) / 2, color);
    }

    private void drawWrapped(String text, int cx, int y, int maxW, int color) {
        for (String line : text.split("\n")) {
            StringBuilder seg = new StringBuilder();
            for (String word : line.split(" ")) {
                String test = seg.length() == 0 ? word : seg + " " + word;
                if (fontRendererObj.getStringWidth(test) > maxW) {
                    drawCenteredString(fontRendererObj, seg.toString(), cx, y, color);
                    y += fontRendererObj.FONT_HEIGHT + 2;
                    seg = new StringBuilder(word);
                } else {
                    seg = new StringBuilder(test);
                }
            }
            if (seg.length() > 0) {
                drawCenteredString(fontRendererObj, seg.toString(), cx, y, color);
                y += fontRendererObj.FONT_HEIGHT + 2;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (animOffset != 0) return;

        Page cur = PAGES[page];

        if (inBox(mouseX, mouseY, togX(), togY(), TOG_W, TOG_H)) {
            setValue(cur, !getValue(cur));
            cur.touched = true;
            return;
        }

        int ny = navY();

        if (page > 0 && inBox(mouseX, mouseY, backX(), ny, NAV_W, NAV_H)) {
            navigateTo(page - 1, 1);
            return;
        }

        if (inBox(mouseX, mouseY, nextX(), ny, NAV_W, NAV_H)) {
            if (firstLaunch && !cur.touched) setValue(cur, false);
            if (isLastPage()) confirm();
            else navigateTo(page + 1, -1);
            return;
        }

        if (firstLaunch && inBox(mouseX, mouseY, skipX(), skipY(), NAV_W, NAV_H)) {
            confirm();
        }
    }

    private void navigateTo(int target, int dir) {
        page = target;
        animOffset = dir * PANEL_W * 0.35f;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) return;
        super.keyTyped(typedChar, keyCode);
    }

    private void confirm() {
        if (ATHRConfig.feature != null) {
            ATHRConfig.feature.network.hasSeenPrivacyNotice = true;
            ATHRConfig.saveConfig();
        }
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private boolean inBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}