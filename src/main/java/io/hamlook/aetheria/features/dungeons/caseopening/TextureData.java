package io.hamlook.aetheria.features.dungeons.caseopening;

import lombok.Getter;
import net.minecraft.util.ResourceLocation;

@Getter
public class TextureData {

    private final ResourceLocation rl;
    private final int frameTime;
    private final int frames;

    public TextureData(ResourceLocation rl, int frameTime, int frames) {
        this.rl = rl;
        this.frameTime = frameTime;
        this.frames = frames;
    }

}