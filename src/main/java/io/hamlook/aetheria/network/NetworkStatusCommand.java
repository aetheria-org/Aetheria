package io.hamlook.aetheria.network;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Set;


@RegisterCommand
public class NetworkStatusCommand extends ASMCommand {

    @Override
    public String getName() {
        return "athrnet";
    }

    @Override
    public String getUsage() {
        return "/athrnet <enable api|github|offline | hide <token> | unhide <token> | reset | list>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("anet");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            ChatUtils.sendMessage("§eUsage: " + getUsage());
            return;
        }
        switch (args[0].toLowerCase()) {
            case "enable":
                enable(args);
                break;
            case "hide":
                setHidden(args, true);
                break;
            case "unhide":
                setHidden(args, false);
                break;
            case "reset":
                reset();
                break;
            case "list":
                list();
                break;
            default:
                ChatUtils.sendMessage("§eUsage: " + getUsage());
        }
    }

    private void enable(String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage("§eUsage: /athrnet enable <api|github|offline>");
            return;
        }
        NetworkStatusInfo.Gate gate = NetworkStatusInfo.gateFromId(args[1]);
        NetworkGuard.enableGate(gate);
        ChatUtils.sendMessage("§aEnabled " + NetworkStatusInfo.enableLabel(gate) + ".");
    }

    private void setHidden(String[] args, boolean dismiss) {
        if (args.length < 2) {
            ChatUtils.sendMessage("§eUsage: /athrnet " + (dismiss ? "hide" : "unhide") + " <token>");
            return;
        }
        String feature = BlockedFeatureMessenger.featureFromToken(args[1]);
        if (feature == null) {
            ChatUtils.sendMessage("§cInvalid token.");
            return;
        }
        BlockedFeatureMessenger.setDismissed(feature, dismiss);
        ChatUtils.sendMessage(dismiss ? "§7Hidden blocked-feature message for §f" + feature + "§7." : "§aWill show blocked-feature message for §f" + feature + "§a again.");
    }

    private void reset() {
        if (ATHRConfig.feature == null) return;
        ATHRConfig.feature.network.dismissedFeatureGateMessages.clear();
        ATHRConfig.saveConfig();
        ChatUtils.sendMessage("§aReset all hidden blocked-feature messages.");
    }

    private void list() {
        if (ATHRConfig.feature == null) return;
        Set<String> set = ATHRConfig.feature.network.dismissedFeatureGateMessages;
        if (set.isEmpty()) {
            ChatUtils.sendMessage("§7No hidden blocked-feature messages.");
            return;
        }
        ChatUtils.sendMessage("§7Hidden blocked-feature messages: §f" + String.join(", ", set));
    }
}