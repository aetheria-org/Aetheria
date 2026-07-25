package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;

import java.util.HashMap;

@AllArgsConstructor
public class Message {

    public String content;
    public HashMap<String,Sticker> stickers;
    public HashMap<String,GEmoji> emojiRefs;

}
