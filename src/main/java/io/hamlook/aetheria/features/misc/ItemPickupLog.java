package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.function.BiConsumer;

@RegisterEvents
public class ItemPickupLog extends Overlay {

    public static final long LIFESPAN_MS = 5_000L;
    public static final int OVERLAY_WIDTH = 140;
    public static final int OVERLAY_HEIGHT = 80;
    private static final int MAX_LINES = 15;
    private static final int IGNORED_HOTBAR_SLOT = 8;
    @Getter
    private static ItemPickupLog instance;
    private final LinkedList<LogEntry> log = new LinkedList<>();
    private final List<BiConsumer<String, Integer>> itemChangeListeners = new ArrayList<>();
    private final List<RichItemChangeListener> richItemChangeListeners = new ArrayList<>();
    private ItemStack[] preScreenSnapshot = null;
    private ItemStack[] previousInventory = null;

    public ItemPickupLog() {
        super(OVERLAY_WIDTH, OVERLAY_HEIGHT);
        instance = this;
    }

    private static void accumulate(Map<String, Integer> map, ItemStack stack) {
        if (stack == null) return;
        map.merge(stack.getDisplayName(), stack.stackSize, Integer::sum);
    }

    private static ItemStack[] copyInventory(ItemStack[] src) {
        ItemStack[] copy = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i] != null ? ItemStack.copyItemStack(src[i]) : null;
        }
        return copy;
    }

    public void addItemChangeListener(BiConsumer<String, Integer> listener) {
        itemChangeListeners.add(listener);
    }

    @FunctionalInterface
    public interface RichItemChangeListener {
        void onItemChange(String internalId, String displayName, int delta);
    }

    public void addRichItemChangeListener(RichItemChangeListener listener) {
        richItemChangeListeners.add(listener);
    }

    private boolean inventoryChanged(ItemStack[] prev, ItemStack[] curr) {
        for (int i = 0; i < prev.length; i++) {
            if (i == IGNORED_HOTBAR_SLOT) continue;
            ItemStack a = prev[i], b = curr[i];
            if (a == null && b == null) continue;
            if (a == null || b == null) return true;
            if (a.stackSize != b.stackSize) return true;
            if (!a.getDisplayName().equals(b.getDisplayName())) return true;
        }
        return false;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.misc.itemPickupLogConfig.itemPickupLogPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.misc.itemPickupLogConfig.itemPickupLogScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.misc.itemPickupLogConfig.itemPickupLogBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.misc.itemPickupLogConfig.itemPickupLogCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.misc.itemPickupLogConfig.itemPickupLog;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!SkyblockData.isOnSkyblock()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        if (event.gui == null) {
            if (preScreenSnapshot != null) {
                ItemStack[] current = mc.thePlayer.inventory.mainInventory;
                if (preScreenSnapshot.length == current.length) {
                    diffAndLog(preScreenSnapshot, current);
                }
                preScreenSnapshot = null;
            }
            previousInventory = copyInventory(mc.thePlayer.inventory.mainInventory);
        } else {
            preScreenSnapshot = copyInventory(mc.thePlayer.inventory.mainInventory);
            previousInventory = null;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !SkyblockData.isOnSkyblock()) {
            previousInventory = null;
            return;
        }

        log.removeIf(LogEntry::isExpired);

        if (mc.currentScreen != null) return;

        ItemStack[] current = mc.thePlayer.inventory.mainInventory;

        if (previousInventory != null
                && previousInventory.length == current.length
                && inventoryChanged(previousInventory, current)) {
            diffAndLog(previousInventory, current);
        }

        previousInventory = copyInventory(current);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        resetSnapshot();
    }

    private static void accumulateInternal(Map<String, Integer> map, Map<String, String> internalToDisplay, ItemStack stack) {
        if (stack == null) return;
        String id = ItemUtils.getInternalName(stack);
        if (id.isEmpty()) return;
        map.merge(id, stack.stackSize, Integer::sum);
        internalToDisplay.putIfAbsent(id, stack.getDisplayName());
    }

    private void diffAndLog(ItemStack[] prev, ItemStack[] curr) {
        Map<String, Integer> prevMap = new HashMap<>();
        Map<String, Integer> currMap = new HashMap<>();
        Map<String, Integer> prevInternalMap = new HashMap<>();
        Map<String, Integer> currInternalMap = new HashMap<>();
        Map<String, String> internalToDisplay = new HashMap<>();

        for (int i = 0; i < prev.length; i++) {
            if (i == IGNORED_HOTBAR_SLOT) continue;
            accumulate(prevMap, prev[i]);
            accumulate(currMap, curr[i]);
            accumulateInternal(prevInternalMap, internalToDisplay, prev[i]);
            accumulateInternal(currInternalMap, internalToDisplay, curr[i]);
        }

        Set<String> allKeys = new HashSet<>(prevMap.keySet());
        allKeys.addAll(currMap.keySet());

        for (String name : allKeys) {
            int delta = currMap.getOrDefault(name, 0) - prevMap.getOrDefault(name, 0);
            if (delta == 0) continue;

            for (BiConsumer<String, Integer> listener : itemChangeListeners) {
                listener.accept(name, delta);
            }

            addOrMerge(name, delta);
        }

        if (!richItemChangeListeners.isEmpty()) {
            Set<String> allInternal = new HashSet<>(prevInternalMap.keySet());
            allInternal.addAll(currInternalMap.keySet());

            for (String id : allInternal) {
                int delta = currInternalMap.getOrDefault(id, 0) - prevInternalMap.getOrDefault(id, 0);
                if (delta == 0) continue;
                String displayName = internalToDisplay.getOrDefault(id, "");
                for (RichItemChangeListener listener : richItemChangeListeners) {
                    listener.onItemChange(id, displayName, delta);
                }
            }
        }
    }

    private void addOrMerge(String name, int delta) {
        for (LogEntry entry : log) {
            if (entry.displayName.equals(name)) {
                boolean sameSign = (delta > 0 && entry.amount > 0) || (delta < 0 && entry.amount < 0);
                if (sameSign) {
                    entry.add(delta);
                    return;
                }
            }
        }
        log.addLast(new LogEntry(name, delta));
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();

        if (preview) {
            lines.add("§a+ 1x §fWarped Aspect of the Void");
            lines.add("§c- 32x §fEnchanted Cobblestone");
            lines.add("§a+ 5x §fMithril Ore");
            lines.add("§a+ 64x §fEnchanted Oak Wood");
            lines.add("§c- 1x §fBoat");
            return lines;
        }

        int start = Math.max(0, log.size() - MAX_LINES);
        List<LogEntry> visible = new ArrayList<>(log).subList(start, log.size());
        for (int i = visible.size() - 1; i >= 0; i--) {
            LogEntry e = visible.get(i);
            String sign = e.amount > 0 ? "§a+" : "§c-";
            lines.add(sign + " " + Math.abs(e.amount) + "x §r" + e.displayName);
        }
        return lines;
    }

    public void resetSnapshot() {
        previousInventory = null;
        preScreenSnapshot = null;
        log.clear();
    }

    private static class LogEntry {
        final String displayName;
        int amount;
        long timestamp;

        LogEntry(String displayName, int amount) {
            this.displayName = displayName;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }

        void add(int delta) {
            this.amount += delta;
            this.timestamp = (this.amount == 0) ? System.currentTimeMillis() - LIFESPAN_MS : System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > LIFESPAN_MS;
        }
    }
}