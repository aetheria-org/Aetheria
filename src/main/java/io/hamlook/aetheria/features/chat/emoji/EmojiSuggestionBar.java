package io.hamlook.aetheria.features.chat.emoji;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.chat.EmojiConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.overlay.Overlay;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiSuggestionBar {

    private static final int MAX_SUGGESTIONS = 100;
    private static final int GRID_COLUMNS = 3;
    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int MIN_LETTERS = 0;
    private static final long SHOW_DELAY_MS = 200L;
    private static final int NO_TOKEN = -1;
    private static final int ICON_SIZE = 10;
    private static final int CELL_SIZE = 18;
    private static final int GAP = 2;
    private static final int PAD = 4;
    private static final int CORNER_RADIUS = 4;
    private static final int GRID_WIDTH = GRID_COLUMNS * CELL_SIZE + (GRID_COLUMNS - 1) * GAP;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final float SCROLL_DAMPING = 0.85f;
    private static final int SCROLL_AMOUNT = 3;

    private static final Pattern PARTIAL_TOKEN = Pattern.compile("(?:^|\\s):([a-zA-Z0-9_]{" + MIN_LETTERS + ",})$");

    private static int tokenStart = -1;
    private static List<String> matches = Collections.emptyList();
    private static String pendingPartial = null;
    private static int pendingStart = NO_TOKEN;
    private static List<String> pendingMatches = Collections.emptyList();
    private static long pendingSince = 0L;

    private static int boxX, boxY, boxW, boxH;

    private static float scrollOffset = 0f;
    private static float scrollVelocity = 0f;
    private static boolean isDraggingScrollbar = false;
    private static int lastMouseX = -1;
    private static int lastMouseY = -1;
    private static int maxScroll = 0;

    private static int cachedScrollbarY = 0;
    private static int cachedScrollbarH = 0;
    private static int cachedThumbH = 0;

    private static String lastText = null;
    private static int lastCursor = -1;
    private static float scale = 1f;
    private static int totalRows = 0;
    private static int lastDragMouseY = -1;
    private static ScaledResolution cachedSr = null;
    private static int cachedSrKey = -1;

    private EmojiSuggestionBar() {
    }

    public static void update(String text, int cursor) {
        if (text == null || cursor < 0 || cursor > text.length()) {
            clear();
            return;
        }

        if (cursor == lastCursor && text.equals(lastText)) return;
        lastText = text;
        lastCursor = cursor;

        Matcher m = PARTIAL_TOKEN.matcher(text).region(0, cursor);
        if (!m.find()) {
            if (pendingStart == NO_TOKEN && pendingMatches.isEmpty()) return;
            queuePending(NO_TOKEN, null, Collections.emptyList());
            return;
        }

        String partial = m.group(1);
        int start = m.start() + (m.group().startsWith(":") ? 0 : 1);
        if (partial.equals(pendingPartial) && start == pendingStart) return;
        queuePending(start, partial, EmojiManager.search(partial, MAX_SUGGESTIONS));
    }

    private static void queuePending(int start, String partial, List<String> results) {
        pendingStart = start;
        pendingPartial = partial;
        pendingMatches = results;
        pendingSince = System.currentTimeMillis();
        scrollOffset = 0f;
        scrollVelocity = 0f;
    }

    private static void applyPendingIfReady() {
        if (System.currentTimeMillis() - pendingSince >= SHOW_DELAY_MS) {
            tokenStart = pendingStart;
            matches = pendingMatches;
            recalculateMaxScroll();
        }
    }

    public static void clear() {
        tokenStart = -1;
        matches = Collections.emptyList();
        pendingPartial = null;
        pendingStart = NO_TOKEN;
        pendingMatches = Collections.emptyList();
        pendingSince = 0L;
        scrollOffset = 0f;
        scrollVelocity = 0f;
        isDraggingScrollbar = false;
        lastMouseX = -1;
        lastMouseY = -1;
        totalRows = 0;
    }

    public static boolean hasSuggestion() {
        return !matches.isEmpty();
    }

    public static boolean handleKeyTypedPre(int keyCode, GuiTextField inputField) {
        if (keyCode == Keyboard.KEY_TAB && hasSuggestion()) {
            complete(inputField);
            return true;
        }
        return false;
    }

    public static void handleKeyTypedPost(int keyCode, GuiTextField inputField) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            clear();
            return;
        }
        update(inputField.getText(), inputField.getCursorPosition());
    }

    public static boolean handleMouseClick(int mouseX, int mouseY, int mouseButton, GuiTextField inputField) {
        if (mouseButton != 0) return false;
        if (matches.isEmpty() || lastMouseX == -1) return false;
        int lx = txMouseX(mouseX);
        int ly = txMouseY(mouseY);
        if (lx < boxX || lx > boxX + boxW || ly < boxY || ly > boxY + boxH) return false;

        int index = hitTest(lx, ly);
        if (index >= 0) {
            complete(inputField, index);
            return true;
        }
        startScrollbarDrag(lx, ly);
        return true;
    }

    public static void complete(GuiTextField inputField) {
        complete(inputField, 0);
    }

    public static void complete(GuiTextField inputField, int index) {
        if (index < 0 || index >= matches.size() || tokenStart < 0) return;

        String text = inputField.getText();
        int cursor = inputField.getCursorPosition();
        if (cursor > text.length() || tokenStart > cursor) {
            clear();
            return;
        }

        String replacement = ":" + matches.get(index) + ": ";
        String newText = text.substring(0, tokenStart) + replacement + text.substring(cursor);
        inputField.setText(newText);
        inputField.setCursorPosition(tokenStart + replacement.length());
        clear();
    }

    public static boolean handleMouseWheel(int wheelDelta) {
        if (matches.isEmpty() || wheelDelta == 0) return false;
        int lx = txMouseX(lastMouseX);
        int ly = txMouseY(lastMouseY);
        if (lx < boxX || lx > boxX + boxW || ly < boxY || ly > boxY + boxH) {
            return false;
        }

        scrollVelocity = (wheelDelta > 0 ? -1 : 1) * SCROLL_AMOUNT;
        applyScrollPhysics();
        return true;
    }

    public static void startScrollbarDrag(int mouseX, int mouseY) {
        if (matches.isEmpty() || maxScroll <= 0) return;
        if (!isMouseOverScrollbar(mouseX, mouseY)) return;
        isDraggingScrollbar = true;
        lastDragMouseY = mouseY;
    }

    public static void updateScrollbarDrag(int mouseY) {
        if (!isDraggingScrollbar || lastDragMouseY < 0) return;

        int dy = mouseY - lastDragMouseY;
        int trackHeight = cachedScrollbarH - cachedThumbH;
        if (trackHeight > 0) {
            float scrollPercent = (float) dy / trackHeight;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + scrollPercent * maxScroll));
        }
        lastDragMouseY = mouseY;
        scrollVelocity = 0f;
    }

    public static void tickDrag(int mouseY) {
        if (!isDraggingScrollbar) return;
        if (MouseCompat.isButtonDown(0)) {
            updateScrollbarDrag(txMouseY(mouseY));
        } else {
            endScrollbarDrag();
        }
    }

    public static void endScrollbarDrag() {
        isDraggingScrollbar = false;
        lastDragMouseY = -1;
    }

    private static void applyScrollPhysics() {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + scrollVelocity));
        scrollVelocity *= SCROLL_DAMPING;
        if (Math.abs(scrollVelocity) < 0.01f) {
            scrollVelocity = 0f;
        }
    }

    private static void recalculateMaxScroll() {
        totalRows = (matches.size() + GRID_COLUMNS - 1) / GRID_COLUMNS;
        maxScroll = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
    }

    private static int gridLeft() {
        return boxX + PAD;
    }

    private static int gridRight() {
        return gridLeft() + GRID_WIDTH;
    }

    private static int gridYTop() {
        return boxY + PAD;
    }

    private static int gridYBot() {
        return boxY + boxH - PAD;
    }

    private static int scrollbarX() {
        return boxX + boxW - PAD - SCROLLBAR_WIDTH;
    }

    private static boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        int sbX = scrollbarX();
        return mouseX >= sbX && mouseX < sbX + SCROLLBAR_WIDTH && mouseY >= gridYTop() && mouseY < gridYBot();
    }

    private static float centerX() {
        return boxX + boxW / 2f;
    }

    private static float centerY() {
        return boxY + boxH / 2f;
    }

    private static int txMouseX(int mouseX) {
        return Math.round((mouseX - centerX()) / scale + centerX());
    }

    private static int txMouseY(int mouseY) {
        return Math.round((mouseY - centerY()) / scale + centerY());
    }

    private static ScaledResolution scaledResolution(Minecraft mc) {
        int key = mc.displayWidth * 31 + mc.displayHeight * 17 + mc.gameSettings.guiScale * 7;
        if (cachedSr == null || key != cachedSrKey) {
            cachedSr = GuiScreenUtils.getScaledResolution();
            cachedSrKey = key;
        }
        return cachedSr;
    }

    private static void drawRoundedBorder(int x, int y, int x2, int y2, int r, int color) {
        r = Math.min(r, Math.min(x2 - x, y2 - y) / 2);
        if (r <= 0) {
            Gui.drawRect(x, y, x2, y2, color);
            return;
        }
        Gui.drawRect(x + r, y, x2 - r, y + 1, color);
        Gui.drawRect(x + r, y2 - 1, x2 - r, y2, color);
        Gui.drawRect(x, y + r, x + 1, y2 - r, color);
        Gui.drawRect(x2 - 1, y + r, x2, y2 - r, color);
        drawCornerArc(x, y, r, color, false, false);
        drawCornerArc(x2 - r, y, r, color, true, false);
        drawCornerArc(x, y2 - r, r, color, false, true);
        drawCornerArc(x2 - r, y2 - r, r, color, true, true);
    }

    private static int cornerCut(int i, int r) {
        return (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - (double) (r - i - 1) * (r - i - 1))));
    }

    private static void drawCornerArc(int cx, int cy, int r, int color, boolean flipX, boolean flipY) {
        for (int i = 0; i < r; i++) {
            drawArcPixel(cx, cy, r, color, flipX, flipY, i, cornerCut(i, r));
        }
        for (int j = 0; j < r; j++) {
            int left = 0;
            while (left < r && cornerCut(left, r) > j) left++;
            if (left < r) drawArcPixel(cx, cy, r, color, flipX, flipY, left, j);
        }
    }

    private static void drawArcPixel(int cx, int cy, int r, int color, boolean flipX, boolean flipY, int u, int v) {
        int px = flipX ? cx + (r - 1 - u) : cx + u;
        int py = flipY ? cy + (r - 1 - v) : cy + v;
        Gui.drawRect(px, py, px + 1, py + 1, color);
    }

    public static void render(GuiTextField inputField, int mouseX, int mouseY) {
        if (inputField == null) return;
        update(inputField.getText(), inputField.getCursorPosition());
        if (matches.isEmpty() && pendingMatches.isEmpty()) return;
        applyPendingIfReady();
        if (matches.isEmpty()) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        ScaledResolution sr = scaledResolution(mc);
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (scrollVelocity != 0) {
            applyScrollPhysics();
        }

        EmojiConfig cfg = ATHRConfig.feature.chat.emojiConfig;
        scale = Math.max(0.1f, cfg.suggestionBarScale);

        int displayRows = Math.min(MAX_VISIBLE_ROWS, totalRows);

        int gridContentHeight = displayRows * (CELL_SIZE + GAP);
        int cachedGridHeight = gridContentHeight > 0 ? gridContentHeight : CELL_SIZE + GAP;

        boxW = PAD * 2 + GRID_WIDTH + (maxScroll > 0 ? SCROLLBAR_WIDTH + PAD : 0);
        boxH = PAD * 2 + cachedGridHeight;
        boxX = inputField.xPosition;
        boxY = inputField.yPosition - boxH - 2;

        float halfW = boxW / 2f * scale;
        float halfH = boxH / 2f * scale;
        int snapW = sr.getScaledWidth();
        int snapH = sr.getScaledHeight();
        int minX = Math.round(halfW - boxW / 2f);
        int maxX = Math.round(snapW - halfW - boxW / 2f);
        boxX = minX > maxX ? Math.round((snapW - boxW) / 2f) : Math.max(minX, Math.min(maxX, boxX));
        int minY = Math.round(halfH - boxH / 2f);
        int maxY = Math.round(snapH - halfH - boxH / 2f);
        boxY = minY > maxY ? Math.round((snapH - boxH) / 2f) : Math.max(minY, Math.min(maxY, boxY));

        cachedScrollbarY = boxY + PAD;
        cachedScrollbarH = cachedGridHeight;

        int bgColor = ChromaColour.specialToChromaRGB(cfg.suggestionBarBG);

        float cx = boxX + boxW / 2f;
        float cy = boxY + boxH / 2f;

        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0);
        GL11.glScalef(scale, scale, 1f);
        GL11.glTranslatef(-cx, -cy, 0);

        Overlay.drawRoundedRect(boxX, boxY, boxX + boxW, boxY + boxH, CORNER_RADIUS, bgColor);

        if (cfg.suggestionsBar) {
            int borderColor = ChromaColour.specialToChromaRGB(cfg.suggestionBarBorder);
            drawRoundedBorder(boxX, boxY, boxX + boxW, boxY + boxH, CORNER_RADIUS, borderColor);
        }

        int gyTop = gridYTop();
        int gyBot = gridYBot();
        int gRight = gridRight();

        int sGridLeft = Math.round(cx + (gridLeft() - cx) * scale);
        int sGridTop = Math.round(cy + (gyTop - cy) * scale);
        int sGridRight = Math.round(cx + (gRight - cx) * scale);
        int sGridBot = Math.round(cy + (gyBot - cy) * scale);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int factor = sr.getScaleFactor();
        GL11.glScissor(sGridLeft * factor, mc.displayHeight - sGridBot * factor, (sGridRight - sGridLeft) * factor, (sGridBot - sGridTop) * factor);

        int firstRow = (int) scrollOffset;
        int lastIdx = Math.min(matches.size(), (firstRow + displayRows) * GRID_COLUMNS);
        int hovered = hitTest(txMouseX(mouseX), txMouseY(mouseY));

        for (int idx = firstRow * GRID_COLUMNS; idx < lastIdx; idx++) {
            int row = idx / GRID_COLUMNS;
            int col = idx % GRID_COLUMNS;
            int cellX = gridLeft() + col * (CELL_SIZE + GAP);
            int cellY = gridYTop() + (int) ((row - scrollOffset) * (CELL_SIZE + GAP));

            if (cellY + CELL_SIZE <= gyTop || cellY >= gyBot) continue;
            if (cellX + CELL_SIZE <= gridLeft() || cellX >= gRight) continue;

            if (idx == hovered) {
                int highlightColor = ChromaColour.specialToChromaRGB(cfg.rowHighlightColor);
                Gui.drawRect(gridLeft(), cellY, gRight, cellY + CELL_SIZE, highlightColor);
            }

            String emojiName = matches.get(idx);
            RenderUtils.drawEmoji(emojiName, cellX + (CELL_SIZE - ICON_SIZE) / 2f, cellY + (CELL_SIZE - ICON_SIZE) / 2f, ICON_SIZE);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll > 0) {
            renderScrollbar(txMouseX(mouseX), txMouseY(mouseY));
        }

        GL11.glPopMatrix();

        if (hovered >= 0) {
            String emojiShortcode = matches.get(hovered);
            String tooltip = ":" + emojiShortcode + ":";
            FontRenderer fr = mc.fontRendererObj;
            int tw = fr.getStringWidth(tooltip);
            int tx = Math.max(2, Math.min(Math.round(cx - tw / 2f), snapW - tw - 2));
            int ty = Math.round(cy - halfH) - 11;
            if (ty < 2) ty = Math.round(cy + halfH) + 2;
            ty = Math.max(2, Math.min(ty, snapH - 10));
            Gui.drawRect(tx - 2, ty - 1, tx + tw + 2, ty + 9, 0xCC222222);
            fr.drawString(tooltip, tx, ty, 0xCCCCCC, false);
        }
    }

    private static void renderScrollbar(int mouseX, int mouseY) {
        int sbX = scrollbarX();
        boolean hovering = isMouseOverScrollbar(mouseX, mouseY);

        Gui.drawRect(sbX, cachedScrollbarY, sbX + SCROLLBAR_WIDTH, cachedScrollbarY + cachedScrollbarH, 0x66000000);

        float thumbPercent = scrollOffset / Math.max(1, maxScroll);
        int thumbH = Math.max(10, (int) (cachedScrollbarH * (MAX_VISIBLE_ROWS / (float) totalRows)));
        cachedThumbH = thumbH;
        int thumbY = cachedScrollbarY + (int) (thumbPercent * (cachedScrollbarH - thumbH));

        int thumbColor = hovering || isDraggingScrollbar ? 0xAAEEEEEE : 0x88CCCCCC;
        Gui.drawRect(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbH, thumbColor);
    }

    public static int hitTest(int mouseX, int mouseY) {
        if (matches.isEmpty()) return -1;
        if (mouseX < boxX || mouseX > boxX + boxW || mouseY < boxY || mouseY > boxY + boxH) return -1;

        if (mouseX < gridLeft() || mouseX >= gridRight()) return -1;

        int relX = mouseX - (boxX + PAD);
        int relY = mouseY - (boxY + PAD);

        int col = relX / (CELL_SIZE + GAP);
        int row = (int) (scrollOffset + relY / (float) (CELL_SIZE + GAP));

        if (col < 0 || col >= GRID_COLUMNS) return -1;
        if (row < 0 || row >= totalRows) return -1;

        int index = row * GRID_COLUMNS + col;
        if (index >= matches.size()) return -1;

        int cellX = gridLeft() + col * (CELL_SIZE + GAP);
        int cellY = gridYTop() + (int) ((row - scrollOffset) * (CELL_SIZE + GAP));
        if (mouseX < cellX || mouseX > cellX + CELL_SIZE) return -1;
        if (mouseY < cellY || mouseY > cellY + CELL_SIZE) return -1;

        return index;
    }
}
