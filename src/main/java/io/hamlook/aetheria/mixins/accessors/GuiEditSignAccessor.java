package io.hamlook.aetheria.mixins.accessors;

import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC < 1.16
@Mixin(GuiEditSign.class)
public interface GuiEditSignAccessor {
    @Accessor("tileSign")
    TileEntitySign ATHR$getTileSign();
}
//#endif
