package io.hamlook.aetheria.features.misc.itemList.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class NpcShopRecipe extends Recipe {
    private final List<JsonObject> recipes;
    private static final int COLS = 7;
    private static final int ROWS = 5;
    private static final int S    = 18;

    private int currentPage = 0;
    private final int itemsPerPage;
    private final int totalPages;

    public NpcShopRecipe(SkyblockItem t, List<JsonObject> recipes) {
        super(t);
        this.recipes = recipes;

        // If it exceeds a standard 7x5 chest layout, shrink itemsPerPage to reserve the bottom row for arrows
        if (recipes.size() > COLS * ROWS) {
            this.itemsPerPage = COLS * (ROWS - 1); // 28 items per page
            this.totalPages = (int) Math.ceil((double) recipes.size() / itemsPerPage);
        } else {
            this.itemsPerPage = COLS * ROWS; // 35 items
            this.totalPages = 1;
        }
    }

    @Override public String typeLabel()      { return "§9NPC Shop"; }
    @Override public int    typeLabelColor() { return 0x55FFFF; }

    @Override
    public int[] preferredSize() {
        return new int[]{ COLS * S + 20, ROWS * S + 10 };
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height,
                     int scrollY, FontRenderer fr, GuiScreen gui) {
        int gridW = COLS * S;
        int gridH = ROWS * S;
        int gridX = x + (width  - gridW) / 2;
        int gridY = y + (height - gridH) / 2;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(gridX * sf, (Minecraft.getMinecraft().displayHeight - (gridY + gridH) * sf), gridW * sf, gridH * sf);

        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, recipes.size());

        for (int i = 0; i < COLS * ROWS; i++) {
            int col = i % COLS, row = i / COLS;
            int sx = gridX + col * S, sy = gridY + row * S;

            RecipeUtils.drawSlot(sx, sy, S);

            // Draw Pagination Arrows if necessary
            if (totalPages > 1 && row == ROWS - 1) {
                if (col == 0 && currentPage > 0) {
                    ItemRenderUtils.drawItemStack(new ItemStack(Items.arrow), sx + 1, sy + 1);
                } else if (col == COLS - 1 && currentPage < totalPages - 1) {
                    ItemRenderUtils.drawItemStack(new ItemStack(Items.arrow), sx + 1, sy + 1);
                }
                continue;
            }

            int itemIndex = startIdx + (row * COLS + col);
            if (itemIndex >= recipes.size() || itemIndex >= endIdx) continue;

            JsonObject recipe = recipes.get(itemIndex);
            if (!recipe.has("result")) continue;

            String resultRaw = recipe.get("result").getAsString();
            String[] rp = resultRaw.split(":");
            String rAmt = rp.length > 1 ? rp[1] : "1";
            SkyblockItem resultItem = RecipeUtils.resolve(rp[0]);

            if (resultItem != null && resultItem.getStack() != null) {
                ItemRenderUtils.drawItemStack(resultItem.getStack(), sx + 1, sy + 1);
                RecipeUtils.drawAmount(fr, rAmt, sx, sy);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public List<String> getTooltip(int mouseX, int mouseY, int x, int y,
                                   int width, int height, int scrollY) {
        int gridW = COLS * S, gridH = ROWS * S;
        int gridX = x + (width  - gridW) / 2;
        int gridY = y + (height - gridH) / 2;

        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, recipes.size());

        for (int i = 0; i < COLS * ROWS; i++) {
            int col = i % COLS, row = i / COLS;
            int sx = gridX + col * S, sy = gridY + row * S;
            if (mouseX < sx || mouseX >= sx + S || mouseY < sy || mouseY >= sy + S) continue;

            // Arrow Tooltips
            if (totalPages > 1 && row == ROWS - 1) {
                if (col == 0 && currentPage > 0) {
                    List<String> tip = new ArrayList<>();
                    tip.add("§aPrevious Page");
                    tip.add("§7Page " + currentPage + " of " + totalPages);
                    return tip;
                } else if (col == COLS - 1 && currentPage < totalPages - 1) {
                    List<String> tip = new ArrayList<>();
                    tip.add("§aNext Page");
                    tip.add("§7Page " + (currentPage + 2) + " of " + totalPages);
                    return tip;
                }
                return null;
            }

            // Item Tooltips
            int itemIndex = startIdx + (row * COLS + col);
            if (itemIndex >= recipes.size() || itemIndex >= endIdx) return null;

            JsonObject recipe = recipes.get(itemIndex);
            if (!recipe.has("result")) return null;
            String[] rp = recipe.get("result").getAsString().split(":");
            SkyblockItem rItem = RecipeUtils.resolve(rp[0]);
            String rAmt = rp.length > 1 ? rp[1] : "1";
            if (rItem == null) return null;

            List<String> tip = RecipeUtils.buildItemTooltipWithAmount(rItem, rAmt);
            tip.add("§8---------------");
            if (recipe.has("cost") && recipe.get("cost").isJsonArray()) {
                JsonArray costArr = recipe.get("cost").getAsJsonArray();
                for (int c = 0; c < costArr.size(); c++) {
                    String[] cp = costArr.get(c).getAsString().split(":");
                    String rId = cp[0], cAmt = cp.length > 1 ? cp[1] : "1";
                    if (rId.equals("SKYBLOCK_COIN")) {
                        tip.add("§7Cost: §6" + String.format("%,d", Long.parseLong(cAmt)) + " Coins");
                    } else {
                        SkyblockItem costItem = RecipeUtils.resolve(rId);
                        if (costItem != null) tip.add("§7Cost: " + costItem.displayName + " §8x" + cAmt);
                    }
                }
            }
            return tip;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int x, int y, int width, int height, int scrollY) {
        if (totalPages <= 1) return false;

        int gridW = COLS * S;
        int gridH = ROWS * S;
        int gridX = x + (width  - gridW) / 2;
        int gridY = y + (height - gridH) / 2;

        int row = ROWS - 1;
        int sy = gridY + row * S;

        if (mouseY >= sy && mouseY <= sy + S) {
            if (currentPage > 0 && mouseX >= gridX && mouseX <= gridX + S) {
                currentPage--;
                return true;
            }
            int nextX = gridX + (COLS - 1) * S;
            if (currentPage < totalPages - 1 && mouseX >= nextX && mouseX <= nextX + S) {
                currentPage++;
                return true;
            }
        }
        return false;
    }
}