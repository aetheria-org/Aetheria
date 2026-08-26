package io.hamlook.aetheria.features.mining;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum OreType {

    MITHRIL("Mithril", "MITHRIL_ORE",
        OreBlock.LOW_TIER_MITHRIL, OreBlock.MID_TIER_MITHRIL, OreBlock.HIGH_TIER_MITHRIL),
    TITANIUM("Titanium", "TITANIUM_ORE", OreBlock.TITANIUM),
    COBBLESTONE("Cobblestone", "COBBLESTONE", OreBlock.STONE, OreBlock.COBBLESTONE),
    COAL("Coal", "COAL", OreBlock.COAL_ORE, OreBlock.PURE_COAL),
    IRON("Iron", "IRON_INGOT", OreBlock.IRON_ORE, OreBlock.PURE_IRON),
    GOLD("Gold", "GOLD_INGOT", OreBlock.GOLD_ORE, OreBlock.PURE_GOLD),
    LAPIS("Lapis Lazuli", "INK_SACK-4", OreBlock.LAPIS_ORE, OreBlock.PURE_LAPIS),
    REDSTONE("Redstone", "REDSTONE", OreBlock.REDSTONE_ORE, OreBlock.PURE_REDSTONE),
    EMERALD("Emerald", "EMERALD", OreBlock.EMERALD_ORE, OreBlock.PURE_EMERALD),
    DIAMOND("Diamond", "DIAMOND", OreBlock.DIAMOND_ORE, OreBlock.PURE_DIAMOND),
    NETHERRACK("Netherrack", "NETHERRACK", OreBlock.NETHERRACK),
    QUARTZ("Nether Quartz", "QUARTZ", OreBlock.QUARTZ_ORE),
    GLOWSTONE("Glowstone", "GLOWSTONE_DUST", OreBlock.GLOWSTONE),
    MYCELIUM("Mycelium", "MYCEL", OreBlock.MYCELIUM),
    RED_SAND("Red Sand", "SAND-1", OreBlock.RED_SAND),
    SULPHUR("Sulphur", "SULPHUR_ORE", OreBlock.SULPHUR),
    GRAVEL("Gravel", "GRAVEL", OreBlock.GRAVEL),
    END_STONE("End Stone", "ENDER_STONE", OreBlock.END_STONE),
    OBSIDIAN("Obsidian", "OBSIDIAN", OreBlock.OBSIDIAN),
    HARD_STONE("Hard Stone", "HARD_STONE", OreBlock.HARD_STONE_HOLLOWS),
    RUBY("Ruby", "ROUGH_RUBY_GEM", OreBlock.RUBY),
    AMBER("Amber", "ROUGH_AMBER_GEM", OreBlock.AMBER),
    AMETHYST("Amethyst", "ROUGH_AMETHYST_GEM", OreBlock.AMETHYST),
    JADE("Jade", "ROUGH_JADE_GEM", OreBlock.JADE),
    SAPPHIRE("Sapphire", "ROUGH_SAPPHIRE_GEM", OreBlock.SAPPHIRE),
    TOPAZ("Topaz", "ROUGH_TOPAZ_GEM", OreBlock.TOPAZ),
    JASPER("Jasper", "ROUGH_JASPER_GEM", OreBlock.JASPER),
    OPAL("Opal", "ROUGH_OPAL_GEM", OreBlock.OPAL);

    private static final Set<OreType> GEMSTONES;
    private static final Set<OreType> LOW_TIER_GEMSTONES;

    static {
        GEMSTONES = Collections.unmodifiableSet(EnumSet.of(
            RUBY, AMBER, AMETHYST, JADE, SAPPHIRE, TOPAZ, JASPER, OPAL));
        LOW_TIER_GEMSTONES = Collections.unmodifiableSet(EnumSet.of(
            RUBY, AMBER, AMETHYST, JADE, SAPPHIRE));
    }

    public final String oreName;
    public final String internalName;
    private final OreBlock[] oreBlocks;

    OreType(String oreName, String internalName, OreBlock... oreBlocks) {
        this.oreName = oreName;
        this.internalName = internalName;
        this.oreBlocks = oreBlocks;
    }

    public Set<OreBlock> getOreBlocks() {
        return Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(oreBlocks)));
    }

    public boolean isGemstone() {
        return GEMSTONES.contains(this);
    }

    public boolean isLowTierGemstone() {
        return LOW_TIER_GEMSTONES.contains(this);
    }

    public boolean isHighTierGemstone() {
        return isGemstone() && !isLowTierGemstone();
    }

    public static OreType getOreType(OreBlock oreBlock) {
        for (OreType type : values()) {
            for (OreBlock block : type.oreBlocks) {
                if (block == oreBlock) return type;
            }
        }
        return null;
    }
}
