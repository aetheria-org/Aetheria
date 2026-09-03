package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

/** /asmcopyinternalname — copies just the internal (SkyBlock) item id of the held item. */
@RegisterCommand
public class AsmCopyInternalNameCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopyinternalname";
    }

    @Override
    public String getUsage() {
        return "/asmcopyinternalname";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopyid");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        ItemStack item = MinecraftCompat.getMinecraft().thePlayer != null
            ? MinecraftCompat.getMinecraft().thePlayer.getHeldItem()
            : null;

        if (item == null) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "No item in hand!");
            return;
        }

        String internalName = ItemUtils.getInternalName(item);
        if (internalName.isEmpty()) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "That item has no internal SkyBlock id (probably a vanilla item).");
            return;
        }

        GuiScreen.setClipboardString(internalName);
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied internal name " + EnumChatFormatting.GRAY + internalName + EnumChatFormatting.YELLOW + " to the clipboard!");
    }
}
