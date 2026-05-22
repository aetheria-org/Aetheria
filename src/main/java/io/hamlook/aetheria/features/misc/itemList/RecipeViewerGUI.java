package io.hamlook.aetheria.features.misc.itemList;

import io.hamlook.aetheria.core.config.gui.GuiTextures;
import io.hamlook.aetheria.features.misc.itemList.recipe.Recipe;
import io.hamlook.aetheria.features.misc.itemList.recipe.RecipeFactory;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import io.hamlook.aetheria.utils.render.NineSliceUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

public class RecipeViewerGUI extends GuiScreen {

    private static final int MIN_W = 220;
    private static final int MIN_H = 130;
    static final int HEADER_H = 36;
    static final int NAV_H = 20;

    private final SkyblockItem item;
    private final List<Recipe>  recipes;
    private int recipeIndex = 0;
    private int scrollY     = 0;

    private int boxX, boxY, boxW, boxH;

    public RecipeViewerGUI(SkyblockItem item) {
        this.item    = item;
        this.recipes = RecipeFactory.build(item);
    }

    @Override public void initGui()      { Keyboard.enableRepeatEvents(true);  recipeIndex = 0; scrollY = 0; }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }

    private void computeBox() {
        int contentW, contentH;

        if (!recipes.isEmpty()) {
            Recipe r = recipes.get(recipeIndex);
            int[] sz = r.preferredSize();
            contentW = sz[0];
            contentH = sz[1];
        } else {
            // Dynamically scale the box based on the actual length and width of the lore
            contentH = (item.baseLore != null ? item.baseLore.size() * 10 : 0) + 10;
            contentW = 200;

            if (this.fontRendererObj != null) {
                if (item.displayName != null) contentW = Math.max(contentW, this.fontRendererObj.getStringWidth(item.displayName));
                if (item.baseLore != null) {
                    for (String line : item.baseLore) {
                        contentW = Math.max(contentW, this.fontRendererObj.getStringWidth(line));
                    }
                }
            }
        }

        // Remove the arbitrary MAX_H and MAX_W limits so huge lores fit, but cap it to screen boundaries
        boxW = Math.max(MIN_W, Math.min(this.width - 20, contentW + 40));
        boxH = Math.max(MIN_H, Math.min(this.height - 20, HEADER_H + contentH + NAV_H + 10));

        boxX = (width  - boxW) / 2;
        boxY = (height - boxH) / 2;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (ItemPaneRenderer.INSTANCE != null) ItemPaneRenderer.INSTANCE.handleKeyInput();
        if (key == Keyboard.KEY_ESCAPE || key == mc.gameSettings.keyBindInventory.getKeyCode())
            mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (ItemPaneRenderer.INSTANCE != null)
            ItemPaneRenderer.INSTANCE.handleClick(width, height, mx, my, btn, null);

        computeBox();
        int contentX = boxX;
        int contentY = boxY + HEADER_H;
        int contentH = boxH - HEADER_H - NAV_H;

        if (!recipes.isEmpty()) {
            if (recipes.get(recipeIndex).mouseClicked(mx, my, btn, contentX, contentY, boxW, contentH, scrollY)) {
                return;
            }
        }

        if (recipes.size() <= 1) return;
        int arrowY = boxY + boxH - NAV_H;
        if (my >= arrowY && my <= arrowY + NAV_H) {
            if (mx >= boxX + 10 && mx <= boxX + 30) {
                recipeIndex = (recipeIndex - 1 + recipes.size()) % recipes.size();
                scrollY = 0;
            } else if (mx >= boxX + boxW - 30 && mx <= boxX + boxW - 10) {
                recipeIndex = (recipeIndex + 1) % recipes.size();
                scrollY = 0;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = Mouse.getEventDWheel();
        if (dw != 0) {
            if (ItemPaneRenderer.INSTANCE != null) {
                Minecraft mc = Minecraft.getMinecraft();
                int mx = Mouse.getEventX() * width / mc.displayWidth;
                int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
                ItemPaneRenderer.INSTANCE.handleMouseInput(width, height, mx, my, null);
            }
            scrollY += dw > 0 ? -20 : 20;
            if (scrollY < 0) scrollY = 0;
        }
    }


    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xA0000000);
        computeBox();

        NineSliceUtils.draw(GuiTextures.storageBackground(1), boxX, boxY, boxW, boxH, 6, 18);

        int cx = boxX + boxW / 2;

        if (recipes.isEmpty()) {
            ItemStack stack = item.getStack();
            String nameStr = item.displayName != null ? item.displayName : "";
            int nameW = fontRendererObj.getStringWidth(nameStr);
            int rowW = (stack != null ? 18 + 4 : 0) + nameW;
            int rowX = cx - rowW / 2;
            int rowY = boxY + 12;

            if (stack != null) {
                ItemRenderUtils.drawItemStack(stack, rowX, rowY - 4);
                rowX += 22;
            }
            fontRendererObj.drawStringWithShadow(nameStr, rowX, rowY, 0xFFFFFF);

            if (item.baseLore != null) {
                int ly = boxY + 32;
                for (int i = 0; i < item.baseLore.size(); i++) {
                    if (ly + 10 > boxY + boxH - NAV_H) break;

                    drawCenteredString(fontRendererObj, item.baseLore.get(i), cx, ly, 0xFFFFFF);
                    ly += 10;
                }
            }
        } else {
            Recipe current = recipes.get(recipeIndex);

            String typeLabel = current.typeLabel();
            String nameStr  = item.displayName != null ? item.displayName : "";
            ItemStack stack = item.getStack();

            int typeW = fontRendererObj.getStringWidth(typeLabel);
            int nameW = fontRendererObj.getStringWidth(nameStr);
            int iconW = stack != null ? 18 : 0;

            int totalW = typeW + (iconW > 0 ? 4 + iconW : 0) + 4 + nameW;
            int currentX = cx - totalW / 2;
            int textY = boxY + 12;

            fontRendererObj.drawStringWithShadow(typeLabel, currentX, textY, current.typeLabelColor());
            currentX += typeW + 4;

            if (stack != null) {
                ItemRenderUtils.drawItemStack(stack, currentX, textY - 4);
                currentX += 18 + 4;
            }
            fontRendererObj.drawStringWithShadow(nameStr, currentX, textY, 0xFFFFFF);

            int contentX = boxX;
            int contentY = boxY + HEADER_H;
            int contentH = boxH - HEADER_H - NAV_H;
            current.draw(mx, my, contentX, contentY, boxW, contentH, scrollY, fontRendererObj, this);

            if (recipes.size() > 1) {
                int arrowY = boxY + boxH - NAV_H + 4;
                drawCenteredString(fontRendererObj, "<", boxX + 20, arrowY, 0xFFFFFF);
                drawCenteredString(fontRendererObj, ">", boxX + boxW - 20, arrowY, 0xFFFFFF);
                drawCenteredString(fontRendererObj, "§7" + (recipeIndex + 1) + " / " + recipes.size(), cx, arrowY, 0xFFFFFF);
            }

            List<String> tip = current.getTooltip(mx, my, contentX, contentY, boxW, contentH, scrollY);
            if (tip != null && !tip.isEmpty()) {
                TextRenderUtils.drawHoveringText(tip, mx, my, fontRendererObj);
            }
        }

        super.drawScreen(mx, my, pt);
        if (ItemPaneRenderer.INSTANCE != null)
            ItemPaneRenderer.INSTANCE.drawPane(width, height, mx, my);
    }
}