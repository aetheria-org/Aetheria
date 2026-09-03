// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class GuiElementColour extends GuiElement {

    // ResourceLocations centralised in Resources
    private static final ResourceLocation colour_selector_dot           = Resources.colour_selector_dot;
    private static final ResourceLocation colour_selector_bar           = Resources.colour_selector_bar;
    private static final ResourceLocation colour_selector_bar_alpha     = Resources.colour_selector_bar_alpha;
    private static final ResourceLocation colour_selector_chroma        = Resources.colour_selector_chroma;
    private static final ResourceLocation colourPickerLocation          = Resources.colourPickerLocation;
    private static final ResourceLocation colourPickerBarValueLocation   = Resources.colourPickerBarValueLocation;
    private static final ResourceLocation colourPickerBarOpacityLocation = Resources.colourPickerBarOpacityLocation;

    private final GuiElementTextField hexField = new GuiElementTextField("",
            GuiElementTextField.SCALE_TEXT | GuiElementTextField.FORCE_CAPS | GuiElementTextField.NO_SPACE);

    private final int x, y;
    private int xSize = 119;
    private final int ySize = 89;

    private float wheelAngle = 0, wheelRadius = 0;
    private int clickedComponent = -1;

    private final Consumer<String> colourChangedCallback;
    private final Runnable closeCallback;
    private String colour;

    private final boolean opacitySlider, valueSlider;

    public GuiElementColour(int x, int y, String initialColour,
                            Consumer<String> colourChangedCallback, Runnable closeCallback) {
        this(x, y, initialColour, colourChangedCallback, closeCallback, true, true);
    }

    public GuiElementColour(int x, int y, String initialColour,
                            Consumer<String> colourChangedCallback, Runnable closeCallback,
                            boolean opacitySlider, boolean valueSlider) {
        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        this.y = Math.max(10, Math.min(sr.getScaledHeight() - ySize - 10, y));
        this.x = Math.max(10, Math.min(sr.getScaledWidth() - xSize - 10, x));
        this.colour = initialColour;
        this.colourChangedCallback = colourChangedCallback;
        this.closeCallback = closeCallback;
        this.opacitySlider = opacitySlider;
        this.valueSlider = valueSlider;

        int col = ChromaColour.specialToSimpleRGB(initialColour);
        Color c = new Color(col);
        updateAngleAndRadius(Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null));

        if (!valueSlider)   xSize -= 15;
        if (!opacitySlider) xSize -= 15;
    }

    public void updateAngleAndRadius(float[] hsv) {
        this.wheelRadius = hsv[1];
        this.wheelAngle  = hsv[0] * 360;
    }

    @Override
    public void render() {
        RenderUtils.drawFloatingRectDark(x, y, xSize, ySize);

        int currentColour = ChromaColour.specialToSimpleRGB(colour);
        Color c = new Color(currentColour, true);
        float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

        // Colour wheel texture
        BufferedImage bi = new BufferedImage(288, 288, BufferedImage.TYPE_INT_ARGB);
        float borderRadius = KeyboardCompat.isKeyDown(Keyboard.KEY_N) ? 0 : 0.05f;
        for (int px = -16; px < 272; px++) {
            for (int py = -16; py < 272; py++) {
                float radius = (float) Math.sqrt(((px - 128) * (px - 128) + (py - 128) * (py - 128)) / 16384f);
                float angle  = (float) Math.toDegrees(Math.atan((128 - px) / (py - 128 + 1E-5)) + Math.PI / 2);
                if (py < 128) angle += 180;
                if (radius <= 1) {
                    bi.setRGB(px + 16, py + 16, Color.getHSBColor(angle / 360f, (float) Math.pow(radius, 1.5f), hsv[2]).getRGB());
                } else if (radius <= 1 + borderRadius) {
                    float inv = Math.abs(radius - 1 - borderRadius / 2) / borderRadius * 2;
                    if (radius > 1 + borderRadius / 2) {
                        bi.setRGB(px + 16, py + 16, (int)((1 - inv) * 255) << 24);
                    } else {
                        Color col = Color.getHSBColor(angle / 360f, 1, hsv[2]);
                        bi.setRGB(px + 16, py + 16, 0xff000000 | (int)(col.getRed()*inv)<<16 | (int)(col.getGreen()*inv)<<8 | (int)(col.getBlue()*inv));
                    }
                }
            }
        }

        // Value bar
        BufferedImage biValue = new BufferedImage(10, 64, BufferedImage.TYPE_INT_ARGB);
        for (int px = 0; px < 10; px++) for (int py = 0; py < 64; py++) {
            if ((px == 0 || px == 9) && (py == 0 || py == 63)) continue;
            biValue.setRGB(px, py, Color.getHSBColor(wheelAngle / 360, wheelRadius, (64 - py) / 64f).getRGB());
        }

        // Opacity bar
        BufferedImage biOpacity = new BufferedImage(10, 64, BufferedImage.TYPE_INT_ARGB);
        for (int px = 0; px < 10; px++) for (int py = 0; py < 64; py++) {
            if ((px == 0 || px == 9) && (py == 0 || py == 63)) continue;
            biOpacity.setRGB(px, py, (currentColour & 0x00FFFFFF) | (Math.min(255, (64 - py) * 4) << 24));
        }

        float selRadius = (float) Math.pow(wheelRadius, 1 / 1.5f) * 32;
        int selX = (int) (Math.cos(Math.toRadians(wheelAngle)) * selRadius);
        int selY = (int) (Math.sin(Math.toRadians(wheelAngle)) * selRadius);

        int valueOffset = 0, opacityOffset = 0;

        if (valueSlider) {
            valueOffset = 15;
            MinecraftCompat.getMinecraft().getTextureManager().loadTexture(colourPickerBarValueLocation, new DynamicTexture(biValue));
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colourPickerBarValueLocation);
            GlStateManagerCompat.color(1, 1, 1, 1);
            RenderUtils.drawTexturedRect(x + 5 + 64 + 5, y + 5, 10, 64, GL11.GL_NEAREST);
        }

        if (opacitySlider) {
            opacityOffset = 15;
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colour_selector_bar_alpha);
            GlStateManagerCompat.color(1, 1, 1, 1);
            RenderUtils.drawTexturedRect(x + 5 + 64 + 5 + valueOffset, y + 5, 10, 64, GL11.GL_NEAREST);
            MinecraftCompat.getMinecraft().getTextureManager().loadTexture(colourPickerBarOpacityLocation, new DynamicTexture(biOpacity));
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colourPickerBarOpacityLocation);
            GlStateManagerCompat.color(1, 1, 1, 1);
            RenderUtils.drawTexturedRect(x + 5 + 64 + 5 + valueOffset, y + 5, 10, 64, GL11.GL_NEAREST);
        }

        int chromaSpeed = ChromaColour.getSpeed(colour);
        int chromaRGB = ChromaColour.specialToChromaRGB(colour);
        Color cChroma = new Color(chromaRGB, true);
        float[] hsvChroma = Color.RGBtoHSB(cChroma.getRed(), cChroma.getGreen(), cChroma.getBlue(), null);

        if (chromaSpeed > 0) {
            drawRect(x+5+64+valueOffset+opacityOffset+5+1, y+5+1, x+5+64+valueOffset+opacityOffset+5+9, y+5+63, Color.HSBtoRGB(hsvChroma[0], 0.8f, 0.8f));
        } else {
            drawRect(x+5+64+valueOffset+opacityOffset+5+1, y+5+27+1, x+5+64+valueOffset+opacityOffset+5+9, y+5+36, Color.HSBtoRGB((hsvChroma[0] + (System.currentTimeMillis() - ChromaColour.startTime) / 1000f) % 1, 0.8f, 0.8f));
        }

        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colour_selector_bar);
        GlStateManagerCompat.color(1, 1, 1, 1);
        if (valueSlider)   RenderUtils.drawTexturedRect(x+5+64+5, y+5, 10, 64, GL11.GL_NEAREST);
        if (opacitySlider) RenderUtils.drawTexturedRect(x+5+64+5+valueOffset, y+5, 10, 64, GL11.GL_NEAREST);

        if (chromaSpeed > 0) {
            RenderUtils.drawTexturedRect(x+5+64+valueOffset+opacityOffset+5, y+5, 10, 64, GL11.GL_NEAREST);
        } else {
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colour_selector_chroma);
            RenderUtils.drawTexturedRect(x+5+64+valueOffset+opacityOffset+5, y+5+27, 10, 10, GL11.GL_NEAREST);
        }

        if (valueSlider)   drawRect(x+5+64+5, y+5+64-(int)(64*hsv[2]), x+5+64+valueOffset, y+5+64-(int)(64*hsv[2])+1, 0xFF000000);
        if (opacitySlider) drawRect(x+5+64+5+valueOffset, y+5+64-c.getAlpha()/4, x+5+64+valueOffset+opacityOffset, y+5+64-c.getAlpha()/4-1, 0xFF000000);
        if (chromaSpeed > 0) drawRect(x+5+64+valueOffset+opacityOffset+5, y+5+64-(int)(chromaSpeed/255f*64), x+5+64+valueOffset+opacityOffset+15, y+5+64-(int)(chromaSpeed/255f*64)+1, 0xFF000000);

        MinecraftCompat.getMinecraft().getTextureManager().loadTexture(colourPickerLocation, new DynamicTexture(bi));
        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colourPickerLocation);
        GlStateManagerCompat.color(1, 1, 1, 1);
        RenderUtils.drawTexturedRect(x+1, y+1, 72, 72, GL11.GL_LINEAR);

        MinecraftCompat.getMinecraft().getTextureManager().bindTexture(colour_selector_dot);
        GlStateManagerCompat.color(1, 1, 1, 1);
        RenderUtils.drawTexturedRect(x+5+32+selX-4, y+5+32+selY-4, 8, 8, GL11.GL_NEAREST);

        TextRenderUtils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY + "" + Math.round(hsv[2]*100), MinecraftCompat.getMinecraft().fontRendererObj, x+5+64+5+5-(Math.round(hsv[2]*100)==100?1:0), y+5+64+5+5, true, 13, -1);
        if (opacitySlider) TextRenderUtils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY + "" + Math.round(c.getAlpha()/255f*100), MinecraftCompat.getMinecraft().fontRendererObj, x+5+64+5+valueOffset+5, y+5+64+5+5, true, 13, -1);
        if (chromaSpeed > 0) TextRenderUtils.drawStringCenteredScaledMaxWidth(EnumChatFormatting.GRAY + "" + (int)ChromaColour.getSecondsForSpeed(chromaSpeed) + "s", MinecraftCompat.getMinecraft().fontRendererObj, x+5+64+5+valueOffset+opacityOffset+6, y+5+64+5+5, true, 13, -1);

        hexField.setSize(48, 10);
        if (!hexField.getFocus()) hexField.setText(Integer.toHexString(c.getRGB() & 0xFFFFFF).toUpperCase());
        StringBuilder sb = new StringBuilder(EnumChatFormatting.GRAY + "#");
        for (int i = 0; i < 6 - hexField.getText().length(); i++) sb.append("0");
        sb.append(EnumChatFormatting.WHITE);
        hexField.setPrependText(sb.toString());
        hexField.render(x+5+8, y+5+64+5);
    }

    @Override
    public boolean mouseInput(int mouseX, int mouseY) {
        float[] mf = KeybindHelper.getMouseCoordsFloat(GuiScreenUtils.getScaledResolution());
        float mxF = mf[0], myF = mf[1];

        if ((MouseCompat.getEventButton() == 0 || MouseCompat.getEventButton() == 1) && MouseCompat.getEventButtonState()) {
            if (mouseX > x+5+8 && mouseX < x+5+56 && mouseY > y+5+64+5 && mouseY < y+5+74) {
                hexField.mouseClicked(mouseX, mouseY, MouseCompat.getEventButton());
                clickedComponent = -1;
                return true;
            }
        }
        if (!MouseCompat.getEventButtonState() && MouseCompat.getEventButton() == 0) clickedComponent = -1;

        if (MouseCompat.getEventButtonState() && MouseCompat.getEventButton() == 0) {
            if (mouseX >= x && mouseX <= x+119 && mouseY >= y && mouseY <= y+89) {
                hexField.unfocus();
                int xW = mouseX - x - 5, yRel = mouseY - y - 5;
                if (xW > 0 && xW < 64 && yRel > 0 && yRel < 64) clickedComponent = 0;

                int opacityOffset = opacitySlider ? 15 : 0;
                int valueOffset   = valueSlider   ? 15 : 0;
                int y2 = mouseY - this.y - 5;
                if (y2 > -5 && y2 <= 69) {
                    if (valueSlider   && mouseX - (x+5+64+5) > 0            && mouseX - (x+5+64+5) < 10)                clickedComponent = 1;
                    if (opacitySlider && mouseX - (x+5+64+5+valueOffset) > 0 && mouseX - (x+5+64+5+valueOffset) < 10)  clickedComponent = 2;
                }
                int xC = mouseX - (x+5+64+valueOffset+opacityOffset+5);
                if (xC > 0 && xC < 10) {
                    int chromaSpeed = ChromaColour.getSpeed(colour);
                    if (chromaSpeed > 0 && y2 > -5 && y2 <= 69) { clickedComponent = 3; }
                    else if (chromaSpeed == 0 && mouseY > this.y+5+27 && mouseY < this.y+5+37) {
                        int cur = ChromaColour.specialToSimpleRGB(colour);
                        colour = ChromaColour.special(200, new Color(cur, true).getAlpha(), cur);
                        colourChangedCallback.accept(colour);
                    }
                }
            } else {
                hexField.unfocus();
                closeCallback.run();
                return false;
            }
        }

        if (MouseCompat.isButtonDown(0) && clickedComponent >= 0) {
            int cur = ChromaColour.specialToSimpleRGB(colour);
            Color c = new Color(cur, true);
            float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            float xW = mxF - x - 5, yW = myF - y - 5;

            if (clickedComponent == 0) {
                float angle = (float) Math.toDegrees(Math.atan((32 - xW) / (yW - 32 + 1E-5)) + Math.PI / 2);
                xW = Math.max(0, Math.min(64, xW)); yW = Math.max(0, Math.min(64, yW));
                float radius = (float) Math.sqrt(((xW-32)*(xW-32)+(yW-32)*(yW-32)) / 1024f);
                if (yW < 32) angle += 180;
                this.wheelAngle = angle;
                this.wheelRadius = (float) Math.pow(Math.min(1, radius), 1.5f);
                colour = ChromaColour.special(ChromaColour.getSpeed(colour), c.getAlpha(), Color.getHSBColor(angle/360f, wheelRadius, hsv[2]).getRGB());
                colourChangedCallback.accept(colour);
                return true;
            }
            float y2 = Math.max(0, Math.min(64, myF - this.y - 5));
            if (clickedComponent == 1) { colour = ChromaColour.special(ChromaColour.getSpeed(colour), c.getAlpha(), Color.getHSBColor(wheelAngle/360, wheelRadius, 1-y2/64f).getRGB()); colourChangedCallback.accept(colour); return true; }
            if (clickedComponent == 2) { colour = ChromaColour.special(ChromaColour.getSpeed(colour), 255-Math.round(y2/64f*255), cur); colourChangedCallback.accept(colour); return true; }
            if (clickedComponent == 3) { colour = ChromaColour.special(255-Math.round(y2/64f*255), c.getAlpha(), cur); colourChangedCallback.accept(colour); }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyboardInput() {
        if (KeyboardCompat.getEventKeyState() && hexField.getFocus()) {
            if (KeyboardCompat.getEventKey() == Keyboard.KEY_ESCAPE) { hexField.unfocus(); return true; }
            String old = hexField.getText();
            hexField.keyTyped(KeyboardCompat.getEventCharacter(), KeyboardCompat.getEventKey());
            if (hexField.getText().length() > 6) {
                hexField.setText(old);
            } else {
                try {
                    int rgb = Integer.parseInt(hexField.getText().toLowerCase(), 16);
                    int alpha = (ChromaColour.specialToSimpleRGB(colour) >> 24) & 0xFF;
                    colour = ChromaColour.special(ChromaColour.getSpeed(colour), alpha, rgb);
                    colourChangedCallback.accept(colour);
                    Color c = new Color(rgb);
                    updateAngleAndRadius(Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null));
                } catch (Exception ignored) {}
            }
            return true;
        }
        return false;
    }
}