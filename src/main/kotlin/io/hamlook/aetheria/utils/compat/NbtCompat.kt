package io.hamlook.aetheria.utils.compat

//#if MC < 1.16
//#else
//$$ import net.minecraft.item.ItemStack
//$$ import net.minecraft.nbt.CompoundTag
//$$ import net.minecraft.nbt.ListTag
//$$ import net.minecraft.nbt.NbtByte
//$$ import net.minecraft.nbt.NbtDouble
//$$ import net.minecraft.nbt.NbtElement
//$$ import net.minecraft.nbt.NbtFloat
//$$ import net.minecraft.nbt.NbtInt
//$$ import net.minecraft.nbt.NbtLong
//$$ import net.minecraft.nbt.NbtShort
//$$ import net.minecraft.nbt.NbtString
//#endif
//#if MC > 1.21
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.NbtComponent
//$$ import net.minecraft.registry.RegistryWrapper
//#endif
//#if MC < 1.21
import net.minecraft.item.ItemStack
import net.minecraft.nbt.JsonToNBT
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagDouble
import net.minecraft.nbt.NBTTagFloat
import net.minecraft.nbt.NBTTagInt
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagLong
import net.minecraft.nbt.NBTTagShort
import net.minecraft.nbt.NBTTagString

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
    fun tagCount(list: NBTTagList): Int =
        //#if MC < 1.16
        list.tagCount()
        //#else
        //$$ list.size
        //#endif

    @JvmStatic
    fun getStringAt(list: NBTTagList, index: Int): String =
        //#if MC < 1.16
        list.getStringTagAt(index)
        //#else
        //$$ list.getString(index)
        //#endif

    @JvmStatic
    fun getCompoundAt(list: NBTTagList, index: Int): NBTTagCompound =
        //#if MC < 1.16
        list.getCompoundTagAt(index)
        //#else
        //$$ list.getCompound(index)
        //#endif

    @JvmStatic
    fun hasKey(compound: NBTTagCompound, key: String): Boolean =
        //#if MC < 1.16
        compound.hasKey(key)
        //#else
        //$$ compound.contains(key)
        //#endif

    @JvmStatic
    fun getCompound(compound: NBTTagCompound, key: String): NBTTagCompound =
        //#if MC < 1.16
        compound.getCompoundTag(key)
        //#else
        //$$ compound.getCompound(key)
        //#endif

    @JvmStatic
    fun getString(compound: NBTTagCompound, key: String): String =
        //#if MC < 1.16
        compound.getString(key)
        //#else
        //$$ compound.getString(key)
        //#endif

    @JvmStatic
    fun getInt(compound: NBTTagCompound, key: String): Int =
        //#if MC < 1.16
        compound.getInteger(key)
        //#else
        //$$ compound.getInt(key)
        //#endif

    @JvmStatic
    fun keySet(compound: NBTTagCompound): Set<String> =
        //#if MC < 1.16
        compound.keySet
        //#else
        //$$ compound.keys
        //#endif

    @JvmStatic
    fun createCompound(): NBTTagCompound =
        //#if MC < 1.16
        NBTTagCompound()
        //#else
        //$$ NbtCompound()
        //#endif

    @JvmStatic
    fun createList(): NBTTagList =
        //#if MC < 1.16
        NBTTagList()
        //#else
        //$$ NbtList()
        //#endif

    @JvmStatic
    fun setString(compound: NBTTagCompound, key: String, value: String) =
        //#if MC < 1.16
        compound.setString(key, value)
        //#else
        //$$ compound.putString(key, value)
        //#endif

    @JvmStatic
    fun setInt(compound: NBTTagCompound, key: String, value: Int) =
        //#if MC < 1.16
        compound.setInteger(key, value)
        //#else
        //$$ compound.putInt(key, value)
        //#endif

    @JvmStatic
    fun appendTag(list: NBTTagList, tag: NBTTagCompound) =
        //#if MC < 1.16
        list.appendTag(tag)
        //#else
        //$$ list.add(list.size, tag)
        //#endif

    @JvmStatic
    fun setTag(compound: NBTTagCompound, key: String, tag: NBTTagCompound) =
        //#if MC < 1.16
        compound.setTag(key, tag)
        //#else
        //$$ compound.put(key, tag)
        //#endif

    @JvmStatic
    fun setTagList(compound: NBTTagCompound, key: String, list: NBTTagList) =
        //#if MC < 1.16
        compound.setTag(key, list)
        //#else
        //$$ compound.put(key, list)
        //#endif

    @JvmStatic
    fun isEmpty(compound: NBTTagCompound): Boolean =
        //#if MC < 1.16
        compound.hasNoTags()
        //#else
        //$$ compound.isEmpty
        //#endif

    @JvmStatic
    fun hasTagCompound(item: ItemStack): Boolean =
        //#if MC < 1.16
        item.hasTagCompound()
        //#else
        //$$ item.contains(DataComponentTypes.CUSTOM_DATA)
        //#endif

    @JvmStatic
    fun setTagCompound(item: ItemStack, tag: NBTTagCompound) =
        //#if MC < 1.16
        item.setTagCompound(tag)
        //#else
        //$$ item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag))
        //#endif

    @JvmStatic
    fun getId(base: NBTBase): Int =
        //#if MC < 1.16
        base.id.toInt()
        //#else
        //$$ when (base) {
        //$$     is NbtByte -> 1
        //$$     is NbtShort -> 2
        //$$     is NbtInt -> 3
        //$$     is NbtLong -> 4
        //$$     is NbtFloat -> 5
        //$$     is NbtDouble -> 6
        //$$     is NbtString -> 8
        //$$     is NbtList -> 9
        //$$     is NbtCompound -> 10
        //$$     else -> 0
        //$$ }
        //#endif

    @JvmStatic
    fun getTag(compound: NBTTagCompound, key: String): NBTBase? =
        //#if MC < 1.16
        compound.getTag(key)
        //#else
        //$$ compound.get(key)
        //#endif

    @JvmStatic
    fun getHeldType(list: NBTTagList): Int =
        //#if MC < 1.16
        list.tagType
        //#else
        //$$ list.heldType.toInt()
        //#endif

    @JvmStatic
    fun getByteValue(base: NBTBase): Byte =
        //#if MC < 1.16
        (base as NBTTagByte).byte
        //#else
        //$$ (base as NbtByte).byteValue()
        //#endif

    @JvmStatic
    fun getShortValue(base: NBTBase): Short =
        //#if MC < 1.16
        (base as NBTTagShort).short
        //#else
        //$$ (base as NbtShort).shortValue()
        //#endif

    @JvmStatic
    fun getIntValue(base: NBTBase): Int =
        //#if MC < 1.16
        (base as NBTTagInt).int
        //#else
        //$$ (base as NbtInt).intValue()
        //#endif

    @JvmStatic
    fun getLongValue(base: NBTBase): Long =
        //#if MC < 1.16
        (base as NBTTagLong).long
        //#else
        //$$ (base as NbtLong).longValue()
        //#endif

    @JvmStatic
    fun getFloatValue(base: NBTBase): Float =
        //#if MC < 1.16
        (base as NBTTagFloat).float
        //#else
        //$$ (base as NbtFloat).floatValue()
        //#endif

    @JvmStatic
    fun getDoubleValue(base: NBTBase): Double =
        //#if MC < 1.16
        (base as NBTTagDouble).double
        //#else
        //$$ (base as NbtDouble).doubleValue()
        //#endif

    @JvmStatic
    fun getStringValue(base: NBTBase): String =
        //#if MC < 1.16
        (base as NBTTagString).string
        //#else
        //$$ (base as NbtString).asString()
        //#endif

    @JvmStatic
    fun encodeStack(stack: ItemStack): String? {
        //#if MC < 1.16
        val tag = NBTTagCompound()
        stack.writeToNBT(tag)
        return tag.toString()
        //#else
        //$$ val regs: RegistryWrapper.WrapperLookup = MinecraftCompat.getLocalWorld()?.registryManager ?: return null
        //$$ return stack.encode(regs).toString()
        //#endif
    }

    @JvmStatic
    fun decodeStack(data: String): ItemStack? {
        val tag = parseTagFromJson(data) ?: return null
        //#if MC < 1.16
        return try {
            ItemStack.loadItemStackFromNBT(tag)
        } catch (e: Exception) {
            null
        }
        //#else
        //$$ val regs: RegistryWrapper.WrapperLookup = MinecraftCompat.getLocalWorld()?.registryManager ?: return null
        //$$ val out = ItemStack.fromNbtOrEmpty(regs, tag)
        //$$ return if (out.isEmpty) null else out
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
