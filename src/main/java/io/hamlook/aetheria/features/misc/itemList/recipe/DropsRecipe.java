package io.hamlook.aetheria.features.misc.itemList.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class DropsRecipe extends Recipe {
    private final List<JsonObject> recipes;
    private static final int ROW_H = 22;
    private static final int S     = 18;
    private List<JsonObject> sortedDrops;

    public DropsRecipe(SkyblockItem t, List<JsonObject> recipes) {
        super(t);
        this.recipes = recipes;
    }

    @Override public String typeLabel()      { return "§6Mob Drops"; }
    @Override public int    typeLabelColor() { return 0xFFAA00; }

    @Override
    public int[] preferredSize() {
        int n = getSortedDrops().size();
        return new int[]{ 220, Math.max(n, 1) * ROW_H + 10 };
    }

    private List<JsonObject> getSortedDrops() {
        if (sortedDrops != null) return sortedDrops;
        sortedDrops = new ArrayList<>();
        if (recipes.isEmpty() || !recipes.get(0).has("drops")) return sortedDrops;
        JsonArray arr = recipes.get(0).get("drops").getAsJsonArray();
        for (JsonElement el : arr) sortedDrops.add(el.getAsJsonObject());
        sortedDrops.sort((a, b) -> Float.compare(
                RecipeUtils.parseChance(b.has("chance") ? b.get("chance").getAsString() : "0"),
                RecipeUtils.parseChance(a.has("chance") ? a.get("chance").getAsString() : "0")));
        return sortedDrops;
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height,
                     int scrollY, FontRenderer fr, GuiScreen gui) {
        List<JsonObject> drops = getSortedDrops();
        int listH = drops.size() * ROW_H;
        int listX = x + 20;
        int listY = y + 5;

        ScaledResolution sr = GuiScreenUtils.getScaledResolution();
        int sf = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * sf, (MinecraftCompat.getMinecraft().displayHeight - (y + height) * sf), width * sf, height * sf);

        for (int i = 0; i < drops.size(); i++) {
            int sy = listY + i * ROW_H - scrollY;
            if (sy + ROW_H < y || sy > y + height) continue;

            JsonObject drop = drops.get(i);
            String dropIdRaw = drop.has("id") ? drop.get("id").getAsString() : "";
            String[] parts = dropIdRaw.split(":");
            String dropId = parts[0];
            String amt = parts.length > 1 ? parts[1] : "1";
            SkyblockItem dropItem = RecipeUtils.resolve(dropId);

            RecipeUtils.drawSlot(listX, sy, S);
            if (dropItem != null && dropItem.getStack() != null) {
                ItemRenderUtils.drawItemStack(dropItem.getStack(), listX + 1, sy + 1);
                RecipeUtils.drawAmount(fr, amt, listX, sy);

                String name = dropItem.displayName != null ? dropItem.displayName : dropId;
                fr.drawStringWithShadow(name, listX + 24, sy + 1, 0xFFFFFF);
            } else {
                fr.drawStringWithShadow(dropId, listX + 24, sy + 1, 0xFFFFFF);
            }

            String rawChance = drop.has("chance") ? drop.get("chance").getAsString() : "100%";
            String fmt = RecipeUtils.formatChance(rawChance);
            String chanceStr = "Chance: " + RecipeUtils.getChanceColor(fmt) + fmt;
            fr.drawStringWithShadow(chanceStr, listX + 24, sy + 11, 0xAAAAAA);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public List<String> getTooltip(int mouseX, int mouseY, int x, int y,
                                   int width, int height, int scrollY) {
        int idx = getHoveredSlotIndex(mouseX, mouseY, x, y, width, height, scrollY);
        if (idx == -1) return null;
        JsonObject drop = getSortedDrops().get(idx);
        String dropIdRaw = drop.has("id") ? drop.get("id").getAsString() : "";
        String[] parts = dropIdRaw.split(":");
        String amt = parts.length > 1 ? parts[1] : "1";
        SkyblockItem dropItem = RecipeUtils.resolve(parts[0]);
        if (dropItem == null) return null;

        String rawChance = drop.has("chance") ? drop.get("chance").getAsString() : "100%";
        String fmt = RecipeUtils.formatChance(rawChance);
        List<String> tip = RecipeUtils.buildItemTooltipWithAmount(dropItem, amt);
        tip.add("§8---------------");
        tip.add("§7Chance: " + RecipeUtils.getChanceColor(fmt) + fmt);
        return tip;
    }

    private int getHoveredSlotIndex(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        List<JsonObject> drops = getSortedDrops();
        int listX = x + 20;
        int listY = y + 5;

        for (int i = 0; i < drops.size(); i++) {
            int sy = listY + i * ROW_H - scrollY;
            if (sy + ROW_H < y || sy > y + height) continue;
            if (mouseX >= listX && mouseX < listX + S && mouseY >= sy && mouseY < sy + S) return i;
        }
        return -1;
    }

    @Override
    public SkyblockItem getSkyblockItemAt(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY) {
        int idx = getHoveredSlotIndex(mouseX, mouseY, x, y, width, height, scrollY);
        if (idx == -1) return null;
        JsonObject drop = getSortedDrops().get(idx);
        String dropIdRaw = drop.has("id") ? drop.get("id").getAsString() : "";
        return RecipeUtils.resolve(dropIdRaw.split(":")[0]);
    }
}