package io.hamlook.aetheria.utils.compat;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.List;

@RegisterCommand
public class IncompatIgnoreCommand extends ASMCommand {

    @Override
    public String getName() {
        return "athrignoreincompat";
    }

    @Override
    public String getUsage() {
        return "/athrignoreincompat <ignore|reset|list> [modId]";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("athri", "aii");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            usage();
            return;
        }
        String action = args[0].toLowerCase();
        switch (action) {
            case "ignore": {
                if (args.length < 2) {
                    usage();
                    return;
                }
                String key = args[1].toLowerCase();
                if (IncompatModChecker.dismiss(key)) {
                    ChatUtils.sendMessage("§a[ATHR] §fWarning hidden for §e" + key + "§f. Use 'reset " + key + "' to show it again.");
                } else {
                    ChatUtils.sendMessage("§e[ATHR] §fWarning for §e" + key + " §fwas already hidden.");
                }
                break;
            }
            case "reset": {
                if (args.length < 2) {
                    usage();
                    return;
                }
                String key = args[1].toLowerCase();
                if (IncompatModChecker.reset(key)) {
                    ChatUtils.sendMessage("§a[ATHR] §fWarning for §e" + key + " §fwill show again on next launch.");
                } else {
                    ChatUtils.sendMessage("§e[ATHR] §fNo hidden warning for §e" + key + "§f.");
                }
                break;
            }
            case "list": {
                if (IncompatModChecker.dismissed().isEmpty()) {
                    ChatUtils.sendMessage("§e[ATHR] §fNo hidden incompatibility warnings.");
                } else {
                    ChatUtils.sendMessage("§e[ATHR] §fHidden warnings: §7" + String.join(", ", IncompatModChecker.dismissed()));
                }
                break;
            }
            default:
                usage();
        }
    }

    private void usage() {
        ChatUtils.sendMessage("§e[ATHR] §fUsage: " + getUsage());
    }
}
