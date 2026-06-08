package io.hamlook.aetheria.features.fishing.trophy;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class TrophyFishTracker {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String ODGER_TITLE = "Trophy Fishing";

    /**
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§9Lavahorse §r§6§lGOLD§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§5Soul Fish §r§8§lBRONZE§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§9Mana Ray §r§8§lBRONZE§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught a §r§fBlobfish §r§7§lSILVER§r§f!
     * REGEX-TEST: §6♔ §r§6§lTROPHY FISH! §r§fYou caught an §r§6Golden Fish §r§7§lSILVER§r§f!
     */
    private static final Pattern TROPHY_CHAT = Pattern.compile("(?:§r)?§6♔ §r§6§lTROPHY FISH! §r§fYou caught an? §r" + "(?<displayName>§[0-9a-fA-F](?:§k)?[\\w -]+?)(?:§r§f)? §r" + "(?<displayRarity>§[0-9a-fA-F]§l\\w+)§r§f!");

    /**
     * REGEX-TEST: §8Bronze §a✔§7
     * REGEX-TEST: §5§o§6Gold §a✔§7
     */
    private static final Pattern RANK_CAUGHT = Pattern.compile("^(?:§5§o)?§.([A-Za-z]+) §a[✔✓](?:§7 \\((\\d+)\\))?$");

    private static final Pattern RANK_EMPTY = Pattern.compile("^(?:§5§o)?§.([A-Za-z]+) §c[✖✗✘]$");
    private static final Pattern DISCOVERED = Pattern.compile("§aDiscovered");

    private static final Pattern BRONZE_LINE = Pattern.compile("^(?:§5§o)?§8Bronze.*");

    private static String getOpenContainerName() {
        return ContainerUtils.getContainerName();
    }

    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return "th";
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (ATHRConfig.feature == null) return;

        Matcher m = TROPHY_CHAT.matcher(event.message.getFormattedText());
        if (!m.find()) return;

        String fishName = ColorUtils.stripColor(m.group("displayName").replace("§k", "")).trim();
        String rarityRaw = ColorUtils.stripColor(m.group("displayRarity")).trim();

        String rarityStr = rarityRaw.charAt(0) + rarityRaw.substring(1).toLowerCase();
        TrophyRarity rarity = TrophyRarity.fromDisplayName(rarityStr);
        if (rarity == null) return;

        TrophyFishStorage storage = TrophyFishStorage.getInstance();
        int newCount = storage.incrementCount(fishName, rarity);
        storage.save();

        boolean hideBronze = ATHRConfig.feature.fishing.trophyFish.trophyBronzeHider && rarity == TrophyRarity.BRONZE && newCount > 1;
        boolean hideSilver = ATHRConfig.feature.fishing.trophyFish.trophySilverHider && rarity == TrophyRarity.SILVER && newCount > 1;
        if (hideBronze || hideSilver) {
            event.setCanceled(true);
            return;
        }

        if (!ATHRConfig.feature.fishing.trophyFish.trophyChatModify) return;

        int total = storage.getTotal(fishName);
        String countPart = newCount == 1 ? "§c§lFIRST! §r" : "§7" + newCount + ordinal(newCount) + " §r";

        String coloredRarity = rarity.formatCode + "§l" + rarity.displayName.toUpperCase();
        String coloredName = rarity.formatCode + fishName;

        String newMsg = "§6♔ §r§6§lTROPHY FISH! " + countPart + coloredRarity + " " + coloredName + " §7(§e" + String.format("%,d", total) + " total§7)";

        event.setCanceled(true);
        ChatUtils.sendMessage(newMsg);
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!ContainerUtils.isInContainer(event.gui, ODGER_TITLE)) return;
        ContainerChest container = ContainerUtils.getOpenChest(event.gui);
        if (container == null) return;

        scanOdger(container);
    }

    private void scanOdger(ContainerChest container) {
        if (mc.thePlayer == null) return;
        TrophyFishStorage storage = TrophyFishStorage.getInstance();
        boolean changed = false;

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == mc.thePlayer.inventory) continue;
            ItemStack item = slot.getStack();
            if (item == null) continue;

            String fishName = ColorUtils.stripColor(item.getDisplayName().replace("§k", "")).trim();
            if (fishName.isEmpty()) continue;

            boolean hasRarityLine = false;

            for (String line : ItemUtils.getLoreLines(item)) {
                Matcher caught = RANK_CAUGHT.matcher(line);
                if (caught.find()) {
                    hasRarityLine = true;
                    TrophyRarity rarity = TrophyRarity.fromDisplayName(caught.group(1));
                    if (rarity == null) continue;
                    if (storage.getCount(fishName, rarity) == 0) {
                        storage.setCount(fishName, rarity, 1);
                        changed = true;
                    }
                    continue;
                }

                Matcher empty = RANK_EMPTY.matcher(line);
                if (empty.find()) {
                    hasRarityLine = true;
                }
            }

            if (hasRarityLine && !storage.getFish().containsKey(fishName)) {
                storage.getFish().put(fishName, new java.util.LinkedHashMap<>());
                changed = true;
            }
        }

        if (changed) storage.save();
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.fishing.trophyFish.trophyOdgerTotal) return;
        if (!ODGER_TITLE.equals(getOpenContainerName())) return;
        if (event.toolTip == null || event.itemStack == null) return;

        List<String> lore = ItemUtils.getLoreLines(event.itemStack);
        boolean discovered = lore.stream().anyMatch(l -> DISCOVERED.matcher(l).find());
        if (!discovered) return;

        String fishName = ColorUtils.stripColor(event.itemStack.getDisplayName().replace("§k", "")).trim();
        TrophyFishStorage storage = TrophyFishStorage.getInstance();

        int total = storage.getTotal(fishName);
        if (total == 0) return;

        List<String> tip = event.toolTip;
        int bronzeIdx = -1;
        for (int i = 0; i < tip.size(); i++) {
            if (BRONZE_LINE.matcher(tip.get(i)).find()) {
                bronzeIdx = i;
                break;
            }
        }

        if (bronzeIdx >= 0) {
            TrophyRarity best = storage.getBestRarity(fishName);
            tip.add(bronzeIdx + 1, "");
            tip.add(bronzeIdx + 2, "§7Total: " + best.formatCode + String.format("%,d", total));
        }
    }
}