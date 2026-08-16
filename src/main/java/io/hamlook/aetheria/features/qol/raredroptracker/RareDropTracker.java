package io.hamlook.aetheria.features.qol.raredroptracker;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.qol.RareDropTrackerConfig;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.features.misc.itemlog.ItemPickupLog;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Rare Drop Tracker.
 * <p>
 * Watches the {@link ItemPickupLog} inventory-diff event for skyblockIDs the player has
 * added to their tracked list (see {@link RareDropTrackerCommand}, /rdt) and fires an
 * alert (chat message / title / sound) the moment one of them is obtained.
 */
@RegisterEvents
public class RareDropTracker {

    private final Set<String> firstTimeAlerted = new HashSet<>();
    private boolean registered = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!registered) {
            ItemPickupLog log = ItemPickupLog.getInstance();
            if (log != null) {
                log.addRichItemChangeListener(this::onItemChange);
                registered = true;
            }
        }
    }

    private void onItemChange(String internalId, String displayName, int delta) {
        if (delta <= 0 || internalId == null || internalId.isEmpty()) return;

        RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;
        if (!config.enabled) return;
        if (!SkyblockData.isOnSkyblock()) return;

        String id = internalId.toLowerCase();
        RareDropTrackerConfig.TrackedItem tracked = config.trackedItems.get(id);
        if (tracked == null) return;

        tracked.count += delta;
        ATHRConfig.markConfigDirty();

        if (config.alertMode == 1) {
            if (firstTimeAlerted.contains(id)) return;
            firstTimeAlerted.add(id);
        }

        String name = (displayName != null && !displayName.isEmpty()) ? displayName : prettyName(id);

        if (config.chatAlert) {
            String progress = tracked.goal > 0 ? " §7(" + tracked.count + "/" + tracked.goal + ")" : "";
            ChatUtils.sendMessage("§d§lRARE DROP! §r" + name + " §7x" + delta + progress);
        }

        if (config.titleAlert) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.ingameGUI != null) {
                mc.ingameGUI.displayTitle("§d§lRARE DROP!", name, 5, 40, 10);
            }
        }

        if (config.playSound) {
            SoundUtils.playSound("random.orb", 1.0f, 1.0f);
        }

        if (config.commandAlert && tracked.command != null && !tracked.command.trim().isEmpty()) {
            RareDropCommandTrigger.arm(tracked.command.trim());
        }
    }

    private String prettyName(String id) {
        SkyblockItem item = ItemRegistry.getItem(id);
        return item != null && item.displayName != null ? item.displayName : id;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        firstTimeAlerted.clear();
    }
}
