package io.hamlook.aetheria.utils.compat

import com.mojang.authlib.GameProfile
//#if MC < 1.16
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
//#else
//$$ import net.minecraft.world.entity.player.Player
//$$ import net.minecraft.world.item.Item
//$$ import net.minecraft.item.Items
//$$ import net.minecraft.world.item.ItemStack
//#endif
//#if MC > 1.16
//$$ import net.minecraft.world.item.DyeItem
//#endif
//#if MC > 1.21
//$$ import net.minecraft.item.tooltip.TooltipType
//$$ import net.minecraft.registry.Registries
//$$ import net.minecraft.util.Identifier
//$$ import net.minecraft.component.DataComponentTypes
//$$ import net.minecraft.component.type.ProfileComponent
//$$ import net.minecraft.text.Text
//#endif

fun isEmptyCompat(item: ItemStack?): Boolean =
    //#if MC < 1.16
    item == null
    //#else
    //$$ item == null || item.isEmpty()
    //#endif

fun ItemStack.getCountCompat(): Int =
    //#if MC < 1.16
    this.stackSize
    //#else
    //$$ this.getCount()
    //#endif

fun areStacksEqual(a: ItemStack?, b: ItemStack?): Boolean {
    if (a == null || b == null) return a === b
    //#if MC < 1.16
    return ItemStack.areItemStacksEqual(a, b)
    //#else
    //$$ return ItemStack.areItemsAndComponentsEqual(a, b)
    //#endif
}

fun ItemStack.getTooltipCompat(player: EntityPlayer?, advanced: Boolean): MutableList<String> {
    //#if MC < 1.16
    return this.getTooltip(player, advanced)
    //#elseif MC < 1.21
        //$ return this.getTooltipLines(MinecraftCompat.getLocalPlayer()) { advanced }.map { TextCompat.getFormattedText(it) }.toMutableList()
    //#else
    //$$ val tooltipType = if (advanced) TooltipType.ADVANCED else TooltipType.BASIC
    //$$ return this.getTooltip(Item.TooltipContext.DEFAULT, MinecraftCompat.getLocalPlayer(), tooltipType).map { TextCompat.getFormattedText(it) }.toMutableList()
    //#endif
}

fun ItemStack.getTooltipCompat(advanced: Boolean): MutableList<String> {
    //#if MC < 1.16
    return this.getTooltip(MinecraftCompat.getLocalPlayer(), advanced)
    //#elseif MC < 1.21
        //$ return this.getTooltipLines(MinecraftCompat.getLocalPlayer()) { advanced }.map { TextCompat.getFormattedText(it) }.toMutableList()
    //#else
    //$$ val tooltipType = if (advanced) TooltipType.ADVANCED else TooltipType.BASIC
    //$$ return this.getTooltip(Item.TooltipContext.DEFAULT, MinecraftCompat.getLocalPlayer(), tooltipType).map { TextCompat.getFormattedText(it) }.toMutableList()
    //#endif
}

fun Item.getIdentifierString(): String {
    //#if MC < 1.16
    return this.registryName
    //#else
    //$$ return Registries.ITEM.getId(this).toString()
    //#endif
}

fun ItemStack.setCustomItemName(name: String): ItemStack {
    //#if MC < 1.16
    this.setStackDisplayName(name)
    //#else
    //$$ this.set(DataComponentTypes.CUSTOM_NAME, Text.of(name))
    //#endif
    return this
}

fun ItemStack.hasDisplayNameCompat(): Boolean =
    //#if MC < 1.16
    this.hasDisplayName()
    //#else
    //$$ this.contains(DataComponentTypes.CUSTOM_NAME)
    //#endif

fun ItemStack.getDisplayNameCompat(): String =
    //#if MC < 1.16
    this.displayName
    //#else
    //$$ TextCompat.getFormattedText(this.name)
    //#endif

//#if MC > 1.21
//$$ fun ItemStack.setCustomItemName(name: Text): ItemStack {
//$$     this.set(DataComponentTypes.CUSTOM_NAME, name)
//$$     return this
//$$ }
//#endif

@JvmOverloads
fun createPlayerHeadStack(size: Int = 1): ItemStack =
    //#if MC < 1.16
    ItemStack(Items.skull, size, 3)
    //#else
    //$$ ItemStack(Items.PLAYER_HEAD, size)
    //#endif

// head skin data, legacy keeps it in SkullOwner nbt, modern in the profile component
class SkullProfile(
    @JvmField val textureValue: String,
    @JvmField val profileId: String,
    @JvmField val profileName: String,
    @JvmField val signature: String
)

fun ItemStack?.skullProfile(): SkullProfile? {
    if (this == null) return null
    //#if MC < 1.21
    val tag = NbtCompat.getTagCompound(this) ?: return null
    if (!NbtCompat.hasKey(tag, "SkullOwner")) return null
    val owner = NbtCompat.getCompound(tag, "SkullOwner")
    val textures = NbtCompat.getTagList(NbtCompat.getCompound(owner, "Properties"), "textures", NbtCompat.TAG_COMPOUND)
    if (NbtCompat.tagCount(textures) == 0) return null
    val entry = NbtCompat.getCompoundAt(textures, 0)
    if (!NbtCompat.hasKey(entry, "Value")) return null
    val value = NbtCompat.getString(entry, "Value")
    if (value.isEmpty()) return null
    return SkullProfile(
        value,
        if (NbtCompat.hasKey(owner, "Id")) NbtCompat.getString(owner, "Id") else "",
        if (NbtCompat.hasKey(owner, "Name")) NbtCompat.getString(owner, "Name") else "",
        if (NbtCompat.hasKey(entry, "Signature")) NbtCompat.getString(entry, "Signature") else ""
    )
    //#else
    //$$ val component = this.get(DataComponentTypes.PROFILE) ?: return null
    //$$ val game = component.gameProfile()
    //$$ val texture = game.properties.get("textures").firstOrNull() ?: return null
    //$$ if (texture.value.isEmpty()) return null
    //$$ return SkullProfile(texture.value, game.id.toString(), game.name, texture.signature ?: "")
    //#endif
}

fun ItemStack.setSkullProfile(profile: GameProfile): ItemStack {
    //#if MC < 1.21
    val texture = profile.properties.get("textures").firstOrNull()
    val entry = NbtCompat.createCompound()
    NbtCompat.setString(entry, "Value", texture?.value ?: "")
    NbtCompat.setString(entry, "Signature", texture?.signature ?: "")
    val textures = NbtCompat.createList()
    NbtCompat.appendTag(textures, entry)
    val properties = NbtCompat.createCompound()
    NbtCompat.setTagList(properties, "textures", textures)
    val owner = NbtCompat.createCompound()
    NbtCompat.setString(owner, "Id", profile.id.toString())
    NbtCompat.setString(owner, "Name", profile.name)
    NbtCompat.setTag(owner, "Properties", properties)
    val tag = NbtCompat.createCompound()
    NbtCompat.setTag(tag, "SkullOwner", owner)
    NbtCompat.setTagCompound(this, tag)
    //#else
    //$$ this.set(DataComponentTypes.PROFILE, ProfileComponent(profile))
    //#endif
    return this
}

@JvmOverloads
fun createFishStack(metadata: Int, size: Int = 1): ItemStack =
    //#if MC < 1.16
    ItemStack(Items.fish, size, metadata)
    //#else
    //$$ ItemStack(when (metadata) { 1 -> Items.SALMON; 2 -> Items.TROPICAL_FISH; 3 -> Items.PUFFERFISH; else -> Items.COD }, size)
    //#endif

@JvmOverloads
fun createDyeStackCompat(dyeColor: Int, size: Int = 1): ItemStack = DyeCompat.createDyeStack(dyeColor, size)

fun String.getVanillaItem(): Item? {
    //#if MC < 1.16
    return Item.getByNameOrId(this)
    //#else
    //$$ val item = Registries.ITEM.get(Identifier.of(this))
    //$$ if (item == Items.AIR) return null
    //$$ return item
    //#endif
}

enum class DyeCompat(
    private val dyeColor: Int,
    //#if MC > 1.16
    //$$ private val stackType: Item
    //#endif
) {
    WHITE(
        15,
        //#if MC > 1.16
        //$$ Items.BONE_MEAL
        //#endif
    ),
    ORANGE(
        14,
        //#if MC > 1.16
        //$$ Items.ORANGE_DYE
        //#endif
    ),
    MAGENTA(
        13,
        //#if MC > 1.16
        //$$ Items.MAGENTA_DYE
        //#endif
    ),
    LIGHT_BLUE(
        12,
        //#if MC > 1.16
        //$$ Items.LIGHT_BLUE_DYE
        //#endif
    ),
    YELLOW(
        11,
        //#if MC > 1.16
        //$$ Items.YELLOW_DYE
        //#endif
    ),
    LIME(
        10,
        //#if MC > 1.16
        //$$ Items.LIME_DYE
        //#endif
    ),
    PINK(
        9,
        //#if MC > 1.16
        //$$ Items.PINK_DYE
        //#endif
    ),
    GRAY(
        8,
        //#if MC > 1.16
        //$$ Items.GRAY_DYE
        //#endif
    ),
    LIGHT_GRAY(
        7,
        //#if MC > 1.16
        //$$ Items.LIGHT_GRAY_DYE
        //#endif
    ),
    CYAN(
        6,
        //#if MC > 1.16
        //$$ Items.CYAN_DYE
        //#endif
    ),
    PURPLE(
        5,
        //#if MC > 1.16
        //$$ Items.PURPLE_DYE
        //#endif
    ),
    BLUE(
        4,
        //#if MC > 1.16
        //$$ Items.LAPIS_LAZULI
        //#endif
    ),
    BROWN(
        3,
        //#if MC > 1.16
        //$$ Items.COCOA_BEANS
        //#endif
    ),
    GREEN(
        2,
        //#if MC > 1.16
        //$$ Items.GREEN_DYE
        //#endif
    ),
    RED(
        1,
        //#if MC > 1.16
        //$$ Items.RED_DYE
        //#endif
    ),
    BLACK(
        0,
        //#if MC > 1.16
        //$$ Items.BLACK_DYE
        //#endif
    )
    ;

    fun createStack(size: Int = 1): ItemStack =
        //#if MC < 1.16
        ItemStack(Items.dye, size, dyeColor)
        //#else
        //$$ ItemStack(stackType, size)
        //#endif

    companion object {

        fun ItemStack.isDye(dye: DyeCompat): Boolean = isDye(dye.dyeColor)

        fun ItemStack.isDye(metadata: Int = -1): Boolean {
            if (metadata == -1) {
                //#if MC < 1.16
                return this.item == Items.dye
                //#else
                //$$ return entries.firstOrNull { this.item == item } != null
                //#endif
            }

            //#if MC < 1.16
            return this.item == Items.dye && this.metadata == metadata
            //#else
            //$$ return this.item == fromDyeColor(metadata).stackType
            //#endif
        }

        private fun fromDyeColor(dyeColor: Int): DyeCompat = entries.firstOrNull { it.dyeColor == dyeColor } ?: GRAY

        fun toDamage(stack: ItemStack): Int {
            //#if MC < 1.21
            return entries.firstOrNull { it.dyeColor == stack.metadata }?.dyeColor ?: 0
            //#else
            //$$ return entries.firstOrNull { it.stackType == stack.item }?.dyeColor ?: 0
            //#endif
        }

        fun createDyeStack(dyeColor: Int, size: Int = 1): ItemStack =
            fromDyeColor(dyeColor).createStack(size)
    }
}
