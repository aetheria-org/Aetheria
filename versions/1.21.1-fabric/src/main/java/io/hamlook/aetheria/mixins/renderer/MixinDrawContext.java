package io.hamlook.aetheria.mixins.renderer;

import io.hamlook.aetheria.events.RenderItemOverlayEvent;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.hooks.FontRendererEmojiHook;
import io.hamlook.aetheria.utils.hooks.GuiScreenHook;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DrawContext.class)
public class MixinDrawContext {

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void athr$onItemOverlay(TextRenderer fr, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (stack == null) return;
        DrawContextUtils.setContext((DrawContext) (Object) this);
        try {
            new RenderItemOverlayEvent((DrawContext) (Object) this, stack, x, y).post();
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$itemTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (GuiScreenHook.shouldCancelStorageTooltip()) {
            ci.cancel();
            return;
        }
        DrawContextUtils.setContext((DrawContext) (Object) this);
        try {
            if (GuiScreenHook.visitorTooltip(stack, x, y)) {
                ci.cancel();
            }
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At("HEAD"), cancellable = true)
    private void athr$clearTooltipState(TextRenderer textRenderer, java.util.List<net.minecraft.text.Text> text,
                                        java.util.Optional<net.minecraft.item.tooltip.TooltipData> data,
                                        int x, int y, CallbackInfo ci) {
        if (GuiScreenHook.shouldCancelStorageTooltip()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I",
            at = @At("HEAD"), cancellable = true)
    private void athr$drawEmojiShadow(TextRenderer textRenderer, String text, int x, int y, int color,
                                       CallbackInfoReturnable<Integer> cir) {
        DrawContextUtils.setContext((DrawContext) (Object) this);
        try {
            int endX = FontRendererEmojiHook.processEmojisModern(textRenderer, text, x, y, color, true);
            if (endX != -1) cir.setReturnValue(endX);
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), cancellable = true)
    private void athr$drawEmojiPlain(TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow,
                                      CallbackInfoReturnable<Integer> cir) {
        DrawContextUtils.setContext((DrawContext) (Object) this);
        try {
            int endX = FontRendererEmojiHook.processEmojisModern(textRenderer, text, x, y, color, shadow);
            if (endX != -1) cir.setReturnValue(endX);
        } finally {
            DrawContextUtils.clearContext();
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I",
            at = @At("HEAD"), cancellable = true)
    private void athr$drawEmojiOrdered(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow,
                                       CallbackInfoReturnable<Integer> cir) {
        DrawContextUtils.setContext((DrawContext) (Object) this);
        try {
            int endX = FontRendererEmojiHook.processEmojisOrdered(textRenderer, text, x, y, color, shadow);
            if (endX != -1) cir.setReturnValue(endX);
        } finally {
            DrawContextUtils.clearContext();
        }
    }
}
