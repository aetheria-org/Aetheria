package io.hamlook.aetheria.features.misc.itemList;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.ThreadUtils;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class WikiPane {

    // info[] index in the item JSON:
    //   0 = hypixelskyblock.minecraft.wiki (hysb.wiki)
    //   1 = wiki.hypixel.net (official)
    private static final int HYSB_WIKI    = 0;
    private static final int HYPIXEL_WIKI = 1;

    public static void open(SkyblockItem item) {
        if (item == null) return;

        SkyblockItem target = item;
        if (item.familyId != null) {
            ItemFamily fam = ItemRegistry.familyRegistry.get(item.familyId);
            if (fam != null && fam.representative() != null) {
                target = fam.representative();
            }
        }

        String url = resolveUrl(target);
        if (url == null || url.isEmpty()) return;

        final String finalUrl = url;
        ThreadUtils.run("WikiPane-Open", () -> {
            try {
                Desktop.getDesktop().browse(new URI(finalUrl));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static String resolveUrl(SkyblockItem item) {
        if (!"WIKI_URL".equals(item.infoType) || item.info == null || item.info.isEmpty()) {
            return null;
        }

        int source = ATHRConfig.feature != null
                ? ATHRConfig.feature.misc.itemList.wikiSource : 0;

        List<String> urls = item.info;

        // Pick the preferred URL by source setting
        // 0 = hysb.wiki (index 0), 1 = hypixel official (index 1)
        int preferred = (source == 1) ? HYPIXEL_WIKI : HYSB_WIKI;

        if (preferred < urls.size() && urls.get(preferred) != null && !urls.get(preferred).isEmpty()) {
            return urls.get(preferred);
        }
        // Fallback to whichever URL exists
        for (String url : urls) {
            if (url != null && !url.isEmpty()) return url;
        }
        return null;
    }
}
