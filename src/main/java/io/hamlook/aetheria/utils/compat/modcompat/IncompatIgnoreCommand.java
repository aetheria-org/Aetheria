package io.hamlook.aetheria.utils.compat.modcompat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;

import java.util.Arrays;

@RegisterEvents
public class IncompatIgnoreCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("athrignoreincompat", builder -> {
            builder.setAliases(Arrays.asList("athri", "aii"));
            builder.description = "Manage dismissed incompatibility warnings";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);

            builder.literal("ignore", ignoreBuilder -> {
                ignoreBuilder.description = "Hide an incompatibility warning";
                ignoreBuilder.legacyCallbackArgs(args -> {
                    if (args.length < 1) {
                        ChatUtils.sendMessage("§e[ATHR] §fUsage: /athrignoreincompat ignore <modId>");
                        return;
                    }
                    String key = args[0].toLowerCase();
                    if (IncompatModChecker.dismiss(key)) {
                        ChatUtils.sendMessage("§a[ATHR] §fWarning hidden for §e" + key + "§f. Use 'reset " + key + "' to show it again.");
                    } else {
                        ChatUtils.sendMessage("§e[ATHR] §fWarning for §e" + key + " §fwas already hidden.");
                    }
                });
            });

            builder.literal("reset", resetBuilder -> {
                resetBuilder.description = "Restore a dismissed incompatibility warning";
                resetBuilder.legacyCallbackArgs(args -> {
                    if (args.length < 1) {
                        ChatUtils.sendMessage("§e[ATHR] §fUsage: /athrignoreincompat reset <modId>");
                        return;
                    }
                    String key = args[0].toLowerCase();
                    if (IncompatModChecker.reset(key)) {
                        ChatUtils.sendMessage("§a[ATHR] §fWarning for §e" + key + " §fwill show again on next launch.");
                    } else {
                        ChatUtils.sendMessage("§e[ATHR] §fNo hidden warning for §e" + key + "§f.");
                    }
                });
            });

            builder.literal("list", listBuilder -> {
                listBuilder.description = "List all dismissed incompatibility warnings";
                listBuilder.simpleCallback(() -> {
                    if (IncompatModChecker.dismissed().isEmpty()) {
                        ChatUtils.sendMessage("§e[ATHR] §fNo hidden incompatibility warnings.");
                    } else {
                        ChatUtils.sendMessage("§e[ATHR] §fHidden warnings: §7" + String.join(", ", IncompatModChecker.dismissed()));
                    }
                });
            });
        });
    }
}
