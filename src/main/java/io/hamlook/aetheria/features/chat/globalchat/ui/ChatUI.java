package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.SyncChecker;
import io.hamlook.aetheria.features.chat.globalchat.ui.util.ChatHelper;
import io.hamlook.aetheria.features.chat.globalchat.ui.util.ChatLine;
import io.hamlook.aetheria.features.chat.globalchat.vars.Message;
import io.hamlook.aetheria.features.chat.globalchat.vars.MessageData;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChatUI extends GuiScreen {

    private static final int CACHE_LIMIT = 500;

    public static int UI_WIDTH;

    private static final int COMPOSER_HEIGHT = 20;
    private static final int COMPOSER_GAP = 8;
    private static final int CLOSE_SIZE = 16;
    private static final int CLOSE_PAD = 6;

    private ChatBox chatBox;
    private final ImageViewer imageViewer = new ImageViewer();
    private final List<ChatLine> renderedLines = new ArrayList<>();

    private int closeX, closeY, closeW, closeH;

    private float scrollOffset = 0;
    private float scrollTarget = 0;
    private boolean autoScroll = true;
    private static final float SCROLL_SPEED = 0.25f;

    private ChatLine editingLine = null;

    private int prevMessageCount = 0;

    public LinkedHashMap<MessageData, ChatLine> cache = new LinkedHashMap<MessageData, ChatLine>(CACHE_LIMIT + 1, 1f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MessageData, ChatLine> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int composerX = 0;
        int composerY = this.height - COMPOSER_HEIGHT;
        int composerWidth = this.width;

        if (chatBox == null) {
            chatBox = new ChatBox(this.fontRendererObj, composerX, composerY, composerWidth, COMPOSER_HEIGHT);
        } else {
            chatBox.setBounds(composerX, composerY, composerWidth, COMPOSER_HEIGHT);
        }

        closeW = CLOSE_SIZE;
        closeH = CLOSE_SIZE;
        closeX = this.width - CLOSE_SIZE - CLOSE_PAD;
        closeY = CLOSE_PAD;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        if (chatBox != null) chatBox.tick();
        scrollOffset += (scrollTarget - scrollOffset) * SCROLL_SPEED;
        if (Math.abs(scrollOffset - scrollTarget) < 0.5f) {
            scrollOffset = scrollTarget;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dwheel = Mouse.getEventDWheel();
        if (dwheel != 0) {
            scrollTarget += dwheel * 0.3f;
            autoScroll = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (imageViewer.isVisible()) {
            imageViewer.keyTyped(keyCode);
            return;
        }

        if (editingLine != null && editingLine.isEditing()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                editingLine.cancelEdit();
                editingLine = null;
                return;
            }
            if (editingLine.handleKeyTyped(typedChar, keyCode)) {
                return;
            }
            if (editingLine != null && !editingLine.isEditing()) {
                editingLine = null;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (chatBox != null && chatBox.isEditing()) {
                chatBox.cancelEdit();
                return;
            }
            this.mc.displayGuiScreen(null);
            return;
        }
        if (chatBox != null && chatBox.keyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (imageViewer.isVisible()) {
            imageViewer.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        if (isHovered(closeX, closeY, closeW, closeH, mouseX, mouseY)) {
            this.mc.displayGuiScreen(null);
            return;
        }

        if (editingLine != null && editingLine.isEditing()) {
            return;
        }

        if (chatBox != null && chatBox.isCancelReplyClicked(mouseX, mouseY)) {
            chatBox.cancelReply();
            return;
        }

        for (ChatLine line : renderedLines) {
            if (line.isEditClicked(mouseX, mouseY)) {
                line.startEditing();
                editingLine = line;
                return;
            }
            if (line.isReplyClicked(mouseX, mouseY)) {
                if (chatBox != null) {
                    chatBox.setReplyContext(line.getMessageID(), line.player);
                }
                return;
            }
            int mediaIndex = line.getClickedMediaIndex(mouseX, mouseY);
            if (mediaIndex >= 0) {
                imageViewer.open(line.getGalleryImages(), mediaIndex);
                return;
            }
        }

        if (chatBox != null) chatBox.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground(mouseX, mouseY);

        int listTop = 0;
        int listLeft = 0;
        int listRight = this.width;
        int listBottom = this.height - COMPOSER_HEIGHT - COMPOSER_GAP;

        UI_WIDTH = listRight - listLeft;

        List<MessageData> messages = GlobalChat.messages;
        messages.sort(Comparator.comparing(o -> o.timestamp));

        int maxHeight = listBottom - listTop;

        if (messages.size() != prevMessageCount) {
            if (messages.size() > prevMessageCount) {
                autoScroll = true;
            }
            prevMessageCount = messages.size();
        }

        int totalContentHeight = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageData msg = messages.get(i);
            if (msg.message == null || msg.message.message == null) continue;
            ChatLine line = getOrCreateLine(msg);
            totalContentHeight += line.getHeight();
        }

        int maxScroll = Math.max(0, totalContentHeight - maxHeight);
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));

        if (autoScroll) {
            scrollTarget = 0;
        }

        renderedLines.clear();
        String lastPlayer = null;
        long lastTimestamp = 0;
        int accumulatedHeight = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageData msg = messages.get(i);
            Message message = msg.message;
            if (message == null || message.message == null) continue;

            ChatLine line = getOrCreateLine(msg);

            if (lastPlayer != null && message.player != null && message.player.equals(lastPlayer)
                    && lastTimestamp - msg.timestamp <= 600000) {
                line.showHeader = false;
            } else {
                line.showHeader = true;
            }
            lastPlayer = message.player;
            lastTimestamp = msg.timestamp;

            int lineHeight = line.getHeight();
            accumulatedHeight += lineHeight;
            int drawY = (int) (listBottom - accumulatedHeight + scrollOffset);

            if (drawY + lineHeight < listTop) continue;
            if (drawY > listBottom) break;

            line.drawLine(listLeft + 8, drawY, mouseX, mouseY);
            renderedLines.add(line);
        }

        if (chatBox != null) {
            chatBox.setStatusMessage(SyncChecker.getStatusMessage());
            chatBox.draw();
        }

        Gui.drawRect(0, listBottom, this.width, listBottom + 1, new Color(0, 0, 0, 60).getRGB());

        boolean closeHovered = isHovered(closeX, closeY, closeW, closeH, mouseX, mouseY);
        drawRect(closeX, closeY, closeX + closeW, closeY + closeH,
                closeHovered ? new Color(240, 71, 71, 200).getRGB() : new Color(129, 131, 132, 150).getRGB());
        this.fontRendererObj.drawString("\u2715", closeX + 4, closeY + 2, -1);

        imageViewer.draw(this.width, this.height, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBackground(int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, new Color(25, 26, 28, 245).getRGB());
        Gui.drawRect(0, 0, this.width, 1, new Color(88, 101, 242, 80).getRGB());
    }

    private ChatLine getOrCreateLine(MessageData msg) {
        ChatLine line = cache.get(msg);
        if (line == null) {
            line = ChatHelper.getContent(msg.message, msg.timestamp);
            cache.put(msg, line);
        }
        return line;
    }

    private boolean isHovered(int x, int y, int w, int h, int mX, int mY) {
        return mX > x && mX < x + w && mY > y && mY < y + h;
    }
}
