package io.hamlook.aetheria.features.chat.globalchat.vars;

import io.hamlook.aetheria.features.chat.globalchat.GlobalChat;
import io.hamlook.aetheria.repo.CapeAPI;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ChatMessage {

    public String messageID,discordID;
    public String channelId,author,replyingMessage,avatar;
    public String content;
    public boolean replying;
    public HashMap<String,EmojiRef> emojiRefs;
    public HashMap<String,Sticker> stickers;
    public long timestamp;
    public List<Attachment> attachments;

    public ChatMessage(String content,String channelID,ChatMessage repliedMessage) {
        this.content = content;
        this.channelId = channelID;
        this.replying = repliedMessage != null;
        this.replyingMessage = replying ? repliedMessage.discordID : null;
        this.discordID = null;
        this.author = Minecraft.getMinecraft().getSession().getUsername();
        this.avatar = CapeAPI.getAPIUrl("avatar") + "/" + author + ".png";
        this.messageID = author + "-" + System.nanoTime() + "-" + channelID;
        this.timestamp = System.currentTimeMillis();
        emojiRefs = new HashMap<>();
        stickers = new HashMap<>();
        attachments = new ArrayList<>();
    }

    public ChatMessage addEmojiRefs(HashMap<String,EmojiRef> emojiRefs) {
        this.emojiRefs.putAll(emojiRefs);
        return this;
    }
    public ChatMessage addStickers(HashMap<String,Sticker> stickers) {
        this.stickers.putAll(stickers);
        return this;
    }
    public ChatMessage addAttachments(List<Attachment> attachments) {
        this.attachments.addAll(attachments);
        return this;
    }

    public void sendMessage() {
        GlobalChat.sendMessage(this);
    }

    public List<ChatLine> getLines() {
        return Collections.singletonList(new ChatLine(this));
    }
}