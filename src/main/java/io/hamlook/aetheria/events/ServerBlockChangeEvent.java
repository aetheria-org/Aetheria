package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class ServerBlockChangeEvent extends AetheriaEvent {

    public final BlockPos pos;
    public final IBlockState newState;

    public ServerBlockChangeEvent(BlockPos pos, IBlockState newState) {
        this.pos = pos;
        this.newState = newState;
    }

    public IBlockState getOldState() {
        return Minecraft.getMinecraft().theWorld.getBlockState(pos);
    }
}
