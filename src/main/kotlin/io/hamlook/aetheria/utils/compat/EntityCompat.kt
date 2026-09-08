package io.hamlook.aetheria.utils.compat

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object EntityCompat {

    @JvmStatic
    fun getEntityWorld(entity: Entity): World = entity.entityWorld

    @JvmStatic
    fun getFirstPassenger(entity: Entity): Entity? = entity.riddenByEntity

    @JvmStatic
    fun getPassengers(entity: Entity): List<Entity> {
        val result = mutableListOf<Entity>()
        var current = entity.riddenByEntity
        while (current != null) {
            result.add(current)
            current = current.riddenByEntity
        }
        return result
    }

    @JvmStatic
    fun getEquipmentInSlot(entity: EntityLivingBase, slot: Int): ItemStack? = entity.getEquipmentInSlot(slot)

    @JvmStatic
    fun getHeldItem(entity: EntityLivingBase): ItemStack? = entity.heldItem

    @JvmStatic
    fun getHelmet(entity: EntityLivingBase): ItemStack? = entity.getEquipmentInSlot(4)

    @JvmStatic
    fun getChestplate(entity: EntityLivingBase): ItemStack? = entity.getEquipmentInSlot(3)

    @JvmStatic
    fun getLeggings(entity: EntityLivingBase): ItemStack? = entity.getEquipmentInSlot(2)

    @JvmStatic
    fun getBoots(entity: EntityLivingBase): ItemStack? = entity.getEquipmentInSlot(1)

    @JvmStatic
    fun getAllEquipment(entity: EntityLivingBase): Array<ItemStack> = entity.inventory.normalizeAsArray()

    @JvmStatic
    fun getHealth(entity: EntityLivingBase): Float = entity.health

    @JvmStatic
    fun getMaxHealth(entity: EntityLivingBase): Float = entity.maxHealth

    @JvmStatic
    fun isDead(entity: Entity): Boolean = entity.isDead

    @JvmStatic
    fun isAlive(entity: Entity): Boolean = !entity.isDead

    @JvmStatic
    fun getEntityBoundingBox(entity: Entity) = entity.entityBoundingBox

    @JvmStatic
    fun getPosX(entity: Entity): Double = entity.posX

    @JvmStatic
    fun getPosY(entity: Entity): Double = entity.posY

    @JvmStatic
    fun getPosZ(entity: Entity): Double = entity.posZ
}
