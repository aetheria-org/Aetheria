package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.SoundCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
//#if FORGE
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;
//#else
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.sound.PositionedSoundInstance;
//$$ import net.minecraft.sound.SoundEvent;
//#endif

public class SoundUtils {

    private SoundUtils() {}

    public static void playSound(String sound, float vol, float pit) {
        try {
            String soundName =
                    //#if MC < 1.16
                    sound
                    //#else
                    //$$ SoundCompat.getModernSoundName(sound)
                    //#endif
                    ;
            //#if FORGE
            ISound s = new PositionedSound(new ResourceLocation(soundName)) {{
                this.volume = vol;
                this.pitch = pit;
                repeat = false;
                repeatDelay = 0;
                attenuationType = ISound.AttenuationType.NONE;
            }};
            MinecraftCompat.getMinecraft().getSoundHandler().playSound(s);
            //#else
            //$$ String clean = soundName.replaceAll("[^a-z0-9/._-]", "");
            //$$ SoundEvent event = SoundEvent.of(TextCompat.createResourceLocation(clean));
            //$$ MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(event, pit, vol));
            //#endif
        } catch (Exception ignored) {}
    }

    public static void playSound(String sound) {
        playSound(sound, 1.0f, 1.0f);
    }
}
