package io.hamlook.aetheria.features.misc.killcombo;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class KillComboCommand {

    private static final String PREFIX = EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "[KillCombo] " + EnumChatFormatting.RESET;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("killcombo", builder -> {
            builder.setAliases(Collections.singletonList("kc"));
            builder.description = "Kill combo tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("reset", resetBuilder -> {
                resetBuilder.description = "Reset kill combo data";
                resetBuilder.simpleCallback(() -> {
                    KillComboTracker.getInstance().reset();
                    ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Kill Combo data reset.");
                });
            });

            builder.literal("toggle", toggleBuilder -> {
                toggleBuilder.description = "Toggle kill combo tracker";
                toggleBuilder.simpleCallback(() -> {
                    ATHRConfig.feature.misc.killCombo.enabled = !ATHRConfig.feature.misc.killCombo.enabled;
                    ATHRConfig.saveConfig();
                    boolean enabled = ATHRConfig.feature.misc.killCombo.enabled;
                    ChatUtils.sendMessage(PREFIX + (enabled ? EnumChatFormatting.GREEN + "Kill Combo tracker enabled." : EnumChatFormatting.RED + "Kill Combo tracker disabled."));
                });
            });
        });
    }
}
