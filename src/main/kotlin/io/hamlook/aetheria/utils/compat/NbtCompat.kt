package io.hamlook.aetheria.utils.compat

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

/**
 * Low-level NBT bridge for version-agnostic tag access. On 1.8.9 tags use hasKey/getCompoundTag;
 * on 1.21+ they use DataComponentTypes. Feature code should call [ItemUtils] for convenience
 * methods (getInternalName, getLoreLines, etc.); this object provides the raw tag primitives only.
 */
object NbtCompat {

    const val TAG_END: Int = 0
    const val TAG_BYTE: Int = 1
    const val TAG_SHORT: Int = 2
    const val TAG_INT: Int = 3
    const val TAG_LONG: Int = 4
    const val TAG_FLOAT: Int = 5
    const val TAG_DOUBLE: Int = 6
    const val TAG_BYTE_ARRAY: Int = 7
    const val TAG_STRING: Int = 8
    const val TAG_LIST: Int = 9
    const val TAG_COMPOUND: Int = 10
    const val TAG_INT_ARRAY: Int = 11
    const val TAG_ANY_NUMERIC: Int = 99

    @JvmStatic
    fun getTagCompound(item: ItemStack): NBTTagCompound? {
        return if (item.hasTagCompound()) item.tagCompound else null
    }

    @JvmStatic
    fun getExtraAttributes(item: ItemStack): NBTTagCompound? {
        val tag = getTagCompound(item) ?: return null
        return if (tag.hasKey("ExtraAttributes")) tag.getCompoundTag("ExtraAttributes") else null
    }

    @JvmStatic
    fun getDisplayCompound(item: ItemStack): NBTTagCompound? {
        val tag = getTagCompound(item) ?: return null
        return if (tag.hasKey("display")) tag.getCompoundTag("display") else null
    }

    @JvmStatic
    fun hasKey(compound: NBTTagCompound, key: String, type: Int): Boolean {
        return compound.hasKey(key, type)
    }

    @JvmStatic
    fun getTagList(compound: NBTTagCompound, key: String, type: Int): NBTTagList {
        return compound.getTagList(key, type)
    }

    @JvmStatic
    fun containsCompound(compound: NBTTagCompound, key: String): Boolean {
        return compound.hasKey(key, TAG_COMPOUND)
    }

    @JvmStatic
    fun containsList(compound: NBTTagCompound, key: String): Boolean {
        return compound.hasKey(key, TAG_LIST)
    }
}
