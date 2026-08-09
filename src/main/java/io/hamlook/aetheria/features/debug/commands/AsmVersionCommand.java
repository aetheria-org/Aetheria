package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;

import java.util.Collections;
import java.util.List;

/** /asmversion — prints and copies the mod + Minecraft version. */
@RegisterCommand
public class AsmVersionCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmversion";
    }

    @Override
    public String getUsage() {
        return "/asmversion";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmver");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        String mcVersion = Loader.instance().getMCVersionString();
        String text = "Aetheria " + Aetheria.VERSION + " on " + mcVersion;
        GuiScreen.setClipboardString(text);
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "You are using " + text);
    }
}
