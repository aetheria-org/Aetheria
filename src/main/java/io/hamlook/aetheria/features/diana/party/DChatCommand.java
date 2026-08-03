package io.hamlook.aetheria.features.diana.party;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.CommunityAccess;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

@RegisterCommand
public class DChatCommand extends ASMCommand {

    @Override
    public String getName() {
        return "dpc";
    }

    @Override
    public String getUsage() {
        return "/" + getName() + " <message>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if(args.length == 0){
            ChatUtils.sendMessage("§b[D-Party] §cPlease enter a message.");
            return;
        }
        CommunityAccess.runIfAllowed(
                "§cDiana Party chat requires your account to be Synced (use /sync) or to be on SkyBlock.",
                () -> {
                    String message = String.join(" ", args);
                    DianaPartyConnector.sendMessage(message).thenAccept(response -> {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        int code = json.getAsJsonObject("data").get("code").getAsInt();
                        if(code != 200){
                            String msg = json.getAsJsonObject("data").get("message").getAsString();
                            ChatUtils.sendMessage("§b[D-Party] §cError Sending Message§7[§c" + code + "§7]: §c" + msg);
                        }
                    });
                }
        );
    }
}
