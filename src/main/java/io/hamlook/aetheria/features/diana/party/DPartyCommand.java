package io.hamlook.aetheria.features.diana.party;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RegisterCommand
public class DPartyCommand extends ASMCommand {
    @Override
    public String getName() {
        return "dparty";
    }

    @Override
    public String getUsage() {
        return "/" + getName() + getArgs();
    }

    private String getArgs() {
        return "<join|create|leave|disband|transfer|kick|setpass>";
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        String[] options = new String[] {"join", "create", "leave", "disband","transfer","kick","setpass"};
        if(args.length == 0) return Arrays.asList(options);
        if(args.length == 1){
            String argument = args[0];
            return Arrays.stream(options).filter(s -> s.toLowerCase().startsWith(argument.toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 1) {
            ChatUtils.sendMessage("§cPlease Enter a Sub Command");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "create":
                createParty(args);
                break;
            case "join":
                joinParty(args);
                break;
            case "leave":
                leaveParty();
                break;
            case "disband":
                disbandParty();
                break;
            case "transfer":
                transferParty(args);
                break;
            case "kick":
                kickFromParty(args);
                break;
            case "setpass":
                setPartyPass(args);
                break;
        }
    }

    public void kickFromParty(String[] args) {
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (!DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        if (args.length < 2) {
            ChatUtils.sendMessage("§cPlease enter a valid party member IGN");
            return;
        }
        CompletableFuture<String> future = DianaPartyConnector.kickFromParty(args[1].toLowerCase());
        if(future == null){
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = json.getAsJsonObject("data");
            int code = data.get("code").getAsInt();
            if(code == 200){
                String kickedPlayer = data.get("kicked").getAsString();
                ChatUtils.sendMessage("§aSuccessfully Kicked " + kickedPlayer + " from the party.");
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Kicking Player§7[§c" + code + "§7]: §c" + msg);
            }
        });
    }

    public void setPartyPass(String[] args) {
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (!DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        if (args.length < 2) {
            ChatUtils.sendMessage("§cPlease enter a valid password.");
            return;
        }
        CompletableFuture<String> future = DianaPartyConnector.setPartyPass(args[1].toLowerCase());
        if(future == null){
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = json.getAsJsonObject("data");
            int code = data.get("code").getAsInt();
            if(code == 200){
                String newPass = data.get("newPass").getAsString();
                ChatUtils.sendMessage("§aSuccessfully Updated Password to " + newPass);
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Changing Password§7[§c" + code + "§7]: §c" + msg);
            }
        });
    }

    public void transferParty(String[] args) {
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (!DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        if (args.length < 2) {
            ChatUtils.sendMessage("§cPlease enter a valid party member IGN");
            return;
        }
        CompletableFuture<String> future = DianaPartyConnector.transferParty(args[1].toLowerCase());
        if(future == null){
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonObject data = json.getAsJsonObject("data");
            int code = data.get("code").getAsInt();
            if(code == 200){
                String oldCreator =  data.get("old").getAsString();
                String newCreator =  data.get("new").getAsString();
                ChatUtils.sendMessage("§aSuccessfully Transferred Diana Party from " + oldCreator + " to " + newCreator);
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Transferring Party§7[§c" + code + "§7]: §c" + msg);
            }
        });
    }
    public void disbandParty() {
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (!DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        CompletableFuture<String> future = DianaPartyConnector.disbandParty();
        if(future == null){
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            int code = json.getAsJsonObject("data").get("code").getAsInt();
            if(code == 200){
                ChatUtils.sendMessage("§aSuccessfully Disbanded Diana Party.");
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Disbanding Party§7[§c" + code + "§7]: §c" + msg);
            }
        });
    }

    public void leaveParty() {
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (!DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        CompletableFuture<String> future = DianaPartyConnector.leaveParty();
        if(future == null){
            ChatUtils.sendMessage("§cYou are not in a Diana Party.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            int code = json.getAsJsonObject("data").get("code").getAsInt();
            if(code == 200){
                ChatUtils.sendMessage("§aSuccessfully Left Diana Party.");
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Leaving Party§7[§c" + code + "§7]: §c" + msg);
            }
        });
    }

    public void joinParty(String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage("§cPlease enter a valid party ID");
            return;
        }
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are already in a diana party, Please leave or disband the party before joining a new one.");
            return;
        }
        String pID = args[1];
        String password = "";
        if(args.length > 2){
            password = args[2];
        }
        CompletableFuture<String> future = DianaPartyConnector.joinParty(pID,password);
        if (future == null){
            ChatUtils.sendMessage("§cEncountered an error while joining party, Please try again in 15 seconds.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            int code = json.getAsJsonObject("data").get("code").getAsInt();
            if(code == 200){
                String name = json.getAsJsonObject("data").get("partyName").getAsString();
                ChatUtils.sendMessage("§aSuccessfully Joined Diana Party: " + name);
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Joining Party§7[§c" + code + "§7]: §c" + msg);
            }
        }).exceptionally(ex -> {
            Aetheria.logger.severe("[D-Party] Join Error: " + ex.getMessage());
            return null;
        });
    }

    public void createParty(String[] args) throws CommandException {
        if (args.length < 2) {
            ChatUtils.sendMessage("§cPlease enter a valid party name");
            return;
        }
        if (!DianaPartyConnector.isConnected) {
            ChatUtils.sendMessage("§cYou are not connected to the api, please try again. If the issue persists, make sure you have API usage allowed");
            if (NetworkGuard.apiAllowed()) {
                DianaPartyConnector.connectToAPI();
            }
            return;
        }
        if (DianaPartyConnector.isInParty()) {
            ChatUtils.sendMessage("§cYou are already in a diana party, Please leave or disband the party before making a new one.");
            return;
        }
        String pName = args[1];
        String password = "";
        if(args.length > 2){
            password = args[2];
        }
        CompletableFuture<String> future = DianaPartyConnector.createParty(pName,password);
        if (future == null){
            ChatUtils.sendMessage("§cEncountered an error while creating party, Please try again in 15 seconds.");
            return;
        }
        future.thenAccept(response -> {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            int code = json.getAsJsonObject("data").get("code").getAsInt();
            if(code == 200){
                ChatUtils.sendMessage("§aSuccessfully Created D-Party: " + pName);
            }else {
                String msg = json.getAsJsonObject("data").get("message").getAsString();
                ChatUtils.sendMessage("§cError While Creating Party§7[§c" + code + "§7]: §c" + msg);
            }
        });
    }
}
