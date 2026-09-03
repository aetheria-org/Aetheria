package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.item.NBTFormatter;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /asmcopyitem — copies detailed info (internal name, lore, NBT) about the held item. */
@RegisterCommand
public class AsmCopyItemCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopyitem";
    }

    @Override
    public String getUsage() {
        return "/asmcopyitem";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
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

        List<String> result = new ArrayList<>();
        result.add("internal name: " + ItemUtils.getInternalName(item));
        result.add("effective id: " + ItemUtils.getEffectiveItemId(item));
        result.add("display name: '" + item.getDisplayName() + "'");
        result.add("minecraft id: '" + item.getItem().getUnlocalizedName() + "'");
        result.add("stack size: " + item.stackSize);
        result.add("");
        result.add("lore:");
        for (String line : ItemUtils.getLoreLines(item)) {
            result.add(" '" + line + "'");
        }
        result.add("");
        net.minecraft.nbt.NBTTagCompound tag = NbtCompat.getTagCompound(item);
        if (tag != null) {
            result.add("nbt:");
            result.add(NBTFormatter.format(tag));
        } else {
            result.add("This item has no NBT data.");
        }

        String joined = String.join("\n", result);
        GuiScreen.setClipboardString(joined);
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied item info to clipboard!");
    }
}
