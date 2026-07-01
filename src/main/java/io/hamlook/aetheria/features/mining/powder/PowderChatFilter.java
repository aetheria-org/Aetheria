package io.hamlook.aetheria.features.mining.powder;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.mining.PowderMiningChatFilterConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatFilter;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class PowderChatFilter {

    private static final Pattern CHEST_UNCOVERED = Pattern.compile("You uncovered a treasure chest!");
    private static final Pattern ALREADY_LOOTED = Pattern.compile("This chest has already been looted\\.");
    private static final Pattern BREAKING_POWER = Pattern.compile("You need a tool with a Breaking Power of .+? to mine .+");
    private static final Pattern COMPACT = Pattern.compile("COMPACT! You found .+");

    private static final Pattern GEMSTONE_POWDER = Pattern.compile("Gemstone Powder x([\\d,]+)");
    private static final Pattern MITHRIL_POWDER = Pattern.compile("Mithril Powder x([\\d,]+)");
    private static final Pattern DIAMOND_ESSENCE = Pattern.compile("Diamond Essence x([\\d,]+)");
    private static final Pattern GOLD_ESSENCE = Pattern.compile("Gold Essence x([\\d,]+)");

    private static final Pattern GEMSTONE_DROP = Pattern.compile("\\S (Rough|Flawed|Fine|Flawless) " + "(Ruby|Sapphire|Amber|Amethyst|Jade|Topaz|Jasper|Opal|Citrine|Aquamarine|Peridot|Onyx) " + "Gemstone x([\\d,]+)");

    private static final Pattern OIL_BARREL = Pattern.compile("Oil Barrel(?: x([\\d,]+))?");
    private static final Pattern ASCENSION_ROPE = Pattern.compile("Ascension Rope(?: x([\\d,]+))?");
    private static final Pattern WISHING_COMPASS = Pattern.compile("Wishing Compass(?: x([\\d,]+))?");
    private static final Pattern JUNGLE_HEART = Pattern.compile("Jungle Heart(?: x([\\d,]+))?");
    private static final Pattern PREHISTORIC_EGG = Pattern.compile("Prehistoric Egg(?: x([\\d,]+))?");
    private static final Pattern PICKONIMBUS = Pattern.compile("Pickonimbus 2000(?: x([\\d,]+))?");
    private static final Pattern SLUDGE_JUICE = Pattern.compile("Sludge Juice(?: x([\\d,]+))?");
    private static final Pattern YOGGIE = Pattern.compile("Yoggie(?: x([\\d,]+))?");

    private static final Pattern ROBOT_PARTS = Pattern.compile("(?:FTX 3070|Synthetic Heart|Control Switch|Robotron Reflector|Electron Transmitter|Superlite Motor)" + "(?: x([\\d,]+))?");
    private static final Pattern TREASURITE = Pattern.compile("Treasurite(?: x([\\d,]+))?");

    private static final Pattern GOBLIN_EGG = Pattern.compile("([a-zA-Z]+)? ?Goblin Egg(?: x([\\d,]+))?");

    private static final Pattern CHEST_WRAPPER = Pattern.compile("§r§[ed].*▬+");
    private static final Pattern LOCK_PICKED = Pattern.compile("§r§6§l.*CHEST LOCKPICKED");
    private static final Pattern LOOT_COLLECTED = Pattern.compile("§r§5§l.*LOOT CHEST COLLECTED");
    private static final Pattern REWARD_HEADER = Pattern.compile("§r§[af]§l.*REWARDS");

    public PowderChatFilter() {
        ChatFilter.hide("mining.powderChat", msg -> {
            if (ATHRConfig.feature == null) return false;

            PowderMiningChatFilterConfig cfg = ATHRConfig.feature.mining.powderMiningChat;
            if (!cfg.enabled) return false;

            if (SkyblockData.getCurrentLocation() != SkyblockData.Location.CRYSTAL_HOLLOWS) return false;

            String clean = StringUtils.stripControlCodes(msg).trim();
            if (clean.isEmpty()) return true;
            if (clean.contains("PRISTINE")) return false;

            if (ChatUtils.isPartyMessage(clean) || ChatUtils.isPlayerMessage(clean) || ChatUtils.isMsgReceived(clean) || ChatUtils.isMsgSent(clean) || ChatUtils.isDonateMessage(clean))
                return false;

            if (CHEST_WRAPPER.matcher(msg).find() || LOCK_PICKED.matcher(msg).find()
                || LOOT_COLLECTED.matcher(msg).find() || REWARD_HEADER.matcher(msg).find()) {
                return cfg.hideRewardWrappers;
            }

            if (CHEST_UNCOVERED.matcher(clean).find()) return cfg.hideChestOpen;
            if (ALREADY_LOOTED.matcher(clean).find()) return cfg.hideChestAlreadyLooted;
            if (BREAKING_POWER.matcher(clean).find()) return cfg.hideBreakingPower;

            if (COMPACT.matcher(clean).find()) return cfg.hideCompact;

            Matcher m;

            m = GEMSTONE_POWDER.matcher(clean);
            if (m.find()) {
                if (!cfg.hidePowder) return false;
                int amount = parseAmount(m.group(1));
                return cfg.powderThreshold == 0 || amount < cfg.powderThreshold;
            }

            m = MITHRIL_POWDER.matcher(clean);
            if (m.find()) {
                if (!cfg.hidePowder) return false;
                int amount = parseAmount(m.group(1));
                return cfg.powderThreshold == 0 || amount < cfg.powderThreshold;
            }

            m = DIAMOND_ESSENCE.matcher(clean);
            if (m.find()) {
                if (!cfg.hideEssence) return false;
                int amount = parseAmount(m.group(1));
                return cfg.essenceThreshold == 0 || amount < cfg.essenceThreshold;
            }

            m = GOLD_ESSENCE.matcher(clean);
            if (m.find()) {
                if (!cfg.hideEssence) return false;
                int amount = parseAmount(m.group(1));
                return cfg.essenceThreshold == 0 || amount < cfg.essenceThreshold;
            }

            m = GEMSTONE_DROP.matcher(clean);
            if (m.find()) {
                if (!cfg.hideGemstones) return false;
                String tier = m.group(1);
                if (cfg.gemstoneTierFilter.equals("Show All")) return false;
                if (cfg.gemstoneTierFilter.equals("Hide All")) return true;
                if (cfg.gemstoneTierFilter.equals("Hide Rough") && tier.equals("Rough")) return true;
                return cfg.gemstoneTierFilter.equals("Hide Rough & Flawed") && (tier.equals("Rough") || tier.equals("Flawed"));
            }

            if (OIL_BARREL.matcher(clean).find()) return cfg.hideOilBarrel;
            if (ASCENSION_ROPE.matcher(clean).find()) return cfg.hideAscensionRope;
            if (WISHING_COMPASS.matcher(clean).find()) return cfg.hideWishingCompass;
            if (JUNGLE_HEART.matcher(clean).find()) return cfg.hideJungleHeart;
            if (PREHISTORIC_EGG.matcher(clean).find()) return cfg.hidePrehistoricEgg;
            if (PICKONIMBUS.matcher(clean).find()) return cfg.hidePickonimbus;
            if (SLUDGE_JUICE.matcher(clean).find()) return cfg.hideSludgeJuice;
            if (YOGGIE.matcher(clean).find()) return cfg.hideYoggie;
            if (ROBOT_PARTS.matcher(clean).find()) return cfg.hideRobotParts;
            if (TREASURITE.matcher(clean).find()) return cfg.hideTreasurite;

            if (GOBLIN_EGG.matcher(clean).find()) return cfg.hideGoblinEggs;

            return false;
        });
    }

    private static int parseAmount(String s) {
        if (s == null || s.isEmpty()) return 1;
        try {
            return Integer.parseInt(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
