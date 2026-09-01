package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.util.BlockPos;

public class BlockBreakEvent extends AetheriaEvent {
    public final BlockPos pos;

    public BlockBreakEvent(BlockPos pos) {
        this.pos = pos;
    }
}
