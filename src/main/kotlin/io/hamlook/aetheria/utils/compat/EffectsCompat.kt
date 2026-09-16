package io.hamlook.aetheria.utils.compat
//#if MC < 1.16
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
//#else
//$$ import net.minecraft.world.entity.LivingEntity
//$$ import net.minecraft.world.effect.MobEffectInstance
//$$ import net.minecraft.world.effect.MobEffects
//#endif
//#if MC > 1.21
//$$ import net.minecraft.entity.effect.StatusEffect
//$$ import net.minecraft.registry.entry.RegistryEntry
//#endif

enum class EffectsCompat(
    //#if MC < 1.21
    val potion: Potion,
    //#else
    //$$ val potion: RegistryEntry<StatusEffect>,
    //#endif
) {
    INVISIBILITY(
        //#if MC < 1.16
        Potion.invisibility
        //#else
        //$$ MobEffects.INVISIBILITY
        //#endif
    ),
    BLINDNESS(
        //#if MC < 1.16
        Potion.blindness
        //#else
        //$$ MobEffects.BLINDNESS
        //#endif
    ),
    ;

    companion object {
        @JvmStatic
        fun hasPotionEffect(entity: EntityLivingBase, effect: EffectsCompat): Boolean =
            //#if MC < 1.16
            entity.isPotionActive(effect.potion)
            //#else
            //$$ entity.hasEffect(effect.potion)
            //#endif

        @JvmStatic
        fun getActivePotionEffect(entity: EntityLivingBase, effect: EffectsCompat): PotionEffect? =
            //#if MC < 1.16
            entity.getActivePotionEffect(effect.potion)
            //#else
            //$$ entity.getEffect(effect.potion)
            //#endif
    }
}
