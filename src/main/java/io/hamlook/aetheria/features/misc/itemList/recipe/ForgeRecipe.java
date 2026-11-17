package io.hamlook.aetheria.features.misc.itemList.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.compat.DrawContextUtils;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ForgeRecipe extends Recipe {
    protected final JsonObject recipeData;
    private final String recipeType;
    private static final int S = 18;

    public ForgeRecipe(SkyblockItem t, JsonObject d)                 { this(t, d, "forge"); }
    public ForgeRecipe(SkyblockItem t, JsonObject d, String type)    { super(t); recipeData = d; recipeType = type; }

    @Override public String typeLabel() {
        switch (recipeType) {
            case "katgrade": return "§dKat Upgrade";
            case "trade":    return "§aTrade";
            default:         return "§cForge";
        }
    }
    @Override public int typeLabelColor() {
        switch (recipeType) {
            case "katgrade": return 0xFF55FF;
            case "trade":    return 0x55FF55;
            default:         return 0xFF5555;
        }
    }

    @Override
    public int[] preferredSize() {
        JsonArray inputs = collectInputs();
        int n = inputs.size();
        return new int[]{ n * (S + 2) + 20 + S + 40, S + 60 };
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height,
                     int fontH, FontRenderer fr, GuiScreen gui) {
        JsonArray inputs = collectInputs();
        int n = inputs.size();

        int dur = recipeData.has("duration") ? recipeData.get("duration").getAsInt()
                : recipeData.has("time")     ? recipeData.get("time").getAsInt() : 0;

        int metaLines = 0;
        if (dur > 0) metaLines++;
        if (recipeData.has("coins")) metaLines++;

        int metaH = metaLines * 12;
        int totalContentH = metaH + 4 + S;
        int rowY  = y + (height - totalContentH) / 2;

        int textY = rowY;
        int cx = x + width / 2;
        if (dur > 0) {
            String t = "§7Time: §f" + (dur / 3600) + "h " + ((dur % 3600) / 60) + "m";
            DrawContextUtils.drawTextWithShadow(fr, t, (int) (cx - DrawContextUtils.getStringWidth(fr, t) / 2f), textY, 0xFFFFFF);
            textY += 12;
        }
        if (recipeData.has("coins")) {
            String c = "§7Coins: §6" + String.format("%,d", recipeData.get("coins").getAsInt());
            DrawContextUtils.drawTextWithShadow(fr, c, (int) (cx - DrawContextUtils.getStringWidth(fr, c) / 2f), textY, 0xFFFFFF);
        }

        int slotRowW = n * (S + 2) + 14 + S;
        int slotX    = x + (width - slotRowW) / 2;
        int slotY    = rowY + metaH + 4;

        for (int i = 0; i < n; i++) {
            String[] parts = inputs.get(i).getAsString().split(":");
            String reqId = parts[0];
            String amt = parts.length > 1 ? parts[1] : "1";

            int sx = slotX + i * (S + 2);
            RecipeUtils.drawSlot(sx, slotY, S);

            SkyblockItem reqItem = RecipeUtils.resolve(reqId);
            if (reqItem != null && reqItem.getStack() != null) {
                ItemRenderUtils.drawItemStack(reqItem.getStack(), sx + 1, slotY + 1);
                RecipeUtils.drawAmount(fr, amt, sx, slotY);
            }
        }

        int arrowX = slotX + n * (S + 2) + 2;
        DrawContextUtils.drawTextWithShadow(fr, ">", arrowX, slotY + 5, 0xFFFFFF);

        int resultX = arrowX + 12;
        RecipeUtils.drawSlot(resultX, slotY, S);
        ItemStack stack = targetItem.getStack();
        if (stack != null) ItemRenderUtils.drawItemStack(stack, resultX + 1, slotY + 1);
    }

    private int getHoveredSlotIndex(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        JsonArray inputs = collectInputs();
        int n = inputs.size();

        int dur = recipeData.has("duration") ? recipeData.get("duration").getAsInt()
                : recipeData.has("time")     ? recipeData.get("time").getAsInt() : 0;
        int metaLines = (dur > 0 ? 1 : 0) + (recipeData.has("coins") ? 1 : 0);
        int metaH = metaLines * 12;
        int totalContentH = metaH + 4 + S;
        int rowY   = y + (height - totalContentH) / 2;
        int slotY  = rowY + metaH + 4;
        int slotRowW = n * (S + 2) + 14 + S;
        int slotX    = x + (width - slotRowW) / 2;

        for (int i = 0; i < n; i++) {
            int sx = slotX + i * (S + 2);
            if (mouseX >= sx && mouseX < sx + S && mouseY >= slotY && mouseY < slotY + S) return i;
        }
        int resultX = slotX + n * (S + 2) + 14;
        if (mouseX >= resultX && mouseX < resultX + S && mouseY >= slotY && mouseY < slotY + S) return n;
        return -1;
    }

    @Override
    public List<String> getTooltip(int mouseX, int mouseY, int x, int y,
                                   int width, int height, int scrollY) {
        int idx = getHoveredSlotIndex(mouseX, mouseY, x, y, width, height, scrollY);
        if (idx == -1) return null;
        JsonArray inputs = collectInputs();
        if (idx == inputs.size()) return RecipeUtils.buildItemTooltip(targetItem);
        String[] parts = inputs.get(idx).getAsString().split(":");
        SkyblockItem reqItem = RecipeUtils.resolve(parts[0]);
        if (reqItem == null) return null;
        return RecipeUtils.buildItemTooltipWithAmount(reqItem, parts.length > 1 ? parts[1] : "1");
    }

    @Override
    public SkyblockItem getSkyblockItemAt(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        int idx = getHoveredSlotIndex(mouseX, mouseY, x, y, width, height, scrollY);
        if (idx == -1) return null;
        JsonArray inputs = collectInputs();
        if (idx == inputs.size()) return targetItem;
        return RecipeUtils.resolve(inputs.get(idx).getAsString().split(":")[0]);
    }

    protected JsonArray collectInputs() {
        JsonArray combined = new JsonArray();
        if (recipeData.has("input")) {
            if (recipeData.get("input").isJsonPrimitive()) combined.add(recipeData.get("input"));
            else if (recipeData.get("input").isJsonArray()) combined.addAll(recipeData.get("input").getAsJsonArray());
        }
        if (recipeData.has("inputs")) combined.addAll(recipeData.get("inputs").getAsJsonArray());
        if (recipeData.has("items"))  combined.addAll(recipeData.get("items").getAsJsonArray());
        if (recipeData.has("cost")) {
            if (recipeData.get("cost").isJsonPrimitive()) combined.add(recipeData.get("cost"));
            else if (recipeData.get("cost").isJsonArray()) combined.addAll(recipeData.get("cost").getAsJsonArray());
        }
        return combined;
    }
}