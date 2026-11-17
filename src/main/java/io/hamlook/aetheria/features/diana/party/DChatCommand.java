package io.hamlook.aetheria.features.diana.party;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.CommunityAccess;
import io.hamlook.aetheria.utils.chat.ChatUtils;

import java.util.concurrent.CompletableFuture;

@RegisterEvents
public class DChatCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("dpc", builder -> {
            builder.description = "Diana party chat";
            builder.setCategory(CommandCategory.COMMUNITY);

            builder.legacyCallbackArgs(args -> {
                if (args.length == 0) {
                    ChatUtils.sendMessage("§b[D-Party] §cPlease enter a message.");
                    return;
                }
                CommunityAccess.runIfAllowed(
                        "§cDiana Party chat requires your account to be Synced (use /sync) or to be on SkyBlock.",
                        () -> {
                            String message = String.join(" ", args);
                            CompletableFuture<String> future = DianaPartyConnector.sendMessage(message);
                            if (future == null) {
                                ChatUtils.sendMessage("§cYou are not connected to the API, dear user, please try again.");
                                return;
                            }
                            future.thenAccept(response -> {
                                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                                int code = json.getAsJsonObject("data").get("code").getAsInt();
                                if (code != 200) {
                                    String msg = json.getAsJsonObject("data").get("message").getAsString();
                                    ChatUtils.sendMessage("§b[D-Party] §cError Sending Message§7[§c" + code + "§7]: §c" + msg);
                                }
                            });
                        }
                );
            });
        });
    }
}
