package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.features.qol.TooltipHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GuiUtils.class, remap = false)
public class MixinTooltipScale {

    @Inject(method = "drawHoveringText", at = @At("HEAD"))
    private static void athr$tooltipHead(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font, CallbackInfo ci) {
        if (textLines.isEmpty()) return;

        int width = 0;
        for (String line : textLines) width = Math.max(width, font.getStringWidth(line));

        int x = mouseX + 12;
        if (x + width + 4 > screenWidth) x = mouseX - 16 - width;

        int y = mouseY - 12;
        int height = 8 + (textLines.size() > 1 ? (textLines.size() - 1) * 10 : 0);
        if (y + height + 6 > screenHeight) y = screenHeight - height - 6;

        TooltipHandler.onTooltipDrawn(x, y, height, width);
        TooltipHandler.tick();
        TooltipHandler.pushTransform();
    }

    @Inject(method = "drawHoveringText", at = @At("RETURN"))
    private static void athr$tooltipTail(CallbackInfo ci) {
        TooltipHandler.popTransform();
    }
}
