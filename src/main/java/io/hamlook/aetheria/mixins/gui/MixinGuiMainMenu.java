package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.custommenu.CustomMainMenu;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    @Inject(method = "initGui", at = @At("HEAD"))
    public void ATHR$initGui(CallbackInfo ci) {
        if (CMMHelper.isEnabled()) {
            Aetheria.logger.info("[CMM] Opening Custom Main Menu");
            MinecraftCompat.getMinecraft().displayGuiScreen(new CustomMainMenu(CMMHelper.getCMMConfig()));
        }
    }
}
