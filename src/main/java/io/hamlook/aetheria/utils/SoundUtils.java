package io.hamlook.aetheria.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

public class SoundUtils {

    private SoundUtils() {}

    public static void playSound(String sound, float volume, float pitch) {
        try {
            ISound s = new PositionedSound(new ResourceLocation(sound)) {{
                repeat = false;
                repeatDelay = 0;
                attenuationType = ISound.AttenuationType.NONE;
            }};
            Minecraft.getMinecraft().getSoundHandler().playSound(s);
        } catch (Exception ignored) {}
    }

    public static void playSound(String sound) {
        playSound(sound, 1.0f, 1.0f);
    }
}
