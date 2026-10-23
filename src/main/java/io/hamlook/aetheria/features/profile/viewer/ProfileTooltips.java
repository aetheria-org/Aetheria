package io.hamlook.aetheria.features.profile.viewer;

import io.hamlook.aetheria.features.misc.itemList.ItemResolver;
import io.hamlook.aetheria.features.profile.data.ItemData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProfileTooltips {

    private ProfileTooltips() {
    }

    public static List<String> itemLines(ItemData data) {
        if (data == null) return Collections.emptyList();
        return itemLines(data, ItemResolver.resolveId(data.skyblockID, data.displayName));
    }

    public static List<String> itemLines(ItemData data, String registryId) {
        if (data == null) return Collections.emptyList();
        int loreCount = data.lore == null ? 0 : data.lore.size();
        List<String> lines = new ArrayList<>(4 + loreCount);
        lines.add(data.displayName);
        if (data.lore != null) lines.addAll(data.lore);
        lines.add("");
        lines.add("§8Item SB_ID: §7" + data.skyblockID);
        lines.add("§8ItemRegistry SB_ID: §7" + registryId);
        return lines;
    }
}
