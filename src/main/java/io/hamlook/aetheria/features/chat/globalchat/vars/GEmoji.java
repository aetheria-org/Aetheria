package io.hamlook.aetheria.features.chat.globalchat.vars;


public class GEmoji {

    public String id;
    public boolean animated;

    public GEmoji(String id,boolean animated) {
        this.id = id;
        this.animated = animated;
    }

    public String constructURL(){
        return "https://cdn.discordapp.com/emojis/" + id + (animated ? ".gif" : ".png");
    }

}
