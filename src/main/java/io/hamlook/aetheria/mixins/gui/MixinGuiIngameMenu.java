package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends GuiScreen {

    @Unique
    private static final int BTN_ATHR = 0x4EF;

    @Unique
    @Inject(method = "initGui", at = @At("TAIL"))
    private void ATHR$addButton(CallbackInfo ci) {
        int lowestY = this.height / 4 + 8;
        for (GuiButton btn : this.buttonList) {
            lowestY = Math.max(lowestY, btn.yPosition + btn.height);
        }
        this.buttonList.add(new GuiButton(
                BTN_ATHR,
                this.width / 2 - 100,
                lowestY + 4,
                200,
                20,
                "Aetheria Mod"
        ));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void ATHR$actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == BTN_ATHR) {
            MinecraftCompat.getMinecraft().displayGuiScreen(new OptionsMenu());
            ci.cancel();
        }
    }
}
