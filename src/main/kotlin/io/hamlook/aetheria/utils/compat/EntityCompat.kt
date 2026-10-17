package io.hamlook.aetheria.utils.compat

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraft.world.WorldSettings
//#if MC > 1.16
//$$ import net.minecraft.world.entity.EquipmentSlot
//$$ import net.minecraft.world.GameMode
//#endif
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
//#if MC < 1.17
//#else
//$$ import net.minecraft.util.TypeFilter
//#endif

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
    fun getYaw(entity: Entity): Float =
        //#if MC < 1.16
        entity.rotationYaw
        //#else
        //$$ entity.getYaw()
        //#endif

    @JvmStatic
    fun getPitch(entity: Entity): Float =
        //#if MC < 1.16
        entity.rotationPitch
        //#else
        //$$ entity.getPitch()
        //#endif

    @JvmStatic
    fun hasNetworkHandler(): Boolean {
        val player = MinecraftCompat.getLocalPlayer() ?: return false
        //#if MC < 1.16
        return player.sendQueue != null
        //#else
        //$$ return player.networkHandler != null
        //#endif
    }

    @JvmStatic
    fun getEyePosition(entity: Entity, partialTicks: Float): Vec3 =
        //#if MC < 1.16
        entity.getPositionEyes(partialTicks)
        //#else
        //$$ entity.getCameraPosVec(partialTicks)
        //#endif

    @JvmStatic
    fun getPrevX(entity: Entity): Double =
        //#if MC < 1.16
        entity.lastTickPosX
        //#else
        //$$ entity.prevX
        //#endif

    @JvmStatic
    fun getPrevY(entity: Entity): Double =
        //#if MC < 1.16
        entity.lastTickPosY
        //#else
        //$$ entity.prevY
        //#endif

    @JvmStatic
    fun getPrevZ(entity: Entity): Double =
        //#if MC < 1.16
        entity.lastTickPosZ
        //#else
        //$$ entity.prevZ
        //#endif

    @JvmStatic
    fun getDistanceTo(a: Entity, b: Entity): Double {
        val dx = getPosX(a) - getPosX(b)
        val dy = getPosY(a) - getPosY(b)
        val dz = getPosZ(a) - getPosZ(b)
        return Math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    @JvmStatic
    fun getDistanceSqTo(a: Entity, b: Entity): Double {
        val dx = getPosX(a) - getPosX(b)
        val dy = getPosY(a) - getPosY(b)
        val dz = getPosZ(a) - getPosZ(b)
        return dx * dx + dy * dy + dz * dz
    }

    @JvmStatic
    fun getDistanceToPos(entity: Entity, x: Double, y: Double, z: Double): Double {
        val dx = getPosX(entity) - x
        val dy = getPosY(entity) - y
        val dz = getPosZ(entity) - z
        return Math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    @JvmStatic
    fun getFishHook(player: EntityPlayerSP): Entity? =
        //#if MC < 1.16
        player.fishEntity
        //#else
        //$$ player.fishHook
        //#endif

    @JvmStatic
    fun isSpectator(info: NetworkPlayerInfo): Boolean =
        //#if MC < 1.16
        info.gameType == WorldSettings.GameType.SPECTATOR
        //#else
        //$$ info.gameMode == GameMode.SPECTATOR
        //#endif

    @JvmStatic
    fun getUniqueId(entity: Entity): java.util.UUID =
        //#if MC < 1.16
        entity.uniqueID
        //#else
        //$$ entity.uuid
        //#endif

    /**
     * First living non-armorstand entity (other than the local player) whose
     * box intersects [stand]'s box expanded by (ex, ey, ez), in world order.
     * Replaces `world.getEntitiesWithinAABB(...).stream().findFirst()`, which
     * has no modern expression; iteration order and intersection semantics match.
     */
    @JvmStatic
    fun findMobNearStand(world: WorldClient, stand: Entity, ex: Double, ey: Double, ez: Double): EntityLivingBase? {
        val box = getEntityBoundingBox(stand).expand(ex, ey, ez)
        val self: Entity? = MinecraftCompat.getLocalPlayer()
        for (e in getLivingInBox(world, box)) {
            if (e === stand || e === self) continue
            if (e is EntityArmorStand) continue
            return e
        }
        return null
    }

    @JvmStatic
    fun getLivingInBox(world: WorldClient, box: AxisAlignedBB): List<EntityLivingBase> =
        //#if MC < 1.17
        world.getEntitiesWithinAABB(EntityLivingBase::class.java, box)
        //#else
        //$$ world.getEntitiesByType(TypeFilter.instanceOf<Entity, LivingEntity>(LivingEntity::class.java), box) { true }
        //#endif
}
