package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class BlockClickEvent extends AetheriaEvent {

    public final BlockPos pos;
    public final EnumFacing facing;

    public BlockClickEvent(BlockPos pos, EnumFacing facing) {
        this.pos = pos;
        this.facing = facing;
    }

    public IBlockState getBlockState() {
        return MinecraftCompat.getLocalWorld().getBlockState(pos);
    }
}
