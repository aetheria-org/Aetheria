package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.VisitorsConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.*;

@RegisterEvents
public class VisitorShoppingListOverlay extends Overlay {

    private static final int MAX_ROWS = 12;
    @Getter
    private static VisitorShoppingListOverlay instance;

    public VisitorShoppingListOverlay() {
        super(120, 40);
        instance = this;
    }

    private static VisitorsConfig config() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.visitors;
    }

    private static LinkedHashMap<String, Integer> previewNeeds() {
        LinkedHashMap<String, Integer> needs = new LinkedHashMap<>();
        needs.put("ENCHANTED_CARROT", 64);
        needs.put("ENCHANTED_POTATO", 128);
        return needs;
    }

    @Override
    public List<String> getLines(boolean preview) {
        VisitorsConfig cfg = config();
        if (cfg == null) return Collections.emptyList();
        clearLineIcons();

        LinkedHashMap<String, Integer> needs = preview ? previewNeeds() : VisitorShoppingList.getMergedNeeds();
        if (needs.isEmpty()) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        double total = cfg.overlay.showPrices ? VisitorShoppingList.totalCost(needs) : 0;
        String timeSuffix = "";
        if (!preview && cfg.showTimeToFarm) {
            long timeMs = VisitorShoppingList.totalTimeToFarmMs(needs);
            if (timeMs > 0) timeSuffix = " §8· §b≈" + VisitorShoppingList.formatFarmTime(timeMs);
        }
        lines.add("§eVisitor Shopping List" + (cfg.overlay.showPrices && total > 0 ? " §7(§6" + VisitorShoppingList.formatPrice(total) + "§7)" : "") + timeSuffix);

        if (cfg.overlay.showProfit) {
            double rewards = VisitorShoppingList.totalRewardValue();
            if (rewards > 0) {
                lines.add("§6Rewards: §a" + VisitorShoppingList.formatPrice(rewards));
                double profit = cfg.overlay.showPrices ? VisitorShoppingList.adjustedProfit(needs) : 0;
                lines.add(profit >= 0 ? "§aProfit: §a+" + VisitorShoppingList.formatPrice(profit) : "§aProfit: §c-" + VisitorShoppingList.formatPrice(-profit));
            }
        }

        int count = 0;
        Map<String, Integer> have = preview ? Collections.emptyMap() : VisitorShoppingList.getHaveCounts();
        boolean showTime = !preview && cfg.showTimeToFarm;
        for (Map.Entry<String, Integer> entry : needs.entrySet()) {
            if (count == MAX_ROWS) {
                lines.add("§7...and " + (needs.size() - MAX_ROWS) + " more");
                break;
            }
            String row = formatRow(entry.getKey(), entry.getValue(), have, cfg.overlay.showPrices, cfg.showHaveCounts, showTime);
            putLineIcon(row, iconFor(entry.getKey()));
            lines.add(row);
            count++;
        }
        return lines;
    }

    private String formatRow(String itemId, int amount, Map<String, Integer> have, boolean showPrices, boolean showHave, boolean showTime) {
        StringBuilder sb = new StringBuilder("§7- §e").append(amount).append("x §f").append(VisitorShoppingList.itemNameOf(itemId));
        if (showHave) {
            int owned = have.getOrDefault(itemId, 0);
            sb.append(owned >= amount ? "§a" : "§e").append(" [").append(owned).append('/').append(amount).append(']');
        }
        if (showPrices) {
            double price = VisitorShoppingList.unitPrice(itemId) * amount;
            if (price > 0) sb.append(" §7(§6").append(VisitorShoppingList.formatPrice(price)).append("§7)");
        }
        if (showTime) {
            long ms = VisitorShoppingList.timeToFarmMs(itemId, amount);
            if (ms > 0) sb.append(" §7(§b≈").append(VisitorShoppingList.formatFarmTime(ms)).append("§7)");
        }
        return sb.toString();
    }

    private ItemStack iconFor(String itemId) {
        SkyblockItem item = ItemRegistry.getItem(itemId);
        return item != null ? item.getStack() : null;
    }

    @Override
    public Position getPosition() {
        VisitorsConfig cfg = config();
        return cfg != null ? cfg.overlay.overlayPos : new Position(20, 100, false, false);
    }

    @Override
    public float getScale() {
        VisitorsConfig cfg = config();
        return cfg != null ? cfg.overlay.scale : 1f;
    }

    @Override
    public int getBgColor() {
        VisitorsConfig cfg = config();
        return cfg != null ? cfg.overlay.bgColor : 0x80000000;
    }

    @Override
    public int getCornerRadius() {
        VisitorsConfig cfg = config();
        return cfg != null ? cfg.overlay.cornerRadius : 4;
    }

    @Override
    protected boolean isEnabled() {
        VisitorsConfig cfg = config();
        if (cfg == null || !cfg.enabled || !cfg.overlay.enabled) return false;
        if (VisitorShoppingList.hiddenAt(cfg.overlay.visible)) return false;
        if (cfg.overlay.hideWhileFarming && FarmingApi.isCurrentlyFarming()) return false;
        return !VisitorShoppingList.getMergedNeeds().isEmpty();
    }
}