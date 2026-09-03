package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.VisitorsConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VisitorTooltips {

    private static final Pattern NAME_X_AMOUNT = Pattern.compile("^(.+?) x(\\d+)$");
    private static final Pattern AMOUNT_X_NAME = Pattern.compile("^(\\d+)x (.+)$");

    private VisitorTooltips() {
    }

    /** Lines to draw instead of the vanilla tooltip, or null to keep vanilla */
    public static List<String> replaceToolTip(ItemStack stack) {
        if (stack == null || ATHRConfig.feature == null) return null;
        VisitorsConfig cfg = ATHRConfig.feature.farming.visitors;
        if (cfg == null || !cfg.enabled || !cfg.customTooltip) return null;
        if (!"Accept Offer".equals(ColorUtils.stripColor(stack.getDisplayName()).trim())) return null;

        String visitor = openVisitorName();
        if (visitor == null) return null;

        VisitorOffer offer = VisitorOfferParser.parse(
                ItemUtils.getLoreLinesWithoutColor(stack), VisitorShoppingList::resolveItemId);

        return buildLines(cfg, offer, stack);
    }

    private static String openVisitorName() {
        GuiScreen screen = MinecraftCompat.getMinecraft().currentScreen;
        if (!(screen instanceof GuiContainer)) return null;
        if (!(InventoryCompat.getContainer((GuiContainer) screen) instanceof ContainerChest)) return null;
        ContainerChest chest = (ContainerChest) InventoryCompat.getContainer((GuiContainer) screen);
        String title = ContainerUtils.getTitle(chest);
        return FarmingApi.getVisitorNeeds().containsKey(title) ? title : null;
    }

    private static List<String> buildLines(VisitorsConfig cfg, VisitorOffer offer, ItemStack stack) {
        Map<String, Integer> have = VisitorShoppingList.getHaveCounts();
        double wantsCost = VisitorShoppingList.totalCost(offer.needs);
        Integer copper = offer.copperAmount();

        List<String> out = new ArrayList<>();
        boolean inNeeds = false;
        boolean sawRewardsHeader = false;
        for (String formatted : ItemUtils.getLoreLines(stack)) {
            String stripped = VisitorOfferParser.strip(formatted).trim();
            if (stripped.equals("Items Required:")) inNeeds = true;
            if (stripped.equals("Rewards")) {
                inNeeds = false;
                sawRewardsHeader = true;
            }

            String line = formatted;
            if (inNeeds && !stripped.isEmpty()) {
                Integer needQty = needQuantity(stripped, offer);
                if (needQty != null) {
                    line = annotateNeedLine(line, have, cfg, needQty);
                }
            }
            if (copper != null && copper > 0 && stripped.startsWith("+") && stripped.endsWith("Copper")
                    && cfg.copperPriceDisplay && wantsCost > 0) {
                line += copperSuffix(cfg, copper, wantsCost);
            }
            out.add(line);
        }

        if (sawRewardsHeader && cfg.panel.showProfit && cfg.panel.showPrices) {
            double rewardsValue = VisitorShoppingList.totalCost(offer.rewards);
            double missingCost = 0;
            for (Map.Entry<String, Integer> entry : offer.needs.entrySet()) {
                int missing = Math.max(0, entry.getValue() - have.getOrDefault(entry.getKey(), 0));
                missingCost += VisitorShoppingList.unitPrice(entry.getKey()) * missing;
            }
            double net = rewardsValue - missingCost;
            int insertAt = out.size();
            while (insertAt > 0 && VisitorOfferParser.strip(out.get(insertAt - 1)).trim().isEmpty()) insertAt--;
            out.add(insertAt, "");
            out.add(insertAt + 1, "§6Net: " + (net >= 0 ? "§a+" : "§c-")
                    + VisitorShoppingList.formatPrice(Math.abs(net)));
        }
        return out;
    }

    private static Integer needQuantity(String stripped, VisitorOffer offer) {
        String name;
        Matcher m = NAME_X_AMOUNT.matcher(stripped);
        int qty;
        if (m.matches()) {
            name = m.group(1).trim();
            qty = Integer.parseInt(m.group(2));
        } else {
            m = AMOUNT_X_NAME.matcher(stripped);
            if (!m.matches()) return null;
            name = m.group(2).trim();
            qty = Integer.parseInt(m.group(1));
        }
        String id = VisitorShoppingList.resolveItemId(name);
        Integer recorded = id == null ? null : offer.needs.get(id);
        return recorded != null && recorded.equals(qty) ? recorded : null;
    }

    private static String annotateNeedLine(String formatted, Map<String, Integer> have,
                                           VisitorsConfig cfg, int needQty) {
        String id = needItemId(formatted);
        if (id == null) return formatted;
        StringBuilder sb = new StringBuilder(formatted);
        if (cfg.showHaveCounts) {
            int owned = have.getOrDefault(id, 0);
            sb.append(" §7[").append(owned >= needQty ? "§a" : "§e").append(owned)
                    .append("§7/").append(needQty).append(']');
        }
        if (cfg.panel.showPrices) {
            double price = VisitorShoppingList.unitPrice(id) * needQty;
            if (price > 0) sb.append(" §7(§6").append(VisitorShoppingList.formatPrice(price)).append("§7)");
        }
        return sb.toString();
    }

    private static String needItemId(String formatted) {
        String stripped = VisitorOfferParser.strip(formatted).trim();
        String name = null;
        Matcher m = NAME_X_AMOUNT.matcher(stripped);
        if (m.matches()) name = m.group(1).trim();
        else {
            m = AMOUNT_X_NAME.matcher(stripped);
            if (m.matches()) name = m.group(2).trim();
        }
        return name == null ? null : VisitorShoppingList.resolveItemId(name);
    }

    private static String copperSuffix(VisitorsConfig cfg, int copper, double wantsCost) {
        double per = wantsCost / copper;
        boolean goodDeal = per <= cfg.copperThreshold;
        String color = goodDeal ? "§a" : "§c";
        return " §7(paying " + color + VisitorShoppingList.formatPrice(per)
                + "§7/c" + (goodDeal ? "" : " §8> " + VisitorShoppingList.formatPrice(cfg.copperThreshold)) + ")";
    }
}
