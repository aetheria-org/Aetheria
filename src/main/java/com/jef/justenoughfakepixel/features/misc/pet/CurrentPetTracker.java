package com.jef.justenoughfakepixel.features.misc.pet;

import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;
import com.jef.justenoughfakepixel.init.RegisterInstance;
import com.jef.justenoughfakepixel.utils.item.ItemUtils;
import com.jef.justenoughfakepixel.utils.chat.ChatUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jef.justenoughfakepixel.features.misc.pet.PetCache.normalizePetName;

public class CurrentPetTracker implements JefStorageManager.Managed {

    private static final Pattern SUMMONED = Pattern.compile("^You summoned your (.+)!$");
    private static final Pattern AUTOPET = Pattern.compile("^Autopet equipped your \\[Lvl \\d+\\] (.+)!$");
    private static final Pattern LEVEL_PREFIX = Pattern.compile("^\\[Lvl \\d+\\] ");

    private static final String PETS_CONTAINER = "Pets";
    private static final String ACTIVE_LORE = "Click to despawn";

    @RegisterInstance
    private static CurrentPetTracker INSTANCE;
    private File file;
    @Getter
    private String currentBaseName = "";

    private CurrentPetTracker() {
    }

    public static CurrentPetTracker getInstance() {
        if (INSTANCE == null) INSTANCE = new CurrentPetTracker();
        return INSTANCE;
    }

    @Override
    public void initFile(File configDir) {
        file = new File(configDir, "current_pet.json");
    }

    @Override
    public void load() {
        String loaded = JefStorageManager.loadSafe(file, String.class, JefGsonBuilder.GSON);
        if (loaded != null) currentBaseName = loaded;
    }

    private void save() {
        JefStorageManager.saveAtomic(file, currentBaseName, JefGsonBuilder.GSON);
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.gui instanceof GuiChest)) return;
        if (!(((GuiChest) event.gui).inventorySlots instanceof ContainerChest)) return;

        ContainerChest container = (ContainerChest) ((GuiChest) event.gui).inventorySlots;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!title.startsWith(PETS_CONTAINER)) return;

        scanContainer(container);
    }

    private void scanContainer(ContainerChest container) {
        PetCache cache = PetCache.getInstance();

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            ItemStack item = slot.getStack();
            if (item == null || item.getItem() == null) continue;

            String texture = ItemUtils.getSkullTexture(item);
            if (texture == null || texture.isEmpty()) continue;

            String formatted = item.getDisplayName();
            formatted = formatted.replace("Â§", "§");
            String base = LEVEL_PREFIX.matcher(StringUtils.stripControlCodes(formatted)).replaceFirst("").trim();

            base = normalizePetName(base);
            if (base.isEmpty()) continue;

            cache.update(base, formatted, texture);

            if (ItemUtils.getLoreLine(item, ACTIVE_LORE) != null && !base.equals(currentBaseName)) {
                currentBaseName = base;
                save();
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (ChatUtils.isFromServer(event)) return;

        String raw = StringUtils.stripControlCodes(event.message.getUnformattedText()).trim();

        Matcher m = SUMMONED.matcher(raw);
        if (!m.matches()) {
            m = AUTOPET.matcher(raw);
            if (!m.matches()) return;
        }

        String name = normalizePetName(m.group(1).trim());
        if (name.equals(currentBaseName)) return;

        currentBaseName = name;
        save();
    }
}