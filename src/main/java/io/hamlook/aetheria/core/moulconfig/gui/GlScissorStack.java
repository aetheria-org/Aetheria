// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.LinkedList;

public class GlScissorStack {

    private static class Bounds {
        int left, top, right, bottom;

        Bounds(int left, int top, int right, int bottom) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }

        Bounds createSubBound(int left, int top, int right, int bottom) {
            left   = Math.max(left,   this.left);
            top    = Math.max(top,    this.top);
            right  = Math.min(right,  this.right);
            bottom = Math.min(bottom, this.bottom);
            if (top > bottom) top = bottom;
            if (left > right) left = right;
            return new Bounds(left, top, right, bottom);
        }

        void set(ScaledResolution sr) {
            int height = MinecraftCompat.getMinecraft().displayHeight;
            int scale  = sr.getScaleFactor();
            GL11.glScissor(left * scale, height - bottom * scale, (right - left) * scale, (bottom - top) * scale);
        }
    }

    private static final LinkedList<Bounds> stack = new LinkedList<>();

    public static void push(int left, int top, int right, int bottom, ScaledResolution sr) {
        if (right < left) { int t = right; right = left; left = t; }
        if (bottom < top) { int t = bottom; bottom = top; top = t; }
        stack.push(stack.isEmpty() ? new Bounds(left, top, right, bottom)
                                   : stack.peek().createSubBound(left, top, right, bottom));
        stack.peek().set(sr);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    public static void pop(ScaledResolution sr) {
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        else stack.peek().set(sr);
    }

    public static void clear() {
        stack.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}