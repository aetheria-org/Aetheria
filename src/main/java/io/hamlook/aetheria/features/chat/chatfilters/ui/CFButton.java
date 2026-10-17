package io.hamlook.aetheria.features.chat.chatfilters.ui;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
//#if MC < 1.21
//#else
//$$ import net.minecraft.client.font.TextRenderer;
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.text.OrderedText;
//#endif

public class CFButton extends GuiButton {

    public float defaultR, defaultG, defaultB;
    public boolean toggled;
    public boolean isToggleButton;
    //#if MC < 1.21
    //#else
    //$$ private String athrLastLabel;
    //$$ private OrderedText athrOrdered;
    //#endif

    public CFButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, float r, float g, float b) {
        //#if MC < 1.21
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        //#else
        //$$ super(x, y, widthIn, heightIn, TextCompat.fromFormatted(buttonText), btn -> {}, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        //#endif
        this.defaultR = r;
        this.defaultG = g;
        this.defaultB = b;
        this.isToggleButton = false;
    }

    public CFButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, boolean isToggleButton, boolean defaultState) {
        //#if MC < 1.21
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        //#else
        //$$ super(x, y, widthIn, heightIn, TextCompat.fromFormatted(buttonText), btn -> {}, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        //#endif
        this.isToggleButton = isToggleButton;
        this.toggled = defaultState;
    }

    public CFButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, float r, float g, float b, Runnable action) {
        //#if MC < 1.21
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        //#else
        //$$ super(x, y, widthIn, heightIn, TextCompat.fromFormatted(buttonText), btn -> action.run(), ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        //#endif
        this.defaultR = r;
        this.defaultG = g;
        this.defaultB = b;
        this.isToggleButton = false;
    }

    public CFButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, boolean isToggleButton, boolean defaultState, Runnable action) {
        //#if MC < 1.21
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        //#else
        //$$ super(x, y, widthIn, heightIn, TextCompat.fromFormatted(buttonText), btn -> action.run(), ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        //#endif
        this.isToggleButton = isToggleButton;
        this.toggled = defaultState;
    }

    public static CFButton create(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, float r, float g, float b, Runnable action) {
        return new CFButton(buttonId, x, y, widthIn, heightIn, buttonText, r, g, b, action);
    }

    public static CFButton create(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, boolean isToggleButton, boolean defaultState, Runnable action) {
        return new CFButton(buttonId, x, y, widthIn, heightIn, buttonText, isToggleButton, defaultState, action);
    }

    private float[] resolveColor() {
        float r, g, b;
        if (isToggleButton) {
            if (toggled) {
                r = 0.2f;
                g = 0.8f;
            } else {
                r = 0.5f;
                g = 0.2f;
            }
            b = 0.2f;
        } else {
            r = defaultR;
            g = defaultG;
            b = defaultB;
        }
        //#if MC < 1.21
        if (!this.enabled) {
        //#else
        //$$ if (!this.active) {
        //#endif
            r *= 0.5f;
            g *= 0.5f;
            b *= 0.5f;
        //#if MC < 1.21
        } else if (this.hovered) {
        //#else
        //$$ } else if (this.isHovered()) {
        //#endif
            r = Math.min(1f, r * 1.3f);
            g = Math.min(1f, g * 1.3f);
            b = Math.min(1f, b * 1.3f);
        }
        return new float[]{r, g, b};
    }

    //#if MC < 1.21
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            FontRenderer fontrenderer = MinecraftCompat.getFontRenderer();
            this.hovered = mouseX >= GuiScreenUtils.getX(this) && mouseY >= GuiScreenUtils.getY(this) && mouseX < GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this) && mouseY < GuiScreenUtils.getY(this) + GuiScreenUtils.getH(this);

            float[] rgb = resolveColor();
            float r = rgb[0], g = rgb[1], b = rgb[2];

            if (isToggleButton && toggled) {
                this.displayString = "§a" + this.displayString;
            }

            GlStateManagerCompat.enableBlend();
            GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManagerCompat.color(r, g, b, 1.0F);
            NineSliceUtils.draw(Resources.storageBackground(1), xPosition, yPosition, width, height, 6, 18);
            GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);

            this.mouseDragged(mc, mouseX, mouseY);

            float scale = ResolutionUtils.getXStatic(1);
            float centerX = GuiScreenUtils.getX(this) + (GuiScreenUtils.getW(this) / 2.0f);
            float centerY = GuiScreenUtils.getY(this) + (GuiScreenUtils.getH(this) / 2.0f);

            if (!this.enabled) {
                GlStateManagerCompat.color(0.6f, 0.6f, 0.6f, 1f);
            }
            TextRenderUtils.drawCenteredStringScaleAware(this.displayString, centerX, centerY, scale * 1.2f, false);
            GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
    //#else
    //$$ @Override
    //$$ protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    //$$     if (!this.visible) return;
    //$$     float[] rgb = resolveColor();
    //$$     int x = GuiScreenUtils.getX(this);
    //$$     int y = GuiScreenUtils.getY(this);
    //$$     int w = GuiScreenUtils.getW(this);
    //$$     int h = GuiScreenUtils.getH(this);
    //$$     GlStateManagerCompat.color(rgb[0], rgb[1], rgb[2], 1.0f);
    //$$     String label = getMessage().getString();
    //$$     if (isToggleButton) {
    //$$         String base = ColorUtils.stripColor(label);
    //$$         label = toggled ? "§a" + base : base;
    //$$     }
    //$$     OrderedText ordered = athrOrdered;
    //$$     if (ordered == null || !label.equals(athrLastLabel)) {
    //$$         setMessage(TextCompat.fromFormatted(label));
    //$$         ordered = getMessage().asOrderedText();
    //$$         athrLastLabel = label;
    //$$         athrOrdered = ordered;
    //$$     }
    //$$     int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
    //$$     DrawContextUtils.setContext(context);
    //$$     try {
    //$$         NineSliceUtils.draw(Resources.storageBackground(1), x, y, w, h, 6, 18);
    //$$         GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
    //$$         DrawContextUtils.drawCenteredTextScaled(MinecraftCompat.getFontRenderer(), ordered,
    //$$                 x + w / 2f, y + h / 2f, ResolutionUtils.getXStatic(1) * 1.2f, color);
    //$$     } finally {
    //$$         DrawContextUtils.clearContext();
    //$$     }
    //$$ }
    //#endif
}
