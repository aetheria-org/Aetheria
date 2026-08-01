package io.hamlook.aetheria.features.chat.globalchat.vars;

import io.hamlook.aetheria.features.chat.emoji.SpritePos;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class IEmoji {

    public String id,identifier,shortcode,url;
    public boolean animated;
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

    public IEmoji() {}

    public IEmoji(String id, String identifier, String shortcode, String url, boolean animated) {
        this.id = id;
        this.identifier = identifier;
        this.shortcode = shortcode;
        this.url = url;
        this.animated = animated;
    }

    public EmojiRef toEmoji(){
        EmojiRef ref = new EmojiRef(
                url,id,animated,shortcode.replace(":","")
        );
        ref.surrogates = surrogates;
        ref.sprite = sprite;
        ref.spriteTexture = spriteTexture;
        ref.spriteX = spriteX;
        ref.spriteY = spriteY;
        ref.frames = frames;
        ref.frameW = frameW;
        ref.frameH = frameH;
        ref.frametime = frametime;
        return ref;
    }

}
