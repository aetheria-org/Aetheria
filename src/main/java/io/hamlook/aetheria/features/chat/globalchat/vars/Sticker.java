package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Sticker {

    public String url;
    public boolean animated;
    public transient AnimatedImage animatedImage;

    public Sticker(String url,boolean animated) {
        this.url = url;
        this.animated = animated;
        animatedImage = null;
    }

}
