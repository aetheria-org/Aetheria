package io.hamlook.aetheria.utils.compat

import net.minecraft.util.DamageSource

object DamageSourceCompat {
    //#if MC < 1.21
    @JvmStatic val cactus get() = DamageSource.cactus
    @JvmStatic val drown get() = DamageSource.drown
    @JvmStatic val fall get() = DamageSource.fall
    @JvmStatic val generic get() = DamageSource.generic
    @JvmStatic val inFire get() = DamageSource.inFire
    @JvmStatic val inWall get() = DamageSource.inWall
    @JvmStatic val lava get() = DamageSource.lava
    @JvmStatic val lightningBolt get() = DamageSource.lightningBolt
    @JvmStatic val magic get() = DamageSource.magic
    @JvmStatic val onFire get() = DamageSource.onFire
    @JvmStatic val outOfWorld get() = DamageSource.outOfWorld
    @JvmStatic val starve get() = DamageSource.starve
    @JvmStatic val wither get() = DamageSource.wither
    //#else
    //$$ private val damageSources get() = MinecraftCompat.getLocalWorldOrError().damageSources
    //$$ @JvmStatic val cactus get() = damageSources.cactus()
    //$$ @JvmStatic val drown get() = damageSources.drown()
    //$$ @JvmStatic val fall get() = damageSources.fall()
    //$$ @JvmStatic val generic get() = damageSources.generic()
    //$$ @JvmStatic val inFire get() = damageSources.inFire()
    //$$ @JvmStatic val inWall get() = damageSources.inWall()
    //$$ @JvmStatic val lava get() = damageSources.lava()
    //$$ @JvmStatic val lightningBolt get() = damageSources.lightningBolt()
    //$$ @JvmStatic val magic get() = damageSources.magic()
    //$$ @JvmStatic val onFire get() = damageSources.onFire()
    //$$ @JvmStatic val outOfWorld get() = damageSources.outOfWorld()
    //$$ @JvmStatic val starve get() = damageSources.starve()
    //$$ @JvmStatic val wither get() = damageSources.wither()
    //#endif
}
