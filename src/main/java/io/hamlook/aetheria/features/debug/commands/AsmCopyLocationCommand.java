package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /asmcopylocation — copies the player's exact coordinates to the clipboard. */
@RegisterCommand
public class AsmCopyLocationCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopylocation";
    }

    @Override
    public String getUsage() {
        return "/asmcopylocation";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopyloc");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
            return;
        }

        String text = String.format(Locale.ROOT, "%.2f, %.2f, %.2f (yaw=%.1f, pitch=%.1f)",
            player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);

        GuiScreen.setClipboardString(text);
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied location to clipboard: " + EnumChatFormatting.GRAY + text);
    }
}
