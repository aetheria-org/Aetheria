package io.hamlook.aetheria.features.misc.itemList.recipe;

import com.google.gson.JsonObject;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class CraftingRecipe extends Recipe {
    private final JsonObject recipeData;
    private static final String[] SLOTS = {"A1","A2","A3","B1","B2","B3","C1","C2","C3"};
    private static final int S = 18;

    public CraftingRecipe(SkyblockItem targetItem, JsonObject recipeData) {
        super(targetItem);
        this.recipeData = recipeData;
    }

    @Override public String typeLabel()      { return "§7Crafting"; }
    @Override public int    typeLabelColor() { return 0xAAAAAA; }

    @Override
    public int[] preferredSize() {
        return new int[]{ 3*S + 10 + 10 + 6 + 26 + 40, 3*S + 20 };
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height,
                     int scrollY, FontRenderer fr, GuiScreen gui) {
        int totalW = 3 * S + 16 + 26;
        int gridStartX = x + (width - totalW) / 2;
        int gridStartY = y + (height - 3 * S) / 2;

        for (int i = 0; i < SLOTS.length; i++) {
            int col = i % 3, row = i / 3;
            int sx = gridStartX + col * S, sy = gridStartY + row * S;
            RecipeUtils.drawSlot(sx, sy, S);
            if (recipeData.has(SLOTS[i])) {
                String req = recipeData.get(SLOTS[i]).getAsString();
                if (req != null && !req.isEmpty()) {
                    String[] parts = req.split(":");
                    String amt = parts.length > 1 ? parts[1] : "1";
                    SkyblockItem reqItem = RecipeUtils.resolve(parts[0]);

                    if (reqItem != null && reqItem.getStack() != null) {
                        ItemRenderUtils.drawItemStack(reqItem.getStack(), sx + 1, sy + 1);
                        RecipeUtils.drawAmount(fr, amt, sx, sy);
                    }
                }
            }
        }

        int arrowX = gridStartX + 3 * S + 4;
        DrawContextUtils.drawTextWithShadow(fr, ">", arrowX, gridStartY + S + 4, 0xFFFFFF);

        int resultX = arrowX + 12;
        int resultY = gridStartY + S - 4;
        RecipeUtils.drawSlot(resultX, resultY, 26);
        ItemStack stack = targetItem.getStack();
        if (stack != null) {
            GL11.glPushMatrix();
            GL11.glTranslatef(resultX + 1, resultY + 1, 0);
            GL11.glScalef(1.5f, 1.5f, 1f);
            ItemRenderUtils.drawItemStack(stack, 0, 0);
            GL11.glPopMatrix();
        }
    }

    private int getHoveredSlotIndex(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        int totalW = 3 * S + 16 + 26;
        int gridStartX = x + (width - totalW) / 2;
        int gridStartY = y + (height - 3 * S) / 2;

        for (int i = 0; i < SLOTS.length; i++) {
            int sx = gridStartX + (i % 3) * S, sy = gridStartY + (i / 3) * S;
            if (mouseX >= sx && mouseX < sx + S && mouseY >= sy && mouseY < sy + S) return i;
        }
        int resultX = gridStartX + 3 * S + 16, resultY = gridStartY + S - 4;
        if (mouseX >= resultX && mouseX < resultX + 26 && mouseY >= resultY && mouseY < resultY + 26) return 9;
        return -1;
    }

    @Override
    public List<String> getTooltip(int mouseX, int mouseY, int x, int y,
                                   int width, int height, int scrollY) {
        int idx = getHoveredSlotIndex(mouseX, mouseY, x, y, width, height, scrollY);
        if (idx == -1) return null;
        if (idx == 9) return RecipeUtils.buildItemTooltip(targetItem);
        if (!recipeData.has(SLOTS[idx])) return null;
        String req = recipeData.get(SLOTS[idx]).getAsString();
        if (req == null || req.isEmpty()) return null;
        String[] parts = req.split(":");
        SkyblockItem reqItem = RecipeUtils.resolve(parts[0]);
        if (reqItem == null) return null;
        return RecipeUtils.buildItemTooltipWithAmount(reqItem, parts.length > 1 ? parts[1] : "1");
    }

    @Override
    public SkyblockItem getSkyblockItemAt(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        int idx = getHoveredSlotIndex(mouseX, mouseY, x, y, width, height, scrollY);
        if (idx == -1) return null;
        if (idx == 9) return targetItem;
        if (!recipeData.has(SLOTS[idx])) return null;
        String req = recipeData.get(SLOTS[idx]).getAsString();
        if (req == null || req.isEmpty()) return null;
        return RecipeUtils.resolve(req.split(":")[0]);
    }
}