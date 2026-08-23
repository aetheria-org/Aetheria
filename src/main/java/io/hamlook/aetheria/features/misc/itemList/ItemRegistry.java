package io.hamlook.aetheria.features.misc.itemList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.profile.data.ItemData;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.RomanNumeralParser;
import io.hamlook.aetheria.utils.ThreadUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemRegistry {

    private static final Gson GSON = new Gson();
    private static final Pattern LEVEL_SUFFIX = Pattern.compile("^(.+?)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX|\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_RARITY = Pattern.compile("^(.+);(\\d)$");
    private static final Pattern RUNE_RARITY = Pattern.compile("^(.+_RUNE);(\\d)$");
    private static final Pattern ACC_TIER = Pattern.compile("^(.+?)_(TALISMAN|RING|ARTIFACT)$");
    private static final String[] RARITY_NAMES = {"§fCommon", "§aUncommon", "§9Rare", "§5Epic", "§6Legendary", "§dMythic", "§bDivine", "§4Special"};
    public static volatile Map<String, ItemFamily> familyRegistry = new LinkedHashMap<>();
    public static volatile boolean isLoaded = false;
    private static volatile Map<String, SkyblockItem> itemRegistry = new HashMap<>();
    private static volatile Map<String, String> skullTextureCache = Collections.emptyMap();

    public static SkyblockItem getItem(String id) {
        if (id == null) return null;
        return itemRegistry.get(ItemResolver.resolveId(id, null));
    }

    public static SkyblockItem getItem(String id, String displayName) {
        if (id == null) return null;
        return itemRegistry.get(ItemResolver.resolveId(id, displayName));
    }

    public static void initialise() {
        ThreadUtils.run("ATHR-ItemRegistry-Loader", () -> {
            long threadStart = System.currentTimeMillis();
            Aetheria.logger.info("[ATHR-DEBUG] Initialization thread started.");
            String json = RepoHandler.getJson(ATHRRepo.KEY_ITEMDATA);
            if (json == null || json.isEmpty()) {
                Aetheria.logger.info("[ATHR-DEBUG] item data not fetched yet - loads when the repo refresh lands");
                RepoHandler.addListener(ATHRRepo.KEY_ITEMDATA, ItemRegistry::loadFromRepo);
                return;
            }
            loadItems(json, threadStart);
        });
    }

    private static void loadFromRepo() {
        String json = RepoHandler.getJson(ATHRRepo.KEY_ITEMDATA);
        if (json == null || json.isEmpty()) return;
        loadItems(json, System.currentTimeMillis());
    }

    private static void loadItems(String json, long threadStart) {
        try {
            Type type = new TypeToken<Map<String, SkyblockItem>>() {
            }.getType();
            Map<String, SkyblockItem> items;
            long parseStart = System.currentTimeMillis();
            try {
                items = GSON.fromJson(json, type);
            } catch (Exception parseEx) {
                Aetheria.logger.severe("[ATHR-DEBUG] itemData.json failed to parse: " + parseEx.getMessage());
                RepoHandler.addListener(ATHRRepo.KEY_ITEMDATA, ItemRegistry::loadFromRepo);
                RepoHandler.invalidateBody(ATHRRepo.KEY_ITEMDATA);
                RepoHandler.refresh(ATHRRepo.KEY_ITEMDATA);
                return;
            }
            if (items == null || items.isEmpty()) {
                Aetheria.logger.severe("[ATHR-DEBUG] Local JSON parsed to null!");
                return;
            }
            Aetheria.logger.info("[ATHR-DEBUG] GSON parse took " + (System.currentTimeMillis() - parseStart) + "ms.");

            Aetheria.logger.info("[ATHR-DEBUG] Fetched " + items.size() + " items. Starting Multi-Threaded Processing...");

            Map<String, SkyblockItem> tempItemRegistry = new ConcurrentHashMap<>();
            AtomicInteger count = new AtomicInteger(0);
            long loopStart = System.currentTimeMillis();

            items.entrySet().parallelStream().forEach(entry -> {
                String id = entry.getKey();
                SkyblockItem item = entry.getValue();

                if (item.displayName != null && stripColor(item.displayName).trim().equalsIgnoreCase("Enchanted Book") && item.baseLore != null && !item.baseLore.isEmpty()) {
                    String firstLore = item.baseLore.get(0);
                    if (firstLore.trim().length() > 2) {
                        item.displayName = firstLore.trim();
                        List<String> mutableLore = new ArrayList<>(item.baseLore);
                        mutableLore.remove(0);
                        item.baseLore = mutableLore;
                    }
                }

                item.skyblockID = id;
                item.idLower = id.toLowerCase();
                item.cleanNameLower = item.displayName != null ? stripColor(item.displayName).trim().toLowerCase() : item.idLower;

                tempItemRegistry.put(id, item);

                try {
                    item.getStack();
                    parseLoreMeta(item);
                } catch (Exception ex) {
                    Aetheria.logger.severe("[ATHR-DEBUG] Failed to pre-load stack for " + id + ": " + ex.getMessage());
                }

                int currentCount = count.incrementAndGet();
                if (currentCount % 2000 == 0) {
                    Aetheria.logger.info("[ATHR-DEBUG] Processed " + currentCount + " items...");
                }
            });

            Aetheria.logger.info("[ATHR-DEBUG] Parallel processing finished.");

            itemRegistry = tempItemRegistry;
            Aetheria.logger.info("[ATHR-DEBUG] Loaded " + itemRegistry.size() + " items total.");

            buildFamilies();

            Aetheria.logger.info("[ATHR-DEBUG] Building skull texture cache...");
            buildSkullTextureCache();
            Aetheria.logger.info("[ATHR-DEBUG] Skull texture cache built with " + skullTextureCache.size() + " entries.");

            Aetheria.logger.info("[ATHR-DEBUG] --- TOTAL INITIALIZATION TIME: " + (System.currentTimeMillis() - threadStart) + "ms ---");

        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR-DEBUG] Exception loading items: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private static void parseLoreMeta(SkyblockItem item) {
        if (item.baseLore != null && !item.baseLore.isEmpty()) {
            String lastLine = stripColor(item.baseLore.get(item.baseLore.size() - 1)).trim();
            String[] rarities = {"COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL", "VERY SPECIAL"};
            for (String r : rarities) {
                if (lastLine.startsWith(r)) {
                    item.itemRarity = r;
                    item.itemType = lastLine.substring(r.length()).trim();
                    return;
                }
            }
            item.itemType = lastLine;
        }
    }

    private static void buildFamilies() {
        Map<String, ItemFamily> pending = new LinkedHashMap<>();

        for (Map.Entry<String, SkyblockItem> entry : itemRegistry.entrySet()) {
            String id = entry.getKey();
            SkyblockItem item = entry.getValue();

            Matcher petM = PET_RARITY.matcher(id);
            if (petM.matches()) {
                String base = petM.group(1);
                int rarityNum = Integer.parseInt(petM.group(2));
                String famKey = "PET_" + base;
                ItemFamily fam = pending.computeIfAbsent(famKey, k -> new ItemFamily(famKey, stripColor(item.displayName), ItemFamily.FamilyType.PET));
                item.familyId = famKey;
                item.familyMemberLabel = rarityNum < RARITY_NAMES.length ? RARITY_NAMES[rarityNum] : "§f?";
                fam.members.add(item);
                fam.members.sort(Comparator.comparing(i -> i.skyblockID));
                continue;
            }

            Matcher runeM = RUNE_RARITY.matcher(id);
            if (runeM.matches()) {
                String base = runeM.group(1);
                String famKey = "RUNE_" + base;
                ItemFamily fam = pending.computeIfAbsent(famKey, k -> new ItemFamily(famKey, stripColor(item.displayName), ItemFamily.FamilyType.ENCHANTMENT));
                item.familyId = famKey;
                item.familyMemberLabel = "Level " + runeM.group(2);
                fam.members.add(item);
                continue;
            }

            Matcher accM = ACC_TIER.matcher(id);
            if (accM.matches()) {
                String base = accM.group(1);
                String tier = accM.group(2);
                String famKey = "ACC_" + base;
                String cleanName = cleanAccessoryName(stripColor(item.displayName), tier);
                ItemFamily fam = pending.computeIfAbsent(famKey, k -> new ItemFamily(famKey, cleanName, ItemFamily.FamilyType.ACCESSORY));
                item.familyId = famKey;
                item.familyMemberLabel = capFirst(tier.toLowerCase());
                fam.members.add(item);
                fam.members.sort(Comparator.comparingInt(ItemRegistry::accTierOrder));
                continue;
            }

            String cleanName = stripColor(item.displayName != null ? item.displayName : id).trim();
            Matcher levelM = LEVEL_SUFFIX.matcher(cleanName);
            if (levelM.matches()) {
                String baseName = levelM.group(1).trim();
                String level = levelM.group(2).trim();
                String famKey = "ENC_" + baseName.toUpperCase().replaceAll("\\s+", "_");
                ItemFamily fam = pending.computeIfAbsent(famKey, k -> new ItemFamily(famKey, "", ItemFamily.FamilyType.ENCHANTMENT));
                item.familyId = famKey;
                item.familyMemberLabel = level;
                fam.members.add(item);
                fam.members.sort(Comparator.comparingInt(i -> romanToInt(stripColor(i.familyMemberLabel))));
                continue;
            }

            String famKey = "SOLO_" + id;
            ItemFamily fam = new ItemFamily(famKey, item.displayName != null ? item.displayName : id, ItemFamily.FamilyType.NONE);
            item.familyId = famKey;
            item.familyMemberLabel = null;
            fam.members.add(item);
            pending.put(famKey, fam);
        }

        for (ItemFamily fam : pending.values()) {
            if (fam.members.isEmpty()) continue;

            SkyblockItem highest = fam.members.get(fam.members.size() - 1);
            String color = "§f";

            if (fam.type == ItemFamily.FamilyType.ENCHANTMENT) {
                String baseName = toTitleCase(highest.skyblockID.replace("ENCHANTMENT_", "").replaceAll("_\\d+$", ""));
                if (highest.displayName != null && highest.displayName.trim().length() >= 2 && highest.displayName.trim().charAt(0) == '§') {
                    color = highest.displayName.trim().substring(0, 2);
                    baseName = stripRomanNumeral(stripColor(highest.displayName));
                }
                fam.updateDisplayName(color + baseName);
            } else {
                if (fam.type == ItemFamily.FamilyType.NONE) {
                    fam.members.sort(Comparator.comparing(i -> stripColor(i.displayName)));
                }
                if (highest.displayName != null && highest.displayName.trim().length() >= 2 && highest.displayName.trim().charAt(0) == '§') {
                    color = highest.displayName.trim().substring(0, 2);
                }
                fam.updateDisplayName(color + stripColor(fam.displayName).trim());
            }
        }

        familyRegistry = pending;
        isLoaded = true;
        Aetheria.logger.info("[ATHR-DEBUG] Built " + familyRegistry.size() + " item families. Initialization Complete!");
    }

    private static String stripRomanNumeral(String name) {
        if (name == null || name.trim().isEmpty()) return name;
        String clean = name.trim();
        String[] parts = clean.split("\\s+");

        if (parts.length > 1) {
            String lastWord = parts[parts.length - 1].toUpperCase();
            if (lastWord.matches("\\d+") || lastWord.matches("^(I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX)$")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    sb.append(parts[i]);
                    if (i < parts.length - 2) sb.append(" ");
                }
                return sb.toString();
            }
        }
        return clean;
    }

    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char c : input.toCharArray()) {
            if (c == ' ' || c == '_') {
                sb.append(' ');
                capitalize = true;
            } else if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }

    private static String cleanAccessoryName(String name, String tier) {
        String t = capFirst(tier.toLowerCase());
        if (name.endsWith(t)) name = name.substring(0, name.length() - t.length()).trim();
        return name;
    }

    private static String capFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int accTierOrder(SkyblockItem i) {
        if (i.skyblockID.endsWith("_TALISMAN")) return 0;
        if (i.skyblockID.endsWith("_RING")) return 1;
        if (i.skyblockID.endsWith("_ARTIFACT")) return 2;
        return 3;
    }

    private static int romanToInt(String name) {
        String[] parts = name.trim().split("\\s+");
        String r = parts[parts.length - 1].toUpperCase();
        try {
            return Integer.parseInt(r);
        } catch (NumberFormatException e) {
            try {
                return RomanNumeralParser.parse(r);
            } catch (IllegalArgumentException ex) {
                return 99;
            }
        }
    }

    public static Collection<SkyblockItem> getAllItems() {
        return Collections.unmodifiableCollection(itemRegistry.values());
    }

    public static Map<String, String> getSkullTextureMap() {
        return skullTextureCache;
    }

    private static String extractTextureHash(String base64Texture) {
        if (base64Texture == null || base64Texture.isEmpty()) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(base64Texture));
            int ti = decoded.indexOf("/texture/");
            if (ti == -1) return null;
            ti += "/texture/".length();
            StringBuilder hash = new StringBuilder();
            while (ti < decoded.length()) {
                char c = decoded.charAt(ti++);
                if (c == '"' || c == '\'' || c == '\\' || c == '?' || c == ' ') break;
                hash.append(c);
            }
            return hash.length() > 0 ? hash.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void buildSkullTextureCache() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SkyblockItem item : itemRegistry.values()) {
            if (item.itemid != null && item.itemid.contains("skull") && item.texture != null && !item.texture.isEmpty()) {
                String hash = extractTextureHash(item.texture);
                if (hash != null) {
                    String name = stripColor(item.displayName).trim();
                    map.put(name, hash);
                }
            }
        }
        skullTextureCache = map;
    }

    public static SkyblockItem getWithItemData(ItemData data) {
        if (data == null || data.skyblockID == null) return null;
        SkyblockItem base = getItem(data.skyblockID, data.displayName);
        if (base == null) return null;
        SkyblockItem item = base.clone();
        if (data.lore != null && !data.lore.isEmpty()) item.baseLore = data.lore;
        item.enchanted = data.enchanted;
        if (data.displayName != null && !data.displayName.isEmpty()) item.displayName = data.displayName;
        if (data.amount > 0) {
            item.amount = data.amount;
        }
        return item;
    }
}