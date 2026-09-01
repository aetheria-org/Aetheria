package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.data.ApiHandler;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.features.price.vars.PriceType;
import io.hamlook.aetheria.features.price.vars.recieve.PriceEntry;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.RomanNumeralParser;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMTooltipEvent;

@RegisterEvents
public class SkyblockTooltips {

    private static boolean isExcludedFromPrice(String id) {
        Set<String> ids = ApiHandler.getNoPriceIds();
        return ids != null && ids.contains(id.toLowerCase());
    }

    private int tickCounter = 0;

    @HandleEvent(priority = HandleEvent.HIGHEST)
    public void onTooltip(ASMTooltipEvent e) {
        if (e.toolTip == null || e.itemStack == null) return;
        if (ATHRConfig.feature == null) return;

        boolean doRoman = ATHRConfig.feature.qol.romanNumerals;
        boolean doSkyblock = ATHRConfig.feature.qol.showSkyblockId;
        boolean doPrice = ATHRConfig.feature.misc.itemPriceConfig.showPriceInLore;
        boolean doPriceWhenShift = ATHRConfig.feature.misc.itemPriceConfig.showPriceWhenShift;
        int priceShowKey = ATHRConfig.feature.misc.itemPriceConfig.showPriceKey;

        if (doRoman) {
            for (int i = 1; i < e.toolTip.size(); i++) {
                String replaced = RomanNumeralParser.replaceInString(e.toolTip.get(i));
                if (!replaced.equals(e.toolTip.get(i))) e.toolTip.set(i, replaced);
            }
        }

        if (doSkyblock) {
            String id = ItemUtils.getInternalName(e.itemStack);
            String eId = ItemUtils.getEffectiveItemId(e.itemStack);
            if (!id.isEmpty() && !eId.isEmpty()) {
                String line = EnumChatFormatting.DARK_GRAY + "skyblock:" + id + (!id.equals(eId) ? "(" + eId + ")" : "");
                if (!e.toolTip.contains(line)) e.toolTip.add(line);
            }
        }
        if (doPrice) {
            if (doPriceWhenShift && !KeybindHelper.isKeyDown(priceShowKey)) {
                if (ItemUtils.isSkyblockItem(e.itemStack)) {
                    String id = ItemUtils.getEffectiveItemId(e.itemStack);
                    if (id != null && !id.isEmpty() && isExcludedFromPrice(id)) return;
                    e.toolTip.add("§7" + KeybindHelper.getKeyName(priceShowKey) + " to view price data.");
                }
                return;
            }
            // Check if item has a valid Skyblock ID
            if (!ItemUtils.isSkyblockItem(e.itemStack)) {
                return;
            }
            String id = ItemUtils.getEffectiveItemId(e.itemStack);
            if (id == null || id.isEmpty()) {
                return;
            }
            if (isExcludedFromPrice(id)) return;
            PriceEntry entry = PriceMap.getPrice(id);
            if(entry == null) return;
            List<String> lines = new ArrayList<>();
            PriceType type = detectType(entry);
            if(type == PriceType.BIN){
                double avgBin = toDecimalDouble(entry.price.getOrDefault("avgBin",-1D));
                double avgAuction = toDecimalDouble(entry.price.getOrDefault("avgAuction",-1D));
                double highBin = toDecimalDouble(entry.price.getOrDefault("highestBin",-1D));
                double lowBin = toDecimalDouble(entry.price.getOrDefault("lowestBin",-1D));

                lines.add("§bAverage BIN: §6" + (avgBin > 0 ? formatPrice(avgBin) : "N/A"));
                lines.add("§bAverage Auction: §6" + (avgAuction > 0 ? formatPrice(avgAuction) : "N/A"));
                lines.add("§bHighest BIN: §6" + (highBin > 0 ? formatPrice(highBin) : "N/A"));
                lines.add("§bLowest BIN: §6" + (lowBin > 0 ? formatPrice(lowBin) : "N/A"));
            }
            else if(type == PriceType.BAZAAR || type == PriceType.BZ_WITH_OFFER){
                double buyPrice = toDecimalDouble(entry.price.getOrDefault("iBuy",-1D));
                double sellPrice = toDecimalDouble(entry.price.getOrDefault("iSell",-1D));
                double oBuyPrice = toDecimalDouble(entry.price.getOrDefault("oBuy",-1D));
                double oSellPrice = toDecimalDouble(entry.price.getOrDefault("oSell",-1D));

                lines.add("§bBazaar Insta-Buy: §6 " + (buyPrice > 0 ? formatPrice(buyPrice) : "N/A"));
                lines.add("§bBazaar Insta-Sell: §6 " + (sellPrice > 0 ? formatPrice(sellPrice) : "N/A"));
                lines.add("§bBazaar Buy-Order: §6 " + (oBuyPrice > 0 ? formatPrice(oBuyPrice) : "N/A"));
                lines.add("§bBazaar Sell-Order: §6 " + (oSellPrice > 0 ? formatPrice(oSellPrice) : "N/A"));
            }
            e.toolTip.addAll(lines);
        }
    }

    public static String formatPrice(double price){
        boolean shortNumberFormat = ATHRConfig.feature.misc.itemPriceConfig.priceShortNumberFormat;
        return shortNumberFormat ? Utils.shortNumberFormat(price,0) : String.format("%.2f",price);
    }
    public static double toDecimalDouble(double initial){
        if(initial == -1) return -1;
        long val = Math.round(initial*100.0);
        return val / 100D;
    }
    public static PriceType detectType(PriceEntry entry) {
        double type = entry.price.getOrDefault("priceType",-2D);
        if(type == -2D) return null;
        if(type == -1D) return PriceType.BIN;
        if(type == 0D) return PriceType.BAZAAR;
        if(type == 1D) return PriceType.BZ_WITH_OFFER;
        return null;
    }

    @HandleEvent
    public void onTick(ASMTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.romanNumerals) return;
        if (++tickCounter % 20 != 0) return;

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;
            Collection<NetworkPlayerInfo> infos = mc.thePlayer.sendQueue.getPlayerInfoMap();
            if (infos == null) return;
            for (NetworkPlayerInfo info : infos) {
                try {
                    if (info.getDisplayName() != null) {
                        String name = info.getDisplayName().getFormattedText();
                        String replaced = RomanNumeralParser.replaceInString(name);
                        if (!replaced.equals(name)) info.setDisplayName(new ChatComponentText(replaced));
                    } else if (info.getGameProfile() != null) {
                        String name = info.getGameProfile().getName();
                        String replaced = RomanNumeralParser.replaceInString(name);
                        if (!replaced.equals(name)) info.setDisplayName(new ChatComponentText(replaced));
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }
}