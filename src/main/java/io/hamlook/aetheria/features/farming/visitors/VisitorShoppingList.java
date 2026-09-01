package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.VisitorsConfig;
import io.hamlook.aetheria.events.DebugReportEvent;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.features.price.PriceMap;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.SoundUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.events.ASMServerJoinEvent;
import io.hamlook.aetheria.events.ASMGuiBackgroundDrawEvent;

@RegisterEvents
public final class VisitorShoppingList {

    private static final long HAVE_CACHE_MS = 1000L;
    private static final long SEARCH_TTL_MS = 60_000L;
    private static final int MAX_ROWS = 12;
    private static final long CONTENTS_WAIT_MS = 1500L;
    private static final int PARSE_LOG_MAX = 15;

    private static final Map<String, String> RESOLVED_IDS = new HashMap<>();
    private static final Map<String, String> ITEM_NAMES = new HashMap<>();
    private static Map<String, SkyblockItem> nameToItem = null;

    private static Map<String, Integer> haveCounts = Collections.emptyMap();
    private static long haveCountsAt = 0L;
    private static GuiScreen lastParsedScreen;
    private static GuiScreen pendingScreen;
    private static long pendingSinceMs;
    private static final ArrayDeque<String> PARSE_LOG = new ArrayDeque<>();

    private static String pendingBzsCommand = null;
    private static int pendingBzsTicks = 0;
    private static long signSubmitAtMs = 0L;
    private static GuiEditSign pendingSubmitSign;
    private static String lastRemoteAddress = null;

    static {
        VisitorBazaarHighlight.register();
    }

    public static void onServerJoined(ASMServerJoinEvent event) {
        String address = event.manager == null ? "" : String.valueOf(event.manager.getRemoteAddress());
        boolean addressChanged = lastRemoteAddress != null && !lastRemoteAddress.equals(address);
        lastRemoteAddress = address;
        clearTransientState();
        int mode = config() != null ? config().resetMode : 2;
        if (mode == 2 || (mode == 0 && addressChanged)) {
            FarmingApi.clearVisitorData();
        }
    }

    @HandleEvent
    public void onUnload(ASMWorldUnloadEvent event) {
        lastParsedScreen = null;
        pendingScreen = null;
        pendingBzsCommand = null;
        signSubmitAtMs = 0L;
        pendingSubmitSign = null;
        int mode = config() != null ? config().resetMode : 2;
        if (mode != 1) {
            FarmingApi.clearVisitorData();
        }
    }

    private static void clearTransientState() {
        lastParsedScreen = null;
        pendingScreen = null;
        PARSE_LOG.clear();
        PENDING_CONFIRMS.clear();
        pendingBzsCommand = null;
        signSubmitAtMs = 0L;
        pendingSubmitSign = null;
    }

    @HandleEvent
    public void onBackgroundDrawn(ASMGuiBackgroundDrawEvent event) {
        if (!(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (!(gui.inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) gui.inventorySlots;
        updateChestSignature(chest);
        if (lastParsedScreen == event.gui) return;
        attemptParse(chest, event.gui);
    }

    public static boolean nameMatchesFlow(String raw) {
        String query = FarmingApi.getSearchedItemName();
        if (query.isEmpty()) return false;
        String s = ColorUtils.stripColor(raw == null ? "" : raw).trim();
        if (s.startsWith("Search: ")) s = s.substring("Search: ".length()).trim();
        if (s.isEmpty()) return false;
        return s.equals(query)
                || (s.length() < query.length() ? query.startsWith(s) : s.startsWith(query));
    }

    public static boolean isOrderFlowActive() {
        String signature = FarmingApi.getLastChestSignature();
        return !signature.isEmpty() && nameMatchesFlow(signature)
                && FarmingApi.isSearchFresh(SEARCH_TTL_MS);
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Delayed /bzs dispatch after closing the current menu
        if (pendingBzsCommand != null) {
            if (mc.currentScreen instanceof GuiContainer) {
                // Player opened another container before dispatch; drop the order
                pendingBzsCommand = null;
            } else if (mc.currentScreen == null && --pendingBzsTicks <= 0) {
                ChatUtils.sendChatCommand(pendingBzsCommand);
                pendingBzsCommand = null;
            }
        }

        if (signSubmitAtMs > 0 && System.currentTimeMillis() >= signSubmitAtMs) {
            GuiEditSign gui = pendingSubmitSign;
            signSubmitAtMs = 0L;
            pendingSubmitSign = null;
            if (gui != null && mc.currentScreen == gui) {
                noteParse("[flow] submitted sign automatically");
                mc.displayGuiScreen(null);
            } else {
                noteParse("[flow] sign changed before auto-submit; aborted");
            }
        }
    }

    @HandleEvent
    public void onDebugReport(DebugReportEvent event) {
        event.title("Visitors");
        List<String> lines = new ArrayList<>();

        VisitorsConfig cfg = config();
        String[] resetModes = {"Changing Servers", "Never", "On Rejoin"};
        String[] showIns = {"Visitors", "Bazaar", "Inventory", "Relevant Menus", "All Menus"};
        int resetMode = cfg != null ? cfg.resetMode : 2;
        int showIn = cfg != null && cfg.panel != null ? cfg.panel.showIn : 3;
        lines.add("reset mode: " + (resetMode >= 0 && resetMode < resetModes.length ? resetModes[resetMode] : resetMode)
                + ", last remote address: " + (lastRemoteAddress == null ? "<none yet>" : lastRemoteAddress));
        lines.add("panel show-in: " + (showIn >= 0 && showIn < showIns.length ? showIns[showIn] : showIn)
                + ", only-show-with-data: " + (cfg == null || cfg.panel == null || cfg.panel.onlyShowWithData));

        List<String> active = FarmingApi.getActiveVisitors();
        if (active.isEmpty()) {
            boolean onGarden = SkyblockData.getCurrentLocation() == SkyblockData.Location.GARDEN;
            lines.add("tab capture: no visitors captured"
                    + (onGarden ? "" : " (location is " + SkyblockData.getCurrentLocation() + ", not Garden)"));
        } else {
            lines.add("tab capture (" + active.size() + "): " + String.join(", ", active));
        }

        List<String> mirror = FarmingApi.getLastGardenVisitorsSnapshot();
        if (!mirror.isEmpty() && mirror.equals(active)) {
            lines.add("garden mirror (" + mirror.size() + "): same as tab capture");
        } else if (!mirror.isEmpty()) {
            lines.add("garden mirror (" + mirror.size() + "): " + String.join(", ", mirror));
        }

        Map<String, LinkedHashMap<String, Integer>> needsMap = FarmingApi.getVisitorNeeds();
        if (needsMap.isEmpty()) {
            lines.add("recorded needs: none (open a visitor menu to learn its offer)");
        } else {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, LinkedHashMap<String, Integer>> entry : needsMap.entrySet()) {
                parts.add(entry.getKey() + " [" + entry.getValue().size() + " items, x"
                        + FarmingApi.effectiveVisitorCount(entry.getKey()) + "]");
            }
            lines.add("recorded needs: " + String.join(";  ", parts));
        }

        Map<String, LinkedHashMap<String, Integer>> rewardsMap = FarmingApi.getVisitorRewards();
        if (!rewardsMap.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, LinkedHashMap<String, Integer>> entry : rewardsMap.entrySet()) {
                parts.add(entry.getKey() + " [" + entry.getValue().size() + " items]");
            }
            lines.add("recorded rewards: " + String.join(";  ", parts));
        }

        if (VisitorPanel.getInstance() != null) {
            lines.add("main panel last gate result: " + VisitorPanel.getInstance().getBlockReason());
        }

        String searchedName = FarmingApi.getSearchedItemName();
        if (searchedName.isEmpty()) {
            lines.add("order flow: none");
        } else {
            lines.add("order flow: '" + searchedName + "', fresh=" + !isSearchExpired()
                    + ", signature='" + FarmingApi.getLastChestSignature()
                    + "', active=" + isOrderFlowActive()
                    + (pendingBzsCommand != null ? ", dispatching '" + pendingBzsCommand + "'" : "")
                    + (signSubmitAtMs > 0 ? ", auto-submit pending" : ""));
        }

        String purseLine = SkyblockData.getPurseLine();
        double purseCoins = getPurseCoins();
        lines.add("purse: " + (purseLine == null ? "sidebar line not seen"
                : "'" + purseLine + "' = " + purseCoins));

        double copperTotal = FarmingApi.bonusTotal(VisitorBonus.Type.COPPER);
        double bitsTotal = FarmingApi.bonusTotal(VisitorBonus.Type.BITS);
        if (copperTotal > 0 || bitsTotal > 0) {
            lines.add("active visitor bonuses: +" + trimAmount(copperTotal) + " Copper, +"
                    + trimAmount(bitsTotal) + " Bits");
        }
        Map<String, Integer> haveKeys = getHaveCounts();
        if (!haveKeys.isEmpty()) {
            List<String> sample = new ArrayList<>(haveKeys.keySet())
                    .subList(0, Math.min(8, haveKeys.size()));
            lines.add("inventory id sample (" + haveKeys.size() + " total): " + sample);
        }

        List<String> parseLog = getParseLogSnapshot();
        if (!parseLog.isEmpty()) {
            lines.add("recent chest parse attempts:");
            for (String entry : parseLog) lines.add("  - " + entry);
        } else {
            lines.add("no chest parse attempts yet this session");
        }

        if (active.isEmpty() && !needsMap.isEmpty()) {
            event.addData(lines);
        } else {
            event.addIrrelevant(lines);
        }
    }

    private static final long COMPLETION_TTL_MS = 10_000L;
    private enum ConfirmType {REFUSE_GOOD, ACCEPT_EXPENSIVE}
    private static final Map<String, PendingConfirm> PENDING_CONFIRMS = new HashMap<>();

    private static final class PendingConfirm {
        final ConfirmType type;
        final long atMs;

        PendingConfirm(ConfirmType type, long atMs) {
            this.type = type;
            this.atMs = atMs;
        }
    }

    /** Coins spent per copper gained for a visitor's recorded offer, or null without data */
    private static Double payingPerCopper(String visitor) {
        VisitorsConfig cfg = config();
        if (cfg == null || !cfg.copperPriceDisplay) return null;
        Map<String, Integer> needs = FarmingApi.getVisitorNeeds()
                .getOrDefault(visitor, new LinkedHashMap<>());
        double wantsCost = totalCost(needs);
        Integer copper = null;
        for (VisitorBonus bonus : FarmingApi.getVisitorBonuses(visitor)) {
            if (bonus.type == VisitorBonus.Type.COPPER) copper = (int) bonus.amount;
        }
        if (copper == null || copper <= 0 || wantsCost <= 0) return null;
        return wantsCost / copper;
    }

    @HandleEvent
    public void onSlotClick(io.hamlook.aetheria.events.SlotClickEvent event) {
        if (event.isCancelled()) return;
        if (event.getSlot() == null) return;
        ItemStack stack = event.getSlot().getStack();
        if (stack == null) return;
        String name = stripped(stack);
        boolean accept = "Accept Offer".equals(name);
        if (!accept && !"Refuse Offer".equals(name)) return;
        if (!(event.getGui().inventorySlots instanceof ContainerChest)) return;
        ContainerChest chest = (ContainerChest) event.getGui().inventorySlots;
        String visitor = ContainerUtils.getTitle(chest);
        if (visitor == null || visitor.isEmpty()) return;
        if (accept && !canAcceptOffer(stack)) return;

        VisitorsConfig cfg = config();
        Double per = payingPerCopper(visitor);
        ConfirmType confirmType = null;
        if (per != null && cfg != null) {
            boolean goodDeal = per <= cfg.copperThreshold;
            if (!accept && goodDeal && cfg.confirmGoodCopperRefuse) confirmType = ConfirmType.REFUSE_GOOD;
            if (accept && !goodDeal && cfg.confirmExpensiveCopperAccept) confirmType = ConfirmType.ACCEPT_EXPENSIVE;
        }

        if (confirmType != null) {
            PendingConfirm pending = PENDING_CONFIRMS.get(visitor);
            long now = System.currentTimeMillis();
            if (pending != null && pending.type == confirmType && now - pending.atMs <= COMPLETION_TTL_MS) {
                PENDING_CONFIRMS.remove(visitor);
                FarmingApi.markVisitorCompleted(visitor);
                return;
            }
            event.cancel();
            PENDING_CONFIRMS.put(visitor, new PendingConfirm(confirmType, now));
            SoundUtils.playSound("note.pling");
            String fmt = formatPrice(per);
            if (confirmType == ConfirmType.REFUSE_GOOD) {
                ChatUtils.sendMessage("§c[ASM] §7Good deal, paying §6" + fmt
                        + "§7/c. Click Refuse again to confirm.");
            } else {
                ChatUtils.sendMessage("§e[ASM] §7Expensive deal, §c" + "paying " + fmt + "§7/c above your §6"
                        + formatPrice(cfg.copperThreshold) + "§7 limit. Click Accept again to confirm.");
            }
            return;
        }

        FarmingApi.markVisitorCompleted(visitor);
        postDealMessage(accept, per, cfg);
    }

    private static void postDealMessage(boolean accepted, Double per, VisitorsConfig cfg) {
        if (per == null || cfg == null || !cfg.copperPriceDisplay) return;
        String fmt = formatPrice(per);
        boolean goodDeal = per <= cfg.copperThreshold;
        if (!accepted && goodDeal) {
            ChatUtils.sendMessage("§a[ASM] §7Refused a good copper deal, paying §6" + fmt + "§7/c.");
        } else if (accepted && !goodDeal) {
            ChatUtils.sendMessage("§e[ASM] §7Accepted an expensive deal, §c" + "paid " + fmt
                    + "§7/c above your §6" + formatPrice(cfg.copperThreshold) + "§7 limit.");
        }
    }

    private static boolean canAcceptOffer(ItemStack stack) {
        for (String rawLine : ItemUtils.getLoreLinesWithoutColor(stack)) {
            String line = rawLine.trim();
            if (line.contains("You don't have the required items!")) return false;
            if (line.equals("Click to accept!")) return true;
        }
        return false;
    }

    private static void updateChestSignature(ContainerChest chest) {
        int size = chest.getLowerChestInventory().getSizeInventory();
        if (size <= 13) return;
        String title = ContainerUtils.getTitle(chest);

        if (hasNoContents(chest)) return;

        if (title.startsWith("Search: ")) {
            if (nameMatchesFlow(title)) {
                FarmingApi.setLastChestSignature(FarmingApi.getSearchedItemName());
                noteParse("[flow] search results opened for '" + FarmingApi.getSearchedItemName() + "'");
            }
            return;
        }

        if (findSlotNamed(chest, "Custom Amount") != null && findSlotNamed(chest, "Buy Instantly") == null) {
            String found = findNeededIdInContents(chest);
            if (found == null) return;
            FarmingApi.setLastChestSignature(found.equals(FarmingApi.getSearchedItemId())
                    ? FarmingApi.getSearchedItemName() : "");
            return;
        }

        ItemStack buySlot = findSlotNamed(chest, "Buy Instantly");
        if (buySlot != null) {
            String id = neededIdByName(firstLoreLine(buySlot));
            if (id != null && id.equals(FarmingApi.getSearchedItemId())) {
                FarmingApi.setLastChestSignature(FarmingApi.getSearchedItemName());
            } else {
                FarmingApi.setLastChestSignature("");
            }
            return;
        }

        if (nameMatchesFlow(title)) {
            FarmingApi.setLastChestSignature(FarmingApi.getSearchedItemName());
        } else {
            FarmingApi.setLastChestSignature("");
        }
    }

    private static boolean hasNoContents(ContainerChest chest) {
        int size = chest.getLowerChestInventory().getSizeInventory();
        for (int i = 0; i < size; i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) return false;
        }
        return true;
    }

    static ItemStack findSlotNamed(ContainerChest chest, String name) {
        int size = chest.getLowerChestInventory().getSizeInventory();
        for (int i = 0; i < size; i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack != null && name.equalsIgnoreCase(stripped(stack))) return stack;
        }
        return null;
    }

    private static String findNeededIdInContents(ContainerChest chest) {
        int size = chest.getLowerChestInventory().getSizeInventory();
        Set<String> needs = FarmingApi.getVisitorNeeds().keySet();
        for (int i = 0; i < size; i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack == null) continue;
            String internal = ItemUtils.getInternalName(stack);
            if (internal != null && needs.contains(internal)) return internal;
        }
        return null;
    }

    static String firstLoreLine(ItemStack stack) {
        for (String raw : ItemUtils.getLoreLinesWithoutColor(stack)) {
            String line = ColorUtils.stripColor(raw).trim();
            if (!line.isEmpty()) return line;
        }
        return null;
    }

    private static final long NEEDED_NAMES_TTL_MS = 1000L;
    private static Map<String, String> neededNamesCache = Collections.emptyMap();
    private static long neededNamesAt = 0L;
    private static int neededNamesCount = -1;

    static String neededIdByName(String displayName) {
        if (displayName == null || displayName.isEmpty()) return null;
        return neededNames().get(displayName.toLowerCase(Locale.ROOT));
    }

    private static Map<String, String> neededNames() {
        Map<String, LinkedHashMap<String, Integer>> needs = FarmingApi.getVisitorNeeds();
        long now = System.currentTimeMillis();
        if (needs.size() != neededNamesCount || neededNamesCache.isEmpty() != needs.isEmpty()
                || now - neededNamesAt >= NEEDED_NAMES_TTL_MS) {
            Map<String, String> map = new HashMap<>();
            for (String id : needs.keySet()) {
                String name = itemNameOf(id);
                if (name != null && !name.isEmpty()) map.put(name.toLowerCase(Locale.ROOT), id);
            }
            neededNamesCache = map;
            neededNamesCount = needs.size();
            neededNamesAt = now;
        }
        return neededNamesCache;
    }

    /**
     * The GUI can render before the server's S30PacketWindowItems arrives (laggy
     * servers), so an empty first frame must NOT mark the screen parsed. Wait up
     * to CONTENTS_WAIT_MS for an "Accept Offer" item to appear; only then decide
     */
    private static void attemptParse(ContainerChest chest, GuiScreen gui) {
        long now = System.currentTimeMillis();
        if (pendingScreen != gui) {
            pendingScreen = gui;
            pendingSinceMs = now;
        }

        ItemStack offer = findAcceptOffer(chest);
        if (offer == null) {
            if (now - pendingSinceMs >= CONTENTS_WAIT_MS) {
                finishScreen(gui);
                noteParse("no 'Accept Offer' item " + CONTENTS_WAIT_MS + "ms after open -> not a visitor menu");
            }
            return;
        }

        String visitor = ContainerUtils.getTitle(chest);
        if (visitor.isEmpty()) {
            finishScreen(gui);
            noteParse("'Accept Offer' found but chest title empty");
            return;
        }

        if (!FarmingApi.getActiveVisitors().contains(visitor)) {
            noteParse("'" + visitor + "' offer seen, waiting for tab capture to list it");
            return;
        }

        finishScreen(gui);
        recordOffer(visitor, offer);
    }

    private static void finishScreen(GuiScreen gui) {
        lastParsedScreen = gui;
        pendingScreen = null;
    }

    static void noteParse(String msg) {
        if (!PARSE_LOG.isEmpty() && PARSE_LOG.peekLast().equals(msg)) return;
        PARSE_LOG.addLast(msg);
        while (PARSE_LOG.size() > PARSE_LOG_MAX) PARSE_LOG.removeFirst();
    }

    public static List<String> getParseLogSnapshot() {
        return new ArrayList<>(PARSE_LOG);
    }

    private static void recordOffer(String visitor, ItemStack offer) {
        VisitorOffer parsed = VisitorOfferParser.parse(ItemUtils.getLoreLinesWithoutColor(offer), VisitorShoppingList::resolveItemId);

        FarmingApi.recordVisitorNeeds(visitor, parsed.needs);
        FarmingApi.recordVisitorRewards(visitor, parsed.rewards);
        FarmingApi.recordVisitorBonuses(visitor, parsed.bonuses);

        StringBuilder msg = new StringBuilder("recorded '").append(visitor)
                .append("' needs=").append(parsed.needs.size())
                .append(" rewards=").append(parsed.rewards.size())
                .append(" bonuses=").append(parsed.bonuses.size());
        if (!parsed.unresolvedNames.isEmpty()) msg.append(" unresolved=").append(parsed.unresolvedNames);
        noteParse(msg.toString());
    }

    private static ItemStack findAcceptOffer(ContainerChest chest) {
        int size = chest.getLowerChestInventory().getSizeInventory();
        for (int i = 0; i < size; i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack != null && "Accept Offer".equals(stripped(stack))) return stack;
        }
        return null;
    }

    private static String stripped(ItemStack stack) {
        return ColorUtils.stripColor(stack.getDisplayName()).trim();
    }

    public static String resolveItemId(String displayName) {
        String key = displayName.toLowerCase(Locale.ROOT);
        if (RESOLVED_IDS.containsKey(key)) return RESOLVED_IDS.get(key);
        String id = null;
        SkyblockItem item = lookupItem(key);
        if (item != null && item.skyblockID != null && !item.skyblockID.isEmpty()) {
            id = item.skyblockID;
            ITEM_NAMES.put(id, displayName);
        }
        if (id != null || ItemRegistry.isLoaded) {
            RESOLVED_IDS.put(key, id);
        } else {
            RESOLVED_IDS.remove(key);
        }
        return id;
    }

    private static SkyblockItem lookupItem(String key) {
        if (nameToItem == null) {
            if (!ItemRegistry.isLoaded) return null;
            Map<String, SkyblockItem> byName = new HashMap<>();
            for (SkyblockItem item : ItemRegistry.getAllItems()) {
                if (item.displayName == null || item.displayName.trim().isEmpty()) continue;
                String clean = ColorUtils.stripColor(item.displayName).trim().toLowerCase(Locale.ROOT);
                if (clean.isEmpty()) continue;

                SkyblockItem prev = byName.putIfAbsent(clean, item);
                if (prev != null && isLegacy(prev) && !isLegacy(item)) byName.put(clean, item);
            }
            nameToItem = byName;
        }
        SkyblockItem exact = nameToItem.get(key);
        if (exact != null) return exact;
        SkyblockItem fallback = null;
        for (Map.Entry<String, SkyblockItem> entry : nameToItem.entrySet()) {
            if (!entry.getKey().startsWith(key)) continue;
            if (!isLegacy(entry.getValue())) return entry.getValue();
            if (fallback == null) fallback = entry.getValue();
        }
        return fallback;
    }

    private static boolean isLegacy(SkyblockItem item) {
        if (item.baseLore == null) return false;
        for (String line : item.baseLore) {
            if (line.contains("Legacy Item")) return true;
        }
        return false;
    }

    public static String itemNameOf(String itemId) {
        String name = ITEM_NAMES.get(itemId);
        if (name != null) return name;
        SkyblockItem item = ItemRegistry.getItem(itemId);
        return item != null && item.displayName != null ? ColorUtils.stripColor(item.displayName).trim() : itemId;
    }

    public static Map<String, Integer> getHaveCounts() {
        long now = System.currentTimeMillis();
        if (now - haveCountsAt < HAVE_CACHE_MS) return haveCounts;
        Map<String, Integer> counts = new HashMap<>();
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null && player.inventory != null) {
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack == null) continue;
                String id = ItemUtils.getEffectiveItemId(stack);
                if (id == null) continue;
                counts.merge(id, stack.stackSize, Integer::sum);
            }
        }
        haveCounts = counts;
        haveCountsAt = now;
        return counts;
    }

    public static LinkedHashMap<String, Integer> getMergedNeeds() {
        LinkedHashMap<String, Integer> merged = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Integer>> visitor : FarmingApi.getVisitorNeeds().entrySet()) {
            int multiplier = FarmingApi.effectiveVisitorCount(visitor.getKey());
            if (multiplier <= 0) continue;
            for (Map.Entry<String, Integer> entry : visitor.getValue().entrySet()) {
                merged.merge(entry.getKey(), entry.getValue() * multiplier, Integer::sum);
            }
        }
        return merged;
    }

    public static double unitPrice(String itemId) {
        double price = PriceMap.Cached.getDPrice(itemId);
        return price > 0 ? price : 0;
    }

    public static double totalCost(Map<String, Integer> needs) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : needs.entrySet()) {
            total += unitPrice(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    public static double totalRewardValue() {
        double total = 0;
        for (Map.Entry<String, LinkedHashMap<String, Integer>> visitor : FarmingApi.getVisitorRewards().entrySet()) {
            int multiplier = FarmingApi.effectiveVisitorCount(visitor.getKey());
            if (multiplier <= 0) continue;
            for (Map.Entry<String, Integer> entry : visitor.getValue().entrySet()) {
                total += unitPrice(entry.getKey()) * entry.getValue() * multiplier;
            }
        }
        return total;
    }

    public static double adjustedProfit(Map<String, Integer> mergedNeeds) {
        Map<String, Integer> have = getHaveCounts();
        double cost = 0;
        for (Map.Entry<String, Integer> entry : mergedNeeds.entrySet()) {
            int missing = Math.max(0, entry.getValue() - have.getOrDefault(entry.getKey(), 0));
            cost += unitPrice(entry.getKey()) * missing;
        }
        return totalRewardValue() - cost;
    }

    public static String formatPrice(double value) {
        return Utils.shortNumberFormat(value, 0);
    }

    /** Estimated farm time for the missing amount of one item, or -1 when unknown */
    public static long timeToFarmMs(String itemId, int amount) {
        int missing = Math.max(0, amount - getHaveCounts().getOrDefault(itemId, 0));
        if (missing == 0) return 0;
        Long rawEquivalent = io.hamlook.aetheria.features.farming.data.Crop.rawEquivalentOf(itemId);
        if (rawEquivalent == null) return -1;
        return FarmingApi.getTimeToFarmMs(rawEquivalent * missing);
    }

    public static String formatFarmTime(long ms) {
        return Utils.formatDuration(ms, true);
    }

    /** Total estimated farm time over all needs, or -1 when no rate data exists */
    public static long totalTimeToFarmMs(Map<String, Integer> needs) {
        long total = 0;
        boolean any = false;
        for (Map.Entry<String, Integer> entry : needs.entrySet()) {
            Long rawEquivalent = io.hamlook.aetheria.features.farming.data.Crop.rawEquivalentOf(entry.getKey());
            if (rawEquivalent == null) continue;
            int missing = Math.max(0, entry.getValue() - getHaveCounts().getOrDefault(entry.getKey(), 0));
            if (missing == 0) continue;
            long ms = FarmingApi.getTimeToFarmMs(rawEquivalent * missing);
            if (ms < 0) return -1;
            total += ms;
            any = true;
        }
        return any ? total : -1;
    }

    public static String formatNeedRow(String itemId, int amount, boolean showPrices, boolean showHave, boolean showTime) {
        StringBuilder sb = new StringBuilder("§e").append(amount).append("x §f").append(itemNameOf(itemId));
        if (showHave) {
            int have = getHaveCounts().getOrDefault(itemId, 0);
            sb.append(have >= amount ? "§a" : "§e").append(" [").append(have).append('/').append(amount).append(']');
        }
        if (showPrices) {
            double price = unitPrice(itemId) * amount;
            if (price > 0) sb.append(" §7(§6").append(formatPrice(price)).append("§7)");
        }
        if (showTime) {
            long ms = timeToFarmMs(itemId, amount);
            if (ms > 0) sb.append(" §7(§b≈").append(formatFarmTime(ms)).append("§7)");
        }
        return sb.toString();
    }

    public static List<VisitorLine> buildMainLines(boolean preview) {
        VisitorsConfig cfg = config();
        if (cfg == null) return Collections.emptyList();
        List<VisitorLine> lines = new ArrayList<>();
        LinkedHashMap<String, Integer> needs = preview ? previewNeeds() : getMergedNeeds();
        if (needs.isEmpty()) {
            lines.add(VisitorLine.text("§eVisitor Shopping List"));
            lines.add(VisitorLine.text("§7No visitor items yet. Open a visitor"));
            return lines;
        }
        double total = cfg.panel.showPrices ? totalCost(needs) : 0;
        String timeSuffix = "";
        if (!preview && cfg.showTimeToFarm) {
            long timeMs = totalTimeToFarmMs(needs);
            if (timeMs > 0) timeSuffix = " §8· §b≈" + formatFarmTime(timeMs);
        }
        lines.add(VisitorLine.text("§eVisitor Shopping List"
                + (cfg.panel.showPrices && total > 0 ? " §7(§6" + formatPrice(total) + "§7)" : "")
                + timeSuffix));
        if (cfg.panel.showProfit) {
            appendProfitLines(lines, needs, cfg.panel.showPrices);
        }
        lines.add(VisitorLine.separator());
        appendNeedRows(lines, needs, cfg.panel.showPrices, cfg.showHaveCounts,
                !preview && cfg.showTimeToFarm);
        return lines;
    }

    public static String trimAmount(double amount) {
        return amount == Math.floor(amount) ? String.valueOf((long) amount)
                : String.valueOf(Math.round(amount * 10.0) / 10.0);
    }

    public static List<VisitorLine> buildSingleEntryLines() {
        List<VisitorLine> lines = new ArrayList<>();
        String itemId = FarmingApi.getSearchedItemId();
        if (itemId == null || itemId.isEmpty()) return buildMainLines(false);
        VisitorsConfig cfg = config();
        int listed = Math.max(1, getMergedNeeds().getOrDefault(itemId, 1));
        lines.add(VisitorLine.text("§eOrdering: §f" + itemNameOf(itemId)));
        lines.add(VisitorLine.separator());
        boolean showTime = cfg != null && cfg.showTimeToFarm;
        appendItem(lines, itemId, listed,
                cfg != null && cfg.panel.showPrices, cfg != null && cfg.showHaveCounts, showTime);
        return lines;
    }

    private static void appendNeedRows(List<VisitorLine> lines, LinkedHashMap<String, Integer> needs,
                                       boolean showPrices, boolean showHave, boolean showTime) {
        int count = 0;
        for (Map.Entry<String, Integer> entry : needs.entrySet()) {
            if (count == MAX_ROWS) {
                lines.add(VisitorLine.text("§7...and " + (needs.size() - MAX_ROWS) + " more"));
                break;
            }
            appendItem(lines, entry.getKey(), entry.getValue(), showPrices, showHave, showTime);
            count++;
        }
    }

    private static void appendItem(List<VisitorLine> lines, String itemId, int amount,
                                   boolean showPrices, boolean showHave, boolean showTime) {
        SkyblockItem item = ItemRegistry.getItem(itemId);
        ItemStack icon = item != null ? item.getStack() : null;
        lines.add(VisitorLine.item(formatNeedRow(itemId, amount, showPrices, showHave, showTime), icon, itemId, amount));
    }

    private static void appendProfitLines(List<VisitorLine> lines, LinkedHashMap<String, Integer> needs,
                                          boolean showPrices) {
        double rewards = totalRewardValue();
        if (rewards <= 0) return;
        double profit = showPrices ? adjustedProfit(needs) : 0;
        lines.add(VisitorLine.text("§6Rewards: §a" + formatPrice(rewards)));
        if (profit >= 0) {
            lines.add(VisitorLine.text("§aProfit: §a+" + formatPrice(profit)));
        } else {
            lines.add(VisitorLine.text("§aProfit: §c-" + formatPrice(-profit)));
        }
    }

    public static void onRowClick(String itemId, int amount) {
        if (itemId == null || amount <= 0) return;
        int missing = missingAmount(itemId, amount);
        if (missing <= 0) {
            SoundUtils.playSound("note.pling");
            ChatUtils.sendMessage("§e[ASM] §7You already have enough §f" + itemNameOf(itemId) + "§7.");
            return;
        }

        VisitorsConfig cfg = config();
        if (cfg != null && cfg.checkInventorySpace && !hasInventorySpace(itemId, missing)) {
            int slotsNeeded = (int) Math.ceil(missing / (double) maxStackSize(itemId));
            SoundUtils.playSound("note.pling");
            ChatUtils.sendMessage("§c[ASM] §7Not enough inventory space! Need §e" + slotsNeeded
                    + "§7 more empty slot(s) for §e" + missing + "x " + itemNameOf(itemId)
                    + "§7 (have §e" + countEmptyMainInventorySlots() + "§7 free).");
            return;
        }
        if (cfg != null && cfg.checkPurseCoins) {
            double cost = unitPrice(itemId) * missing;
            double purse = getPurseCoins();
            if (purse >= 0 && purse < cost) {
                SoundUtils.playSound("note.pling");
                ChatUtils.sendMessage("§c[ASM] §7Not enough coins! Need §6" + formatPrice(cost)
                        + "§7, purse has §6" + formatPrice(purse) + "§7.");
                return;
            }
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiEditSign) {
            writeIntoSign(mc, missing);
            return;
        }

        String name = itemNameOf(itemId);
        FarmingApi.setSearchedItem(itemId, name);
        FarmingApi.setPendingSign(amount);

        if (cfg != null && cfg.copyAmountToClipboard) {
            Utils.copyToClipboard(String.valueOf(missing));
        }
        pendingBzsCommand = "/bzs " + name;
        pendingBzsTicks = 3;
        if (mc.currentScreen != null) mc.thePlayer.closeScreen();
        noteParse("[flow] ordering " + name + " x" + missing + " (closed menu, dispatching /bzs)");
    }

    public static int missingAmount(String itemId, int listedTotal) {
        if (itemId == null) return 0;
        int have = getHaveCounts().getOrDefault(itemId, 0);
        return Math.max(0, listedTotal - have);
    }

    /** Purse as a numeric value from the sidebar line; -1 when absent/unparseable. */
    private static double getPurseCoins() {
        String line = SkyblockData.getPurseLine();
        if (line == null) return -1;
        int ci = line.indexOf(": ");
        String value = ci >= 0 ? line.substring(ci + 2) : line;
        return parseCoinAmount(value);
    }

    /** Parses "1,234.5k"-style coin amounts; -1 when unparseable. */
    private static double parseCoinAmount(String raw) {
        if (raw == null) return -1;
        String s = ColorUtils.stripColor(raw).trim().replace(",", "");
        if (s.isEmpty()) return -1;
        double multiplier = 1;
        switch (Character.toLowerCase(s.charAt(s.length() - 1))) {
            case 'k': multiplier = 1_000; break;
            case 'm': multiplier = 1_000_000; break;
            case 'b': multiplier = 1_000_000_000; break;
            default:
                if (!Character.isDigit(s.charAt(s.length() - 1))) return -1;
        }
        try {
            return Double.parseDouble(multiplier == 1 ? s : s.substring(0, s.length() - 1)) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Bazaar deliveries never top up partial stacks: the missing amount needs
     * ceil(missing / maxStackSize) fully empty slots.
     */
    private static boolean hasInventorySpace(String itemId, int missing) {
        int maxStack = maxStackSize(itemId);
        int slotsNeeded = (int) Math.ceil(missing / (double) maxStack);
        return slotsNeeded <= countEmptyMainInventorySlots();
    }

    private static int maxStackSize(String itemId) {
        SkyblockItem item = ItemRegistry.getItem(itemId);
        ItemStack stack = item != null ? item.getStack() : null;
        return stack != null ? stack.getMaxStackSize() : 64;
    }

    /** Empty slots across hotbar + main inventory (armor is a separate array). */
    public static int countEmptyMainInventorySlots() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.inventory == null) return 0;
        int empty = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack == null || stack.stackSize <= 0) empty++;
        }
        return empty;
    }

    public static void scheduleSignSubmit(GuiEditSign gui) {
        pendingSubmitSign = gui;
        signSubmitAtMs = System.currentTimeMillis() + 200L;
    }

    private static void writeIntoSign(Minecraft mc, int amount) {
        TileEntitySign sign = ((io.hamlook.aetheria.mixins.accessors.GuiEditSignAccessor) mc.currentScreen).ATHR$getTileSign();
        if (sign == null || sign.signText == null || sign.signText.length == 0) return;
        if (!sign.signText[0].getUnformattedText().isEmpty()) return;
        sign.signText[0] = new net.minecraft.util.ChatComponentText(String.valueOf(amount));
    }

    public static boolean isBazaarAmountSign(TileEntitySign sign) {
        if (sign == null || sign.signText == null || sign.signText.length < 4) return false;
        if (!"^^^^^^^^^^^^^^^".equals(text(sign.signText[1]))) return false;
        if (!"Enter amount".equals(text(sign.signText[2]))) return false;
        String purpose = text(sign.signText[3]);
        return purpose.startsWith("to order") || purpose.startsWith("to sell");
    }

    private static String text(net.minecraft.util.IChatComponent component) {
        return component == null ? "" : ColorUtils.stripColor(component.getUnformattedText()).trim();
    }

    public static boolean hiddenAt(int mode) {
        if (!SkyblockData.isOnSkyblock()) return true;
        SkyblockData.Location loc = SkyblockData.getCurrentLocation();
        switch (mode) {
            case 0:
                return loc != SkyblockData.Location.GARDEN;
            case 1:
                return loc != SkyblockData.Location.GARDEN
                        && loc != SkyblockData.Location.BARN
                        && loc != SkyblockData.Location.HUB;
            default:
                return false;
        }
    }

    public static boolean isSearchExpired() {
        return !FarmingApi.isSearchFresh(SEARCH_TTL_MS);
    }

    private static VisitorsConfig config() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.visitors;
    }

    private static LinkedHashMap<String, Integer> previewNeeds() {
        LinkedHashMap<String, Integer> needs = new LinkedHashMap<>();
        needs.put("ENCHANTED_CARROT", 64);
        needs.put("ENCHANTED_POTATO", 128);
        return needs;
    }
}
