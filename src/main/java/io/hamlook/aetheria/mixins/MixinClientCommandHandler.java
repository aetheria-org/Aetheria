package io.hamlook.aetheria.mixins;

import io.hamlook.aetheria.command.CommandRegistry;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientCommandHandler.class)
public class MixinClientCommandHandler {

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void requireSlash(ICommandSender sender, String input, CallbackInfoReturnable<Integer> cir) {
        String first = CommandRegistry.firstWordOf(input);
        if (CommandRegistry.isRegistered(first)) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }
}
