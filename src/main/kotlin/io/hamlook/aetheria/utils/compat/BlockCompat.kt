package io.hamlook.aetheria.utils.compat

import net.minecraft.block.Block
import net.minecraft.block.BlockColored
import net.minecraft.block.BlockSand
import net.minecraft.block.BlockStone
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

object BlockCompat {

    @JvmStatic
    fun isAir(block: Block): Boolean = block == Blocks.air

    @JvmStatic
    fun isRedstoneOre(block: Block): Boolean = block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore

    @JvmStatic
    fun isStone(state: IBlockState): Boolean =
        state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE

    @JvmStatic
    fun isTitanium(state: IBlockState): Boolean =
        state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.DIORITE_SMOOTH

    @JvmStatic
    fun isSmoothAndesite(state: IBlockState): Boolean =
        state.block == Blocks.stone && state.getValue(BlockStone.VARIANT) == BlockStone.EnumType.ANDESITE_SMOOTH

    @JvmStatic
    fun isRedSand(state: IBlockState): Boolean =
        state.block == Blocks.sand && state.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND

    @JvmStatic
    fun isPrismarine(block: Block): Boolean = block == Blocks.prismarine

    @JvmStatic
    fun isClay(block: Block): Boolean = block == Blocks.clay

    @JvmStatic
    fun isStoneBricks(block: Block): Boolean = block == Blocks.stonebrick

    @JvmStatic
    fun isHardenedClay(block: Block): Boolean = block == Blocks.hardened_clay

    @JvmStatic
    fun isLever(block: Block): Boolean = block == Blocks.lever

    @JvmStatic
    fun isPistonHead(block: Block): Boolean = block == Blocks.piston_head

    @JvmStatic
    fun isStainedHardenedClay(block: Block): Boolean = block == Blocks.stained_hardened_clay

    @JvmStatic
    fun isWoolWithColor(state: IBlockState, color: EnumDyeColor): Boolean =
        state.block == Blocks.wool && state.getValue(BlockColored.COLOR) == color

    @JvmStatic
    fun getBlockName(block: Block): String = Block.blockRegistry.getNameForObject(block).toString()

    @JvmStatic
    fun getLog(variant: Int): ItemStack = ItemStack(Blocks.log, 1, variant)

    @JvmStatic
    fun getLog2(variant: Int): ItemStack = ItemStack(Blocks.log2, 1, variant)

    @JvmStatic
    fun getDoublePlant(variant: Int): ItemStack = ItemStack(Item.getItemFromBlock(Blocks.double_plant), 1, variant)

    @JvmStatic
    fun getRedFlower(variant: Int): ItemStack = ItemStack(Item.getItemFromBlock(Blocks.red_flower), 1, variant)

    @JvmStatic
    fun getLilyPad(): ItemStack = ItemStack(Blocks.waterlily)

    @JvmStatic
    fun getStoneItem(): ItemStack = ItemStack(Blocks.stone)

    @JvmStatic
    fun getStoneBrickItem(): ItemStack = ItemStack(Blocks.stonebrick)

    @JvmStatic
    fun getHardenedClayItem(): ItemStack = ItemStack(Blocks.hardened_clay)
}
