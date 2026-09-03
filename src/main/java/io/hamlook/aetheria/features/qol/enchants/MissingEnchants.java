package io.hamlook.aetheria.features.qol.enchants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMTooltipEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;

import java.util.*;

@RegisterEvents
public class MissingEnchants {

    private static final String[] RARITY_PREFIXES = {"§f§l", "§a§l", "§9§l", "§5§l", "§6§l", "§d§l", "§b§l", "§c§l"};

    private static String toTitleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) {
            if (!word.isEmpty())
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    @HandleEvent(priority = HandleEvent.LOW)
    public void onTooltip(ASMTooltipEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.missingEnchants) return;
        if (!KeyboardCompat.isKeyDown(Keyboard.KEY_LSHIFT)) return;

        ItemStack stack = event.itemStack;
        if (stack == null) return;

        NBTTagCompound enchNbt = ItemUtils.getEnchantments(stack);
        if (enchNbt == null) return;
        Set<String> enchantIds = enchNbt.getKeySet();
        if (enchantIds.isEmpty()) return;

        JsonObject enchantsConst = RepoHandler.get(ATHRRepo.KEY_ENCHANTS, JsonObject.class, null);
        if (enchantsConst == null) return;

        Set<String> ignoreFromPool = getIgnoredFromPools(enchantsConst, enchantIds);
        JsonArray allItemEnchs = getAllEnchantsForItem(enchantsConst, stack);
        if (allItemEnchs == null) return;

        List<String> missing = new ArrayList<>();
        for (JsonElement el : allItemEnchs) {
            String enchId = el.getAsString();
            if (!enchId.startsWith("ultimate_") && !ignoreFromPool.contains(enchId) && !enchantIds.contains(enchId))
                missing.add(enchId);
        }

        if (missing.isEmpty()) return;

        int insertAt = findInsertionPoint(event.toolTip, enchantIds);
        if (insertAt == -1) return;

        List<String> lines = buildMissingLines(missing);
        for (int i = lines.size() - 1; i >= 0; i--)
            event.toolTip.add(insertAt, lines.get(i));
    }

    private Set<String> getIgnoredFromPools(JsonObject enchantsConst, Set<String> enchantIds) {
        Set<String> ignored = new HashSet<>();
        try {
            JsonArray pools = enchantsConst.get("enchant_pools").getAsJsonArray();
            for (JsonElement poolEl : pools) {
                JsonArray pool = poolEl.getAsJsonArray();
                Set<String> poolSet = new HashSet<>();
                for (JsonElement e : pool) poolSet.add(e.getAsString());
                for (String id : poolSet) {
                    if (enchantIds.contains(id)) {
                        ignored.addAll(poolSet);
                        break;
                    }
                }
            }
        } catch (Exception ignored2) {
        }
        return ignored;
    }

    private JsonArray getAllEnchantsForItem(JsonObject enchantsConst, ItemStack stack) {
        try {
            JsonObject enchantsObj = enchantsConst.get("enchants").getAsJsonObject();
            List<String> lore = ItemUtils.getLoreLines(stack);

            String rarityLine = "";
            for (int i = lore.size() - 1; i >= 0; i--) {
                String clean = ColorUtils.stripColor(lore.get(i)).trim();
                if (!clean.isEmpty()) {
                    rarityLine = clean;
                    break;
                }
            }

            if (rarityLine.isEmpty()) return null;

            for (Map.Entry<String, JsonElement> entry : enchantsObj.entrySet()) {
                if (rarityLine.contains(entry.getKey())) {
                    return entry.getValue().getAsJsonArray();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int findInsertionPoint(List<String> tooltip, Set<String> enchantIds) {
        boolean gotToEnchants = false;
        for (int i = 0; i < tooltip.size(); i++) {
            String line = tooltip.get(i);
            boolean lineHasEnch = false;
            for (String id : enchantIds) {
                String name = toTitleCase(id.replace("_", " "));
                if (line.contains(name)) {
                    lineHasEnch = true;
                    break;
                }
            }
            if (lineHasEnch) {
                gotToEnchants = true;
            } else if (gotToEnchants && ColorUtils.stripColor(line).trim().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private List<String> buildMissingLines(List<String> missing) {
        List<String> result = new ArrayList<>();
        result.add("");
        StringBuilder current = new StringBuilder("§cMissing: §7");
        for (int i = 0; i < missing.size(); i++) {
            String name = toTitleCase(missing.get(i).replace("_", " "));
            String separator = (i < missing.size() - 1) ? ", " : "";
            if (current.length() > 10 && ColorUtils.stripColor(current.toString()).length() + name.length() > 40) {
                result.add(current.toString());
                current = new StringBuilder("§7" + name + separator);
            } else {
                current.append(name).append(separator);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }
}