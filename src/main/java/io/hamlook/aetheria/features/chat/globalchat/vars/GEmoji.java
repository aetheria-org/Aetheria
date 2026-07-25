package io.hamlook.aetheria.features.chat.globalchat.vars;


public class GEmoji {

    public String id;
    public boolean animated;
    public transient AnimatedImage animatedImage;

    public GEmoji(String id,boolean animated) {
        this.id = id;
        this.animated = animated;
        animatedImage = null;
    }

    public String constructURL(){
        return "https://cdn.discordapp.com/emojis/" + id + (animated ? ".gif" : ".png");
    }

}
