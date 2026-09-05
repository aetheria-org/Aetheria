// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.MouseCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;

public class GuiOptionEditorAccordion extends GuiOptionEditor {

    private final int accordionId;
    private boolean accordionToggled;

    public GuiOptionEditorAccordion(ConfigProcessor.ProcessedOption option, int accordionId) {
        super(option);
        this.accordionToggled = (boolean) option.get();
        this.accordionId = accordionId;
    }

    @Override
    public int getHeight() {
        return 20;
    }

    public int getAccordionId() {
        return accordionId;
    }

    public boolean getToggled() {
        return accordionToggled;
    }

    @Override
    public void render(int x, int y, int width) {
        int height = getHeight();
        RenderUtils.drawFloatingRectDark(x, y, width, height, true);

        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManagerCompat.color(1, 1, 1, 1);
        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.TRIANGLES, TessellatorCompat.POSITION);
        if (accordionToggled) {
            vb.pos((double) x + 6, (double) y + 6, 0.0D).endVertex();
            vb.pos((double) x + 9.75f, (double) y + 13.5f, 0.0D).endVertex();
            vb.pos((double) x + 13.5f, (double) y + 6, 0.0D).endVertex();
        } else {
            vb.pos((double) x + 6, (double) y + 13.5f, 0.0D).endVertex();
            vb.pos((double) x + 13.5f, (double) y + 9.75f, 0.0D).endVertex();
            vb.pos((double) x + 6, (double) y + 6, 0.0D).endVertex();
        }
        vb.draw();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.disableBlend();

        TextRenderUtils.drawStringScaledMaxWidth(option.name, MinecraftCompat.getFontRenderer(), x + 18, y + 6, false, width - 10, 0xc0c0c0);
    }

    @Override
    public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
        if (MouseCompat.getEventButtonState() && mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + getHeight()) {
            accordionToggled = !accordionToggled;
            return true;
        }

        return false;
    }

    @Override
    public boolean keyboardInput() {
        return false;
    }
}
