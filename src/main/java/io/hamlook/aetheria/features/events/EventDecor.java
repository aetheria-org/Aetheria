package io.hamlook.aetheria.features.events;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * One themed decoration drawn around/behind/in front of the notification box. Position is
 * expressed as a corner/center {@link Anchor} plus a small pixel nudge, not an absolute
 * coordinate, so it stays sensible regardless of how wide the box ends up (box width varies with
 * the toast's text). Either {@link #item} or {@link #texture} is set, never both.
 */
public class EventDecor {

    public enum Layer {
        /** Drawn before the box background — mostly covered by it, only peeking out at the edges. */
        BEHIND,
        /** Drawn after everything else — sits on top, clipping the border like a badge. */
        FRONT
    }

    /** RIGHT_MID/LEFT_MID are right/left-aligned but vertically centered on the box, so a
     *  decoration taller than the box clips its top and bottom equally. */
    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, RIGHT_MID, LEFT_MID }

    public final Layer layer;
    public final ItemStack item;
    public final ResourceLocation texture;
    public final Anchor anchor;
    public final int dx, dy;
    public final int width, height;

    private EventDecor(Layer layer, ItemStack item, ResourceLocation texture, Anchor anchor, int dx, int dy, int width, int height) {
        this.layer = layer;
        this.item = item;
        this.texture = texture;
        this.anchor = anchor;
        this.dx = dx;
        this.dy = dy;
        this.width = width;
        this.height = height;
    }

    public static EventDecor item(Layer layer, ItemStack item, Anchor anchor, int dx, int dy, int size) {
        return new EventDecor(layer, item, null, anchor, dx, dy, size, size);
    }

    public static EventDecor texture(Layer layer, ResourceLocation texture, Anchor anchor, int dx, int dy, int width, int height) {
        return new EventDecor(layer, null, texture, anchor, dx, dy, width, height);
    }
}
