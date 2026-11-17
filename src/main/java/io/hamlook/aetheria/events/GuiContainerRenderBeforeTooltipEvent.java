package io.hamlook.aetheria.events;

import io.hamlook.aetheria.api.event.RenderingAetheriaEvent;
import io.hamlook.aetheria.utils.compat.DrawContext;
//#if MC < 1.21
import net.minecraft.client.gui.inventory.GuiContainer;
//#else
//$$ import net.minecraft.client.gui.screen.ingame.HandledScreen;
//#endif

public class GuiContainerRenderBeforeTooltipEvent extends RenderingAetheriaEvent {

    //#if MC < 1.21
    public final GuiContainer gui;
    //#else
    //$$ public final HandledScreen<?> gui;
    //#endif
    public final int mouseX;
    public final int mouseY;
    /**
     * True when posted from a translated foreground context
     * (legacy container draw / modern {@code HandledScreen.drawForeground}).
     * False from untranslated screen-space posts (modern inventory render).
     * Consumers skip the -guiLeft/-guiTop compensation when false.
     */
    public final boolean fromForeground;

    //#if MC < 1.21
    public GuiContainerRenderBeforeTooltipEvent(DrawContext context, GuiContainer gui, int mouseX, int mouseY) {
        this(context, gui, mouseX, mouseY, true);
    }

    public GuiContainerRenderBeforeTooltipEvent(DrawContext context, GuiContainer gui, int mouseX, int mouseY, boolean fromForeground) {
    //#else
    //$$ public GuiContainerRenderBeforeTooltipEvent(DrawContext context, HandledScreen<?> gui, int mouseX, int mouseY) {
    //$$     this(context, gui, mouseX, mouseY, true);
    //$$ }
    //$$
    //$$ public GuiContainerRenderBeforeTooltipEvent(DrawContext context, HandledScreen<?> gui, int mouseX, int mouseY, boolean fromForeground) {
    //#endif
        super(context);
        this.gui = gui;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.fromForeground = fromForeground;
    }
}
