package io.hamlook.aetheria.utils.compat
//#if MC < 1.16
import net.minecraft.block.Block
import net.minecraft.block.BlockColored
import net.minecraft.block.BlockSand
import net.minecraft.block.BlockStone
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
//#else
//$$ import net.minecraft.registry.Registries
//$$ import net.minecraft.world.item.DyeColor
//$$ import net.minecraft.world.level.block.Block
//$$ import net.minecraft.world.level.block.Blocks
//$$ import net.minecraft.world.level.block.state.BlockState
//$$ import net.minecraft.world.item.ItemStack
//$$ import net.minecraft.world.item.Items
//#endif
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
//#if MC < 1.16
//#else
//$$ import net.minecraft.util.hit.BlockHitResult
//#endif

object BlockCompat {

    @JvmStatic
    fun getHitBlockPos(target: MovingObjectPosition): BlockPos =
        //#if MC < 1.16
        target.getBlockPos()
        //#else
        //$$ (target as BlockHitResult).getBlockPos()
        //#endif

    @JvmStatic
    fun isBlockHit(target: MovingObjectPosition): Boolean =
        //#if MC < 1.16
        target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
        //#else
        //$$ target.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
        //#endif

    @JvmStatic
    fun getSelectionAABB(pos: BlockPos): AxisAlignedBB {
        //#if MC < 1.16
        val world = MinecraftCompat.getLocalWorld()
        val x = pos.getX().toDouble()
        val y = pos.getY().toDouble()
        val z = pos.getZ().toDouble()
        if (world == null) {
            return AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1)
        }
        val block = world.getBlockState(pos).getBlock()
        val aabb = block.getSelectedBoundingBox(world, pos)
        return aabb ?: AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1)
        //#else
        //$$ val x = pos.getX().toDouble()
        //$$ val y = pos.getY().toDouble()
        //$$ val z = pos.getZ().toDouble()
        //$$ val full = Box(x, y, z, x + 1, y + 1, z + 1)
        //$$ val world = MinecraftCompat.getLocalWorld() ?: return full
        //$$ val state = world.getBlockState(pos)
        //$$ val shape = state.getOutlineShape(world, pos, net.minecraft.block.ShapeContext.absent())
        //$$ if (shape.isEmpty) return full
        //$$ val b = shape.boundingBox
        //$$ return Box(x + b.minX, y + b.minY, z + b.minZ, x + b.maxX, y + b.maxY, z + b.maxZ)
        //#endif
    }

    @JvmStatic
    fun isAir(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.air
        //#else
        //$$ block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR
        //#endif

    @JvmStatic
    fun isRedstoneOre(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore
        //#else
        //$$ block == Blocks.REDSTONE_ORE
        //#endif

    @JvmStatic
    fun isStone(state: IBlockState): Boolean =
        //#if MC < 1.16
        state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE
        //#else
        //$$ state.block == Blocks.STONE
        //#endif

    @JvmStatic
    fun isStoneFamily(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.stone
        //#else
        //$$ block == Blocks.STONE || block == Blocks.DIORITE || block == Blocks.ANDESITE ||
        //$$     block == Blocks.GRANITE || block == Blocks.POLISHED_DIORITE ||
        //$$     block == Blocks.POLISHED_ANDESITE || block == Blocks.POLISHED_GRANITE
        //#endif

    @JvmStatic
    fun isTitanium(state: IBlockState): Boolean =
        //#if MC < 1.16
        state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH
        //#else
        //$$ state.block == Blocks.POLISHED_DIORITE
        //#endif

    @JvmStatic
    fun isSmoothAndesite(state: IBlockState): Boolean =
        //#if MC < 1.16
        state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.ANDESITE_SMOOTH
        //#else
        //$$ state.block == Blocks.POLISHED_ANDESITE
        //#endif

    @JvmStatic
    fun isRedSand(state: IBlockState): Boolean =
        //#if MC < 1.16
        state.block == Blocks.sand && state.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND
        //#else
        //$$ state.block == Blocks.RED_SAND
        //#endif

    @JvmStatic
    fun isPrismarine(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.prismarine
        //#else
        //$$ block == Blocks.PRISMARINE
        //#endif

    @JvmStatic
    fun isClay(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.clay
        //#else
        //$$ block == Blocks.CLAY
        //#endif

    @JvmStatic
    fun isStoneBricks(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.stonebrick
        //#else
        //$$ block == Blocks.STONE_BRICKS
        //#endif

    @JvmStatic
    fun isHardenedClay(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.hardened_clay
        //#else
        //$$ block == Blocks.TERRACOTTA
        //#endif

    @JvmStatic
    fun isLever(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.lever
        //#else
        //$$ block == Blocks.LEVER
        //#endif

    @JvmStatic
    fun isPistonHead(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.piston_head
        //#else
        //$$ block == Blocks.PISTON_HEAD
        //#endif

    @JvmStatic
    fun isStainedHardenedClay(block: Block): Boolean =
        //#if MC < 1.16
        block == Blocks.stained_hardened_clay
        //#else
        //$$ block in listOf(
        //$$     Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
        //$$     Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
        //$$     Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
        //$$     Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
        //$$     Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA,
        //$$     Blocks.BLACK_TERRACOTTA
        //$$ )
        //#endif

    @JvmStatic
    fun isWoolWithColor(state: IBlockState, color: EnumDyeColor): Boolean =
        //#if MC < 1.16
        state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == color
        //#else
        //$$ state.block == getWoolBlock(color)
        //#endif

    @JvmStatic
    fun getBlockName(block: Block): String =
        //#if MC < 1.16
        Block.blockRegistry.getNameForObject(block).toString()
        //#else
        //$$ Registries.BLOCK.getId(block).toString()
        //#endif

    @JvmStatic
    fun distanceSq(a: BlockPos, b: BlockPos): Double =
        //#if MC < 1.16
        a.distanceSq(b)
        //#else
        //$$ a.getSquaredDistance(b)
        //#endif

    @JvmStatic
    fun getLog(variant: Int): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.log, 1, variant)
        //#else
        //$$ ItemStack(getLogBlock(variant))
        //#endif

    @JvmStatic
    fun getLog2(variant: Int): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.log2, 1, variant)
        //#else
        //$$ ItemStack(getLog2Block(variant))
        //#endif

    @JvmStatic
    fun getDoublePlant(variant: Int): ItemStack =
        //#if MC < 1.16
        ItemStack(Item.getItemFromBlock(Blocks.double_plant), 1, variant)
        //#else
        //$$ ItemStack(getDoublePlantBlock(variant))
        //#endif

    @JvmStatic
    fun getRedFlower(variant: Int): ItemStack =
        //#if MC < 1.16
        ItemStack(Item.getItemFromBlock(Blocks.red_flower), 1, variant)
        //#else
        //$$ ItemStack(getRedFlowerBlock(variant))
        //#endif

    @JvmStatic
    fun getLilyPad(): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.waterlily)
        //#else
        //$$ ItemStack(Blocks.LILY_PAD)
        //#endif

    @JvmStatic
    fun getStoneItem(): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.stone)
        //#else
        //$$ ItemStack(Blocks.STONE)
        //#endif

    @JvmStatic
    fun getStoneBrickItem(): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.stonebrick)
        //#else
        //$$ ItemStack(Blocks.STONE_BRICKS)
        //#endif

    @JvmStatic
    fun getHardenedClayItem(): ItemStack =
        //#if MC < 1.16
        ItemStack(Blocks.hardened_clay)
        //#else
        //$$ ItemStack(Blocks.TERRACOTTA)
        //#endif

    //#if MC > 1.16
    //$$ private fun getWoolBlock(color: DyeColor): Block = when (color) {
    //$$     DyeColor.WHITE -> Blocks.WHITE_WOOL
    //$$     DyeColor.ORANGE -> Blocks.ORANGE_WOOL
    //$$     DyeColor.MAGENTA -> Blocks.MAGENTA_WOOL
    //$$     DyeColor.LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL
    //$$     DyeColor.YELLOW -> Blocks.YELLOW_WOOL
    //$$     DyeColor.LIME -> Blocks.LIME_WOOL
    //$$     DyeColor.PINK -> Blocks.PINK_WOOL
    //$$     DyeColor.GRAY -> Blocks.GRAY_WOOL
    //$$     DyeColor.LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL
    //$$     DyeColor.CYAN -> Blocks.CYAN_WOOL
    //$$     DyeColor.PURPLE -> Blocks.PURPLE_WOOL
    //$$     DyeColor.BLUE -> Blocks.BLUE_WOOL
    //$$     DyeColor.BROWN -> Blocks.BROWN_WOOL
    //$$     DyeColor.GREEN -> Blocks.GREEN_WOOL
    //$$     DyeColor.RED -> Blocks.RED_WOOL
    //$$     DyeColor.BLACK -> Blocks.BLACK_WOOL
    //$$ }
    //$$
    //$$ private fun getLogBlock(variant: Int): Block = when (variant) {
    //$$     0 -> Blocks.OAK_LOG
    //$$     1 -> Blocks.SPRUCE_LOG
    //$$     2 -> Blocks.BIRCH_LOG
    //$$     3 -> Blocks.JUNGLE_LOG
    //$$     else -> Blocks.OAK_LOG
    //$$ }
    //$$
    //$$ private fun getLog2Block(variant: Int): Block = when (variant) {
    //$$     0 -> Blocks.ACACIA_LOG
    //$$     1 -> Blocks.DARK_OAK_LOG
    //$$     else -> Blocks.OAK_LOG
    //$$ }
    //$$
    //$$ private fun getDoublePlantBlock(variant: Int): Block = when (variant) {
    //$$     0 -> Blocks.SUNFLOWER
    //$$     1 -> Blocks.LILAC
    //$$     2 -> Blocks.TALL_GRASS
    //$$     3 -> Blocks.LARGE_FERN
    //$$     4 -> Blocks.ROSE_BUSH
    //$$     5 -> Blocks.PEONY
    //$$     else -> Blocks.SUNFLOWER
    //$$ }
    //$$
    //$$ private fun getRedFlowerBlock(variant: Int): Block = when (variant) {
    //$$     0 -> Blocks.POPPY
    //$$     1 -> Blocks.BLUE_ORCHID
    //$$     2 -> Blocks.ALLIUM
    //$$     3 -> Blocks.AZURE_BLUET
    //$$     4 -> Blocks.RED_TULIP
    //$$     5 -> Blocks.ORANGE_TULIP
    //$$     6 -> Blocks.WHITE_TULIP
    //$$     7 -> Blocks.PINK_TULIP
    //$$     8 -> Blocks.OXEYE_DAISY
    //$$     else -> Blocks.POPPY
    //$$ }
    //#endif
}
