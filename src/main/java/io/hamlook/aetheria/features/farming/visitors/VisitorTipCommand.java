package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;

@RegisterEvents
public class VisitorTipCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("visitortip", builder -> {
            builder.setAliases(Collections.singletonList("asmvisitortip"));
            builder.description = "Toggle the shopping list tip";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.literal("hide", hideBuilder -> {
                hideBuilder.simpleCallback(() -> {
                    if (ATHRConfig.feature == null || ATHRConfig.feature.farming == null || ATHRConfig.feature.farming.visitors == null) {
                        ChatUtils.sendMessage(EnumChatFormatting.RED + "[ASM] Config not loaded yet.");
                        return;
                    }
                    ATHRConfig.feature.farming.visitors.shoppingListTipHidden = true;
                    ATHRConfig.saveConfig();
                    ChatUtils.sendMessage(EnumChatFormatting.GREEN + "[ASM] Shopping list tip hidden. " + EnumChatFormatting.GRAY + "Use /visitortip show to bring it back.");
                });
            });

            builder.literal("show", showBuilder -> {
                showBuilder.simpleCallback(() -> {
                    if (ATHRConfig.feature == null || ATHRConfig.feature.farming == null || ATHRConfig.feature.farming.visitors == null) {
                        ChatUtils.sendMessage(EnumChatFormatting.RED + "[ASM] Config not loaded yet.");
                        return;
                    }
                    ATHRConfig.feature.farming.visitors.shoppingListTipHidden = false;
                    ATHRConfig.saveConfig();
                    ChatUtils.sendMessage(EnumChatFormatting.GREEN + "[ASM] Shopping list tip re-enabled.");
                });
            });
        });
    }
}
