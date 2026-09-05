package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class AsmCopyInternalNameCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopyinternalname", builder -> {
            builder.setAliases(Collections.singletonList("asmcopyid"));
            builder.description = "Copies just the internal (SkyBlock) item id of the held item";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                ItemStack item = MinecraftCompat.getLocalPlayer() != null
                    ? MinecraftCompat.getLocalPlayer().getHeldItem()
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

                ClipboardCompat.setClipboard(internalName);
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied internal name " + EnumChatFormatting.GRAY + internalName + EnumChatFormatting.YELLOW + " to the clipboard!");
            });
        });
    }
}
