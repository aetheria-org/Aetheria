package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.item.ItemStack;

@RegisterEvents
public class PlayerInventoryCache {

    private static final PlayerInventoryCache INSTANCE = new PlayerInventoryCache();
    private static final ItemStack[] EMPTY = new ItemStack[0];

    private static int tickCounter;
    private static int cachedTick = -1;
    private static ItemStack[] cachedMain = EMPTY;

    private PlayerInventoryCache() {
    }

    static boolean isItemInHotbar(String internalId) {
        if (internalId == null || internalId.isEmpty()) return false;
        ItemStack[] inventory = mainInventory();
        for (int i = 0; i < 9 && i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack == null) continue;
            if (internalId.equals(ItemUtils.getInternalName(stack))) return true;
        }
        return false;
    }

    private static ItemStack[] mainInventory() {
        if (MinecraftCompat.getLocalPlayer() == null) {
            reset();
            return EMPTY;
        }
        if (cachedTick == tickCounter) return cachedMain;
        cachedTick = tickCounter;
        cachedMain = InventoryCompat.getMainInventory();
        return cachedMain;
    }

    private static void reset() {
        cachedTick = -1;
        cachedMain = EMPTY;
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickPhase.START) return;
        tickCounter++;
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        reset();
    }
}
