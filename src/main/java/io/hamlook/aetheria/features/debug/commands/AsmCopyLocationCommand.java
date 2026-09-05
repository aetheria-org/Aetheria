package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.Locale;

@RegisterEvents
public class AsmCopyLocationCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmcopylocation", builder -> {
            builder.setAliases(Collections.singletonList("asmcopyloc"));
            builder.description = "Copies the player's exact coordinates to the clipboard";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                EntityPlayer player = MinecraftCompat.getLocalPlayer();
                if (player == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
                    return;
                }

                String text = String.format(Locale.ROOT, "%.2f, %.2f, %.2f (yaw=%.1f, pitch=%.1f)",
                    player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);

                ClipboardCompat.setClipboard(text);
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied location to clipboard: " + EnumChatFormatting.GRAY + text);
            });
        });
    }
}
