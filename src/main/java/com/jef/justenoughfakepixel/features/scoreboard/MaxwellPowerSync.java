package com.jef.justenoughfakepixel.features.scoreboard;

import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;
import com.jef.justenoughfakepixel.init.RegisterInstance;
import com.jef.justenoughfakepixel.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;

public class MaxwellPowerSync implements JefStorageManager.Managed, JefStorageManager.AutoSaveable {

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
        PowerData loaded = JefStorageManager.loadSafe(file, PowerData.class, JefGsonBuilder.GSON);
        if (loaded != null) data = loaded;
    }

    private void save() {
        JefStorageManager.saveAtomic(file, data, JefGsonBuilder.GSON);
    }

    @Override
    public void autoSave() {
        save();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) mc.currentScreen;
        IInventory inv = ((ContainerChest) chest.inventorySlots).getLowerChestInventory();

        String title = ColorUtils.stripColor(inv.getDisplayName().getUnformattedText());
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
            NBTTagList lore = item.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) {
                if (ColorUtils.stripColor(lore.getStringTagAt(i)).trim().equals("Power is selected!")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static class PowerData {
        String power = null;
    }
}