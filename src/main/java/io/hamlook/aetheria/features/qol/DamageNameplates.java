package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DamageNameplates {

    private static final char STAR = '✧';
    private static final char OVERLOAD_STAR = '✯';
    private static final char S = '§';

    private static final Pattern CRIT = Pattern.compile(S + "f" + STAR + "((?:" + S + ".[0-9.,kKmMbBtT])+?)" + S + "." + STAR + "(.*)");
    private static final Pattern OVERLOAD = Pattern.compile("(" + S + ".)" + OVERLOAD_STAR + "((?:" + S + ".[0-9.,kKmMbBtT])+)(" + S + ".)" + OVERLOAD_STAR + S + "r");
    private static final Pattern NO_CRIT = Pattern.compile("(" + S + ".)([0-9,.]+[kKmMbBtT]?)(.*)");

    private static final EnumChatFormatting[] CRIT_COLORS = {EnumChatFormatting.WHITE, EnumChatFormatting.YELLOW, EnumChatFormatting.GOLD, EnumChatFormatting.RED, EnumChatFormatting.RED, EnumChatFormatting.WHITE};

    public static IChatComponent replaceName(EntityLivingBase entity) {
        if (ATHRConfig.feature == null) return entity.getDisplayName();
        if (!(entity instanceof EntityArmorStand)) return entity.getDisplayName();
        if (!entity.hasCustomName()) return entity.getDisplayName();

        boolean hideCrit = ATHRConfig.feature.qol.damageSplashes.hideCritSplashes;
        boolean hideNonCrit = ATHRConfig.feature.qol.damageSplashes.hideNonCritSplashes;
        boolean formatDamage = ATHRConfig.feature.qol.damageSplashes.formatDamage;

        if (!hideCrit && !hideNonCrit && !formatDamage) return entity.getDisplayName();

        String text = entity.getCustomNameTag();
        if (text == null || text.isEmpty()) return entity.getDisplayName();

        boolean hasStar = text.indexOf(STAR) != -1 || text.indexOf(OVERLOAD_STAR) != -1;
        boolean hasDigit = hasDigit(text);

        if (hasStar && hasDigit) {
            Matcher critMatcher = CRIT.matcher(text);

            if (critMatcher.matches()) {
                if (hideCrit) {
                    return new ChatComponentText("");
                } else if (formatDamage) {
                    ChatComponentText replacement = formatCritDamage(critMatcher.group(1), S + "f" + STAR, S + "f" + STAR + critMatcher.group(2));
                    if (replacement != null) return replacement;
                }
            } else {
                Matcher overloadMatcher = OVERLOAD.matcher(text);
                if (overloadMatcher.matches()) {
                    if (hideCrit) {
                        return new ChatComponentText("");
                    } else if (formatDamage) {
                        ChatComponentText replacement = formatCritDamage(overloadMatcher.group(2), overloadMatcher.group(1) + OVERLOAD_STAR, overloadMatcher.group(3) + OVERLOAD_STAR + S + "r");
                        if (replacement != null) return replacement;
                    }
                }
            }
        } else if (hasDigit) {
            Matcher noCritMatcher = NO_CRIT.matcher(text);
            if (noCritMatcher.matches()) {
                char code = noCritMatcher.group(1).length() >= 2 ? noCritMatcher.group(1).charAt(1) : '\0';
                boolean gray = code == '7';
                boolean fire = code == '6' || code == 'c' || code == 'C';
                String rest = noCritMatcher.group(3);
                boolean cleanEnd = rest == null || rest.isEmpty() || rest.equals("§r");

                if ((gray || fire) && cleanEnd) {
                    if (hideNonCrit) {
                        return new ChatComponentText("");
                    } else if (formatDamage) {
                        String suffix = rest != null && rest.startsWith("§r") ? rest : "§r" + (rest != null ? rest : "");
                        ChatComponentText replacement = formatNonCritDamage(noCritMatcher.group(2), noCritMatcher.group(1), suffix);
                        if (replacement != null) return replacement;
                    }
                }
            }
        }

        return entity.getDisplayName();
    }

    private static boolean hasDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') return true;
        }
        return false;
    }

    private static long parseNumber(String text) {
        text = text.toUpperCase().replace(",", "");
        long multiplier = 1;

        if (text.endsWith("K")) {
            multiplier = 1_000L;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("M")) {
            multiplier = 1_000_000L;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("B")) {
            multiplier = 1_000_000_000L;
            text = text.substring(0, text.length() - 1);
        } else if (text.endsWith("T")) {
            multiplier = 1_000_000_000_000L;
            text = text.substring(0, text.length() - 1);
        }

        return (long) (Double.parseDouble(text) * multiplier);
    }

    private static ChatComponentText formatCritDamage(String numbersRaw, String prefix, String suffix) {
        String numbers = StringUtils.cleanColour(numbersRaw);

        try {
            long damage = parseNumber(numbers);
            if (damage <= 999) return null;

            String formatted = Utils.shortNumberFormat(damage, 0);
            StringBuilder colored = new StringBuilder();
            int colorIndex = 0;

            for (int i = 0; i < formatted.length(); i++) {
                char c = formatted.charAt(i);
                colored.append(CRIT_COLORS[colorIndex++ % CRIT_COLORS.length]);
                colored.append(c);
            }

            return new ChatComponentText(prefix + colored + suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ChatComponentText formatNonCritDamage(String numbersRaw, String prefix, String suffix) {
        try {
            long damage = parseNumber(numbersRaw);
            if (damage <= 999) return null;

            String formatted = Utils.shortNumberFormat(damage, 0);
            return new ChatComponentText(prefix + formatted + suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}