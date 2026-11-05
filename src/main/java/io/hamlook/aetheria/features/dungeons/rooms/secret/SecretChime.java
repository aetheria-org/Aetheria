package io.hamlook.aetheria.features.dungeons.rooms.secret;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.features.dungeons.SecretSoundsConfig;
import io.hamlook.aetheria.core.features.dungeons.SecretsConfig;
import io.hamlook.aetheria.events.SecretGainedEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;

@RegisterEvents
public class SecretChime {

    private static final String[] SOUNDS = {
            "mob.blaze.hit", "fire.ignite", "random.orb", "random.break",
            "mob.guardian.land.hit", "note.pling"};
    private static final String FALLBACK_SOUND = SOUNDS[0];
    private static final long DEBOUNCE_MS = 50L;

    private static long lastPlayed;

    @HandleEvent
    public void onSecretGained(SecretGainedEvent event) {
        SecretsConfig config = SecretsConfig.get();
        if (config == null) return;

        SecretSoundsConfig settings = config.sounds;
        if (settings == null || !settings.enabled) return;

        long now = System.currentTimeMillis();
        if (now - lastPlayed < DEBOUNCE_MS) return;
        lastPlayed = now;

        play(settings);
    }

    public static void playTest() {
        SecretsConfig config = SecretsConfig.get();
        if (config == null) return;
        play(config.sounds);
    }

    private static void play(SecretSoundsConfig settings) {
        SoundUtils.playSound(name(settings), settings.volume, settings.pitch);
    }

    private static String name(SecretSoundsConfig settings) {
        if (settings.sound >= 0 && settings.sound < SOUNDS.length) return SOUNDS[settings.sound];
        if (settings.customSound == null) return FALLBACK_SOUND;

        String custom = settings.customSound.trim();
        return custom.isEmpty() ? FALLBACK_SOUND : custom;
    }
}
