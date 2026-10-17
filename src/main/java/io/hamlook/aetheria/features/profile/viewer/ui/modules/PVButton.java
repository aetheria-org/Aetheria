package io.hamlook.aetheria.features.profile.viewer.ui.modules;

import io.hamlook.aetheria.features.profile.viewer.ui.ProfileViewerGUI;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
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

public class PVButton extends GuiButton {

    //#if MC < 1.21
    //#else
    //$$ private String athrLastLabel;
    //$$ private OrderedText athrOrdered;
    //#endif
    //#if MC < 1.21
    //#else
    //$$ public String displayString;
    //#endif

    public PVButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        //#if MC < 1.21
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        //#else
        //$$ super(x, y, widthIn, heightIn, TextCompat.fromFormatted(buttonText), btn -> {}, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        //$$ this.displayString = buttonText;
        //#endif
    }

    public PVButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, Runnable action) {
        //#if MC < 1.21
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        //#else
        //$$ super(x, y, widthIn, heightIn, TextCompat.fromFormatted(buttonText), btn -> action.run(), ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        //$$ this.displayString = buttonText;
        //#endif
    }

    public static PVButton create(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, Runnable action) {
        return new PVButton(buttonId, x, y, widthIn, heightIn, buttonText, action);
    }

    public void athr$drawManual(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        //#if MC < 1.21
        drawButton(mc, mouseX, mouseY);
        //#else
        //$$ renderWidget(DrawContextUtils.getDrawContext(), mouseX, mouseY, partialTicks);
        //#endif
    }

    //#if MC < 1.21
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            FontRenderer fontrenderer = MinecraftCompat.getFontRenderer();
            GlStateManagerCompat.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= GuiScreenUtils.getX(this) && mouseY >= GuiScreenUtils.getY(this) && mouseX < GuiScreenUtils.getX(this) + GuiScreenUtils.getW(this) && mouseY < GuiScreenUtils.getY(this) + GuiScreenUtils.getH(this);
            GlStateManagerCompat.enableBlend();
            GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManagerCompat.blendFunc(770, 771);
            NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, xPosition, yPosition, width, height, 6, 18);
            this.mouseDragged(mc, mouseX, mouseY);
            float centerX = GuiScreenUtils.getX(this) + (GuiScreenUtils.getW(this) / 2.0f);
            float centerY = GuiScreenUtils.getY(this) + (GuiScreenUtils.getH(this) / 2.0f);

            TextRenderUtils.drawCenteredStringScaleAware(this.displayString, centerX, centerY, (ProfileViewerGUI.uiScale * 2f), false);
        }
    }
    //#else
    //$$ @Override
    //$$ protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    //$$     if (!this.visible) return;
    //$$     int x = GuiScreenUtils.getX(this);
    //$$     int y = GuiScreenUtils.getY(this);
    //$$     int w = GuiScreenUtils.getW(this);
    //$$     int h = GuiScreenUtils.getH(this);
    //$$     GlStateManagerCompat.color(1.0f, 1.0f, 1.0f, 1.0f);
    //$$     String label = this.displayString;
    //$$     OrderedText ordered = athrOrdered;
    //$$     if (ordered == null || !label.equals(athrLastLabel)) {
    //$$         setMessage(TextCompat.fromFormatted(label));
    //$$         ordered = getMessage().asOrderedText();
    //$$         athrLastLabel = label;
    //$$         athrOrdered = ordered;
    //$$     }
    //$$     DrawContextUtils.setContext(context);
    //$$     try {
    //$$         NineSliceUtils.draw(ProfileViewerGUI.CONTAINER_BG, x, y, w, h, 6, 18);
    //$$         DrawContextUtils.drawCenteredTextScaled(MinecraftCompat.getFontRenderer(), ordered,
    //$$                 x + w / 2f, y + h / 2f, ProfileViewerGUI.uiScale * 2f, 0xFFFFFFFF);
    //$$     } finally {
    //$$         DrawContextUtils.clearContext();
    //$$     }
    //$$ }
    //#endif
}
