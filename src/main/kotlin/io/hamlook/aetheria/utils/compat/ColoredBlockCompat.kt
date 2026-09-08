package io.hamlook.aetheria.utils.compat

import net.minecraft.block.BlockColored
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack

enum class ColoredBlockCompat(
    val metaColor: Int,
) {
    WHITE(0),
    ORANGE(1),
    MAGENTA(2),
    LIGHT_BLUE(3),
    YELLOW(4),
    LIME(5),
    PINK(6),
    GRAY(7),
    LIGHT_GRAY(8),
    CYAN(9),
    PURPLE(10),
    BLUE(11),
    BROWN(12),
    GREEN(13),
    RED(14),
    BLACK(15);

    @JvmOverloads fun createGlassStack(amount: Int = 1): ItemStack = ItemStack(Blocks.stained_glass, amount, metaColor)

    @JvmOverloads fun createGlassPaneStack(amount: Int = 1): ItemStack = ItemStack(Blocks.stained_glass_pane, amount, metaColor)

    @JvmOverloads fun createWoolStack(amount: Int = 1): ItemStack = ItemStack(Blocks.wool, amount, metaColor)

    @JvmOverloads fun createStainedClay(amount: Int = 1): ItemStack = ItemStack(Blocks.stained_hardened_clay, amount, metaColor)

    fun getDyeColor(): EnumDyeColor = EnumDyeColor.values().firstOrNull { it.ordinal == metaColor } ?: EnumDyeColor.WHITE

    companion object {
        @JvmStatic fun isStainedGlass(state: IBlockState, color: EnumDyeColor? = null): Boolean {
            if (state.block != Blocks.stained_glass) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
        }

        @JvmStatic fun isStainedGlassPane(state: IBlockState, color: EnumDyeColor? = null): Boolean {
            if (state.block != Blocks.stained_glass_pane) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
        }

        @JvmStatic fun isWool(state: IBlockState, color: EnumDyeColor? = null): Boolean {
            if (state.block != Blocks.wool) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
        }

        @JvmStatic fun isStainedClay(state: IBlockState, color: EnumDyeColor? = null): Boolean {
            if (state.block != Blocks.stained_hardened_clay) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
        }

        @JvmStatic fun fromMeta(meta: Int): ColoredBlockCompat =
            values().firstOrNull { it.metaColor == meta } ?: WHITE
    }
}
