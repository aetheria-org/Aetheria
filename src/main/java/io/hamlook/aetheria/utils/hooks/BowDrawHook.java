package io.hamlook.aetheria.utils.hooks;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.item.ItemStack;

public class BowDrawHook {

    private static final String[] EXCLUDED_BOWS = {"TERMINATOR", "SHORTBOW", "MACHINE_GUN_BOW", "ITEM_SPIRIT_BOW"};

    public static boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.bugFixes.bowDrawFix;
    }

    public static boolean canKeepDrawing(ItemStack held) {
        String id = ItemUtils.getInternalName(held);
        if (id.isEmpty()) return false;
        for (String excluded : EXCLUDED_BOWS) {
            if (id.contains(excluded)) return false;
        }
        return true;
    }
}
