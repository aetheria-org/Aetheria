package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.data.ApiHandler;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.features.price.vars.AuctionEntry;
import io.hamlook.aetheria.features.price.vars.BazaarEntry;
import io.hamlook.aetheria.features.price.vars.PriceType;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.RomanNumeralParser;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RegisterEvents
public class SkyblockTooltips {

    private static boolean isExcludedFromPrice(String id) {
        Set<String> ids = ApiHandler.getNoPriceIds();
        return ids != null && ids.contains(id.toLowerCase());
    }

    private int tickCounter = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTooltip(ItemTooltipEvent e) {
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
            if (!id.isEmpty()) {
                String line = EnumChatFormatting.DARK_GRAY + "skyblock:" + id;
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
            List<BazaarEntry> entry = PriceMap.getBZPrice(id, 1);
            List<String> lines = new ArrayList<>();
            if (entry == null || entry.isEmpty()) {

                List<AuctionEntry> ahEntry = PriceMap.getAHPrice(id, -1);
                if (ahEntry == null || ahEntry.isEmpty()) {
                    lines.add("§cThis Item does not have an updated price yet.");
                } else {
                    double lowestBin = -1;
                    double highestBin = -1;
                    double averageBin = -1;
                    double averageAH = -1;
                    int bins = 0;
                    for (AuctionEntry each : ahEntry) {
                        if (each.type == PriceType.BIN) {
                            if (each.price < lowestBin || lowestBin == -1) lowestBin = each.price;
                            if (each.price > highestBin) highestBin = each.price;
                            averageBin += each.price;
                            bins++;
                        }
                        if (each.type == PriceType.AUCTION) averageAH += each.price;
                    }
                    averageBin = averageBin / bins;
                    averageAH = averageAH / (ahEntry.size() - bins);

                    lines.add("§6§bLowest BIN: §r§a" + (lowestBin > 0 ? Utils.shortNumberFormat(lowestBin, 0) : "N/A") + " coins.");
                    lines.add("§6§bHighest BIN: §r§a" + (highestBin > 0 ? Utils.shortNumberFormat(highestBin, 0) : "N/A") + " coins.");
                    lines.add("§6§bAverage BIN: §r§a" + (averageBin > 0 ? Utils.shortNumberFormat(averageBin, 0) : "N/A") + " coins.");
                    lines.add("§6§bAverage AH: §r§a" + (averageAH > 0 ? Utils.shortNumberFormat(averageAH, 0) : "N/A") + " coins.");
                }
            } else {
                BazaarEntry price = entry.get(0);
                if (price != null) {
                    lines.add("§6§bBZ Insta-Buy: §r§a" + (price.iBuy >= 0 ? Utils.shortNumberFormat(price.iBuy, 0) : "N/A"));
                    lines.add("§6§bBZ Insta-Sell: §r§a" + (price.iSell >= 0 ? Utils.shortNumberFormat(price.iSell, 0) : "N/A"));
                    if (price.priceType == PriceType.BZ_WITH_OFFER) {
                        lines.add("§6§bBZ Buy Offer: §r§a" + (price.oBuy >= 0 ? Utils.shortNumberFormat(price.oBuy, 0) : "N/A"));
                        lines.add("§6§bBZ Sell Order: §r§a" + (price.oSell >= 0 ? Utils.shortNumberFormat(price.oSell, 0) : "N/A"));
                    }
                }
            }
            e.toolTip.addAll(lines);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
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