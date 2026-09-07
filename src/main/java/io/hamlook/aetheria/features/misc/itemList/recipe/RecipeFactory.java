package io.hamlook.aetheria.features.misc.itemList.recipe;

import com.google.gson.JsonObject;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;

import java.util.ArrayList;
import java.util.List;

public class RecipeFactory {

    private RecipeFactory() {}

    public static List<Recipe> build(SkyblockItem item) {
        List<Recipe> result = new ArrayList<>();
        if (item.recipes == null || item.recipes.isEmpty()) return result;

        String firstType = getType(item.recipes.get(0));

        if (firstType.equals("npc_shop")) {
            result.add(new NpcShopRecipe(item, item.recipes));
            return result;
        }

        if (firstType.equals("drops")) {
            result.add(new DropsRecipe(item, item.recipes));
            return result;
        }

        for (JsonObject r : item.recipes) {
            String type = getType(r);
            switch (type) {
                case "forge":     result.add(new ForgeRecipe(item, r));    break;
                case "katgrade":  result.add(new KatgradeRecipe(item, r)); break;
                case "trade":     result.add(new TradeRecipe(item, r));    break;
                default:          result.add(new CraftingRecipe(item, r)); break;
            }
        }

        return result;
    }

    private static String getType(JsonObject r) {
        return r.has("type") ? r.get("type").getAsString() : "crafting";
    }
}