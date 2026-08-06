package io.hamlook.aetheria.features.chat.globalchat.vars;

import io.hamlook.aetheria.features.chat.emoji.SpritePos;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class EmojiRef {

    public String url;
    public String id;
    public boolean animated;
    public transient String name = "Unknown";
    /** Raw unicode characters for default (non-custom) emojis; empty for Discord server emojis. */
    public String surrogates = "";
    /** True when this emoji is drawn from a local spritesheet instead of a URL. */
    public transient boolean sprite = false;
    /** Spritesheet texture (custom sheet for animated/custom emojis; null = resolve the configured theme sheet at draw time). */
    public transient ResourceLocation spriteTexture = null;
    /** Top-left pixel of the cell in the default emoji sheet. */
    public transient int spriteX = -1, spriteY = -1;
    /** Animation frames for custom animated emojis (pixel positions in the custom sheet). */
    public transient List<SpritePos> frames = null;
    /** Custom emoji frame size + delay. */
    public transient int frameW = 0, frameH = 0, frametime = 0;

    public EmojiRef(String url, String id, boolean animated, String name){
        this.url = url;
        this.id = id;
        this.animated = animated;
        this.name = name;
    }

    public EmojiRef(boolean animated, String name, String id){
        this.id = id;
        this.url = "https://cdn.discordapp.com/emojis/" + id + (animated ? ".gif" : ".png");
        this.name = name;
        this.animated = animated;
    }
}
