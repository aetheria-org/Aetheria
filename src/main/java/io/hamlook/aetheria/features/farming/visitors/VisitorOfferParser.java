package io.hamlook.aetheria.features.farming.visitors;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the "Accept Offer" item lore into a structured offer
 */
public final class VisitorOfferParser {

    private static final Pattern NAME_X_AMOUNT = Pattern.compile("^(.+?) x(\\d+)$");
    private static final Pattern AMOUNT_X_NAME = Pattern.compile("^(\\d+)x (.+)$");
    private static final Pattern COPPER = Pattern.compile("^\\+([\\d,.]+) Copper$");
    private static final Pattern BITS = Pattern.compile("^\\+([\\d,.]+) Bits$");
    private static final Pattern FARMING_XP = Pattern.compile("^\\+([\\d,.]+) Farming (?:XP|Wisdom)$");
    private static final Pattern GARDEN_XP = Pattern.compile("^\\+([\\d,.]+) Garden (?:XP|Experience)$");
    private VisitorOfferParser() {
    }

    public static VisitorOffer parse(List<String> loreLines, IdResolver resolver) {
        VisitorOffer offer = new VisitorOffer();
        String section = null;
        for (String rawLine : loreLines) {
            String line = rawLine.trim();
            if (line.equals("Items Required:")) {
                section = "needs";
                continue;
            }
            if (line.equals("Rewards")) {
                section = "rewards";
                continue;
            }
            if (line.isEmpty() || line.startsWith("You ") || line.startsWith("Click ")) continue;

            Matcher m;
            m = COPPER.matcher(line);
            if (m.matches()) {
                offer.bonuses.add(new VisitorBonus(VisitorBonus.Type.COPPER, amount(m.group(1)), line));
                continue;
            }
            m = BITS.matcher(line);
            if (m.matches()) {
                offer.bonuses.add(new VisitorBonus(VisitorBonus.Type.BITS, amount(m.group(1)), line));
                continue;
            }
            m = FARMING_XP.matcher(line);
            if (m.matches()) {
                offer.bonuses.add(new VisitorBonus(VisitorBonus.Type.FARMING_XP, amount(m.group(1)), line));
                continue;
            }
            m = GARDEN_XP.matcher(line);
            if (m.matches()) {
                offer.bonuses.add(new VisitorBonus(VisitorBonus.Type.GARDEN_XP, amount(m.group(1)), line));
                continue;
            }

            Map<String, Integer> target = "needs".equals(section) ? offer.needs : offer.rewards;

            if (line.startsWith("+")) {
                String bonusName = line.substring(1).trim();
                if (bonusName.isEmpty() || bonusName.matches("^\\d.*")) continue;
                String id = resolver.resolve(bonusName);
                if (id != null) {
                    target.merge(id, 1, Integer::sum);
                } else {
                    trackUnresolved(offer, "+" + bonusName);
                }
                continue;
            }

            String name = null;
            int amount = 0;
            m = NAME_X_AMOUNT.matcher(line);
            if (m.matches()) {
                name = m.group(1).trim();
                amount = intAmount(m.group(2));
            } else {
                m = AMOUNT_X_NAME.matcher(line);
                if (m.matches()) {
                    name = m.group(2).trim();
                    amount = intAmount(m.group(1));
                }
            }
            if (name == null || name.isEmpty() || amount <= 0) continue;
            String id = resolver.resolve(name);
            if (id == null) {
                trackUnresolved(offer, name);
                continue;
            }
            target.merge(id, amount, Integer::sum);
        }
        return offer;
    }

    public static String strip(String s) {
        return s.replaceAll("§.", "");
    }

    public static String lower(String s) {
        return strip(s).trim().toLowerCase(Locale.ROOT);
    }

    private static double amount(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int intAmount(String raw) {
        return (int) amount(raw);
    }

    private static void trackUnresolved(VisitorOffer offer, String name) {
        if (offer.unresolvedNames.size() < 8 && !offer.unresolvedNames.contains(name)) {
            offer.unresolvedNames.add(name);
        }
    }

    public interface IdResolver {
        String resolve(String displayName);
    }
}
