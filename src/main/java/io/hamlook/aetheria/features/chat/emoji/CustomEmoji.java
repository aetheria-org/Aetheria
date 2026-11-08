package io.hamlook.aetheria.features.chat.emoji;

import java.util.ArrayList;
import java.util.List;

public class CustomEmoji {
    public String shortcode;
    public List<SpritePos> sprites = new ArrayList<>();
    public int frametime;
    public int width;
    public int height;
    public boolean animated;
}
