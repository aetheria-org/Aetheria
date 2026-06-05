package io.hamlook.aetheria.features.misc

import io.hamlook.aetheria.command.ASMCommand
import io.hamlook.aetheria.init.RegisterCommand
import net.minecraft.command.ICommandSender

@RegisterCommand
class PretendThisDoesntExist : ASMCommand() {
    
    override fun getName() = "ATHRthisisatestdontusethispls"
    
    override fun getUsage() = "/ATHRthisisatestdontusethispls"
    
    override fun execute(sender: ICommandSender, args: Array<String>) {
        DVD.forceCornerHit()
    }
}
