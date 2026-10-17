package io.hamlook.aetheria.mixins.gui;

import io.hamlook.aetheria.events.SignSubmitEvent;
import io.hamlook.aetheria.features.farming.visitors.VisitorSignFill;
import io.hamlook.aetheria.mixins.accessors.SignEditScreenAccessor;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {

    @Inject(method = "init()V", at = @At("TAIL"))
    private void athr$onSignOpened(CallbackInfo ci) {
        VisitorSignFill.onSignOpenedModern((AbstractSignEditScreen) (Object) this);
    }

    @Inject(method = "finishEditing()V", at = @At("HEAD"))
    private void athr$onSignSubmit(CallbackInfo ci) {
        String[] messages = ((SignEditScreenAccessor) (Object) this).ATHR$getMessages();
        if (messages == null) return;
        String[] lines = messages.clone();
        SignSubmitEvent event = new SignSubmitEvent((AbstractSignEditScreen) (Object) this, lines);
        event.post();
        for (int i = 0; i < messages.length && i < event.lines.length; i++) {
            messages[i] = event.lines[i];
        }
    }
}
