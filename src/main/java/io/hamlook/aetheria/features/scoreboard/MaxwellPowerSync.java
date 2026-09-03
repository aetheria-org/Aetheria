package io.hamlook.aetheria.features.scoreboard;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterInstance;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;

public class MaxwellPowerSync implements StorageManager.Managed, StorageManager.AutoSaveable {

    @RegisterInstance
    private static MaxwellPowerSync INSTANCE;
    private File file = null;
    private PowerData data = new PowerData();

    private MaxwellPowerSync() {
    }

    public static MaxwellPowerSync getInstance() {
        if (INSTANCE == null) INSTANCE = new MaxwellPowerSync();
        return INSTANCE;
    }

    public static String getPower() {
        if (INSTANCE == null) return null;
        return INSTANCE.data.power;
    }

    @Override
    public void initFile(File configDir) {
        this.file = new File(configDir, "maxwell_power.json");
    }

    @Override
    public void load() {
        PowerData loaded = StorageManager.loadSafe(file, PowerData.class, GsonBuilder.GSON);
        if (loaded != null) data = loaded;
    }

    private void save() {
        StorageManager.saveAtomic(file, data, GsonBuilder.GSON);
    }

    @Override
    public void autoSave() {
        save();
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!ContainerUtils.isChestOpen()) return;

        IInventory inv = ContainerUtils.getLowerInventory();
        if (inv == null) return;

        String title = ColorUtils.stripColor(TextCompat.getUnformattedText(inv.getDisplayName()));
        if (!title.contains("Accessory Bag Thaumaturgy")) return;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack item = inv.getStackInSlot(i);
            if (item == null || !item.hasTagCompound()) continue;

            if (hasSelectedLine(item)) {
                String name = ColorUtils.stripColor(item.getDisplayName()).trim();
                if (!name.isEmpty() && !name.equals(data.power)) {
                    data.power = name;
                    save();
                }
                return;
            }
        }
    }

    private boolean hasSelectedLine(ItemStack item) {
        try {
            String line = ItemUtils.getLoreLine(item, "Power is selected!");
            return line != null;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static class PowerData {
        String power = null;
    }
}