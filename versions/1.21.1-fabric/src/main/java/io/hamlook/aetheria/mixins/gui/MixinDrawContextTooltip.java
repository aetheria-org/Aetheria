package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.features.qol.TooltipHandler;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public class MixinDrawContextTooltip {

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At("HEAD"))
    private void athr$tooltipHead(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
        if (components.isEmpty()) return;

        DrawContextUtils.setContext((DrawContext) (Object) this);

        int width = 0;
        int height = 0;
        for (TooltipComponent component : components) {
            width = Math.max(width, component.getWidth(textRenderer));
            height += component.getHeight();
        }

        Vector2ic pos = positioner.getPosition(GuiScreenUtils.getScaledWindowWidth(), GuiScreenUtils.getScaledWindowHeight(), x, y, width, height);

        TooltipHandler.onTooltipDrawn(pos.x(), pos.y(), height, width);
        TooltipHandler.tick();
        TooltipHandler.pushTransform();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At("RETURN"))
    private void athr$tooltipTail(CallbackInfo ci) {
        try {
            TooltipHandler.popTransform();
        } finally {
            DrawContextUtils.clearContext();
        }
    }
}
