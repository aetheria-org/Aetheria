package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.RenderItemOverlayEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemStackUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class DungeonPassFloorTip {

    @SubscribeEvent
    public void onItemOverlay(RenderItemOverlayEvent event) {
        if (ATHRConfig.feature == null) return;
        if (!ATHRConfig.feature.misc.itemStackTips) return;
        if (!ContainerUtils.isInContainer("Catacombs Gate")) return;

        String id = ItemUtils.getInternalName(event.stack);
        String tip = getDungeonFloor(id);
        if (tip == null) return;
        ItemStackUtils.drawTip(tip, event.x, event.y);
    }

    private static String getDungeonFloor(String id) {
        String suffix = null;
        if (id.startsWith("MASTER_CATACOMBS_PASS_")) {
            suffix = id.substring("MASTER_CATACOMBS_PASS_".length());
        } else if (id.startsWith("CATACOMBS_PASS_")) {
            suffix = id.substring("CATACOMBS_PASS_".length());
        }
        if (suffix == null) return null;
        try {
            int floor = Integer.parseInt(suffix) - 3;
            return floor > 0 ? String.valueOf(floor) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
