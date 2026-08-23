package io.hamlook.aetheria.features.events;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.profile.viewer.SkinManager;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * Renders whichever single {@link EventToast} is currently active (see
 * {@link EventNotifierTracker#activeToast()}) — only one is ever on screen at a time, queued
 * ones play sequentially. Each event type gets its own background/border color pair
 * ({@link EventTheme}), the toast's first icon is drawn oversized, overlapping the top-left
 * corner/border like a badge, and a hand-placed set of {@link EventDecor} decorations render
 * around it — some in front (clipping the border like the badge), some behind the box background
 * (mostly covered, only peeking out past the edges). This layout can't be expressed by
 * {@link Overlay}'s generic single-icon-per-line rendering, so {@link #render(boolean)} is fully
 * overridden; position, scale and corner radius still come from the base class like every other
 * overlay.
 */
@RegisterEvents
public class EventNotifierOverlay extends Overlay {

    @Getter
    private static EventNotifierOverlay instance;

    private static final int ICON_SIZE = 14;
    private static final int ICON_GAP = 2;
    /** The lead icon (event-type icon) renders larger than inline icons... */
    private static final int BADGE_SIZE = 20;
    /** ...and pokes this many pixels past the box's top-left corner/border. */
    private static final int BADGE_OVERHANG = 5;
    private static final int BADGE_GAP = 4;
    private static final int BORDER_THICKNESS = 2;
    private static final int CORNER_RADIUS = 4;

    public EventNotifierOverlay() {
        super(160, 20);
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.eventNotification.overlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.eventNotification.overlayScale;
    }

    @Override
    public int getBgColor() {
        // Only exists to satisfy Overlay's abstract contract — render(boolean) below never calls
        // this, it looks up the per-event color via EventTheme.forType(toast.eventType) instead.
        return EventTheme.forType(null).bgColor;
    }

    @Override
    public int getCornerRadius() {
        return CORNER_RADIUS;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null
                && ATHRConfig.feature.eventNotification.masterEnabled
                && EventNotifierTracker.activeToast() != null;
    }

    @Override
    protected boolean hideOnChat() {
        return false;
    }

    @Override
    protected boolean hideOnTab() {
        return false;
    }

    @Override
    protected boolean hideOnDebug() {
        return false;
    }

    @Override
    public List<String> getLines(boolean preview) {
        if (preview) return Collections.singletonList("Farming Contest starts in 1 Minute");
        EventToast toast = EventNotifierTracker.activeToast();
        return toast != null ? Collections.singletonList(toast.text()) : Collections.emptyList();
    }

    @Override
    public void render(boolean preview) {
        if (preview && isLiveActive()) return;
        if (!preview && !extraGuard()) return;

        EventToast toast;
        float alpha;
        if (preview) {
            toast = EventToast.staticText(Collections.singletonList(new ItemStack(Items.wooden_hoe)), "Farming Contest", "Farming Contest starts in 1 Minute");
            alpha = 1f;
        } else {
            toast = EventNotifierTracker.activeToast();
            if (toast == null) return;
            alpha = EventNotifierTracker.activeAlpha();
        }

        EventTheme theme = EventTheme.forType(toast.eventType);
        String text = toast.text();

        List<ItemStack> icons = toast.icons;
        ItemStack badgeIcon = icons.isEmpty() ? null : icons.get(0);
        List<ItemStack> inlineIcons = icons.size() > 1 ? icons.subList(1, icons.size()) : Collections.emptyList();
        int badgeSlot = badgeIcon != null ? BADGE_SIZE + BADGE_GAP : 0;

        List<EventDecor> decorations = preview ? Collections.emptyList() : EventDecorations.decorationsFor(toast.eventType);
        int rightClearance = preview ? 0 : EventDecorations.rightClearanceFor(toast.eventType);

        float scale = getScale();
        int w = Math.max(getBaseWidth(), PADDING * 2 + badgeSlot + iconsWidth(inlineIcons) + mc.fontRendererObj.getStringWidth(text) + rightClearance);
        int h = LINE_HEIGHT + PADDING * 2;
        lastW = w;
        lastH = h;

        Position pos = getPosition();
        int x = pos.getAbsX(sr, (int) (w * scale));
        int y = pos.getAbsY(sr, (int) (h * scale));
        if (pos.isCenterX()) x -= (int) (w * scale / 2);
        if (pos.isCenterY()) y -= (int) (h * scale / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);

        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);

        for (EventDecor d : decorations) {
            if (d.layer == EventDecor.Layer.BEHIND) drawDecor(d, w, h, alpha);
        }

        int bgColor = withAlpha(theme.bgColor, alpha);
        if ((bgColor >>> 24) != 0) drawRoundedRect(-PADDING, -PADDING, w, h - PADDING, getCornerRadius(), bgColor);
        drawRoundedRectBorder(-PADDING, -PADDING, w, h - PADDING, getCornerRadius(), BORDER_THICKNESS, withAlpha(theme.borderColor, alpha));

        // Front decorations sit on the box but strictly below the readable layer (icons/text/
        // badge) drawn next — they can never cover them, only the plain background.
        for (EventDecor d : decorations) {
            if (d.layer == EventDecor.Layer.FRONT) drawDecor(d, w, h, alpha);
        }

        GlStateManager.color(1f, 1f, 1f, alpha);

        int tx = badgeSlot;
        mc.fontRendererObj.drawStringWithShadow(text, tx, 0, withAlpha(0xFFFFFF, alpha));
        tx += mc.fontRendererObj.getStringWidth(text) + ICON_GAP;
        // Farming Contest's actual crop icons (dynamic per contest) — inline after the text,
        // vertically centered on the box's true content area (h/2 - PADDING, not the box's raw
        // half-height) so they sit centered inside the box instead of hanging off the bottom
        // border regardless of each crop's own sprite size.
        int iconY = (h / 2 - PADDING) - ICON_SIZE / 2;
        for (ItemStack icon : inlineIcons) {
            renderEventIcon(icon, tx, iconY, ICON_SIZE, alpha);
            tx += ICON_SIZE + ICON_GAP;
        }

        // Badge drawn last of all, oversized, overlapping the box's top-left corner/border.
        if (badgeIcon != null) {
            ResourceLocation badgeTex = EventBadgeTextures.forType(toast.eventType);
            if (badgeTex != null) {
                mc.getTextureManager().bindTexture(badgeTex);
                GlStateManager.color(1f, 1f, 1f, alpha);
                Utils.drawTexturedRect(-PADDING - BADGE_OVERHANG, -PADDING - BADGE_OVERHANG, BADGE_SIZE, BADGE_SIZE);
            } else {
                renderEventIcon(badgeIcon, -PADDING - BADGE_OVERHANG, -PADDING - BADGE_OVERHANG, BADGE_SIZE, alpha);
            }
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();

        GL11.glPopMatrix();
    }

    private void drawDecor(EventDecor d, int boxW, int boxH, float alpha) {
        int x = anchorX(d.anchor, boxW, d.width) + d.dx;
        int y = anchorY(d.anchor, boxH, d.height) + d.dy;
        GlStateManager.color(1f, 1f, 1f, alpha);
        if (d.texture != null) {
            mc.getTextureManager().bindTexture(d.texture);
            Utils.drawTexturedRect(x, y, d.width, d.height);
        } else if (d.item != null) {
            renderEventIcon(d.item, x, y, d.width, alpha);
        }
    }

    /**
     * Single dispatch point for every event badge/inline-icon/item-decoration: picks whichever
     * render path can actually fade this specific stack for real, only falling back to the shrink
     * workaround where nothing else is possible.
     * <ul>
     *     <li>A hand-drawn replacement registered in {@link EventItemTextures} (badges, crops,
     *     Spooky's dead bush) draws that bundled PNG directly — always preferred when present.</li>
     *     <li>Otherwise, a textured player head (an ItemRegistry-sourced skull with no custom
     *     texture registered) head-crops via {@link SkinManager}, same real-fade technique as
     *     {@code RenderUtils.renderPlayerHead}. Sirius/Oringo's badges currently never reach this
     *     branch — {@link EventBadgeTextures} intercepts both before the badge gets here.</li>
     *     <li>A plain flat-icon item (tools, food, fireworks, the couple of crops without a
     *     custom texture) draws its atlas sprite directly via
     *     {@link ItemRenderUtils#renderFlatItemIcon}.</li>
     *     <li>Anything else — full 3D block-form items with no single flat face to pull and no
     *     custom texture (Brown Mushroom) — shrinks instead, since real alpha isn't available for
     *     those at all.</li>
     * </ul>
     */
    private void renderEventIcon(ItemStack stack, int x, int y, int size, float alpha) {
        if (stack == null) return;
        ResourceLocation customTex = EventItemTextures.forStack(stack);
        if (customTex != null) {
            mc.getTextureManager().bindTexture(customTex);
            GlStateManager.color(1f, 1f, 1f, alpha);
            int drawY = EventItemTextures.nudgesUpQuarter(stack) ? y - size / 4 : y;
            Utils.drawTexturedRect(x, drawY, size, size);
            return;
        }
        ResourceLocation skin = SkinManager.getSkinFromStack(stack);
        if (skin != null) {
            drawHeadCrop(skin, x, y, size, alpha);
        } else if (stack.getItem() instanceof ItemBlock) {
            renderShrinkingItem(stack, x, y, size, alpha);
        } else {
            ItemRenderUtils.renderFlatItemIcon(stack, x, y, size, alpha);
        }
    }

    private void drawHeadCrop(ResourceLocation skin, int x, int y, int size, float alpha) {
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.color(1f, 1f, 1f, alpha);
        // Same head + hat-overlay crop as RenderUtils.renderPlayerHead, just via a plain textured
        // quad so ambient alpha actually applies.
        Utils.drawTexturedRect(x, y, size, size, 8f / 64f, 16f / 64f, 8f / 64f, 16f / 64f);
        Utils.drawTexturedRect(x, y, size, size, 40f / 64f, 48f / 64f, 8f / 64f, 16f / 64f);
    }

    /**
     * Vanilla's item-icon renderer ({@code RenderItem.renderItemIntoGUI}) unconditionally resets
     * GL color to fully opaque white before drawing, so a plain alpha fade has no effect here at
     * all. Used only for full 3D block-form items now (see {@link #renderEventIcon}), which shrink
     * toward their own center in sync with the fade instead, so everything still finishes
     * disappearing together.
     */
    private void renderShrinkingItem(ItemStack item, int x, int y, int size, float alpha) {
        int shrunk = Math.max(1, Math.round(size * alpha));
        int offset = (size - shrunk) / 2;
        ItemRenderUtils.renderItemIcon(mc, item, x + offset, y + offset, shrunk);
    }

    private int anchorX(EventDecor.Anchor anchor, int boxW, int decorW) {
        switch (anchor) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
            case RIGHT_MID:
                return boxW - PADDING - decorW;
            case CENTER:
                return boxW / 2 - decorW / 2;
            default:
                return -PADDING;
        }
    }

    private int anchorY(EventDecor.Anchor anchor, int boxH, int decorH) {
        switch (anchor) {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return boxH - PADDING - decorH;
            case CENTER:
            case RIGHT_MID:
            case LEFT_MID:
                return boxH / 2 - decorH / 2;
            default:
                return -PADDING;
        }
    }

    private int iconsWidth(List<ItemStack> icons) {
        return icons.isEmpty() ? 0 : icons.size() * (ICON_SIZE + ICON_GAP);
    }

    private int withAlpha(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    protected int getBaseWidth() {
        return 160;
    }
}
