package io.hamlook.aetheria.features.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Type;
import java.util.*;

public class WaypointGroupGui extends GuiElement {

    private static final int MAX_W = 440;
    private static final int MAX_H = 360;
    private static final int PAD = 10;
    private static final int TITLE_H = 20;
    private static final int ROW_H = 24;
    private static final int ROW_PAD = 4;
    private static final int WP_H = 18;
    private static final int WP_PAD = 2;
    private static final int SF_H = 16;
    private static final int BTN_W = 42;
    private static final int BTN_H = 14;
    private static final int SBTN_W = 14;
    private static final int SBTN_H = 12;
    private static final int INDENT = 16;

    private final Set<String> expandedGroups = new HashSet<>();
    private int scrollOffset = 0;

    private GuiTextField searchField;
    private GuiTextField importField;
    private GuiTextField createField;
    private boolean importOpen = false;
    private boolean createOpen = false;

    private int pw, ph, px, py;

    static String exportSoopy(WaypointGroup g) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WaypointPoint wp : g.waypoints) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", wp.x);
            m.put("y", wp.y);
            m.put("z", wp.z);
            m.put("r", 0);
            m.put("g", 1);
            m.put("b", 0);
            Map<String, Object> opts = new LinkedHashMap<>();
            opts.put("name", wp.name != null ? wp.name : "");
            m.put("options", opts);
            list.add(m);
        }
        return new GsonBuilder().create().toJson(list);
    }

    static List<WaypointPoint> parseSoopy(String json) {
        try {
            if (json.startsWith("[")) {
                Type type = new TypeToken<List<Map<String, Object>>>() {
                }.getType();
                List<Map<String, Object>> raw = new Gson().fromJson(json, type);
                List<WaypointPoint> wps = new ArrayList<>();
                for (int i = 0; i < raw.size(); i++) {
                    Map<String, Object> m = raw.get(i);
                    double x = toD(m.get("x")), y = toD(m.get("y")), z = toD(m.get("z"));
                    String name = String.valueOf(i + 1);
                    if (m.containsKey("options")) {
                        @SuppressWarnings("unchecked") Map<String, Object> opts = (Map<String, Object>) m.get("options");
                        if (opts != null && opts.containsKey("name")) name = String.valueOf(opts.get("name"));
                    }
                    wps.add(new WaypointPoint(x, y, z, name));
                }
                wps.sort((a, b) -> {
                    try {
                        return Integer.compare(Integer.parseInt(a.name), Integer.parseInt(b.name));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
                return wps;
            }
            if (json.matches("(?s).*\\d.*")) {
                List<WaypointPoint> wps = new ArrayList<>();
                String[] rows = json.split("[\\r\\n]+");
                for (int i = 0; i < rows.length; i++) {
                    String[] parts = rows[i].trim().split("\\s+");
                    if (parts.length >= 3)
                        wps.add(new WaypointPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), String.valueOf(i + 1)));
                }
                return wps.isEmpty() ? null : wps;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static double toD(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }

    private void updatePanel(ScaledResolution sr) {
        pw = Math.min(MAX_W, sr.getScaledWidth() - PAD * 2);
        ph = Math.min(MAX_H, sr.getScaledHeight() - PAD * 2);
        px = (sr.getScaledWidth() - pw) / 2;
        py = (sr.getScaledHeight() - ph) / 2;
    }

    private int computeTotalH(List<WaypointGroup> groups) {
        int h = 0;
        for (WaypointGroup g : groups) {
            h += ROW_H + ROW_PAD;
            if (expandedGroups.contains(g.name)) h += (g.waypoints.size() + 1) * (WP_H + WP_PAD) + ROW_PAD;
        }
        return h;
    }

    private List<RowItem> buildRows(int listTopY, List<WaypointGroup> groups) {
        List<RowItem> rows = new ArrayList<>();
        int rowY = listTopY - scrollOffset;
        for (WaypointGroup g : groups) {
            GroupRow gr = new GroupRow();
            gr.g = g;
            gr.y = rowY;
            gr.expanded = expandedGroups.contains(g.name);
            rows.add(gr);
            rowY += ROW_H + ROW_PAD;
            if (gr.expanded) {
                for (int i = 0; i < g.waypoints.size(); i++) {
                    WpRow wr = new WpRow();
                    wr.g = g;
                    wr.idx = i;
                    wr.y = rowY;
                    rows.add(wr);
                    rowY += WP_H + WP_PAD;
                }
                AddRow ar = new AddRow();
                ar.g = g;
                ar.y = rowY;
                rows.add(ar);
                rowY += WP_H + ROW_PAD;
            }
        }
        return rows;
    }

    @Override
    public void render() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        FontRenderer fr = mc.fontRendererObj;
        updatePanel(sr);

        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0xaa050508);

        GlStateManagerCompat.color(0.18f, 0.18f, 0.18f, 1f);
        NineSliceUtils.draw(Resources.storageBackground(1), px, py, pw, ph, 6, 18);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        int curY = py + PAD;

        fr.drawStringWithShadow(EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Waypoints", px + PAD, curY + 5, -1);
        String escHint = EnumChatFormatting.DARK_GRAY + "ESC";
        fr.drawStringWithShadow(escHint, px + pw - fr.getStringWidth(escHint) - PAD, curY + 5, -1);
        Gui.drawRect(px + PAD, curY + TITLE_H, px + pw - PAD, curY + TITLE_H + 1, 0xff252535);
        curY += TITLE_H + ROW_PAD + 2;

        WaypointState state = WaypointState.getInstance();
        WaypointStorage storage = WaypointStorage.getInstance();

        GlStateManagerCompat.color(0.14f, 0.14f, 0.14f, 1f);
        NineSliceUtils.draw(Resources.storageBackground(1), px + PAD, curY, pw - PAD * 2, ROW_H, 6, 18);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);

        if (state.hasGroup()) {
            String dot = EnumChatFormatting.GREEN + "▶ ";
            String label = EnumChatFormatting.WHITE + state.loadedGroup.name + EnumChatFormatting.DARK_GRAY + " (" + (state.currentIndex + 1) + "/" + state.size() + ")";
            fr.drawStringWithShadow(dot + label, px + PAD + 6, curY + 6, -1);
            int bx = px + pw - BTN_W - PAD - 4;
            drawBtn(bx, curY + 5, EnumChatFormatting.RED + "Unload", fr, isHovered(bx, curY + 5, BTN_W, BTN_H));
        } else {
            fr.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "No group loaded", px + PAD + 6, curY + 6, -1);
        }
        curY += ROW_H + ROW_PAD;

        int sfW = pw - PAD * 2 - BTN_W * 2 - 12;
        ensureSearchField(px + PAD, curY, sfW, fr);
        searchField.xPosition = px + PAD;
        searchField.yPosition = curY;
        searchField.width = sfW;
        searchField.height = SF_H;
        drawInputBg(px + PAD, curY, sfW);
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused())
            fr.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "Search...", px + PAD + 3, curY + 4, -1);

        int impTogX = px + pw - BTN_W * 2 - 10;
        int newTogX = px + pw - BTN_W - PAD;
        drawToolBtn(impTogX, curY, importOpen ? EnumChatFormatting.YELLOW + "Cancel" : EnumChatFormatting.GRAY + "Import", fr, isHovered(impTogX, curY, BTN_W, SF_H));
        drawToolBtn(newTogX, curY, createOpen ? EnumChatFormatting.YELLOW + "Cancel" : EnumChatFormatting.WHITE + "+ New", fr, isHovered(newTogX, curY, BTN_W, SF_H));
        curY += SF_H + ROW_PAD;

        if (importOpen) {
            int ifW = pw - PAD * 2 - BTN_W - 8;
            importField = ensureGenericField(importField, 2, px + PAD, curY, ifW, fr);
            importField.xPosition = px + PAD;
            importField.yPosition = curY;
            importField.width = ifW;
            importField.height = SF_H;
            drawInputBg(px + PAD, curY, ifW);
            importField.drawTextBox();
            if (importField.getText().isEmpty())
                fr.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "Group name for import...", px + PAD + 3, curY + 4, -1);
            int goBtnX = px + pw - BTN_W - PAD;
            drawToolBtn(goBtnX, curY, EnumChatFormatting.WHITE + "Go", fr, isHovered(goBtnX, curY, BTN_W, SF_H));
            curY += SF_H + ROW_PAD;
        }

        if (createOpen) {
            int cfW = pw - PAD * 2 - BTN_W - 8;
            createField = ensureGenericField(createField, 3, px + PAD, curY, cfW, fr);
            createField.xPosition = px + PAD;
            createField.yPosition = curY;
            createField.width = cfW;
            createField.height = SF_H;
            drawInputBg(px + PAD, curY, cfW);
            createField.drawTextBox();
            if (createField.getText().isEmpty())
                fr.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "New group name...", px + PAD + 3, curY + 4, -1);
            int createBtnX = px + pw - BTN_W - PAD;
            drawToolBtn(createBtnX, curY, EnumChatFormatting.WHITE + "Create", fr, isHovered(createBtnX, curY, BTN_W, SF_H));
            curY += SF_H + ROW_PAD;
        }
        Gui.drawRect(px + PAD, curY, px + pw - PAD, curY + 1, 0xff252535);
        curY += 4;
        int listTopY = curY;
        int listBottomY = py + ph - PAD;
        int visibleH = Math.max(0, listBottomY - listTopY);
        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        List<WaypointGroup> groups = filteredGroups(storage, query);
        int totalH = computeTotalH(groups);
        int maxScroll = Math.max(0, totalH - visibleH);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        List<RowItem> rows = buildRows(listTopY, groups);
        int scale = sr.getScaleFactor();
        GlStateManagerCompat.pushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px * scale, mc.displayHeight - listBottomY * scale, pw * scale, visibleH * scale);

        for (RowItem item : rows) {
            if (item.y + item.h() <= listTopY || item.y >= listBottomY) continue;
            if (item instanceof GroupRow) renderGroupRow(px, pw, (GroupRow) item, state, fr);
            else if (item instanceof WpRow) renderWpRow(px, pw, (WpRow) item, fr);
            else if (item instanceof AddRow) renderAddRow(px, pw, (AddRow) item, fr);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManagerCompat.popMatrix();

        if (groups.isEmpty()) {
            String msg = query.isEmpty() ? EnumChatFormatting.DARK_GRAY + "No groups — click " + EnumChatFormatting.WHITE + "+ New" + EnumChatFormatting.DARK_GRAY + " or /w create <name>" : EnumChatFormatting.DARK_GRAY + "No groups match \"" + query + "\"";
            fr.drawStringWithShadow(msg, px + PAD + 6, listTopY + 16, -1);
        }

        if (maxScroll > 0) {
            int sbX = px + pw - 5;
            int barH = Math.max(16, (int) (visibleH * (float) visibleH / totalH));
            int barY = listTopY + (int) ((visibleH - barH) * ((float) scrollOffset / maxScroll));
            Gui.drawRect(sbX, listTopY, sbX + 4, listBottomY, 0xff141418);
            Gui.drawRect(sbX, barY, sbX + 4, barY + barH, 0xff505060);
            Gui.drawRect(sbX, barY, sbX + 4, barY + 1, 0xff8080a0);
        }
    }

    private void renderGroupRow(int panelX, int panelW, GroupRow gr, WaypointState state, FontRenderer fr) {
        boolean isLoaded = state.loadedGroup != null && state.loadedGroup.name.equalsIgnoreCase(gr.g.name);

        GlStateManagerCompat.color(isLoaded ? 0.12f : 0.14f, isLoaded ? 0.16f : 0.14f, isLoaded ? 0.12f : 0.14f, 1f);
        NineSliceUtils.draw(Resources.storageBackground(1), panelX + PAD, gr.y, panelW - PAD * 2, ROW_H, 6, 18);
        GlStateManagerCompat.color(1f, 1f, 1f, 1f);
        if (isLoaded) Gui.drawRect(panelX + PAD, gr.y, panelX + PAD + 2, gr.y + ROW_H, 0xff55aa55);
        String arrow = gr.expanded ? EnumChatFormatting.GRAY + "▼" : EnumChatFormatting.DARK_GRAY + "▶";
        fr.drawStringWithShadow(arrow, panelX + PAD + 5, gr.y + 7, -1);
        String nameStr = (isLoaded ? EnumChatFormatting.GREEN : EnumChatFormatting.WHITE) + gr.g.name + EnumChatFormatting.DARK_GRAY + " (" + gr.g.waypoints.size() + ")";
        int maxNameW = panelW - PAD * 2 - INDENT - BTN_W * 3 - 20;
        TextRenderUtils.drawStringScaledMaxWidth(nameStr, fr, panelX + PAD + INDENT, gr.y + 7, false, maxNameW, -1);
        int delX = panelX + panelW - BTN_W - PAD - 4;
        int expX = delX - BTN_W - 3;
        int loadX = expX - BTN_W - 3;
        int btnY = gr.y + 5;
        drawBtn(loadX, btnY, isLoaded ? EnumChatFormatting.AQUA + "Reload" : EnumChatFormatting.WHITE + "Load", fr, isHovered(loadX, btnY, BTN_W, BTN_H));
        drawBtn(expX, btnY, EnumChatFormatting.GRAY + "Export", fr, isHovered(expX, btnY, BTN_W, BTN_H));
        drawBtn(delX, btnY, EnumChatFormatting.RED + "Delete", fr, isHovered(delX, btnY, BTN_W, BTN_H));
    }

    private void renderWpRow(int panelX, int panelW, WpRow wr, FontRenderer fr) {
        WaypointPoint wp = wr.g.waypoints.get(wr.idx);
        int rowBg = wr.idx % 2 == 0 ? 0x18ffffff : 0x10ffffff;
        Gui.drawRect(panelX + PAD + INDENT, wr.y, panelX + panelW - PAD, wr.y + WP_H, rowBg);
        Gui.drawRect(panelX + PAD + INDENT, wr.y, panelX + PAD + INDENT + 2, wr.y + WP_H, 0xff303048);

        String idxStr = EnumChatFormatting.DARK_GRAY + "" + (wr.idx + 1) + ".";
        fr.drawStringWithShadow(idxStr, panelX + PAD + INDENT + 5, wr.y + 5, -1);
        int indexW = fr.getStringWidth("99.") + 4;

        int delX = panelX + panelW - PAD - SBTN_W - 4;
        int downX = delX - SBTN_W - 2;
        int upX = downX - SBTN_W - 2;
        int btnY = wr.y + 3;
        drawSmallBtn(delX, btnY, EnumChatFormatting.RED + "x", fr, isHovered(delX, btnY, SBTN_W, SBTN_H));
        drawSmallBtn(downX, btnY, EnumChatFormatting.GRAY + "v", fr, isHovered(downX, btnY, SBTN_W, SBTN_H));
        drawSmallBtn(upX, btnY, EnumChatFormatting.WHITE + "^", fr, isHovered(upX, btnY, SBTN_W, SBTN_H));

        String coords = EnumChatFormatting.DARK_GRAY + "" + (int) wp.x + ", " + (int) wp.y + ", " + (int) wp.z;
        int coordsX = upX - fr.getStringWidth(coords) - 5;
        fr.drawStringWithShadow(coords, coordsX, wr.y + 5, -1);

        int nameAreaW = coordsX - (panelX + PAD + INDENT + indexW) - 4;
        if (nameAreaW > 0) {
            String name = EnumChatFormatting.WHITE + (wp.name != null ? wp.name : "");
            TextRenderUtils.drawStringScaledMaxWidth(name, fr, panelX + PAD + INDENT + indexW, wr.y + 5, false, nameAreaW, -1);
        }
    }

    private void renderAddRow(int panelX, int panelW, AddRow ar, FontRenderer fr) {
        int rx = panelX + PAD + INDENT, rw = panelW - PAD - INDENT - PAD;
        boolean hov = isHovered(rx, ar.y, rw, WP_H);
        Gui.drawRect(rx, ar.y, rx + rw, ar.y + WP_H, hov ? 0x20ffffff : 0x10ffffff);
        fr.drawStringWithShadow(EnumChatFormatting.DARK_GRAY + "+ " + EnumChatFormatting.GRAY + "Add waypoint here", rx + 6, ar.y + 5, -1);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        int dWheel = MouseCompat.getEventDWheel();
        if (dWheel != 0) {
            scrollOffset = Math.max(0, scrollOffset - (dWheel > 0 ? 20 : -20));
            return false;
        }
        if (!MouseCompat.getEventButtonState() || MouseCompat.getEventButton() != 0) return false;

        Minecraft mc = MinecraftCompat.getMinecraft();
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        updatePanel(sr);
        WaypointState state = WaypointState.getInstance();
        WaypointStorage storage = WaypointStorage.getInstance();
        int curY = py + PAD + TITLE_H + ROW_PAD + 2;
        if (state.hasGroup()) {
            int bx = px + pw - BTN_W - PAD - 4;
            if (inBounds(mouseX, mouseY, bx, curY + 5, BTN_W, BTN_H)) {
                state.unload();
                return true;
            }
        }
        curY += ROW_H + ROW_PAD;

        if (searchField != null) searchField.mouseClicked(mouseX, mouseY, 0);

        int impTogX = px + pw - BTN_W * 2 - 10;
        int newTogX = px + pw - BTN_W - PAD;
        if (inBounds(mouseX, mouseY, impTogX, curY, BTN_W, SF_H)) {
            importOpen = !importOpen;
            if (!importOpen) importField = null;
            return true;
        }
        if (inBounds(mouseX, mouseY, newTogX, curY, BTN_W, SF_H)) {
            createOpen = !createOpen;
            if (!createOpen) createField = null;
            return true;
        }
        curY += SF_H + ROW_PAD;

        if (importOpen) {
            if (importField != null) importField.mouseClicked(mouseX, mouseY, 0);
            if (inBounds(mouseX, mouseY, px + pw - BTN_W - PAD, curY, BTN_W, SF_H)) {
                doImport(storage);
                return true;
            }
            curY += SF_H + ROW_PAD;
        }
        if (createOpen) {
            if (createField != null) createField.mouseClicked(mouseX, mouseY, 0);
            if (inBounds(mouseX, mouseY, px + pw - BTN_W - PAD, curY, BTN_W, SF_H)) {
                doCreate(storage);
                return true;
            }
            curY += SF_H + ROW_PAD;
        }

        curY += 5;
        int listTopY = curY;
        int listBottomY = py + ph - PAD;

        String query = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        List<WaypointGroup> groups = filteredGroups(storage, query);
        List<RowItem> rows = buildRows(listTopY, groups);

        for (RowItem item : rows) {
            if (item.y + item.h() <= listTopY || item.y >= listBottomY) continue;

            if (item instanceof GroupRow) {
                GroupRow gr = (GroupRow) item;
                if (inBounds(mouseX, mouseY, px + PAD, gr.y, INDENT + 8, ROW_H)) {
                    if (gr.expanded) expandedGroups.remove(gr.g.name);
                    else expandedGroups.add(gr.g.name);
                    return true;
                }
                int delX = px + pw - BTN_W - PAD - 4;
                int expX = delX - BTN_W - 3;
                int loadX = expX - BTN_W - 3;
                int btnY = gr.y + 5;
                if (inBounds(mouseX, mouseY, loadX, btnY, BTN_W, BTN_H)) {
                    state.load(gr.g);
                    return true;
                }
                if (inBounds(mouseX, mouseY, expX, btnY, BTN_W, BTN_H)) {
                    GuiScreen.setClipboardString(exportSoopy(gr.g));
                    return true;
                }
                if (inBounds(mouseX, mouseY, delX, btnY, BTN_W, BTN_H)) {
                    if (state.loadedGroup != null && state.loadedGroup.name.equalsIgnoreCase(gr.g.name)) state.unload();
                    storage.removeGroup(gr.g.name);
                    storage.saveIfDirty();
                    expandedGroups.remove(gr.g.name);
                    return true;
                }

            } else if (item instanceof WpRow) {
                WpRow wr = (WpRow) item;
                int delX = px + pw - PAD - SBTN_W - 4;
                int downX = delX - SBTN_W - 2;
                int upX = downX - SBTN_W - 2;
                int btnY = wr.y + 3;
                if (inBounds(mouseX, mouseY, delX, btnY, SBTN_W, SBTN_H)) {
                    wr.g.waypoints.remove(wr.idx);
                    storage.markDirty();
                    storage.saveIfDirty();
                    return true;
                }
                if (inBounds(mouseX, mouseY, downX, btnY, SBTN_W, SBTN_H)) {
                    if (wr.idx < wr.g.waypoints.size() - 1) {
                        Collections.swap(wr.g.waypoints, wr.idx, wr.idx + 1);
                        storage.markDirty();
                        storage.saveIfDirty();
                    }
                    return true;
                }
                if (inBounds(mouseX, mouseY, upX, btnY, SBTN_W, SBTN_H)) {
                    if (wr.idx > 0) {
                        Collections.swap(wr.g.waypoints, wr.idx, wr.idx - 1);
                        storage.markDirty();
                        storage.saveIfDirty();
                    }
                    return true;
                }

            } else if (item instanceof AddRow) {
                AddRow ar = (AddRow) item;
                int rx = px + PAD + INDENT, rw = pw - PAD - INDENT - PAD;
                if (inBounds(mouseX, mouseY, rx, ar.y, rw, WP_H)) {
                    double bx = Math.floor(mc.thePlayer.posX);
                    double by = Math.floor(mc.thePlayer.posY) - 1;
                    double bz = Math.floor(mc.thePlayer.posZ);
                    ar.g.waypoints.add(new WaypointPoint(bx, by, bz, String.valueOf(ar.g.waypoints.size() + 1)));
                    storage.markDirty();
                    storage.saveIfDirty();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyboardInput() {
        if (!KeyboardCompat.getEventKeyState()) return false;
        int key = KeyboardCompat.getEventKey();
        char c = KeyboardCompat.getEventCharacter();

        boolean sf = searchField != null && searchField.isFocused();
        boolean imf = importField != null && importField.isFocused();
        boolean crf = createField != null && createField.isFocused();

        if (!sf && !imf && !crf) {
            if (key == Keyboard.KEY_DOWN) {
                scrollOffset += 12;
                return true;
            }
            if (key == Keyboard.KEY_UP) {
                scrollOffset = Math.max(0, scrollOffset - 12);
                return true;
            }
        }
        if (imf && key == Keyboard.KEY_RETURN) {
            doImport(WaypointStorage.getInstance());
            return true;
        }
        if (crf && key == Keyboard.KEY_RETURN) {
            doCreate(WaypointStorage.getInstance());
            return true;
        }
        if (sf) {
            searchField.textboxKeyTyped(c, key);
            return true;
        }
        if (imf) {
            importField.textboxKeyTyped(c, key);
            return true;
        }
        if (crf) {
            createField.textboxKeyTyped(c, key);
            return true;
        }
        return false;
    }

    private void doImport(WaypointStorage storage) {
        if (importField == null) return;
        String name = importField.getText().trim().toLowerCase();
        if (name.isEmpty()) return;
        String clip = GuiScreen.getClipboardString();
        if (clip == null || clip.trim().isEmpty()) return;
        List<WaypointPoint> wps = parseSoopy(clip.trim());
        if (wps == null || wps.isEmpty()) return;
        WaypointGroup g = storage.getGroup(name);
        if (g == null) g = new WaypointGroup(name);
        g.waypoints = wps;
        storage.putGroup(g);
        storage.saveIfDirty();
        importOpen = false;
        importField = null;
    }

    private void doCreate(WaypointStorage storage) {
        if (createField == null) return;
        String name = createField.getText().trim().toLowerCase();
        if (name.isEmpty() || storage.getGroup(name) != null) return;
        storage.putGroup(new WaypointGroup(name));
        storage.saveIfDirty();
        createOpen = false;
        createField = null;
    }

    private void drawBtn(int x, int y, String label, FontRenderer fr, boolean hov) {
        Gui.drawRect(x, y, x + BTN_W, y + BTN_H, hov ? 0xff282830 : 0xff1a1a22);
        Gui.drawRect(x, y, x + BTN_W, y + 1, hov ? 0xff505060 : 0xff303038);
        Gui.drawRect(x, y + BTN_H - 1, x + BTN_W, y + BTN_H, 0xff0a0a0e);
        TextRenderUtils.drawStringCenteredScaledMaxWidth(label, fr, x + BTN_W / 2f, y + BTN_H / 2f + 1, false, BTN_W - 4, -1);
    }

    private void drawSmallBtn(int x, int y, String label, FontRenderer fr, boolean hov) {
        Gui.drawRect(x, y, x + SBTN_W, y + SBTN_H, hov ? 0xff282830 : 0xff1a1a22);
        Gui.drawRect(x, y, x + SBTN_W, y + 1, 0xff303038);
        TextRenderUtils.drawStringCenteredScaledMaxWidth(label, fr, x + SBTN_W / 2f, y + SBTN_H / 2f + 1, false, SBTN_W - 2, -1);
    }

    private void drawToolBtn(int x, int y, String label, FontRenderer fr, boolean hov) {
        Gui.drawRect(x, y, x + BTN_W, y + SF_H, hov ? 0xff282830 : 0xff1a1a22);
        Gui.drawRect(x, y, x + BTN_W, y + 1, hov ? 0xff505060 : 0xff303038);
        Gui.drawRect(x, y + SF_H - 1, x + BTN_W, y + SF_H, 0xff0a0a0e);
        TextRenderUtils.drawStringCenteredScaledMaxWidth(label, fr, x + BTN_W / 2f, y + SF_H / 2f + 1, false, BTN_W - 4, -1);
    }

    private void drawInputBg(int x, int y, int w) {
        Gui.drawRect(x, y, x + w, y + SF_H, 0xff111118);
        Gui.drawRect(x, y, x + w, y + 1, 0xff252530);
        Gui.drawRect(x, y + SF_H - 1, x + w, y + SF_H, 0xff252530);
    }


    private void ensureSearchField(int x, int y, int w, FontRenderer fr) {
        if (searchField == null) {
            searchField = new GuiTextField(0, fr, x, y, w, WaypointGroupGui.SF_H);
            searchField.setMaxStringLength(64);
            searchField.setEnableBackgroundDrawing(false);
            searchField.setCanLoseFocus(true);
            searchField.setFocused(false);
        }
    }

    private GuiTextField ensureGenericField(GuiTextField existing, int id, int x, int y, int w, FontRenderer fr) {
        if (existing != null) return existing;
        GuiTextField f = new GuiTextField(id, fr, x, y, w, WaypointGroupGui.SF_H);
        f.setMaxStringLength(64);
        f.setEnableBackgroundDrawing(false);
        f.setCanLoseFocus(true);
        f.setFocused(true);
        return f;
    }

    private List<WaypointGroup> filteredGroups(WaypointStorage storage, String query) {
        List<WaypointGroup> result = new ArrayList<>();
        for (WaypointGroup g : storage.getGroups().values())
            if (query.isEmpty() || g.name.toLowerCase().contains(query)) result.add(g);
        return result;
    }

    private boolean inBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean isHovered(int x, int y, int w, int h) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int[] mouse = KeybindHelper.getMouseCoords(sr);
        int mx = mouse[0], my = mouse[1];
        return inBounds(mx, my, x, y, w, h);
    }

    private abstract static class RowItem {
        int y;

        abstract int h();
    }

    private static class GroupRow extends RowItem {
        WaypointGroup g;
        boolean expanded;

        int h() {
            return ROW_H;
        }
    }

    private static class WpRow extends RowItem {
        WaypointGroup g;
        int idx;

        int h() {
            return WP_H;
        }
    }

    private static class AddRow extends RowItem {
        WaypointGroup g;

        int h() {
            return WP_H;
        }
    }
}