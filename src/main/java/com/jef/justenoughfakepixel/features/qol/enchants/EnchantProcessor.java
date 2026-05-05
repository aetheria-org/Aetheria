package com.jef.justenoughfakepixel.features.qol.enchants;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.core.config.editors.ChromaColour;
import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.repo.JefRepo;
import com.jef.justenoughfakepixel.repo.RepoHandler;
import com.jef.justenoughfakepixel.utils.ColorUtils;
import com.jef.justenoughfakepixel.utils.RomanNumeralParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class EnchantProcessor {

    private static final Pattern ENCHANTMENT_PATTERN = Pattern.compile("(?<enchant>[A-Za-z][A-Za-z -]+) (?<levelNumeral>[IVXLCDM]+|\\d+)(?=, |$| [\\d,]+$)");
    private static final Pattern GREY_ENCHANT_PATTERN = Pattern.compile("^(Respiration|Aqua Affinity|Depth Strider|Efficiency).*");
    private static final String COMMA = ", ";
    private static final String GRAY_COMMA = "§7" + COMMA;

    // Layout mode constants
    private static final int LAYOUT_SINGLE_LINE = 1;
    private static final int LAYOUT_TWO_COLUMN = 0;

    private static final Map<String, EnchantMeta> BY_LORE = new HashMap<>();
    private static final Map<Integer, String> MC_COLOR_CACHE = new HashMap<>();
    private static final Cache LORE_CACHE = new Cache();
    private static String lastLoadedJson = null;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        if (event == null || event.itemStack == null || event.toolTip == null || JefConfig.feature == null) return;
        if (!JefConfig.feature.qol.enchantParser.enchantHighlight) return;

        ensureLoaded();
        if (BY_LORE.isEmpty()) return;

        NBTTagCompound enchantNBT = getEnchantNBT(event.itemStack);
        if (enchantNBT != null && enchantNBT.hasNoTags()) return;

        List<String> loreList = event.toolTip;
        if (LORE_CACHE.isCached(loreList)) {
            loreList.clear();
            loreList.addAll(LORE_CACHE.cachedAfter);
            return;
        }
        LORE_CACHE.updateBefore(loreList);
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;

        int startEnchant = -1, endEnchant = -1, maxTooltipWidth = 0;

        int indexOfLastGreyEnchant = accountForAndRemoveGreyEnchants(loreList, event.itemStack);
        for (int i = indexOfLastGreyEnchant == -1 ? 0 : indexOfLastGreyEnchant + 1; i < loreList.size(); i++) {
            String line = loreList.get(i);
            String stripped = ColorUtils.stripColor(line);
            if (startEnchant == -1) {
                if (containsEnchantment(enchantNBT, stripped)) startEnchant = i;
            } else if (stripped.trim().isEmpty() && endEnchant == -1) {
                endEnchant = i - 1;
            }
            if (startEnchant == -1 || endEnchant != -1) {
                maxTooltipWidth = Math.max(fontRenderer.getStringWidth(line), maxTooltipWidth);
            }
        }

        if (startEnchant == -1) {
            LORE_CACHE.updateAfter(loreList);
            return;
        }
        if (enchantNBT == null && endEnchant == -1) endEnchant = startEnchant;
        if (endEnchant == -1) {
            LORE_CACHE.updateAfter(loreList);
            return;
        }

        maxTooltipWidth = correctTooltipWidth(maxTooltipWidth);

        TreeSet<FormattedEnchant> orderedEnchants = new TreeSet<>();
        FormattedEnchant lastEnchant = null;
        boolean hasLore = false;

        for (int i = startEnchant; i <= endEnchant && i < loreList.size(); i++) {
            String unformattedLine = ColorUtils.stripColor(loreList.get(i));
            Matcher m = ENCHANTMENT_PATTERN.matcher(unformattedLine);
            boolean containsEnchant = false;
            while (m.find()) {
                String loreName = m.group("enchant").trim().toLowerCase(Locale.US);
                EnchantMeta meta = BY_LORE.get(loreName);
                if (meta == null) continue;
                int level = parseLevel(m.group("levelNumeral"));
                lastEnchant = new FormattedEnchant(meta, level);
                orderedEnchants.add(lastEnchant);
                containsEnchant = true;
            }
            if (!containsEnchant && lastEnchant != null) {
                lastEnchant.lore.add(loreList.get(i));
                hasLore = true;
            }
        }

        if (orderedEnchants.isEmpty()) {
            LORE_CACHE.updateAfter(loreList);
            return;
        }

        loreList.subList(startEnchant, endEnchant + 1).clear();
        List<String> insertEnchants = buildLayout(new ArrayList<>(orderedEnchants), hasLore, maxTooltipWidth);
        loreList.addAll(startEnchant, insertEnchants);
        LORE_CACHE.updateAfter(loreList);
    }

    private NBTTagCompound getEnchantNBT(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        NBTTagCompound extra = stack.getTagCompound().getCompoundTag("ExtraAttributes");
        return extra.hasKey("enchantments", 10) ? extra.getCompoundTag("enchantments") : null;
    }

    private boolean containsEnchantment(NBTTagCompound enchantNBT, String line) {
        Matcher m = ENCHANTMENT_PATTERN.matcher(line);
        while (m.find()) {
            EnchantMeta meta = BY_LORE.get(m.group("enchant").trim().toLowerCase(Locale.US));
            if (meta == null) continue;
            if (enchantNBT == null || enchantNBT.hasKey(meta.nbtName)) return true;
        }
        return false;
    }

    private void ensureLoaded() {
        String json = RepoHandler.getJson(JefRepo.KEY_ENCHANTS);
        if (json == null || json.equals(lastLoadedJson)) return;
        lastLoadedJson = json;
        BY_LORE.clear();
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            loadSection(root, "NORMAL", 2);
            loadSection(root, "ULTIMATE", 0);
            loadSection(root, "STACKING", 1);
        } catch (Exception ignored) {
        }
    }

    private void loadSection(JsonObject root, String section, int sortType) {
        if (!root.has(section)) return;
        JsonObject bucket = root.getAsJsonObject(section);
        for (Map.Entry<String, JsonElement> entry : bucket.entrySet()) {
            try {
                JsonObject obj = entry.getValue().getAsJsonObject();
                EnchantMeta meta = new EnchantMeta(obj.get("loreName").getAsString(), obj.get("nbtName").getAsString().toLowerCase(Locale.US), obj.get("goodLevel").getAsInt(), obj.get("maxLevel").getAsInt(), sortType);
                BY_LORE.put(meta.loreName.toLowerCase(Locale.US), meta);
            } catch (Exception ignored) {
            }
        }
    }

    private int parseLevel(String raw) {
        if (raw == null || raw.isEmpty()) return 1;
        if (raw.chars().allMatch(Character::isDigit)) return Integer.parseInt(raw);
        try {
            return RomanNumeralParser.parse(raw);
        } catch (Exception ignored) {
            return 1;
        }
    }

    private int accountForAndRemoveGreyEnchants(List<String> tooltip, ItemStack item) {
        if (item == null || item.getEnchantmentTagList() == null || item.getEnchantmentTagList().tagCount() == 0)
            return -1;

        int total = 0;
        for (int i = 1; total < 1 + item.getEnchantmentTagList().tagCount() && i < tooltip.size(); total++) {
            String line = tooltip.get(i);
            if (GREY_ENCHANT_PATTERN.matcher(line).matches()) {
                tooltip.remove(i);
            } else {
                i++;
            }
        }
        return -1;
    }

    private int correctTooltipWidth(int maxTooltipWidth) {
        final ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
        final int mouseX = Mouse.getX() * scaled.getScaledWidth() / Minecraft.getMinecraft().displayWidth;
        int tooltipX = mouseX + 12;
        if (tooltipX + maxTooltipWidth + 4 > scaled.getScaledWidth()) {
            tooltipX = mouseX - 16 - maxTooltipWidth;
            if (tooltipX < 4) {
                if (mouseX > scaled.getScaledWidth() / 2) maxTooltipWidth = mouseX - 12 - 8;
                else maxTooltipWidth = scaled.getScaledWidth() - 16 - mouseX;
            }
        }
        if (scaled.getScaledWidth() > 0 && maxTooltipWidth > scaled.getScaledWidth()) {
            maxTooltipWidth = scaled.getScaledWidth();
        }
        return maxTooltipWidth;
    }

    private List<String> buildLayout(List<FormattedEnchant> enchants, boolean hasLore, int maxWidth) {
        List<String> out = new ArrayList<>();
        int layout = JefConfig.feature.qol.enchantParser.enchantLayout;

        if (layout == LAYOUT_SINGLE_LINE && enchants.size() > 1) {
            FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
            int commaLength = fr.getStringWidth(GRAY_COMMA);
            int sum = 0;
            StringBuilder builder = new StringBuilder();
            for (FormattedEnchant enchant : enchants) {
                if (sum + enchant.renderLength() > maxWidth && builder.length() > 0) {
                    removeTrailingComma(builder);
                    out.add(builder.toString());
                    builder = new StringBuilder();
                    sum = 0;
                }
                builder.append(enchant.formatted()).append(GRAY_COMMA);
                sum += enchant.renderLength() + commaLength;
            }
            if (builder.length() >= GRAY_COMMA.length()) {
                removeTrailingComma(builder);
                out.add(builder.toString());
            }
            return out;
        }

        if (layout == LAYOUT_TWO_COLUMN && !hasLore) {
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (FormattedEnchant enchant : enchants) {
                builder.append(enchant.formatted());
                if (i % 2 == 0) {
                    builder.append(GRAY_COMMA);
                } else {
                    out.add(builder.toString());
                    builder = new StringBuilder();
                }
                i++;
            }
            if (builder.length() >= GRAY_COMMA.length()) {
                removeTrailingComma(builder);
                out.add(builder.toString());
            }
            return out;
        }

        for (FormattedEnchant enchant : enchants) {
            out.add(enchant.formatted());
            out.addAll(enchant.lore);
        }
        return out;
    }

    private void removeTrailingComma(StringBuilder builder) {
        builder.delete(builder.length() - GRAY_COMMA.length(), builder.length());
    }


    private String formatColor(EnchantMeta meta, int level) {
        String color;
        if (meta.sortType == 0) color = JefConfig.feature.qol.enchantParser.enchantUltimateColor;
        else if (level >= meta.maxLevel) color = JefConfig.feature.qol.enchantParser.enchantPerfectColor;
        else if (level > meta.goodLevel) color = JefConfig.feature.qol.enchantParser.enchantGreatColor;
        else if (level == meta.goodLevel) color = JefConfig.feature.qol.enchantParser.enchantGoodColor;
        else color = JefConfig.feature.qol.enchantParser.enchantPoorColor;

        int argb = ChromaColour.specialToSimpleRGB(color);
        String prefix = nearestMcColor(argb);
        if (JefConfig.feature.qol.enchantParser.enchantChroma && ChromaColour.getSpeed(color) > 0) prefix += "§z";
        if (meta.sortType == 0) prefix += "§l";
        return prefix;
    }

    private String nearestMcColor(int argb) {
        // Cache lookup - avoid expensive distance calculations
        if (MC_COLOR_CACHE.containsKey(argb)) {
            return MC_COLOR_CACHE.get(argb);
        }

        int bestIndex = findNearestColorIndex(argb);
        String[] codes = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        String result = "§" + codes[bestIndex];
        MC_COLOR_CACHE.put(argb, result);
        return result;
    }

    private int findNearestColorIndex(int argb) {
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        int[] values = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            int cr = (values[i] >> 16) & 255;
            int cg = (values[i] >> 8) & 255;
            int cb = values[i] & 255;
            long dist = sq(r - cr) + sq(g - cg) + sq(b - cb);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private long sq(long x) {
        return x * x;
    }

    private String toRoman(int number) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        int n = Math.max(1, number);
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                n -= values[i];
                out.append(numerals[i]);
            }
        }
        return out.toString();
    }

    private static class EnchantMeta {
        private final String loreName;
        private final String nbtName;
        private final int goodLevel;
        private final int maxLevel;
        private final int sortType;

        private EnchantMeta(String loreName, String nbtName, int goodLevel, int maxLevel, int sortType) {
            this.loreName = loreName;
            this.nbtName = nbtName;
            this.goodLevel = goodLevel;
            this.maxLevel = maxLevel;
            this.sortType = sortType;
        }
    }

    private static class Cache {
        private List<String> cachedBefore = new ArrayList<>();
        private List<String> cachedAfter = new ArrayList<>();

        private void updateBefore(List<String> loreBefore) {
            cachedBefore = new ArrayList<>(loreBefore);
        }

        private void updateAfter(List<String> loreAfter) {
            cachedAfter = new ArrayList<>(loreAfter);
        }

        private boolean isCached(List<String> loreBefore) {
            if (loreBefore.size() != cachedBefore.size()) return false;
            for (int i = 0; i < loreBefore.size(); i++) {
                if (!Objects.equals(loreBefore.get(i), cachedBefore.get(i))) return false;
            }
            return true;
        }
    }

    private class FormattedEnchant implements Comparable<FormattedEnchant> {
        private final EnchantMeta meta;
        private final int level;
        private final List<String> lore = new ArrayList<>();
        private String cachedFormatted = null;
        private int cachedRenderLength = -1;

        private FormattedEnchant(EnchantMeta meta, int level) {
            this.meta = meta;
            this.level = Math.max(1, level);
        }

        private String formatted() {
            if (cachedFormatted == null) {
                String lvl = JefConfig.feature.qol.romanNumerals ? Integer.toString(level) : toRoman(level);
                cachedFormatted = formatColor(meta, level) + meta.loreName + " " + lvl;
            }
            return cachedFormatted;
        }

        private int renderLength() {
            if (cachedRenderLength == -1) {
                cachedRenderLength = Minecraft.getMinecraft().fontRendererObj.getStringWidth(formatted());
            }
            return cachedRenderLength;
        }

        @Override
        public int compareTo(FormattedEnchant o) {
            if (this.meta.sortType != o.meta.sortType) return Integer.compare(this.meta.sortType, o.meta.sortType);
            return this.meta.loreName.compareTo(o.meta.loreName);
        }
    }
}