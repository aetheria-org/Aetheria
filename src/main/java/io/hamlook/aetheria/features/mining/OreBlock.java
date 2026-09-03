package io.hamlook.aetheria.features.mining;

import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.ColoredBlockCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;

import java.util.function.Predicate;

public enum OreBlock {

    LOW_TIER_MITHRIL(OreBlock::isLowTierMithril, OreBlock::inDwarven, OreCategory.DWARVEN_METAL),
    MID_TIER_MITHRIL(OreBlock::isMidTierMithril, OreBlock::inDwarvenOrCrystal, OreCategory.DWARVEN_METAL),
    HIGH_TIER_MITHRIL(OreBlock::isHighTierMithril, OreBlock::inDwarvenOrCrystal, OreCategory.DWARVEN_METAL),

    TITANIUM(OreBlock::isTitanium, OreBlock::inDwarven, OreCategory.DWARVEN_METAL),

    STONE(OreBlock::isStone, OreBlock::inDwarven, OreCategory.BLOCK),
    COBBLESTONE(blockEquals(Blocks.cobblestone), OreBlock::inDwarven, OreCategory.BLOCK),
    COAL_ORE(blockEquals(Blocks.coal_ore), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    IRON_ORE(blockEquals(Blocks.iron_ore), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    GOLD_ORE(blockEquals(Blocks.gold_ore), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    LAPIS_ORE(blockEquals(Blocks.lapis_ore), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    REDSTONE_ORE(OreBlock::isRedstoneOre, OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    EMERALD_ORE(blockEquals(Blocks.emerald_ore), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    DIAMOND_ORE(blockEquals(Blocks.diamond_ore), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),

    NETHERRACK(blockEquals(Blocks.netherrack), OreBlock::inCrimson, OreCategory.BLOCK),
    QUARTZ_ORE(blockEquals(Blocks.quartz_ore), OreBlock::inCrimsonOrCrystal, OreCategory.ORE),
    GLOWSTONE(blockEquals(Blocks.glowstone), OreBlock::inCrimson, OreCategory.BLOCK),
    MYCELIUM(blockEquals(Blocks.mycelium), OreBlock::inCrimson, OreCategory.BLOCK),
    RED_SAND(OreBlock::isRedSand, OreBlock::inCrimson, OreCategory.BLOCK),
    SULPHUR(blockEquals(Blocks.sponge), OreBlock::inCrimson, OreCategory.ORE),

    GRAVEL(blockEquals(Blocks.gravel), OreBlock::inSpidersDen, OreCategory.BLOCK, false),

    END_STONE(blockEquals(Blocks.end_stone), OreBlock::inEnd, OreCategory.BLOCK),
    OBSIDIAN(blockEquals(Blocks.obsidian), OreBlock::inCrystalOrEnd, OreCategory.ORE),

    HARD_STONE_HOLLOWS(OreBlock::isHardStoneHollows, OreBlock::inCrystal, OreCategory.BLOCK),

    PURE_COAL(blockEquals(Blocks.coal_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    PURE_IRON(blockEquals(Blocks.iron_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE, false),
    PURE_GOLD(blockEquals(Blocks.gold_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE, false),
    PURE_LAPIS(blockEquals(Blocks.lapis_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),
    PURE_REDSTONE(blockEquals(Blocks.redstone_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE, false),
    PURE_EMERALD(blockEquals(Blocks.emerald_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE, false),
    PURE_DIAMOND(blockEquals(Blocks.diamond_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE, false),
    PURE_QUARTZ(blockEquals(Blocks.quartz_block), OreBlock::inDwarvenOrCrystal, OreCategory.ORE),

    RUBY(gemstoneCheck(EnumDyeColor.RED), OreBlock::inCrystal, OreCategory.GEMSTONE),
    AMBER(gemstoneCheck(EnumDyeColor.ORANGE), OreBlock::inCrystal, OreCategory.GEMSTONE),
    AMETHYST(gemstoneCheck(EnumDyeColor.PURPLE), OreBlock::inCrystal, OreCategory.GEMSTONE),
    JADE(gemstoneCheck(EnumDyeColor.LIME), OreBlock::inCrystal, OreCategory.GEMSTONE),
    SAPPHIRE(gemstoneCheck(EnumDyeColor.LIGHT_BLUE), OreBlock::inCrystal, OreCategory.GEMSTONE),
    TOPAZ(gemstoneCheck(EnumDyeColor.YELLOW), OreBlock::inCrystal, OreCategory.GEMSTONE),
    JASPER(gemstoneCheck(EnumDyeColor.MAGENTA), OreBlock::inCrystal, OreCategory.GEMSTONE),
    OPAL(gemstoneCheck(EnumDyeColor.WHITE), OreBlock::inCrimson, OreCategory.GEMSTONE);

    public final Predicate<IBlockState> checkBlock;
    public final java.util.function.Supplier<Boolean> checkArea;
    public final OreCategory category;
    public final boolean hasInitSound;

    OreBlock(Predicate<IBlockState> checkBlock, java.util.function.Supplier<Boolean> checkArea, OreCategory category, boolean hasInitSound) {
        this.checkBlock = checkBlock;
        this.checkArea = checkArea;
        this.category = category;
        this.hasInitSound = hasInitSound;
    }

    OreBlock(Predicate<IBlockState> checkBlock, java.util.function.Supplier<Boolean> checkArea, OreCategory category) {
        this(checkBlock, checkArea, category, true);
    }

    public static OreBlock getByStateOrNull(IBlockState state) {
        for (OreBlock ore : MiningApi.getCurrentAreaOreBlocks()) {
            if (ore.checkBlock.test(state)) return ore;
        }
        return null;
    }

    private static Predicate<IBlockState> blockEquals(Block block) {
        return state -> state.getBlock() == block;
    }

    private static Predicate<IBlockState> gemstoneCheck(EnumDyeColor color) {
        return state -> {
            Block block = state.getBlock();
            if (block == Blocks.stained_glass) {
                return ColoredBlockCompat.isStainedGlass(state, color);
            }
            if (block == Blocks.stained_glass_pane) {
                return ColoredBlockCompat.isStainedGlassPane(state, color);
            }
            return false;
        };
    }

    private static SkyblockData.Location loc() {
        return SkyblockData.getCurrentLocation();
    }

    private static boolean inDwarven() { return loc() == SkyblockData.Location.DWARVEN; }
    private static boolean inCrystal() { return loc() == SkyblockData.Location.CRYSTAL_HOLLOWS; }
    private static boolean inCrimson() { return loc() == SkyblockData.Location.CRIMSON_ISLE; }
    private static boolean inEnd() { return loc() == SkyblockData.Location.THE_END; }
    private static boolean inSpidersDen() { return loc() == SkyblockData.Location.SPIDERS_DEN; }

    private static boolean inDwarvenOrCrystal() { return inDwarven() || inCrystal(); }
    private static boolean inCrimsonOrCrystal() { return inCrimson() || inCrystal(); }
    private static boolean inCrystalOrEnd() { return inCrystal() || inEnd(); }

    private static boolean isLowTierMithril(IBlockState state) {
        if (BlockCompat.isWoolWithColor(state, EnumDyeColor.GRAY)) return true;
        if (BlockCompat.isStainedHardenedClay(state.getBlock()) && ColoredBlockCompat.isStainedClay(state, EnumDyeColor.CYAN)) return true;
        return false;
    }

    private static boolean isMidTierMithril(IBlockState state) {
        return BlockCompat.isPrismarine(state.getBlock());
    }

    private static boolean isHighTierMithril(IBlockState state) {
        return BlockCompat.isWoolWithColor(state, EnumDyeColor.LIGHT_BLUE);
    }

    private static boolean isTitanium(IBlockState state) {
        return BlockCompat.isTitanium(state);
    }

    static boolean isTitaniumBlock(IBlockState state) {
        return isTitanium(state);
    }

    private static boolean isStone(IBlockState state) {
        return BlockCompat.isStone(state);
    }

    private static boolean isRedstoneOre(IBlockState state) {
        return BlockCompat.isRedstoneOre(state.getBlock());
    }

    private static boolean isRedSand(IBlockState state) {
        return BlockCompat.isRedSand(state);
    }

    private static boolean isHardStoneHollows(IBlockState state) {
        Block block = state.getBlock();
        if (BlockCompat.isWoolWithColor(state, EnumDyeColor.GRAY) || BlockCompat.isWoolWithColor(state, EnumDyeColor.GREEN)) {
            return true;
        }
        if (BlockCompat.isStainedHardenedClay(block)) {
            EnumDyeColor color = state.getValue(net.minecraft.block.BlockColored.COLOR);
            switch (color) {
                case CYAN: case BROWN: case GRAY: case BLACK:
                case LIME: case GREEN: case BLUE: case RED: case SILVER:
                    return true;
                default:
                    return false;
            }
        }
        return BlockCompat.isClay(block) || BlockCompat.isStoneBricks(block) || block == Blocks.stone;
    }

    public enum OreCategory {
        BLOCK,
        ORE,
        DWARVEN_METAL,
        GEMSTONE
    }
}
