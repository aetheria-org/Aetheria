package io.hamlook.aetheria.mixins.render;

import io.hamlook.aetheria.utils.render.EntityOutlines;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntityGlowColor {

    @Inject(method = "getTeamColorValue()I", at = @At("HEAD"), cancellable = true)
    private void ATHR$glowColor(CallbackInfoReturnable<Integer> cir) {
        Integer color = EntityOutlines.glowColor((Entity) (Object) this);
        if (color != null) {
            cir.setReturnValue(color);
        }
    }
}
