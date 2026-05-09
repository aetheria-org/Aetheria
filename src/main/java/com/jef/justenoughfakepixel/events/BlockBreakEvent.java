package com.jef.justenoughfakepixel.events;

import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BlockBreakEvent extends Event {
    public final BlockPos pos;

    public BlockBreakEvent(BlockPos pos) {
        this.pos = pos;
    }
}
