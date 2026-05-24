package io.hamlook.aetheria.features.chat;

import net.minecraft.client.gui.ChatLine;

/**
 * Interface injected onto {@code net.minecraft.client.gui.GuiNewChat} by
 * {@link io.hamlook.aetheria.mixins.chat.MixinGuiNewChat}.
 *
 * Exposes the currently-hovered ChatLine so the copy mixin in
 * {@link io.hamlook.aetheria.mixins.chat.MixinGuiChat} can retrieve it without
 * directly accessing {@code GuiNewChat}'s private fields.
 */
public interface GuiNewChatHook {

    /**
     * Returns the {@link ChatLine} currently under the mouse cursor, or
     * {@code null} if the mouse is not hovering over any visible chat line.
     *
     * @param rawMouseX raw (pre-scaled) mouse X from {@code Mouse.getX()}
     * @param rawMouseY raw (pre-scaled) mouse Y from {@code displayHeight - Mouse.getY() - 1}
     */
    ChatLine chatutils$getHoveredChatLine(int rawMouseX, int rawMouseY);

    /**
     * Returns the hovered line that was computed at the start of the current
     * {@code drawChat} call, or {@code null} outside of that frame.
     */
    ChatLine chatutils$getCurrentHoveredLine();
}
