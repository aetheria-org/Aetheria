package io.hamlook.aetheria.utils.compat

import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

enum class EffectsCompat(
    val potion: Potion,
) {
    INVISIBILITY(Potion.invisibility),
    BLINDNESS(Potion.blindness),
    ;

    companion object {
        @JvmStatic
        fun hasPotionEffect(entity: EntityLivingBase, effect: EffectsCompat): Boolean =
            entity.isPotionActive(effect.potion)

        @JvmStatic
        fun getActivePotionEffect(entity: EntityLivingBase, effect: EffectsCompat): PotionEffect? =
            entity.getActivePotionEffect(effect.potion)
    }
}
