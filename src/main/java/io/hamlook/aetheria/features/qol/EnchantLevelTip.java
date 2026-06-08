package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.RenderItemOverlayEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.RomanNumeralParser;
import io.hamlook.aetheria.utils.item.ItemStackUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class EnchantLevelTip {

    @SubscribeEvent
    public void onItemOverlay(RenderItemOverlayEvent event) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.misc.itemStackTips) return;

        String id = ItemUtils.getInternalName(event.stack);
        if (!"ENCHANTED_BOOK".equals(id)) return;

        String tip = getEnchantLevel(event.stack);
        if (tip == null) return;
        ItemStackUtils.drawTip(tip, event.x, event.y);
    }

    private static String getEnchantLevel(ItemStack stack) {
        String name = ColorUtils.stripColor(stack.getDisplayName());
        if (name.isEmpty()) return null;
        String[] parts = name.split(" ");
        String last = parts[parts.length - 1];
        if (last.isEmpty() || !last.chars().allMatch(c -> "IVXLCDM".indexOf(c) >= 0)) return null;
        if (!RomanNumeralParser.isValid(last)) return null;
        int level = RomanNumeralParser.parse(last);
        return level > 0 ? String.valueOf(level) : null;
    }
}
