package io.hamlook.aetheria.features.price;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.features.price.vars.AuctionEntry;
import io.hamlook.aetheria.features.price.vars.BazaarEntry;
import io.hamlook.aetheria.features.price.vars.PriceData;
import io.hamlook.aetheria.features.price.vars.PriceType;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.repo.OtherDataAPI;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class PriceDetector {

    public static boolean scanning = false;

    private static final Map<String, List<BazaarEntry>> bazaarMap = new HashMap<>();
    private static final Map<String, List<AuctionEntry>> auctionMap = new HashMap<>();
    private static final Gson gson = new Gson();
    private static final long DEDUP_INTERVAL_MS = 120_000;
    private static final long REPARSE_COOLDOWN_MS = 1_000;


    private static int tickCounter = 0;
    private static long lastParseTime = 0;
    private static long lastFetchTime = 0;
    private static boolean initialised = false;
    private static int sendIntervalTicks;
    private static long fetchIntervalMs;

    private static boolean shouldAddBazaar(String itemID) {
        List<BazaarEntry> existing = bazaarMap.get(itemID);
        if (existing == null || existing.isEmpty()) return true;
        BazaarEntry last = existing.get(existing.size() - 1);
        return System.currentTimeMillis() - last.timestamp >= DEDUP_INTERVAL_MS;
    }

    public static double parseRawDouble(String raw) {
        String s = raw.trim().replace(",", "");
        if (s.isEmpty()) return 0.0;
        char suffix = Character.toUpperCase(s.charAt(s.length() - 1));
        double multiplier = 1.0;
        if (suffix == 'K') { multiplier = 1_000.0; s = s.substring(0, s.length() - 1); }
        else if (suffix == 'M') { multiplier = 1_000_000.0; s = s.substring(0, s.length() - 1); }
        else if (suffix == 'B') { multiplier = 1_000_000_000.0; s = s.substring(0, s.length() - 1); }
        return Double.parseDouble(s) * multiplier;
    }

    private static long parseDurationToMs(String timeStr) {
        long totalMs = 0;
        Matcher matcher = Pattern.compile("(\\d+)([dhms])").matcher(timeStr.toLowerCase());
        while (matcher.find()) {
            long val = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);
            switch (unit) {
                case 'd': totalMs += val * 24 * 60 * 60 * 1000L; break;
                case 'h': totalMs += val * 60 * 60 * 1000L; break;
                case 'm': totalMs += val * 60 * 1000L; break;
                case 's': totalMs += val * 1000L; break;
            }
        }
        return totalMs;
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        scanning = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;


        if (!initialised) {
            Aetheria.logger.info("[PriceDetector] Starting Initialised Ticking of PriceDetector");
            initialised = true;
            sendIntervalTicks = (int) (OtherDataAPI.getPriceUploadInterval() / 50);
            fetchIntervalMs = OtherDataAPI.getPriceFetchInterval();
            lastFetchTime = System.currentTimeMillis();
            PriceMap.fetch();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastFetchTime >= fetchIntervalMs) {
            Aetheria.logger.info("[PriceDetector] Updating PriceMap");
            lastFetchTime = now;
            PriceMap.fetch();
            Aetheria.logger.info("[PriceDetector] Updated PriceMap");
        } else if (PriceMap.fetchFailCount > 0 && PriceMap.fetchFailCount < PriceMap.MAX_RETRIES && now - lastFetchTime >= 60_000L) {
            lastFetchTime = now;
            PriceMap.fetch();
        }

        tickCounter++;
        if (tickCounter >= sendIntervalTicks) {
            tickCounter = 0;
            sendPrices();
        }
    }

    public static void sendNow() {
        tickCounter = 0;
        sendPrices();
    }

    private static void sendPrices() {
        if(!NetworkGuard.apiAllowed()) return;
        if(!ATHRConfig.feature.misc.itemPriceConfig.enabled
        || !ATHRConfig.feature.misc.itemPriceConfig.sendToDB) return;
        if (bazaarMap.isEmpty() && auctionMap.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        PriceData payload = new PriceData();
        payload.bazaar.putAll(bazaarMap);
        payload.auction.putAll(auctionMap);

        String json = gson.toJson(payload);

        if (mc.thePlayer != null) {
            Aetheria.logger.info("Sending " + bazaarMap.size() + " bazaar and " + auctionMap.size() + " auction entries to API");
        }

        new Thread(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("upload-price"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                Aetheria.logger.info("Sent all prices, response: " + responseCode + " | " + conn.getResponseMessage());
                bazaarMap.clear();
                auctionMap.clear();
                PriceMap.fetch();
            } catch (Exception e) {
                Aetheria.logger.info("Failed to send prices: " + e.getMessage());
            }
        }).start();

        bazaarMap.clear();
        auctionMap.clear();
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (scanning || !ATHRConfig.feature.misc.itemPriceConfig.enabled) return;
        long now = System.currentTimeMillis();
        if (now - lastParseTime < REPARSE_COOLDOWN_MS) return;

        if (!(event.gui instanceof GuiContainer)) return;
        ContainerChest chest = ContainerUtils.getOpenChest(event.gui);
        if (chest == null) return;
        String title = chest.getLowerChestInventory().getName();

        if (title.contains("Auction Browser") && ATHRConfig.feature.misc.itemPriceConfig.auctionEnabled) {
            scanning = true;
            parseAuctionHouse(chest);
            return;
        }
        if(ATHRConfig.feature.misc.itemPriceConfig.bazaarEnabled){
            scanning = parseBZMenus(chest);
            lastParseTime = now;
        }
    }

    private static void addBazaarEntries(List<BazaarEntry> entries) {
        int added = 0;
        for (BazaarEntry entry : entries) {
            if (!shouldAddBazaar(entry.itemID)) continue;
            bazaarMap.computeIfAbsent(entry.itemID, k -> new ArrayList<>()).add(entry);
            added++;
        }
        Aetheria.logger.info("Added " + added + "/" + entries.size() + " items to bazaarMap");
    }

    private static void addAuctionEntries(List<AuctionEntry> entries) {
        for (AuctionEntry entry : entries) {
            auctionMap.computeIfAbsent(entry.itemID, k -> new ArrayList<>()).add(entry);
        }
        Aetheria.logger.info("Added " + entries.size() + " items to auctionMap");
    }

    private static String extractPrice(String s) {
        return s.replaceAll("[^0-9.]", "");
    }

    private static String parseOrderPrice(List<String> lore, String sectionHeader) {
        for (String s : lore) {
            if (s.equals("This item does not support")) return null;
            if (s.startsWith(sectionHeader)) {
                int index = lore.indexOf(s) + 1;
                if (index >= lore.size()) return "";
                String line = lore.get(index);
                if (line.startsWith("-")) {
                    String pricePart = line.contains("|") ? line.substring(0, line.indexOf("|")).trim() : line;
                    String priceS = pricePart.replaceAll("[^0-9.]", "");
                    return priceS.isEmpty() ? "" : priceS;
                }
            }
        }
        return "";
    }

    public static boolean parseBZMenus(ContainerChest chest) {
        List<BazaarEntry> entries = new ArrayList<>();
        String title = chest.getLowerChestInventory().getName();
        long now = System.currentTimeMillis();

        if (title.contains("➜")) {
            int chestSlots = chest.getLowerChestInventory().getSizeInventory();
            for (int i = 0; i < chestSlots; i++) {
                Slot slot = chest.getSlot(i);
                if (slot == null || !slot.getHasStack()) continue;
                ItemStack stack = slot.getStack();
                if (!ItemUtils.isSkyblockItem(stack)) continue;

                List<String> lore = ProfileParser.getLore(stack);
                String buyPrice = "", sellPrice = "";

                for (String s : lore) {
                    if (s.startsWith("Buy price:")) buyPrice = extractPrice(s);
                    if (s.startsWith("Sell price:")) sellPrice = extractPrice(s);
                }
                if (buyPrice.isEmpty() || sellPrice.isEmpty()) continue;

                String internalName = ItemUtils.getEffectiveItemId(stack);
                entries.add(new BazaarEntry(internalName, parseRawDouble(buyPrice), parseRawDouble(sellPrice), -1, -1, PriceType.BAZAAR, now));
            }
        } else {
            Slot slot = chest.getSlot(13);
            if (slot == null || !slot.getHasStack()) return false;
            ItemStack stack = slot.getStack();
            if (!ItemUtils.isSkyblockItem(stack)) return false;

            String internalName = ItemUtils.getEffectiveItemId(stack);

            Slot iBuy = chest.getSlot(10);
            Slot iSell = chest.getSlot(11);
            Slot buyOffer = chest.getSlot(15);
            Slot sellOffer = chest.getSlot(16);
            if (buyOffer == null || sellOffer == null || !buyOffer.getHasStack() || !sellOffer.getHasStack()) return false;
            if (iBuy == null || iSell == null || !iBuy.getHasStack() || !iSell.getHasStack()) return false;

            ItemStack buy = buyOffer.getStack();
            ItemStack sell = sellOffer.getStack();
            ItemStack iBuyStack = iBuy.getStack();
            ItemStack iSellStack = iSell.getStack();
            if (buy == null || sell == null || iBuyStack == null || iSellStack == null) return false;

            List<String> oBuyLore = ProfileParser.getLore(buy);
            String oBuyPrice = parseOrderPrice(oBuyLore, "Top Orders");
            if (oBuyPrice == null) return false;

            List<String> oSellLore = ProfileParser.getLore(sell);
            String oSellPrice = parseOrderPrice(oSellLore, "Top Offers");
            if (oSellPrice == null) return false;

            String iBuyPrice = "", iSellPrice = "";
            for (String s : ProfileParser.getLore(iBuyStack)) {
                if (s.startsWith("Price per unit:") || s.startsWith("Price pet unit:")) iBuyPrice = extractPrice(s);
            }
            for (String s : ProfileParser.getLore(iSellStack)) {
                if (s.startsWith("Price per unit:") || s.startsWith("Price pet unit:")) iSellPrice = extractPrice(s);
            }
            if (iBuyPrice.isEmpty() || iSellPrice.isEmpty()) return false;
            if (oBuyPrice.isEmpty() && oSellPrice.isEmpty()) return false;

            entries.add(new BazaarEntry(internalName, parseRawDouble(iBuyPrice), parseRawDouble(iSellPrice), parseRawDouble(oBuyPrice), parseRawDouble(oSellPrice), PriceType.BZ_WITH_OFFER, now));
        }

        if (!entries.isEmpty()) addBazaarEntries(entries);
        return !entries.isEmpty();
    }

    public static void parseAuctionHouse(ContainerChest chest) {
        List<AuctionEntry> entries = new ArrayList<>();
        int totalSlots = chest.getInventory().size();
        long now = System.currentTimeMillis();

        for (int i = 0; i < totalSlots; i++) {
            Slot slot = chest.getSlot(i);
            if (slot == null || !slot.getHasStack()) continue;
            ItemStack stack = slot.getStack();
            if (!ItemUtils.isSkyblockItem(stack)) continue;

            String price = "";
            PriceType type = PriceType.AUCTION;
            String internalName = ItemUtils.getEffectiveItemId(stack);
            List<String> formattedLore = new ArrayList<>();
            JsonObject extraAttributes = new JsonObject();

            String playerUsername = "";
            long expireTime = 0;

            if (stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();

                if (tag.hasKey("display", 10)) {
                    NBTTagList loreList = tag.getCompoundTag("display").getTagList("Lore", 8);
                    boolean cutLore = false;

                    for (int j = 0; j < loreList.tagCount(); j++) {
                        String rawLine = loreList.getStringTagAt(j);
                        String stripped = ColorUtils.stripColor(rawLine);

                        if (!cutLore) {
                            if (stripped.contains("-----------------")) {
                                cutLore = true;
                            }else{
                                formattedLore.add(rawLine);
                            }
                        }

                        if (stripped.startsWith("Buy it now:")) {
                            price = extractPrice(stripped);
                            type = PriceType.BIN;
                        } else if (stripped.startsWith("Starting bid:") || stripped.startsWith("Top bid:")) {
                            price = extractPrice(stripped);
                        } else if (stripped.startsWith("Seller: ")) {
                            String sellerContent = stripped.substring("Seller: ".length()).trim();
                            String[] parts = sellerContent.split(" ");
                            playerUsername = parts[parts.length - 1];
                        } else if (stripped.startsWith("Ends in: ")) {
                            String timeStr = stripped.substring("Ends in: ".length()).trim();
                            expireTime = now + parseDurationToMs(timeStr);
                        }
                    }
                }

                if (tag.hasKey("ExtraAttributes", 10)) {
                    extraAttributes = nbtToJson(tag.getCompoundTag("ExtraAttributes"));
                }
            }

            if (price.isEmpty()) continue;

            entries.add(new AuctionEntry(internalName, formattedLore, extraAttributes, parseRawDouble(price), type, now, expireTime, playerUsername));
        }

        if (!entries.isEmpty()) addAuctionEntries(entries);
    }

    private static JsonObject nbtToJson(NBTTagCompound compound) {
        JsonObject json = new JsonObject();
        for (String key : compound.getKeySet()) {
            NBTBase base = compound.getTag(key);
            byte id = base.getId();

            if (id == 1) json.addProperty(key, ((NBTTagByte) base).getByte());
            else if (id == 2) json.addProperty(key, ((NBTTagShort) base).getShort());
            else if (id == 3) json.addProperty(key, ((NBTTagInt) base).getInt());
            else if (id == 4) json.addProperty(key, ((NBTTagLong) base).getLong());
            else if (id == 5) json.addProperty(key, ((NBTTagFloat) base).getFloat());
            else if (id == 6) json.addProperty(key, ((NBTTagDouble) base).getDouble());
            else if (id == 8) json.addProperty(key, ((NBTTagString) base).getString());
            else if (id == 10) json.add(key, nbtToJson((NBTTagCompound) base));
            else if (id == 9) {
                JsonArray arr = new JsonArray();
                NBTTagList list = (NBTTagList) base;
                for (int i = 0; i < list.tagCount(); i++) {
                    if (list.getTagType() == 8) arr.add(new JsonPrimitive(list.getStringTagAt(i)));
                }
                json.add(key, arr);
            }
        }
        return json;
    }

}