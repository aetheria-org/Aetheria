package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
//#else
//$$ import net.minecraft.item.ItemStack
//$$ import net.minecraft.nbt.CompoundTag
//$$ import net.minecraft.nbt.ListTag
//#endif
//#if MC > 1.21
//$$ import net.minecraft.component.DataComponentTypes
//#endif
//#if MC < 1.21
import net.minecraft.item.ItemStack
import net.minecraft.nbt.JsonToNBT
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

//#else
//$$ import net.minecraft.nbt.StringNbtReader
//$$ import com.mojang.brigadier.exceptions.CommandSyntaxException
//#endif

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
        //#if MC < 1.16
        return if (item.hasTagCompound()) item.tagCompound else null
        //#elseif MC < 1.21
        //$$ return item.tag
        //#else
        //$$ return item.get(DataComponentTypes.CUSTOM_DATA)?.copyNbt()
        //#endif
    }

    @JvmStatic
    fun getExtraAttributes(item: ItemStack): NBTTagCompound? {
        //#if MC < 1.16
        val tag = getTagCompound(item) ?: return null
        return if (tag.hasKey("ExtraAttributes")) tag.getCompoundTag("ExtraAttributes") else null
        //#elseif MC < 1.21
        //$$ val tag = item.tag ?: return null
        //$$ return if (tag.contains("ExtraAttributes")) tag.getCompound("ExtraAttributes") else null
        //#else
        //$$ val tag = getTagCompound(item) ?: return null
        //$$ return if (tag.contains("ExtraAttributes")) tag.getCompound("ExtraAttributes") else null
        //#endif
    }

    @JvmStatic
    fun getDisplayCompound(item: ItemStack): NBTTagCompound? {
        //#if MC < 1.16
        val tag = getTagCompound(item) ?: return null
        return if (tag.hasKey("display")) tag.getCompoundTag("display") else null
        //#elseif MC < 1.21
        //$$ val tag = item.tag ?: return null
        //$$ return if (tag.contains("display")) tag.getCompound("display") else null
        //#else
        //$$ val tag = getTagCompound(item) ?: return null
        //$$ return if (tag.contains("display")) tag.getCompound("display") else null
        //#endif
    }

    @JvmStatic
    fun hasKey(compound: NBTTagCompound, key: String, type: Int): Boolean {
        //#if MC < 1.16
        return compound.hasKey(key, type)
        //#else
        //$$ return compound.contains(key, type)
        //#endif
    }

    @JvmStatic
    fun getTagList(compound: NBTTagCompound, key: String, type: Int): NBTTagList {
        //#if MC < 1.16
        return compound.getTagList(key, type)
        //#else
        //$$ return compound.getList(key, type)
        //#endif
    }

    @JvmStatic
    fun containsCompound(compound: NBTTagCompound, key: String): Boolean {
        //#if MC < 1.16
        return compound.hasKey(key, TAG_COMPOUND)
        //#elseif MC < 1.21
        //$$ return compound.contains(key, TAG_COMPOUND)
        //#else
        //$$ return compound.contains(key) && compound.get(key) is NbtCompound
        //#endif
    }

    @JvmStatic
    fun containsList(compound: NBTTagCompound, key: String): Boolean {
        //#if MC < 1.16
        return compound.hasKey(key, TAG_LIST)
        //#elseif MC < 1.21
        //$$ return compound.contains(key, TAG_LIST)
        //#else
        //$$ return compound.contains(key) && compound.get(key) is NbtList
        //#endif
    }

    @JvmStatic
    fun parseTagFromJson(data: String): NBTTagCompound? =
        //#if MC < 1.21
        try {
            JsonToNBT.getTagFromJson(data)
        } catch (e: Exception) {
            null
        }
    //#else
    //$$ try {
    //$$     StringNbtReader.parse(data)
    //$$ } catch (e: CommandSyntaxException) {
    //$$     null
    //$$ }
    //#endif
}

//#if MC > 1.21
//$$ fun NbtCompound.getStringOrDefault(key: String): String {
//$$     return this.getString(key)
//$$ }
//$$
//$$ fun NbtCompound.getIntOrDefault(key: String): Int {
//$$     return this.getInt(key)
//$$ }
//$$
//$$ fun NbtCompound.getLongOrDefault(key: String): Long {
//$$     return this.getLong(key)
//$$ }
//$$
//$$ fun NbtCompound.getFloatOrDefault(key: String): Float {
//$$     return this.getFloat(key)
//$$ }
//$$
//$$ fun NbtCompound.getDoubleOrDefault(key: String): Double {
//$$     return this.getDouble(key)
//$$ }
//$$
//$$ fun NbtCompound.getBooleanOrDefault(key: String): Boolean {
//$$     return this.getBoolean(key)
//$$ }
//$$
//$$ fun NbtCompound.getByteOrDefault(key: String): Byte? {
//$$     return this.getByte(key)
//$$ }
//$$
//$$ fun NbtCompound.getCompoundOrDefault(key: String): NbtCompound {
//$$     if (this.getCompound(key).isEmpty) {
//$$         return NbtCompound()
//$$     }
//$$     return this.getCompound(key)
//$$ }
//$$
//$$ fun NbtList.getStringOrDefault(index: Int): String {
//$$     return this.getString(index)
//$$ }
//$$
//$$ fun NbtList.getCompoundOrDefault(index: Int): NbtCompound {
//$$     return this.getCompound(index)
//$$ }
//#endif
