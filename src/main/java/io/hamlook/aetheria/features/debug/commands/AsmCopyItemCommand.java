package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.item.NBTFormatter;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class AsmCopyItemCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopyitem", builder -> {
            builder.description = "Copies detailed info (internal name, lore, NBT) about the held item";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                ItemStack item = MinecraftCompat.getLocalPlayer() != null
                    ? MinecraftCompat.getLocalPlayer().getHeldItem()
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
                ClipboardCompat.setClipboard(joined);
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied item info to clipboard!");
            });
        });
    }
}
