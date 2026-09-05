package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;

import java.util.Collections;

@RegisterEvents
public class AsmVersionCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmversion", builder -> {
            builder.setAliases(Collections.singletonList("asmver"));
            builder.description = "Prints and copies the mod + Minecraft version";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);
            builder.simpleCallback(() -> {
                String mcVersion = Loader.instance().getMCVersionString();
                String text = "Aetheria " + Aetheria.VERSION + " on " + mcVersion;
                ClipboardCompat.setClipboard(text);
                ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "You are using " + text);
            });
        });
    }
}
