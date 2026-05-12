package com.jef.justenoughfakepixel.core.config.command;

import com.jef.justenoughfakepixel.core.JefConfig;
import com.jef.justenoughfakepixel.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class JefCommand extends SimpleCommand {

    @Override
    public String getName() { return "jef"; }

    @Override
    public String getUsage() { return "/jef | /jef config | /jef <category> | /jef reload"; }

    @Override
    public List<String> getAliases() { return Collections.singletonList("justenoughfakepixel"); }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            JefConfig.reloadRepo();
            ChatUtils.sendMessage("§a[JEF] §fRepo refresh triggered.");
        } else if (args.length > 0 && args[0].equalsIgnoreCase("config")) {
            JefConfig.openGui();
        } else if (args.length == 0) {
            JefConfig.openOptionsGui();
        } else {
            JefConfig.openCategory(StringUtils.join(args, " "));
        }
    }
}