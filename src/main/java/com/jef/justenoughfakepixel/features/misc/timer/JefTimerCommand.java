package com.jef.justenoughfakepixel.features.misc.timer;

import com.jef.justenoughfakepixel.core.config.command.SimpleCommand;
import com.jef.justenoughfakepixel.init.RegisterCommand;
import com.jef.justenoughfakepixel.utils.chat.ChatUtils;
import com.jef.justenoughfakepixel.utils.time.TimeFormatter;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RegisterCommand
public class JefTimerCommand extends SimpleCommand {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);

    private static final String PREFIX = "§b[JEF Timer] §f";
    private static final List<String> TAB_OPTS = Arrays.asList("cancel", "pause", "resume", "show", "add");

    private static long parseTime(String input) {
        Matcher m = TIME_PATTERN.matcher(input);
        long total = 0;
        boolean any = false;
        while (m.find()) {
            any = true;
            long val = Long.parseLong(m.group(1));
            switch (m.group(2).toLowerCase()) {
                case "d":
                    total += val * 86_400_000L;
                    break;
                case "h":
                    total += val * 3_600_000L;
                    break;
                case "m":
                    total += val * 60_000L;
                    break;
                case "s":
                    total += val * 1_000L;
                    break;
            }
        }
        return any ? total : -1L;
    }

    private static void printStatus(UptimeManager mgr) {
        if (!mgr.isActive()) {
            ChatUtils.sendMessage(PREFIX + "No timer running. Use §e/jeftimer <time>§f to start one.");
            return;
        }
        String state = mgr.isPaused() ? " §7(paused)§f" : "";
        ChatUtils.sendMessage(PREFIX + "Remaining: §b" + TimeFormatter.formatCountdown(mgr.getRemainingMs()) + "§f" + state);
    }

    @Override
    public String getName() {
        return "jeftimer";
    }

    @Override
    public String getUsage() {
        return "/jeftimer <time|show|pause|resume|add <time>|cancel>";
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        if (args.length == 1) return TAB_OPTS;
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) return Collections.singletonList("<time>");
        return Collections.emptyList();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        UptimeManager mgr = UptimeManager.getInstance();

        // No arguments print status
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
                    ChatUtils.sendMessage(PREFIX + "§eTimer is already paused. Use §b/jeftimer resume§e.");
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
                    ChatUtils.sendMessage(PREFIX + "§cUsage: §e/jeftimer add <time> §7(e.g. 5m, 1h)");
                    break;
                }
                long addMs = parseTime(String.join("", Arrays.copyOfRange(args, 1, args.length)));
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
                long durationMs = parseTime(String.join("", args));
                if (durationMs <= 0) {
                    ChatUtils.sendMessage(PREFIX + "§cUnknown sub-command or invalid time. " + "Examples: §e/jeftimer 1h30m §c| §e/jeftimer pause §c| §e/jeftimer show");
                    break;
                }
                mgr.start(durationMs);
                ChatUtils.sendMessage(PREFIX + "§aStarted! Counting down from §b" + TimeFormatter.formatCountdown(durationMs) + "§a.");
                break;
            }
        }
    }
}