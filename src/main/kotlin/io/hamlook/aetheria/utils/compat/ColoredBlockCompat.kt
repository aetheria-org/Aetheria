package io.hamlook.aetheria.utils.compat
//#if MC < 1.16
import net.minecraft.block.BlockColored
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
//#else
//$$ import net.minecraft.world.item.DyeColor
//$$ import net.minecraft.world.level.block.Blocks
//$$ import net.minecraft.world.level.block.state.BlockState
//$$ import net.minecraft.world.item.ItemStack
//#endif

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

    @JvmOverloads fun createGlassStack(amount: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.stained_glass, amount, metaColor)
        //#else
        //$$ ItemStack(getGlassBlock().asItem(), amount)
        //#endif

    @JvmOverloads fun createGlassPaneStack(amount: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.stained_glass_pane, amount, metaColor)
        //#else
        //$$ ItemStack(getGlassPaneBlock().asItem(), amount)
        //#endif

    @JvmOverloads fun createWoolStack(amount: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.wool, amount, metaColor)
        //#else
        //$$ ItemStack(getWoolBlock().asItem(), amount)
        //#endif

    @JvmOverloads fun createStainedClay(amount: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.stained_hardened_clay, amount, metaColor)
        //#else
        //$$ ItemStack(getStainedClayBlock().asItem(), amount)
        //#endif

    //#if MC < 1.16
    fun getDyeColor(): EnumDyeColor =
    //#else
    //$$ fun getDyeColor(): DyeColor =
    //#endif
        //#if MC < 1.16
        EnumDyeColor.values().firstOrNull { it.ordinal == metaColor } ?: EnumDyeColor.WHITE
        //#else
        //$$ DyeColor.values().firstOrNull { it.id == metaColor } ?: DyeColor.WHITE
        //#endif

    //#if MC > 1.16
    //$$ private fun getGlassBlock() = when (this) {
    //$$     WHITE -> Blocks.WHITE_STAINED_GLASS
    //$$     ORANGE -> Blocks.ORANGE_STAINED_GLASS
    //$$     MAGENTA -> Blocks.MAGENTA_STAINED_GLASS
    //$$     LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS
    //$$     YELLOW -> Blocks.YELLOW_STAINED_GLASS
    //$$     LIME -> Blocks.LIME_STAINED_GLASS
    //$$     PINK -> Blocks.PINK_STAINED_GLASS
    //$$     GRAY -> Blocks.GRAY_STAINED_GLASS
    //$$     LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS
    //$$     CYAN -> Blocks.CYAN_STAINED_GLASS
    //$$     PURPLE -> Blocks.PURPLE_STAINED_GLASS
    //$$     BLUE -> Blocks.BLUE_STAINED_GLASS
    //$$     BROWN -> Blocks.BROWN_STAINED_GLASS
    //$$     GREEN -> Blocks.GREEN_STAINED_GLASS
    //$$     RED -> Blocks.RED_STAINED_GLASS
    //$$     BLACK -> Blocks.BLACK_STAINED_GLASS
    //$$ }
    //$$
    //$$ private fun getGlassPaneBlock() = when (this) {
    //$$     WHITE -> Blocks.WHITE_STAINED_GLASS_PANE
    //$$     ORANGE -> Blocks.ORANGE_STAINED_GLASS_PANE
    //$$     MAGENTA -> Blocks.MAGENTA_STAINED_GLASS_PANE
    //$$     LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE
    //$$     YELLOW -> Blocks.YELLOW_STAINED_GLASS_PANE
    //$$     LIME -> Blocks.LIME_STAINED_GLASS_PANE
    //$$     PINK -> Blocks.PINK_STAINED_GLASS_PANE
    //$$     GRAY -> Blocks.GRAY_STAINED_GLASS_PANE
    //$$     LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS_PANE
    //$$     CYAN -> Blocks.CYAN_STAINED_GLASS_PANE
    //$$     PURPLE -> Blocks.PURPLE_STAINED_GLASS_PANE
    //$$     BLUE -> Blocks.BLUE_STAINED_GLASS_PANE
    //$$     BROWN -> Blocks.BROWN_STAINED_GLASS_PANE
    //$$     GREEN -> Blocks.GREEN_STAINED_GLASS_PANE
    //$$     RED -> Blocks.RED_STAINED_GLASS_PANE
    //$$     BLACK -> Blocks.BLACK_STAINED_GLASS_PANE
    //$$ }
    //$$
    //$$ private fun getWoolBlock() = when (this) {
    //$$     WHITE -> Blocks.WHITE_WOOL
    //$$     ORANGE -> Blocks.ORANGE_WOOL
    //$$     MAGENTA -> Blocks.MAGENTA_WOOL
    //$$     LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL
    //$$     YELLOW -> Blocks.YELLOW_WOOL
    //$$     LIME -> Blocks.LIME_WOOL
    //$$     PINK -> Blocks.PINK_WOOL
    //$$     GRAY -> Blocks.GRAY_WOOL
    //$$     LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL
    //$$     CYAN -> Blocks.CYAN_WOOL
    //$$     PURPLE -> Blocks.PURPLE_WOOL
    //$$     BLUE -> Blocks.BLUE_WOOL
    //$$     BROWN -> Blocks.BROWN_WOOL
    //$$     GREEN -> Blocks.GREEN_WOOL
    //$$     RED -> Blocks.RED_WOOL
    //$$     BLACK -> Blocks.BLACK_WOOL
    //$$ }
    //$$
    //$$ private fun getStainedClayBlock() = when (this) {
    //$$     WHITE -> Blocks.WHITE_TERRACOTTA
    //$$     ORANGE -> Blocks.ORANGE_TERRACOTTA
    //$$     MAGENTA -> Blocks.MAGENTA_TERRACOTTA
    //$$     LIGHT_BLUE -> Blocks.LIGHT_BLUE_TERRACOTTA
    //$$     YELLOW -> Blocks.YELLOW_TERRACOTTA
    //$$     LIME -> Blocks.LIME_TERRACOTTA
    //$$     PINK -> Blocks.PINK_TERRACOTTA
    //$$     GRAY -> Blocks.GRAY_TERRACOTTA
    //$$     LIGHT_GRAY -> Blocks.LIGHT_GRAY_TERRACOTTA
    //$$     CYAN -> Blocks.CYAN_TERRACOTTA
    //$$     PURPLE -> Blocks.PURPLE_TERRACOTTA
    //$$     BLUE -> Blocks.BLUE_TERRACOTTA
    //$$     BROWN -> Blocks.BROWN_TERRACOTTA
    //$$     GREEN -> Blocks.GREEN_TERRACOTTA
    //$$     RED -> Blocks.RED_TERRACOTTA
    //$$     BLACK -> Blocks.BLACK_TERRACOTTA
    //$$ }
    //#endif

    companion object {
        //#if MC < 1.16
        @JvmStatic fun isStainedGlass(state: IBlockState, color: EnumDyeColor? = null): Boolean {
        //#else
        //$$ @JvmStatic fun isStainedGlass(state: BlockState, color: DyeColor? = null): Boolean {
        //#endif
            //#if MC < 1.16
            if (state.block != Blocks.stained_glass) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
            //#else
            //$$ val block = state.block
            //$$ val glassBlocks = listOf(
            //$$     Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS,
            //$$     Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
            //$$     Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS,
            //$$     Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
            //$$     Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS,
            //$$     Blocks.BLACK_STAINED_GLASS
            //$$ )
            //$$ if (block !in glassBlocks) return false
            //$$ color ?: return true
            //$$ return glassBlocks.indexOf(block) == color.id
            //#endif
        }

        //#if MC < 1.16
        @JvmStatic fun isStainedGlassPane(state: IBlockState, color: EnumDyeColor? = null): Boolean {
        //#else
        //$$ @JvmStatic fun isStainedGlassPane(state: BlockState, color: DyeColor? = null): Boolean {
        //#endif
            //#if MC < 1.16
            if (state.block != Blocks.stained_glass_pane) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
            //#else
            //$$ val block = state.block
            //$$ val paneBlocks = listOf(
            //$$     Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS_PANE,
            //$$     Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE,
            //$$     Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
            //$$     Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE,
            //$$     Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE,
            //$$     Blocks.BLACK_STAINED_GLASS_PANE
            //$$ )
            //$$ if (block !in paneBlocks) return false
            //$$ color ?: return true
            //$$ return paneBlocks.indexOf(block) == color.id
            //#endif
        }

        //#if MC < 1.16
        @JvmStatic fun isWool(state: IBlockState, color: EnumDyeColor? = null): Boolean {
        //#else
        //$$ @JvmStatic fun isWool(state: BlockState, color: DyeColor? = null): Boolean {
        //#endif
            //#if MC < 1.16
            if (state.block != Blocks.wool) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
            //#else
            //$$ val block = state.block
            //$$ val woolBlocks = listOf(
            //$$     Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL,
            //$$     Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL,
            //$$     Blocks.PINK_WOOL, Blocks.GRAY_WOOL, Blocks.LIGHT_GRAY_WOOL,
            //$$     Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL,
            //$$     Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL,
            //$$     Blocks.BLACK_WOOL
            //$$ )
            //$$ if (block !in woolBlocks) return false
            //$$ color ?: return true
            //$$ return woolBlocks.indexOf(block) == color.id
            //#endif
        }

        //#if MC < 1.16
        @JvmStatic fun isStainedClay(state: IBlockState, color: EnumDyeColor? = null): Boolean {
        //#else
        //$$ @JvmStatic fun isStainedClay(state: BlockState, color: DyeColor? = null): Boolean {
        //#endif
            //#if MC < 1.16
            if (state.block != Blocks.stained_hardened_clay) return false
            color ?: return true
            return state.getValue(BlockColored.COLOR) == color
            //#else
            //$$ val block = state.block
            //$$ val clayBlocks = listOf(
            //$$     Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
            //$$     Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
            //$$     Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
            //$$     Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            //$$     Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA,
            //$$     Blocks.BLACK_TERRACOTTA
            //$$ )
            //$$ if (block !in clayBlocks) return false
            //$$ color ?: return true
            //$$ return clayBlocks.indexOf(block) == color.id
            //#endif
        }

        //#if MC < 1.16
        @JvmStatic fun stainedClayColorOf(state: IBlockState): ColoredBlockCompat? {
        //#else
        //$$ @JvmStatic fun stainedClayColorOf(state: BlockState): ColoredBlockCompat? {
        //#endif
            //#if MC < 1.16
            if (state.block != Blocks.stained_hardened_clay) return null
            val color = state.getValue(BlockColored.COLOR)
            return values().firstOrNull { it.getDyeColor() == color }
            //#else
            //$$ return values().firstOrNull { it.getStainedClayBlock() == state.block }
            //#endif
        }

        @JvmStatic fun fromMeta(meta: Int): ColoredBlockCompat =
            values().firstOrNull { it.metaColor == meta } ?: WHITE
    }
}
