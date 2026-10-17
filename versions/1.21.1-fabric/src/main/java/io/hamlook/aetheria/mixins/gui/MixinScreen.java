package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.features.misc.SearchBar;
import io.hamlook.aetheria.features.misc.itemList.ItemPaneRenderer;
import io.hamlook.aetheria.features.storage.StorageManager;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.ParentElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes typed characters into Aetheria's own search fields.
 *
 * <p>Targets {@link ParentElement} rather than {@code Element}, which declares
 * {@code charTyped} as a default method that {@code ParentElement} overrides
 * with a {@code getFocused()} short-circuit — so the {@code Element} default is
 * never reached from a screen. {@code public default} is required because
 * interface mixins reject private and abstract handlers.
 */
@Mixin(ParentElement.class)
public interface MixinScreen {

    @Inject(method = "charTyped(CI)Z", at = @At("HEAD"), cancellable = true)
    public default void athr$charTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != MinecraftCompat.getCurrentScreen()) return;
        if (SearchBar.handleCharTyped(chr)) {
            cir.setReturnValue(true);
            return;
        }
        if (StorageManager.handleCharTyped(chr)) {
            cir.setReturnValue(true);
            return;
        }
        if (ItemPaneRenderer.INSTANCE != null && ItemPaneRenderer.INSTANCE.handleCharTyped(chr)) {
            cir.setReturnValue(true);
        }
    }
}
