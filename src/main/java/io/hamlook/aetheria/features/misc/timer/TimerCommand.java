package io.hamlook.aetheria.features.misc.timer;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.time.TimeFormatter;

import java.util.Arrays;


@RegisterEvents
public class TimerCommand {

    private static final String PREFIX = "§b[ATHR Timer] §f";

    private static void printStatus(UptimeManager mgr) {
        if (!mgr.isActive()) {
            ChatUtils.sendMessage(PREFIX + "No timer running. Use §e/athrtimer<time>§f to start one.");
            return;
        }
        String state = mgr.isPaused() ? " §7(paused)§f" : "";
        ChatUtils.sendMessage(PREFIX + "Remaining: §b" + TimeFormatter.formatCountdown(mgr.getRemainingMs()) + "§f" + state);
    }

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("athrtimer", builder -> {
            builder.setAliases(Arrays.asList("aetheriatimer", "jeftimer", "asmtimer"));
            builder.description = "Timer commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(args -> {
                UptimeManager mgr = UptimeManager.getInstance();

                if (args.length == 0) {
                    printStatus(mgr);
                    return;
                }

                String sub = args[0].toLowerCase();

                switch (sub) {

                    case "show":
                        printStatus(mgr);
                        break;

                    case "cancel":
                    case "stop":
                    case "clear":
                        mgr.cancel();
                        ChatUtils.sendMessage(PREFIX + "§cTimer cancelled.");
                        break;

                    case "pause":
                        if (!mgr.isActive()) {
                            ChatUtils.sendMessage(PREFIX + "§cNo timer is running.");
                        } else if (mgr.isPaused()) {
                            ChatUtils.sendMessage(PREFIX + "§eTimer is already paused. Use §b/athrtimerresume§e.");
                        } else {
                            mgr.pause();
                            ChatUtils.sendMessage(PREFIX + "§ePaused at §b" + TimeFormatter.formatCountdown(mgr.getRemainingMs()) + "§e.");
                        }
                        break;

                    case "resume":
                        if (!mgr.isPaused()) {
                            ChatUtils.sendMessage(PREFIX + "§cTimer is not paused.");
                        } else {
                            mgr.resume();
                            ChatUtils.sendMessage(PREFIX + "§aResumed. §b" + TimeFormatter.formatCountdown(mgr.getRemainingMs()) + "§a remaining.");
                        }
                        break;

                    case "add": {
                        if (args.length < 2) {
                            ChatUtils.sendMessage(PREFIX + "§cUsage: §e/athrtimeradd <time> §7(e.g. 5m, 1h)");
                            break;
                        }
                        long addMs = TimeFormatter.parseDurationMs(String.join("", Arrays.copyOfRange(args, 1, args.length)));
                        if (addMs <= 0) {
                            ChatUtils.sendMessage(PREFIX + "§cInvalid time. Examples: §e5m §c| §e30s §c| §e1h");
                            break;
                        }
                        if (!mgr.isActive()) {
                            mgr.start(addMs);
                            ChatUtils.sendMessage(PREFIX + "§aStarted §b" + TimeFormatter.formatCountdown(addMs) + "§a timer.");
                        } else {
                            mgr.addTime(addMs);
                            ChatUtils.sendMessage(PREFIX + "§aAdded §b" + TimeFormatter.formatCountdown(addMs) + "§a. Now §b" + TimeFormatter.formatCountdown(mgr.getRemainingMs()) + "§a remaining.");
                        }
                        break;
                    }

                    default: {
                        long durationMs = TimeFormatter.parseDurationMs(String.join("", args));
                        if (durationMs <= 0) {
                            ChatUtils.sendMessage(PREFIX + "§cUnknown sub-command or invalid time. " + "Examples: §e/athrtimer1h30m §c| §e/athrtimerpause §c| §e/athrtimershow");
                            break;
                        }
                        mgr.start(durationMs);
                        ChatUtils.sendMessage(PREFIX + "§aStarted! Counting down from §b" + TimeFormatter.formatCountdown(durationMs) + "§a.");
                        break;
                    }
                }
            });
        });
    }
}
