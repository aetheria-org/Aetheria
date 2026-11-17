package io.hamlook.aetheria.features.farming.pests;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.PestAlertConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.features.farming.pests.overlay.PestAlertOverlay;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.time.TimeFormatter;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.Collections;

@RegisterEvents
public class PestAlertCommand {

    private static final String PREFIX = EnumChatFormatting.GOLD + "[ASM] " + EnumChatFormatting.RESET;
    private static final String USAGE = "Usage: /asmpest <time|off|status|test>";

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmpest", builder -> {
            builder.setAliases(Collections.singletonList("pestcd"));
            builder.description = "Pest cooldown alert commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(args -> {
                if (args.length == 0) {
                    printUsage();
                    return;
                }

                switch (args[0].toLowerCase()) {
                    case "off":
                        setEnabled(false);
                        break;
                    case "status":
                        printStatus();
                        break;
                    case "test":
                        PestAlertOverlay.fireTest();
                        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Test alert fired through your enabled channels.");
                        break;
                    case "cd":
                        setTime(Arrays.copyOfRange(args, 1, args.length));
                        break;
                    default:
                        setTime(args);
                        break;
                }
            });
        });
    }

    private static PestAlertConfig config() {
        if (ATHRConfig.feature == null || ATHRConfig.feature.farming == null
                || ATHRConfig.feature.farming.pests == null) {
            return null;
        }
        return ATHRConfig.feature.farming.pests.pestAlert;
    }

    private void setTime(String[] timeArgs) {
        if (timeArgs.length == 0) {
            printUsage();
            return;
        }
        if (timeArgs[0].equalsIgnoreCase("off")) {
            setEnabled(false);
            return;
        }
        long durationMs = TimeFormatter.parseDurationMs(String.join("", timeArgs));
        if (durationMs <= 0) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Couldn't read that time.");
            printExamples();
            return;
        }
        int seconds = (int) Math.max(5, Math.min(300, durationMs / 1000L));
        PestAlertConfig cfg = config();
        if (cfg == null) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Config not loaded yet.");
            return;
        }
        cfg.alertBelowSeconds = seconds;
        cfg.enabled = true;
        ATHRConfig.saveConfig();
        String pretty = seconds >= 60 ? (seconds / 60) + "m " + (seconds % 60) + "s" : seconds + "s";
        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Alerting when the pest cooldown goes below "
                + EnumChatFormatting.YELLOW + pretty + EnumChatFormatting.GRAY + " (" + seconds + "s)"
                + EnumChatFormatting.GREEN + ".");
    }

    private void setEnabled(boolean enabled) {
        PestAlertConfig cfg = config();
        if (cfg == null) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Config not loaded yet.");
            return;
        }
        cfg.enabled = enabled;
        ATHRConfig.saveConfig();
        if (enabled) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Pest alerts enabled (threshold "
                    + EnumChatFormatting.YELLOW + cfg.alertBelowSeconds + "s" + EnumChatFormatting.GREEN + ").");
        } else {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Pest alerts disabled.");
        }
    }

    private void printStatus() {
        PestAlertConfig cfg = config();
        if (cfg == null) {
            ChatUtils.sendMessage(PREFIX + EnumChatFormatting.RED + "Config not loaded yet.");
            return;
        }
        long cooldownMs = FarmingApi.getGardenCooldownMs();
        String cooldown = cooldownMs < 0 ? "not on cooldown / unknown" : (cooldownMs / 1000) + "s";
        ChatUtils.sendMessage(PREFIX + "Enabled: " + (cfg.enabled ? EnumChatFormatting.GREEN + "yes" : EnumChatFormatting.RED + "no")
                + EnumChatFormatting.GRAY + ", alert below: " + cfg.alertBelowSeconds + "s"
                + ", live cooldown: " + cooldown);
    }

    private void printUsage() {
        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.YELLOW + USAGE);
    }

    private void printExamples() {
        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GRAY + "Examples: §e/asmpest 45§7, §e/asmpest 2m30s§7, "
                + "§e/asmpest 2min 45§7, §e/asmpest off");
    }
}
