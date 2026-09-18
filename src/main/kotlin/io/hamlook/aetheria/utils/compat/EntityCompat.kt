package io.hamlook.aetheria.utils.compat
//#if MC < 1.16
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.world.World
//#else
//$$ import net.minecraft.world.entity.Entity
//$$ import net.minecraft.world.entity.LivingEntity
//$$ import net.minecraft.world.item.ItemStack
//$$ import net.minecraft.world.level.Level
//$$ import net.minecraft.world.entity.EquipmentSlot
//#endif
import net.minecraft.util.Vec3

object EntityCompat {

    @JvmStatic
    fun getEntityWorld(entity: Entity): World =
        //#if MC < 1.16
        entity.entityWorld
        //#else
        //$$ entity.level
        //#endif

    @JvmStatic
    fun getFirstPassenger(entity: Entity): Entity? =
        //#if MC < 1.16
        entity.riddenByEntity
        //#else
        //$$ entity.passengers.firstOrNull()
        //#endif

    @JvmStatic
    fun getPassengers(entity: Entity): List<Entity> =
        //#if MC < 1.16
        mutableListOf<Entity>().apply {
            var current = entity.riddenByEntity
            while (current != null) {
                add(current)
                current = current.riddenByEntity
            }
        }
        //#else
        //$$ entity.passengers.toList()
        //#endif

    @JvmStatic
    fun getEquipmentInSlot(entity: EntityLivingBase, slot: Int): ItemStack? =
        //#if MC < 1.16
        entity.getEquipmentInSlot(slot)
        //#else
        //$$ when (slot) {
        //$$     0 -> entity.getItemBySlot(EquipmentSlot.MAINHAND)
        //$$     1 -> entity.getItemBySlot(EquipmentSlot.FEET)
        //$$     2 -> entity.getItemBySlot(EquipmentSlot.LEGS)
        //$$     3 -> entity.getItemBySlot(EquipmentSlot.CHEST)
        //$$     4 -> entity.getItemBySlot(EquipmentSlot.HEAD)
        //$$     else -> null
        //$$ }
        //#endif

    @JvmStatic
    fun getHeldItem(entity: EntityLivingBase): ItemStack? =
        //#if MC < 1.16
        entity.heldItem
        //#else
        //$$ entity.getItemBySlot(EquipmentSlot.MAINHAND)
        //#endif

    @JvmStatic
    fun getHelmet(entity: EntityLivingBase): ItemStack? =
        //#if MC < 1.16
        entity.getEquipmentInSlot(4)
        //#else
        //$$ entity.getItemBySlot(EquipmentSlot.HEAD)
        //#endif

    @JvmStatic
    fun getChestplate(entity: EntityLivingBase): ItemStack? =
        //#if MC < 1.16
        entity.getEquipmentInSlot(3)
        //#else
        //$$ entity.getItemBySlot(EquipmentSlot.CHEST)
        //#endif

    @JvmStatic
    fun getLeggings(entity: EntityLivingBase): ItemStack? =
        //#if MC < 1.16
        entity.getEquipmentInSlot(2)
        //#else
        //$$ entity.getItemBySlot(EquipmentSlot.LEGS)
        //#endif

    @JvmStatic
    fun getBoots(entity: EntityLivingBase): ItemStack? =
        //#if MC < 1.16
        entity.getEquipmentInSlot(1)
        //#else
        //$$ entity.getItemBySlot(EquipmentSlot.FEET)
        //#endif

    @JvmStatic
    fun getAllEquipment(entity: EntityLivingBase): Array<ItemStack> =
        //#if MC < 1.16
        entity.inventory.normalizeAsArray()
        //#elseif MC < 1.21
        //$$ entity.armorSlots.map { it }.toTypedArray()
        //#else
        //$$ entity.armorItems.toList().toTypedArray()
        //#endif

    @JvmStatic
    fun getHealth(entity: EntityLivingBase): Float = entity.health

    @JvmStatic
    fun getMaxHealth(entity: EntityLivingBase): Float = entity.maxHealth

    @JvmStatic
    fun getName(entity: Entity): String =
        //#if MC < 1.16
        entity.name
        //#else
        //$$ TextCompat.getUnformattedText(entity.name)
        //#endif

    @JvmStatic
    fun isDead(entity: Entity): Boolean =
        //#if MC < 1.16
        entity.isDead
        //#else
        //$$ entity.isRemoved
        //#endif

    @JvmStatic
    fun isAlive(entity: Entity): Boolean =
        //#if MC < 1.16
        !entity.isDead
        //#else
        //$$ !entity.isRemoved
        //#endif

    @JvmStatic
    fun getEntityBoundingBox(entity: Entity) =
        //#if MC < 1.16
        entity.entityBoundingBox
        //#else
        //$$ entity.boundingBox
        //#endif

    @JvmStatic
    fun getPosX(entity: Entity): Double =
        //#if MC < 1.16
        entity.posX
        //#else
        //$$ entity.x
        //#endif

    @JvmStatic
    fun getPosY(entity: Entity): Double =
        //#if MC < 1.16
        entity.posY
        //#else
        //$$ entity.y
        //#endif

    @JvmStatic
    fun getPosZ(entity: Entity): Double =
        //#if MC < 1.16
        entity.posZ
        //#else
        //$$ entity.z
        //#endif

    @JvmStatic
    fun getEyePosition(entity: Entity, partialTicks: Float): Vec3 =
        //#if MC < 1.16
        entity.getPositionEyes(partialTicks)
        //#else
        //$$ entity.getCameraPosVec(partialTicks)
        //#endif
}
