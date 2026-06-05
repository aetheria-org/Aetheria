package io.hamlook.aetheria.command;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class Command extends ASMCommand {

    @Override
    public String getName() { return "athr"; }

    @Override
    public String getUsage() { return "/athr | /athr config | /athr <category> | /athr reload"; }

    @Override
    public List<String> getAliases() { return Arrays.asList("aetheria","jef","asm"); }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    ATHRConfig.reloadRepo();
                    ChatUtils.sendMessage("§a[ATHR] §fRepo refresh triggered.");
                    break;
                case "config":
                    ATHRConfig.openGui();
                    break;
                default:
                    ATHRConfig.openCategory(StringUtils.join(args, " "));
                    break;
            }
        } else {
            ATHRConfig.openOptionsGui();
        }
    }
}