package io.hamlook.aetheria.utils.compat

import net.minecraft.util.DamageSource

object DamageSourceCompat {
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
}
