package io.hamlook.aetheria.features.chat.globalchat.ui.util;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.features.chat.globalchat.SyncChecker;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.features.chat.globalchat.vars.Reaction;
import io.hamlook.aetheria.utils.render.RenderUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatLine {

    public List<ChatObject> objects;
    public String player;
    public String timestamp;
    public GCImage skin;
    @Getter
    public String messageID;
    @Getter
    public String rawContent;

    public String replyTo;
    public String replyAuthor;
    public String replyPreview;
    public HashMap<String, Reaction> reactions = new HashMap<>();

    public static int PADDING = 4;
    public static int AVATAR_SIZE = 20;
    public static int AVATAR_GAP = 6;
    public static int NAME_GAP = 2;
    public static int NAME_COLOR = 0x5865F2;
    public static int REPLY_COLOR = 0x8E9297;
    public static int REPLY_ACCENT_COLOR = 0x5865F2;
    public static int EDIT_LABEL_COLOR = 0xAAAAAA;
    public static int EDIT_LABEL_COLOR_HOVERED = 0xFFFFFF;
    public static int EMBED_LABEL_COLOR = 0x8E9297;

    public static int EMOJI_JUMBO_SIZE = 44;
    public static int STICKER_SIZE = 88;
    public static int MEDIA_MAX_SIZE = 100;
    public static int MEDIA_MIN_SIZE = 50;
    public static int MEDIA_GAP = 6;

    public static int REACTION_SIZE = 18;
    public static int REACTION_PAD = 8;

    public boolean showHeader = true;

    private boolean editIconVisible = false;
    private boolean replyIconVisible = false;
    private int editIconX, editIconY, editIconW, editIconH;
    private int replyIconX, replyIconY, replyIconW, replyIconH;

    private final List<int[]> mediaBounds = new ArrayList<>();

    private boolean isEditing = false;
    private GuiTextField editField;
    private String editingText;

    public ChatLine(ChatObject... contents){
        objects = new ArrayList<>(Arrays.asList(contents));
    }

    private static class LaidOutItem {
        ChatObject object;
        int width;
        int height;
        int imageHeight;
        String label;
        int xOffset;
    }

    private static class Row {
        List<LaidOutItem> items = new ArrayList<>();
        int height = 0;
        boolean isMediaRow = false;
    }

    public void startEditing() {
        isEditing = true;
        editingText = rawContent;
    }

    public void confirmEdit() {
        if (editField != null) {
            String newText = editField.getText();
            if (!newText.equals(rawContent)) {
                if (SyncChecker.isSynced()) {
                    GlobalChat.editMessage(messageID, newText);
                }
            }
        }
        isEditing = false;
        editField = null;
    }

    public void cancelEdit() {
        isEditing = false;
        editField = null;
    }

    public boolean isEditing() {
        return isEditing;
    }

    public boolean handleKeyTyped(char typedChar, int keyCode) {
        if (!isEditing || editField == null) return false;
        if (keyCode == 28 || keyCode == 156) {
            confirmEdit();
            return true;
        }
        return editField.textboxKeyTyped(typedChar, keyCode);
    }

    private List<String> wordWrap(String text, int maxWidth, FontRenderer fr) {
        List<String> lines = new ArrayList<>();
        while (text != null && !text.isEmpty()) {
            if (fr.getStringWidth(text) <= maxWidth) {
                lines.add(text);
                return lines;
            }
            int lastSpace = -1;
            int breakIdx = text.length();
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == ' ') lastSpace = i;
                if (fr.getStringWidth(text.substring(0, i + 1)) > maxWidth) {
                    breakIdx = i;
                    break;
                }
            }
            int splitPos;
            if (lastSpace > 0) {
                splitPos = lastSpace;
                lines.add(text.substring(0, splitPos));
                text = text.substring(splitPos + 1);
            } else {
                splitPos = Math.max(breakIdx, 1);
                lines.add(text.substring(0, splitPos));
                text = text.substring(splitPos);
            }
        }
        return lines;
    }

    private List<Row> computeLayout(FontRenderer fr) {
        List<ChatObject> textLike = new ArrayList<>();
        List<ChatObject> mediaLike = new ArrayList<>();
        for (ChatObject o : objects) {
            boolean isMedia = o.type == ChatObject.ObjectType.STICKER
                    || o.type == ChatObject.ObjectType.ATTACHMENT
                    || o.type == ChatObject.ObjectType.EMBED;
            (isMedia ? mediaLike : textLike).add(o);
        }

        boolean hasRealText = textLike.stream().anyMatch(o ->
                o.type == ChatObject.ObjectType.TEXT && o.text != null && !o.text.trim().isEmpty());

        List<Row> rows = new ArrayList<>();
        int wrapWidth = ChatUI.UI_WIDTH - AVATAR_SIZE - AVATAR_GAP;

        Row current = new Row();
        int xOff = 0;
        for (ChatObject object : textLike) {
            if (object.type == ChatObject.ObjectType.TEXT) {
                String text = object.text;
                if (text == null || text.isEmpty()) continue;
                int textWidth = fr.getStringWidth(text);

                if (xOff + textWidth <= wrapWidth) {
                    LaidOutItem item = new LaidOutItem();
                    item.object = object;
                    item.width = textWidth;
                    item.height = fr.FONT_HEIGHT;
                    item.imageHeight = item.height;
                    item.xOffset = xOff;
                    current.items.add(item);
                    current.height = Math.max(current.height, item.height);
                    xOff += textWidth;
                } else {
                    if (xOff > 0) {
                        rows.add(current);
                        current = new Row();
                        xOff = 0;
                    }
                    List<String> wrappedLines = wordWrap(text, wrapWidth, fr);
                    for (int li = 0; li < wrappedLines.size(); li++) {
                        String line = wrappedLines.get(li);
                        if (line.isEmpty()) continue;
                        if (li > 0) {
                            rows.add(current);
                            current = new Row();
                        }
                        LaidOutItem item = new LaidOutItem();
                        item.object = new ChatObject(null, line);
                        item.width = fr.getStringWidth(line);
                        item.height = fr.FONT_HEIGHT;
                        item.imageHeight = item.height;
                        item.xOffset = 0;
                        current.items.add(item);
                        current.height = Math.max(current.height, item.height);
                        xOff = item.width;
                    }
                }
            } else {
                int size = hasRealText ? fr.FONT_HEIGHT : EMOJI_JUMBO_SIZE;
                if (xOff + size > wrapWidth && xOff > 0) {
                    rows.add(current);
                    current = new Row();
                    xOff = 0;
                }
                LaidOutItem item = new LaidOutItem();
                item.object = object;
                item.width = size;
                item.height = size;
                item.imageHeight = item.height;
                item.xOffset = xOff;
                current.items.add(item);
                current.height = Math.max(current.height, item.height);
                xOff += size;
            }
        }
        if (!current.items.isEmpty()) rows.add(current);

        if (!mediaLike.isEmpty()) {
            boolean soloSticker = objects.size() == 1 && mediaLike.size() == 1
                    && mediaLike.get(0).type == ChatObject.ObjectType.STICKER;

            Row mediaRow = new Row();
            mediaRow.isMediaRow = true;
            int mediaX = 0;

            for (ChatObject object : mediaLike) {
                LaidOutItem item = new LaidOutItem();
                item.object = object;

                int imgW, imgH;
                if (object.type == ChatObject.ObjectType.STICKER && soloSticker) {
                    imgW = imgH = STICKER_SIZE;
                } else {
                    int[] dims = mediaDimensions(object.image);
                    imgW = dims[0];
                    imgH = dims[1];
                }

                boolean hasLabel = object.type == ChatObject.ObjectType.EMBED && object.embedLabel != null;
                int labelHeight = hasLabel ? fr.FONT_HEIGHT + 2 : 0;

                item.width = imgW;
                item.imageHeight = imgH;
                item.height = imgH + labelHeight;
                item.label = hasLabel ? object.embedLabel : null;

                item.xOffset = mediaX;
                mediaRow.items.add(item);
                mediaRow.height = Math.max(mediaRow.height, item.height);
                mediaX += item.width + MEDIA_GAP;

                if (mediaX > wrapWidth) {
                    rows.add(mediaRow);
                    mediaRow = new Row();
                    mediaRow.isMediaRow = true;
                    mediaX = 0;
                }
            }
            if (!mediaRow.items.isEmpty()) rows.add(mediaRow);
        }

        return rows;
    }

    private int[] mediaDimensions(GCImage image) {
        if (image == null || image.width <= 0 || image.height <= 0) {
            return new int[]{MEDIA_MIN_SIZE, MEDIA_MIN_SIZE};
        }
        int w = image.width;
        int h = image.height;
        int maxDim = Math.max(w, h);
        if (maxDim <= MEDIA_MAX_SIZE) {
            return new int[]{w, h};
        }
        float scale = MEDIA_MAX_SIZE / (float) maxDim;
        return new int[]{Math.round(w * scale), Math.round(h * scale)};
    }

    public int drawLine(int xPos, int yPos, int mouseX, int mouseY) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        mediaBounds.clear();

        List<Row> rows = computeLayout(fr);
        int nameHeight = showHeader ? fr.FONT_HEIGHT + NAME_GAP : 0;
        int contentHeight = 0;
        for (Row row : rows) contentHeight += row.height;
        int totalHeight = showHeader ? Math.max(AVATAR_SIZE, nameHeight + contentHeight) : contentHeight;

        int lineWidth = ChatUI.UI_WIDTH - AVATAR_SIZE - AVATAR_GAP;

        int replyHeight = 0;
        if (showHeader && replyTo != null) {
            replyHeight = fr.FONT_HEIGHT + NAME_GAP;
        }

        if (showHeader) {
            drawAvatar(xPos, yPos, mouseX, mouseY);
            int contentX = xPos + AVATAR_SIZE + AVATAR_GAP;

            if (replyTo != null) {
                int rY = yPos;
                Gui.drawRect(contentX, rY + fr.FONT_HEIGHT / 2, contentX + 2, rY + fr.FONT_HEIGHT / 2 + replyHeight, REPLY_ACCENT_COLOR);
                String replyLabel = "Replying to §" + (replyAuthor != null ? replyAuthor : "Unknown");
                fr.drawString("\u21A9 " + (replyAuthor != null ? replyAuthor : "Unknown"), contentX + 8, rY, REPLY_COLOR);
                if (replyPreview != null && !replyPreview.isEmpty()) {
                    String preview = replyPreview.length() > 40 ? replyPreview.substring(0, 40) + "..." : replyPreview;
                    fr.drawString(preview, contentX + 8 + fr.getStringWidth(replyAuthor != null ? replyAuthor : "Unknown") + 16, rY, 0x666666);
                }
            }

            fr.drawString(player + " - " + timestamp, contentX, yPos + replyHeight, NAME_COLOR);

            boolean lineHovered = isHovered(xPos, yPos, lineWidth, totalHeight, mouseX, mouseY);
            editIconVisible = lineHovered && isOwnMessage();
            replyIconVisible = lineHovered;

            int rightX = xPos + lineWidth;
            if (editIconVisible) {
                String label = "Edit";
                editIconW = fr.getStringWidth(label);
                editIconH = fr.FONT_HEIGHT;
                editIconX = rightX - editIconW;
                editIconY = yPos;
                boolean iconHovered = isHovered(editIconX, editIconY, editIconW, editIconH, mouseX, mouseY);
                fr.drawString(label, editIconX, editIconY, iconHovered ? EDIT_LABEL_COLOR_HOVERED : EDIT_LABEL_COLOR);
                rightX = editIconX - 12;
            }
            if (replyIconVisible) {
                String label = "Reply";
                replyIconW = fr.getStringWidth(label);
                replyIconH = fr.FONT_HEIGHT;
                replyIconX = rightX - replyIconW;
                replyIconY = yPos;
                boolean iconHovered = isHovered(replyIconX, replyIconY, replyIconW, replyIconH, mouseX, mouseY);
                fr.drawString(label, replyIconX, replyIconY, iconHovered ? EDIT_LABEL_COLOR_HOVERED : EDIT_LABEL_COLOR);
            }
        } else {
            editIconVisible = false;
            replyIconVisible = false;
        }

        int contentX = xPos + AVATAR_SIZE + AVATAR_GAP;
        int rowY = yPos + nameHeight + replyHeight;

        if (isEditing) {
            int efX = contentX;
            int efY = rowY;
            int efW = lineWidth;
            int efH = fr.FONT_HEIGHT + 6;

            if (editField == null || editField.xPosition != efX || editField.yPosition != efY) {
                editField = new GuiTextField(0, fr, efX, efY, efW, efH);
                editField.setText(editingText != null ? editingText : "");
                editField.setFocused(true);
                editField.setMaxStringLength(2000);
                editField.setCursorPositionEnd();
                editField.setEnableBackgroundDrawing(true);
            }

            Gui.drawRect(efX - 1, efY - 1, efX + efW + 1, efY + efH + 1, 0xFF1E1F22);
            editField.drawTextBox();
            editingText = editField.getText();

            rowY += efH + 4;
            for (Row row : rows) {
                if (row.isMediaRow) {
                    for (LaidOutItem item : row.items) {
                        int itemX = contentX + item.xOffset;
                        int imgY = rowY;
                        if (item.label != null) {
                            fr.drawString(item.label, itemX, imgY, EMBED_LABEL_COLOR);
                            imgY += fr.FONT_HEIGHT + 2;
                        }
                        drawImage(item.object.image, itemX, imgY, mouseX, mouseY, item.width, item.imageHeight);
                        mediaBounds.add(new int[]{itemX, imgY, item.width, item.imageHeight});
                    }
                    rowY += row.height;
                }
            }

            return totalHeight + PADDING;
        }

        for (Row row : rows) {
            for (LaidOutItem item : row.items) {
                int itemX = contentX + item.xOffset;

                if (row.isMediaRow) {
                    int imgY = rowY;
                    if (item.label != null) {
                        fr.drawString(item.label, itemX, imgY, EMBED_LABEL_COLOR);
                        imgY += fr.FONT_HEIGHT + 2;
                    }
                    drawImage(item.object.image, itemX, imgY, mouseX, mouseY, item.width, item.imageHeight);
                    mediaBounds.add(new int[]{itemX, imgY, item.width, item.imageHeight});
                } else {
                    int itemY = rowY + (row.height - item.height) / 2;
                    if (!item.object.isImage()) {
                        if (item.object.text != null) {
                            fr.drawString(item.object.text, itemX, itemY, -1);
                        }
                    } else {
                        drawImage(item.object.image, itemX, itemY, mouseX, mouseY, item.width, item.height);
                    }
                }
            }
            rowY += row.height;
        }

        return totalHeight + PADDING;
    }

    private boolean isOwnMessage() {
        if (player == null) return false;
        String username = Minecraft.getMinecraft().getSession().getUsername();
        return username != null && username.equalsIgnoreCase(player);
    }

    public boolean isEditClicked(int mouseX, int mouseY) {
        return editIconVisible && isHovered(editIconX, editIconY, editIconW, editIconH, mouseX, mouseY);
    }

    public boolean isReplyClicked(int mouseX, int mouseY) {
        return replyIconVisible && isHovered(replyIconX, replyIconY, replyIconW, replyIconH, mouseX, mouseY);
    }

    public String getClickedReactionKey(int mouseX, int mouseY) {
        return null;
    }

    public List<GCImage> getGalleryImages() {
        List<GCImage> gallery = new ArrayList<>();
        for (ChatObject o : objects) {
            if (o.type == ChatObject.ObjectType.STICKER || o.type == ChatObject.ObjectType.ATTACHMENT
                    || o.type == ChatObject.ObjectType.EMBED) {
                gallery.add(o.image);
            }
        }
        return gallery;
    }

    public int getClickedMediaIndex(int mouseX, int mouseY) {
        for (int i = 0; i < mediaBounds.size(); i++) {
            int[] b = mediaBounds.get(i);
            if (isHovered(b[0], b[1], b[2], b[3], mouseX, mouseY)) return i;
        }
        return -1;
    }

    private void drawAvatar(int xPos, int yPos, int mX, int mY) {
        if (skin == null) return;
        boolean hovered = isHovered(xPos, yPos, AVATAR_SIZE, AVATAR_SIZE, mX, mY);
        ResourceLocation frame = skin.getTextureToRender(hovered);
        if (frame == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(frame);
        RenderUtils.drawTexturedRect(xPos, yPos, AVATAR_SIZE, AVATAR_SIZE, 0f, 1f, 0f, 1f);
        GlStateManager.popMatrix();
    }

    private void drawImage(GCImage image, int xPos, int yPos, int mX, int mY, int width, int height) {
        ResourceLocation frame = image.getTextureToRender(isHovered(xPos, yPos, width, height, mX, mY));
        if (frame == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(frame);
        RenderUtils.drawTexturedRect(xPos, yPos, width, height, 0f, 1f, 0f, 1f);
        GlStateManager.popMatrix();
    }

    private boolean isHovered(int x, int y, int w, int h, int mX, int mY) {
        return mX > x && mX < x + w && mY > y && mY < y + h;
    }

    public int getHeight() {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int nameHeight = showHeader ? fr.FONT_HEIGHT + NAME_GAP : 0;

        List<Row> rows = computeLayout(fr);
        int contentHeight = 0;
        for (Row row : rows) contentHeight += row.height;

        int totalHeight = showHeader ? Math.max(AVATAR_SIZE, nameHeight + contentHeight) : contentHeight;

        if (!reactions.isEmpty()) {
            int wrapWidth = ChatUI.UI_WIDTH - AVATAR_SIZE - AVATAR_GAP;
            int rX = 0;
            int rY = 0;
            for (Map.Entry<String, Reaction> entry : reactions.entrySet()) {
                Reaction r = entry.getValue();
                if (r.count <= 0) continue;
                String display = r.name;
                if (display != null && display.length() > 5) display = display.substring(0, 5);
                String badge = (display != null ? display : "?") + " " + r.count;
                int bw = fr.getStringWidth(badge) + 8;
                if (rX + bw > wrapWidth) {
                    rX = 0;
                    rY += REACTION_SIZE + 2;
                }
                rX += bw + 4;
            }
            int reactionH = rY + REACTION_SIZE + 2;
            if (reactionH > 0) totalHeight += reactionH + 4;
        }

        return totalHeight + PADDING;
    }
}
