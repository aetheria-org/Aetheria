package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.util.BlockPos;

public class PlaySoundEvent extends AetheriaEvent {

    public final String soundName;
    public final double x;
    public final double y;
    public final double z;
    public final float pitch;
    public final float volume;

    public PlaySoundEvent(String soundName, double x, double y, double z, float pitch, float volume) {
        this.soundName = soundName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.volume = volume;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }
}
