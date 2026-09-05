package io.hamlook.aetheria.command;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.utils.chat.ChatUtils;

import java.util.Arrays;

@RegisterEvents
public class Command {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("athr", builder -> {
            builder.setAliases(Arrays.asList("aetheria", "jef", "asm"));
            builder.description = "Aetheria main command";
            builder.setCategory(CommandCategory.INTERNAL);

            builder.legacyCallbackArgs(args -> {
                if (args.length > 0) {
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            if (!NetworkGuard.requiresGithub("Repo Data")) break;
                            ATHRConfig.reloadRepo();
                            ChatUtils.sendMessage("§a[ATHR] §fRepo refresh triggered.");
                            break;
                        case "config":
                            ATHRConfig.openGui();
                            break;
                        default:
                            ATHRConfig.openSearch(String.join(" ", args));
                            break;
                    }
                } else {
                    ATHRConfig.openOptionsGui();
                }
            });
        });
    }
}
