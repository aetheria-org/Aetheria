package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;

public class ASMPlayerInteractEvent extends AetheriaEvent {

    public static final int RIGHT_CLICK_BLOCK = 1;
    public static final int INTERACT_ENTITY = 2;

    public final int action;
    public final BlockPos pos;
    // set only for entity clicks, pos is the entity block position there
    public final Entity entity;

    public ASMPlayerInteractEvent(int action, BlockPos pos) {
        this(action, pos, null);
    }

    public ASMPlayerInteractEvent(int action, BlockPos pos, Entity entity) {
        this.action = action;
        this.pos = pos;
        this.entity = entity;
    }
}
