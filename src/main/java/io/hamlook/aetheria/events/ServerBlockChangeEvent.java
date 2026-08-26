package io.hamlook.aetheria.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ServerBlockChangeEvent extends Event {

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
