package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.mixins.accessors.GuiEditSignAccessor;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;

public final class VisitorSignFill {

    private VisitorSignFill() {
    }

    public static void onSignOpened(GuiEditSign gui) {
        if (ATHRConfig.feature == null) return;
        if (ATHRConfig.feature.farming.visitors.signFillMode != 0) return;
        if (VisitorShoppingList.isSearchExpired()) {
            VisitorShoppingList.noteParse("[signfill] declined: search expired");
            return;
        }

        TileEntitySign sign = ((GuiEditSignAccessor) gui).ATHR$getTileSign();
        if (!VisitorShoppingList.isBazaarAmountSign(sign)) {
            VisitorShoppingList.noteParse("[signfill] declined: not a bazaar amount sign");
            return;
        }

        if (sign.signText == null || sign.signText.length == 0) return;
        if (!sign.signText[0].getUnformattedText().isEmpty()) {
            VisitorShoppingList.noteParse("[signfill] declined: sign text occupied");
            return;
        }
        String signature = FarmingApi.getLastChestSignature();
        if (!VisitorShoppingList.nameMatchesFlow(signature)) {
            VisitorShoppingList.noteParse("[signfill] declined: no flow match, signature='" + signature + "'");
            return;
        }

        int listedTotal = FarmingApi.consumePendingSign();
        if (listedTotal <= 0) return;

        int missing = VisitorShoppingList.missingAmount(FarmingApi.getSearchedItemId(), listedTotal);
        if (missing <= 0) {
            ChatUtils.sendMessage("§e[ASM] §7You already have enough §f" + FarmingApi.getSearchedItemName() + "§7.");
            return;
        }

        sign.signText[0] = new ChatComponentText(String.valueOf(missing));
        VisitorShoppingList.noteParse("[signfill] filled " + missing + "x " + FarmingApi.getSearchedItemName());
        VisitorShoppingList.scheduleSignSubmit(gui);
    }
}