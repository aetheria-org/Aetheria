package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.SyncChecker;
import io.hamlook.aetheria.features.chat.globalchat.vars.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.HashMap;

public class ChatBox {

    private final FontRenderer fontRenderer;
    private GuiTextField textField;
    private int x, y, width, height;
    private String editingMessageID = null;
    private String replyToMessageID = null;
    private String replyToAuthor = null;
    private String statusMessage = null;

    public ChatBox(FontRenderer fontRenderer, int x, int y, int width, int height) {
        this.fontRenderer = fontRenderer;
        setBounds(x, y, width, height);
    }

    public void setReplyContext(String messageID, String author) {
        this.replyToMessageID = messageID;
        this.replyToAuthor = author;
        textField.setFocused(true);
    }

    public void setStatusMessage(String msg) {
        this.statusMessage = msg;
    }

    public void cancelReply() {
        replyToMessageID = null;
        replyToAuthor = null;
    }

    public boolean hasReply() {
        return replyToMessageID != null;
    }

    public String getReplyToMessageID() {
        return replyToMessageID;
    }

    public String getReplyToAuthor() {
        return replyToAuthor;
    }

    public void enterEditMode(String messageID, String currentText) {
        this.editingMessageID = messageID;
        textField.setText(currentText);
        textField.setFocused(true);
    }

    public boolean isEditing() {
        return editingMessageID != null;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        String preservedText = (this.textField != null) ? this.textField.getText() : "";
        boolean wasFocused = (this.textField == null) || this.textField.isFocused();

        this.textField = new GuiTextField(0, fontRenderer, x, y, width, height);
        this.textField.setMaxStringLength(500);
        this.textField.setEnableBackgroundDrawing(false);
        this.textField.setText(preservedText);
        this.textField.setFocused(wasFocused);
    }

    public void draw() {
        int composerY = y;

        if (replyToMessageID != null) {
            composerY = y - fontRenderer.FONT_HEIGHT - 6;
        }

        drawRect(x - 4, composerY - 4, x + width + 4, y + height + 4, new Color(32, 34, 37, 255).getRGB());
        if (replyToMessageID != null) {
            drawRect(x - 4, composerY - 4, x - 1, y + height + 4, new Color(88, 101, 242, 200).getRGB());
            String replyText = "Replying to " + (replyToAuthor != null ? replyToAuthor : "Unknown");
            fontRenderer.drawString(replyText, x + 2, composerY, 0xAAAAAA);
            String cancelChar = "X";
            int cancelX = x + width - fontRenderer.getStringWidth(cancelChar) - 2;
            fontRenderer.drawString(cancelChar, cancelX, composerY, 0xFF6666);
        }

        if (statusMessage != null) {
            fontRenderer.drawString(statusMessage, x + 2, composerY - fontRenderer.FONT_HEIGHT - 2, 0xFF6666);
        }

        if (textField.getText().isEmpty() && !textField.isFocused()) {
            fontRenderer.drawString("Message Global Chat...", x + 2, y + (height - fontRenderer.FONT_HEIGHT) / 2,
                    0x72767D);
        }

        textField.drawTextBox();
    }

    public boolean isCancelReplyClicked(int mouseX, int mouseY) {
        if (replyToMessageID == null) return false;
        String cancelChar = "X";
        int cancelX = x + width - fontRenderer.getStringWidth(cancelChar) - 2;
        int cancelY = y - fontRenderer.FONT_HEIGHT - 6;
        return mouseX > cancelX && mouseX < cancelX + fontRenderer.getStringWidth(cancelChar)
                && mouseY > cancelY && mouseY < cancelY + fontRenderer.FONT_HEIGHT;
    }

    public void tick() {
        textField.updateCursorCounter();
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            trySend();
            return true;
        }
        return textField.textboxKeyTyped(typedChar, keyCode);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        textField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void trySend() {
        String text = textField.getText();
        if (text == null || text.trim().isEmpty()) return;

        if (!SyncChecker.isSynced()) {
            statusMessage = SyncChecker.getStatusMessage();
            return;
        }
        statusMessage = null;
        if (editingMessageID != null) {
            GlobalChat.editMessage(editingMessageID, text);
            editingMessageID = null;
            textField.setText("");
        } else {
            String username = Minecraft.getMinecraft().getSession().getUsername();

            Message.Content content = new Message.Content();
            content.content = text;
            content.stickers = new HashMap<>();
            content.emojiRefs = new HashMap<>();
            content.attachments = new HashMap<>();

            String skinUrl = "https://capeapi.qzz.io/avatar/" + username.toLowerCase() + ".png";

            Message message = new Message(content, username, skinUrl, username + "-mc", null, replyToMessageID, replyToAuthor, null);

            GlobalChat.send(message);
            textField.setText("");
            cancelReply();
        }
    }

    public String getText() {
        return textField.getText();
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }

    public void cancelEdit() {
        editingMessageID = null;
        textField.setText("");
    }
}