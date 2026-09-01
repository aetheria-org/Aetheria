package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.AetheriaEvent;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ASMTooltipEvent extends AetheriaEvent {

    public final ItemStack itemStack;
    public final List<String> toolTip;

    public ASMTooltipEvent(ItemStack itemStack, List<String> toolTip) {
        this.itemStack = itemStack;
        this.toolTip = toolTip;
    }
}
