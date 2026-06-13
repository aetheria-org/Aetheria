package io.hamlook.aetheria.features.dungeons.reward;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.item.ItemStack;

import lombok.Getter;

@Getter
public final class DungeonReward {

    private final ItemStack item;
    private final double price;
    private final double singlePrice;

    public DungeonReward(ItemStack item, double price) {
        double price1;
        singlePrice = price;
        this.item = item;
        price1 = price;
        if(ItemUtils.getInternalName(item).startsWith("essence")){
            String displayName = ColorUtils.stripColor(item.getDisplayName());
            Aetheria.logger.info("Checking Amount for " + displayName);
            if(displayName.contains("x")){
                int index = displayName.lastIndexOf("x");
                String amount = displayName.substring(index + 1);
                Aetheria.logger.info("Checking Amount in " + amount);
                int iA = 0;
                try{
                    iA = Integer.parseInt(amount);
                } catch (NumberFormatException e) {
                    Aetheria.logger.info("ERROR converting " + amount + " to numbers.");
                }
                if(iA > 0) price1 *= iA;
            }
        }
        this.price = price1;
    }
    public String getText() {
        return item == null ? "" : item.getDisplayName() + " §7: " +
                                   getPriceText();
    }

    private String getPriceText() {
        if(price <= 0) return "§cCould not determine price";
        if(singlePrice == price)  return "§6" + Utils.shortNumberFormat(price,0) + " Coins.";
        else return "§6" + Utils.shortNumberFormat(price,0) + " Coins." + "§7(§6" + Utils.shortNumberFormat(price,0) + " §7)";
    }
}
