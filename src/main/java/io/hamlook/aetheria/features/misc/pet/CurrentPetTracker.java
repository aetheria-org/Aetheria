package io.hamlook.aetheria.features.misc.pet;

import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.events.SlotClickEvent;
import io.hamlook.aetheria.init.RegisterInstance;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.hamlook.aetheria.features.misc.pet.PetCache.normalizePetName;

public class CurrentPetTracker implements StorageManager.Managed {

    private static final String PETS_CONTAINER = "Pets";
    private static final String ACTIVE_LORE = "Click to despawn";

    // §cAutopet §eequipped your §7[Lvl 100] §dEnderman§9 ✦§e! §a§lVIEW RULE
    private static final Pattern AUTOPET = Pattern.compile("§r§cAutopet §r§eequipped your §r§7\\[Lvl (\\d+)] §r§(.)([^§]+?)§r(§. ?✦)?§r§e!(?:§r §a§lVIEW RULE)?§r?");
    private static final Pattern LVL_PATTERN = Pattern.compile("^\\[Lvl (\\d+)] (.+)$");
    private static final Pattern SKIN_PATTERN = Pattern.compile("(§. ?✦)");
    private static final Pattern RARITY_PATTERN = Pattern.compile("§7\\[Lvl \\d+] (§.)");
    @RegisterInstance
    private static CurrentPetTracker INSTANCE;
    private long ignoreContainerUpdatesUntil = 0L;
    private long lastContainerScan = 0L;
    private File file;

    @Getter
    private String currentBaseName = "";

    private CurrentPetTracker() {
    }

    public static CurrentPetTracker getInstance() {
        if (INSTANCE == null) INSTANCE = new CurrentPetTracker();
        return INSTANCE;
    }

    private static PetItemData parsePetItem(ItemStack item) {
        String rawName = item.getDisplayName().replace("Â§", "§");
        String stripped = StringUtils.stripControlCodes(rawName).trim();

        int level = 0;
        String nameWithoutLevel = stripped;
        Matcher lvlM = LVL_PATTERN.matcher(stripped);
        if (lvlM.matches()) {
            level = Integer.parseInt(lvlM.group(1));
            nameWithoutLevel = lvlM.group(2);
        }

        String skinTag = "";
        Matcher starM = SKIN_PATTERN.matcher(rawName);
        if (starM.find()) skinTag = starM.group(1).trim();

        String rarityColor = "";
        Matcher rarM = RARITY_PATTERN.matcher(rawName);
        if (rarM.find()) rarityColor = rarM.group(1);

        String base = normalizePetName(nameWithoutLevel.replace("✦", "").trim());
        if (level <= 0) return null;
        if (base.equalsIgnoreCase("Autopet")) return null;
        return base.isEmpty() ? null : new PetItemData(base, level, rarityColor, skinTag);
    }

    @Override
    public void initFile(File configDir) {
        file = new File(configDir, "current_pet.json");
        PetFileValidator.deleteIfLegacy(file);
    }

    @Override
    public void load() {
        String loaded = PetFileValidator.load(file, String.class);
        if (loaded != null) currentBaseName = loaded;
    }

    private void save() {
        PetFileValidator.save(file, currentBaseName);
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.gui instanceof GuiChest)) return;
        ContainerChest container = (ContainerChest) ((GuiChest) event.gui).inventorySlots;
        if (!container.getLowerChestInventory().getDisplayName().getUnformattedText().startsWith(PETS_CONTAINER))
            return;
        long now = System.currentTimeMillis();

        if (now - lastContainerScan >= 500L) {
            lastContainerScan = now;
            scanContainer(container);
        }
    }

    private void scanContainer(ContainerChest container) {
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;

            ItemStack item = slot.getStack();
            if (item == null || item.getItem() == null) continue;

            String texture = ItemUtils.getSkullTexture(item);
            if (texture == null || texture.isEmpty()) continue;

            PetItemData data = parsePetItem(item);
            if (data == null) continue;
            if (data.level <= 0) continue;
            PetCache.getInstance().update(data.base, data.level, data.rarityColor, data.skinTag, texture);
            if (System.currentTimeMillis() < ignoreContainerUpdatesUntil) {
                continue;
            }
            if (ItemUtils.getLoreLine(item, ACTIVE_LORE) != null) {
                String newKey = data.key();

                if (!newKey.equals(currentBaseName)) {
                    currentBaseName = newKey;
                    save();
                }
            }
        }
    }

    @SubscribeEvent
    public void onSlotClick(SlotClickEvent event) {
        if (!(event.getGui() instanceof GuiChest)) return;
        if (event.getClickType() != 0 || event.getSlot() == null) return;

        ContainerChest container = (ContainerChest) ((GuiChest) event.getGui()).inventorySlots;

        if (!container.getLowerChestInventory().getDisplayName().getUnformattedText().startsWith(PETS_CONTAINER))
            return;

        if (event.getSlot().inventory == Minecraft.getMinecraft().thePlayer.inventory) return;

        ItemStack item = event.getSlot().getStack();
        if (item == null || item.getItem() == null) return;

        String texture = ItemUtils.getSkullTexture(item);
        if (texture == null || texture.isEmpty()) return;

        PetItemData data = parsePetItem(item);
        if (data == null) return;

        // Ignore skulls like Autopet
        if (data.level <= 0) return;

        PetCache.getInstance().update(data.base, data.level, data.rarityColor, data.skinTag, texture);

        if (ItemUtils.getLoreLine(item, ACTIVE_LORE) != null) {
            currentBaseName = "";
        } else {
            currentBaseName = data.key();
        }
        ignoreContainerUpdatesUntil = System.currentTimeMillis() + 1500L;
        save();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (ChatUtils.isFromServer(event)) return;
        Matcher am = AUTOPET.matcher(event.message.getFormattedText());
        if (!am.find()) return;

        int level = Integer.parseInt(am.group(1));
        String rarity = "§" + am.group(2);
        String petName = normalizePetName(StringUtils.stripControlCodes(am.group(3)).trim());
        String skinTag = am.group(4) != null ? am.group(4).trim() : "";

        if (petName.isEmpty()) return;
        PetCache.getInstance().updateFromChat(petName, level, rarity, skinTag);
        String key = PetCache.makeKey(rarity, petName, skinTag);
        if (!key.equals(currentBaseName)) {
            currentBaseName = key;
            save();
        }
    }

    private static final class PetItemData {
        final String base, rarityColor, skinTag;
        final int level;

        PetItemData(String base, int level, String rarityColor, String skinTag) {
            this.base = base;
            this.level = level;
            this.rarityColor = rarityColor;
            this.skinTag = skinTag;
        }

        String key() {
            return PetCache.makeKey(rarityColor, base, skinTag);
        }
    }
}
