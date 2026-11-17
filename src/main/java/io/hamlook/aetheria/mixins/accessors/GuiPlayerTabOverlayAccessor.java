package io.hamlook.aetheria.mixins.accessors;

import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiPlayerTabOverlay.class)
public interface GuiPlayerTabOverlayAccessor {
    @Accessor("footer")
    IChatComponent ATHR$getFooter();
}
