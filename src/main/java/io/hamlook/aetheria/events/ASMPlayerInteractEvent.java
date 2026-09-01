package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.util.BlockPos;

public class ASMPlayerInteractEvent extends AetheriaEvent {

    public final int action;
    public final BlockPos pos;

    public ASMPlayerInteractEvent(int action, BlockPos pos) {
        this.action = action;
        this.pos = pos;
    }
}
