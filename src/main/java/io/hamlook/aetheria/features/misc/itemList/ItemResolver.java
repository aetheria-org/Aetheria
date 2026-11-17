package io.hamlook.aetheria.features.misc.itemList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemResolver {

    private static final Pattern LEVEL_SUFFIX = Pattern.compile("^(.+?)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X)$", Pattern.CASE_INSENSITIVE);

    public static String resolveId(String skyblockId, String displayName) {
        if (skyblockId == null) return null;
        if (displayName == null) return skyblockId;

        String resolved = skyblockId;
        String cleanName = stripColor(displayName).trim();

        // Remove "PET_" prefix if present, as requested in previous instructions
        if (resolved.startsWith("PET_") && !resolved.startsWith("PET_ITEM_")) {
            resolved = resolved.replaceFirst("PET_", "");
        }

        if (skyblockId.equals("ANCIENT_CHARM")) {
            int rarity = getRarityFromColor(displayName);
            if (rarity != -1) {
                resolved = "ANCIENT_CHARM;" + rarity;
            }
        }
        else if (skyblockId.endsWith("_RUNE")) {
            String baseId = skyblockId;
            if (baseId.equals("BLOOD_RUNE")) {
                baseId = "BLOOD_2_RUNE";
            }
            
            Matcher m = LEVEL_SUFFIX.matcher(cleanName);
            if (m.matches()) {
                int level = romanToInt(m.group(2));
                resolved = baseId + ";" + level;
            } else {
                resolved = baseId;
            }
        }
        // Pet items and skins - the user mentioned them, but didn't give explicit rules.
        // I will add a debug print for all items to help them.
        
        // Trophy Fish Resolution
        if (cleanName.endsWith(" BRONZE") || cleanName.endsWith(" SILVER") || cleanName.endsWith(" GOLD") || cleanName.endsWith(" DIAMOND")) {
            String[] parts = cleanName.split("\\s+");
            String tier = parts[parts.length - 1]; // This will be BRONZE, SILVER, GOLD, or DIAMOND
            String fishName = cleanName.substring(0, cleanName.length() - tier.length()).trim().toUpperCase().replace(" ", "_");
            resolved = fishName + "_" + tier;
        }

        // Dungeon Potion Resolution
        if (skyblockId.equals("DUNGEON_POTION")) {
            String[] parts = cleanName.split("\\s+");
            for (String part : parts) {
                if (part.matches("^(I|II|III|IV|V|VI|VII|VIII|IX|X)$")) {
                    int level = romanToInt(part);
                    resolved = "POTION_DUNGEON;" + level;
                    break;
                }
            }
        }

        return resolved;
    }

    private static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }

    private static int getRarityFromColor(String name) {
        if (name == null || name.isEmpty()) return -1;
        String lastColor = "f";
        for (int i = 0; i < name.length() - 1; i++) {
            if (name.charAt(i) == '§' || name.charAt(i) == '&') {
                char c = Character.toLowerCase(name.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) != -1) {
                    lastColor = String.valueOf(c);
                }
            }
        }
        switch (lastColor) {
            case "f": return 0; // Common
            case "a": return 1; // Uncommon
            case "9": return 2; // Rare
            case "5": return 3; // Epic
            case "6": return 4; // Legendary
            case "d": return 5; // Mythic
            case "b": return 6; // Divine
            case "c": return 7; // Special
            default: return -1;
        }
    }

    private static int romanToInt(String r) {
        switch (r.toUpperCase()) {
            case "X": return 10;
            case "IX": return 9;
            case "VIII": return 8;
            case "VII": return 7;
            case "VI": return 6;
            case "V": return 5;
            case "IV": return 4;
            case "III": return 3;
            case "II": return 2;
            default: return 1;
        }
    }
}
