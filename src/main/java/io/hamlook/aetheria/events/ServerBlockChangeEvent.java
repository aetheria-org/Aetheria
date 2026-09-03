package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public class ServerBlockChangeEvent extends AetheriaEvent {

    public final BlockPos pos;
    public final IBlockState newState;

    public ServerBlockChangeEvent(BlockPos pos, IBlockState newState) {
        this.pos = pos;
        this.newState = newState;
    }

    public IBlockState getOldState() {
        return MinecraftCompat.getMinecraft().theWorld.getBlockState(pos);
    }
}
