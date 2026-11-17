package io.hamlook.aetheria.features.qol;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
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

        //#if MC < 1.21
        return replaceNameCoded(entity, hideCrit, hideNonCrit, formatDamage);
        //#else
        //$$ return replaceNameStyled(entity, hideCrit, hideNonCrit, formatDamage);
        //#endif
    }

    public static boolean shouldHideNametag(EntityLivingBase entity) {
        if (ATHRConfig.feature == null) return false;
        if (!(entity instanceof EntityArmorStand)) return false;
        if (!entity.hasCustomName()) return false;

        boolean hideCrit = ATHRConfig.feature.qol.damageSplashes.hideCritSplashes;
        boolean hideNonCrit = ATHRConfig.feature.qol.damageSplashes.hideNonCritSplashes;
        if (!hideCrit && !hideNonCrit) return false;

        //#if MC < 1.21
        String text = TextCompat.getFormattedText(entity.getCustomNameTag());
        if (text == null || text.isEmpty()) return false;
        if (!hasDigit(text)) return false;

        if (text.indexOf(STAR) != -1 || text.indexOf(OVERLOAD_STAR) != -1) {
            return hideCrit && (CRIT.matcher(text).matches() || OVERLOAD.matcher(text).matches());
        }
        return hideNonCrit && NO_CRIT.matcher(text).matches();
        //#else
        //$$ TextCompat.StyledText flat = TextCompat.flattenStyled(entity.getCustomName());
        //$$ String text = flat.getPlain();
        //$$ if (text == null || text.isEmpty()) return false;
        //$$ if (!hasDigit(text)) return false;
        //$$
        //$$ if (text.indexOf(STAR) != -1 || text.indexOf(OVERLOAD_STAR) != -1) {
        //$$     return hideCrit && (CLEAN_CRIT.matcher(text).matches() || CLEAN_OVERLOAD.matcher(text).matches())
        //$$             && TextCompat.styledColorRgb(flat, 0) == WHITE;
        //$$ }
        //$$ int rgb = TextCompat.styledColorRgb(flat, 0);
        //$$ return hideNonCrit && (rgb == GRAY || rgb == GOLD || rgb == RED)
        //$$         && CLEAN_NO_CRIT.matcher(text.trim()).matches();
        //#endif
    }

    //#if MC < 1.21
    private static IChatComponent replaceNameCoded(EntityLivingBase entity, boolean hideCrit, boolean hideNonCrit, boolean formatDamage) {
        String text = TextCompat.getFormattedText(entity.getCustomNameTag());
        if (text == null || text.isEmpty()) return entity.getDisplayName();

        boolean hasStar = text.indexOf(STAR) != -1 || text.indexOf(OVERLOAD_STAR) != -1;
        boolean hasDigit = hasDigit(text);

        if (hasStar && hasDigit) {
            Matcher critMatcher = CRIT.matcher(text);

            if (critMatcher.matches()) {
                if (hideCrit) {
                    return TextCompat.createText("");
                } else if (formatDamage) {
                    IChatComponent replacement = formatCritDamage(critMatcher.group(1), S + "f" + STAR, S + "f" + STAR + critMatcher.group(2));
                    if (replacement != null) return replacement;
                }
            } else {
                Matcher overloadMatcher = OVERLOAD.matcher(text);
                if (overloadMatcher.matches()) {
                    if (hideCrit) {
                        return TextCompat.createText("");
                    } else if (formatDamage) {
                        IChatComponent replacement = formatCritDamage(overloadMatcher.group(2), overloadMatcher.group(1) + OVERLOAD_STAR, overloadMatcher.group(3) + OVERLOAD_STAR + S + "r");
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
                        return TextCompat.createText("");
                    } else if (formatDamage) {
                        String suffix = rest != null && rest.startsWith("§r") ? rest : "§r" + (rest != null ? rest : "");
                        IChatComponent replacement = formatNonCritDamage(noCritMatcher.group(2), noCritMatcher.group(1), suffix);
                        if (replacement != null) return replacement;
                    }
                }
            }
        }

        return entity.getDisplayName();
    }
    //#endif

    //#if MC >= 1.21
    // Detection runs on the component's code-free text and compares exact RGB values,
    // so it needs the lossless styled form; a §-string formatter would quantise the
    // colours to the nearest of the 16 legacy ones and break the match.
    //$$ private static final int WHITE = 0xFFFFFF, GRAY = 0xAAAAAA, GOLD = 0xFFAA00, RED = 0xFF5555;
    //$$ private static final int[] CRIT_RGB = {WHITE, 0xFFFF55, GOLD, RED, RED, WHITE};
    //$$ private static final Pattern CLEAN_CRIT = Pattern.compile(STAR + "([0-9][0-9.,kKmMbBtT]*)" + STAR + "(.*)");
    //$$ private static final Pattern CLEAN_OVERLOAD = Pattern.compile(OVERLOAD_STAR + "([0-9][0-9.,kKmMbBtT]*)" + OVERLOAD_STAR + "(.*)");
    //$$ private static final Pattern CLEAN_NO_CRIT = Pattern.compile("([0-9][0-9.,]*[kKmMbBtT]?)");
    //$$
    //$$ private static Text replaceNameStyled(LivingEntity entity, boolean hideCrit, boolean hideNonCrit, boolean formatDamage) {
    //$$     TextCompat.StyledText flat = TextCompat.flattenStyled(entity.getCustomName());
    //$$     String plain = flat.getPlain();
    //$$     if (plain == null || plain.isEmpty()) return entity.getDisplayName();
    //$$
    //$$     boolean hasStar = plain.indexOf(STAR) != -1 || plain.indexOf(OVERLOAD_STAR) != -1;
    //$$     boolean hasDigit = hasDigit(plain);
    //$$
    //$$     if (hasStar && hasDigit) {
    //$$         Matcher crit = CLEAN_CRIT.matcher(plain);
    //$$         Matcher overload = CLEAN_OVERLOAD.matcher(plain);
    //$$         boolean isCrit = crit.matches();
    //$$         if (!isCrit && !overload.matches()) return entity.getDisplayName();
    //$$         if (TextCompat.styledColorRgb(flat, 0) != WHITE) return entity.getDisplayName();
    //$$         if (hideCrit) return TextCompat.createText("");
    //$$         if (!formatDamage) return entity.getDisplayName();
    //$$         Matcher m = isCrit ? crit : overload;
    //$$         Text styled = formatStyledDamage(m.group(1), flat, m.start(), m.end(), m.group(2), isCrit);
    //$$         return styled != null ? styled : entity.getDisplayName();
    //$$     }
    //$$
    //$$     if (!hasDigit) return entity.getDisplayName();
    //$$     Matcher noCrit = CLEAN_NO_CRIT.matcher(plain.trim());
    //$$     if (!noCrit.matches()) return entity.getDisplayName();
    //$$     int rgb = TextCompat.styledColorRgb(flat, 0);
    //$$     if (rgb != GRAY && rgb != GOLD && rgb != RED) return entity.getDisplayName();
    //$$     if (hideNonCrit) return TextCompat.createText("");
    //$$     if (!formatDamage) return entity.getDisplayName();
    //$$     Text styled = formatStyledDamage(noCrit.group(1), flat, 0, noCrit.end(), "", false);
    //$$     return styled != null ? styled : entity.getDisplayName();
    //$$ }
    //$$
    //$$ private static Text formatStyledDamage(String numbers, TextCompat.StyledText flat,
    //$$                                      int numberStart, int numberEnd, String trail, boolean crit) {
    //$$     long damage;
    //$$     try {
    //$$         damage = parseNumber(numbers);
    //$$     } catch (NumberFormatException e) {
    //$$         return null;
    //$$     }
    //$$     if (damage <= 999) return null;
    //$$     String formatted = Utils.shortNumberFormat(damage, 0);
    //$$     Text out = TextCompat.createText("");
    //$$
    //$$     if (!crit) {
    //$$         return TextCompat.appendStyledText(out, formatted, TextCompat.styledColorRgb(flat, numberStart));
    //$$     }
    //$$
    //$$     out = TextCompat.appendStyledText(out, String.valueOf(STAR), TextCompat.styledColorRgb(flat, numberStart - 1));
    //$$     for (int i = 0; i < formatted.length(); i++) {
    //$$         out = TextCompat.appendStyledText(out, String.valueOf(formatted.charAt(i)), CRIT_RGB[i % CRIT_RGB.length]);
    //$$     }
    //$$     out = TextCompat.appendStyledText(out, String.valueOf(STAR), TextCompat.styledColorRgb(flat, numberEnd));
    //$$     if (!trail.isEmpty()) out = TextCompat.appendPlainText(out, trail);
    //$$     return out;
    //$$ }
    //#endif

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

    private static IChatComponent formatCritDamage(String numbersRaw, String prefix, String suffix) {
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

            return TextCompat.fromFormatted(prefix + colored + suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static IChatComponent formatNonCritDamage(String numbersRaw, String prefix, String suffix) {
        try {
            long damage = parseNumber(numbersRaw);
            if (damage <= 999) return null;

            String formatted = Utils.shortNumberFormat(damage, 0);
            return TextCompat.fromFormatted(prefix + formatted + suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
