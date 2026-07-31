package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EmojiRef {

    public String url;
    public String id;
    public boolean animated;
    public transient String name = "Unknown";

    public EmojiRef(boolean animated,String name,String id){
        this.id = id;
        this.url = "https://cdn.discordapp.com/emojis/" + id + (animated ? ".gif" : ".png");
        this.name = name;
        this.animated = animated;
    }
}
