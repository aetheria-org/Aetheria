package io.hamlook.aetheria.features.misc.itemlog;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.misc.ItemLogAlertsConfig;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;

import java.util.*;

@RegisterCommand
public class ItemLogAlertsCommand extends ASMCommand {

    private static final String PREFIX = "§b[ItemAlert] §7";
    private static final List<String> SUBCOMMANDS = Arrays.asList("add", "remove", "list", "clear");

    @Override
    public String getName() {
        return "itemlogalert";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("ila", "itemalert");
    }

    @Override
    public String getUsage() {
        return "/itemlogalert <add|remove|list|clear>";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        ItemLogAlertsConfig config = ATHRConfig.feature.misc.itemLogAlerts;
        Map<String, ItemLogAlertsConfig.AlertEntry> alerts = config.alerts;
        if (alerts == null) return;

        if (args.length == 0) {
            showAlertList(sender, alerts);
            return;
        }

        switch (args[0].toLowerCase()) {

            case "add": {
                if (args.length < 2) {
                    ChatUtils.sendMessage(PREFIX + "§cUsage: /itemlogalert add <skyblockId> [text...]");
                    return;
                }
                String id = args[1].toLowerCase();
                String text = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
                alerts.put(id, new ItemLogAlertsConfig.AlertEntry(text));
                ATHRConfig.saveConfig();
                ChatUtils.sendMessage(PREFIX + "§aAdded alert for §f" + id);
                break;
            }

            case "remove": {
                if (args.length < 2) {
                    ChatUtils.sendMessage(PREFIX + "§cUsage: /itemlogalert remove <skyblockId>");
                    return;
                }
                String id = args[1].toLowerCase();
                ItemLogAlertsConfig.AlertEntry removed = alerts.remove(id);
                if (removed != null) {
                    ATHRConfig.saveConfig();
                    ChatUtils.sendMessage(PREFIX + "§aRemoved alert for §f" + id);
                } else {
                    ChatUtils.sendMessage(PREFIX + "§cNo alert found for §f" + id);
                }
                break;
            }

            case "list":
                showAlertList(sender, alerts);
                break;
            case "clear": {
                int count = alerts.size();
                alerts.clear();
                ATHRConfig.saveConfig();
                ChatUtils.sendMessage(PREFIX + "§aRemoved §f" + count + " §aalerts.");
                break;
            }
            default:
                ChatUtils.sendMessage(PREFIX + "§cUnknown subcommand. Use: add, remove, list, clear");
        }
    }

    private void showAlertList(ICommandSender sender, Map<String, ItemLogAlertsConfig.AlertEntry> alerts) {
        sender.addChatMessage(TextCompat.createText(""));
        sender.addChatMessage(TextCompat.createText("§b§lItem Log Alerts"));

        if (alerts.isEmpty()) {
            sender.addChatMessage(TextCompat.createText(" §7No alerts configured."));
        } else {
            for (Map.Entry<String, ItemLogAlertsConfig.AlertEntry> e : alerts.entrySet()) {
                String id = e.getKey();
                ItemLogAlertsConfig.AlertEntry entry = e.getValue();
                String text = entry.customText.isEmpty() ? "§o<display name>§r" : entry.customText.replace("§", "&");
                IChatComponent root = TextCompat.createText("");
                IChatComponent label = TextCompat.createText(" §7- §f" + id + " §8" + text);
                TextCompat.appendSibling(root, label);
                IChatComponent del = TextCompat.createText(" §c§l[DEL]");
                TextCompat.setClickRunCommand(TextCompat.getChatStyle(del), "/ila remove " + id);
                TextCompat.setHoverShowText(TextCompat.getChatStyle(del), "§cRemove " + id);
                TextCompat.appendSibling(root, del);

                sender.addChatMessage(root);
            }
        }

        sender.addChatMessage(TextCompat.createText(""));
        IChatComponent addNew = TextCompat.createText("§a§l[ADD NEW]");
        TextCompat.setClickSuggestCommand(TextCompat.getChatStyle(addNew), "/ila add ");
        TextCompat.setHoverShowText(TextCompat.getChatStyle(addNew), "§aAdd a new alert");
        sender.addChatMessage(addNew);
        IChatComponent clear = TextCompat.createText(" §c§l[CLEAR ALL]");
        TextCompat.setClickRunCommand(TextCompat.getChatStyle(clear), "/ila clear");
        TextCompat.setHoverShowText(TextCompat.getChatStyle(clear), "§cRemove all alerts");
        sender.addChatMessage(clear);
        sender.addChatMessage(TextCompat.createText(""));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return SUBCOMMANDS;
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            Map<String, ItemLogAlertsConfig.AlertEntry> alerts = ATHRConfig.feature.misc.itemLogAlerts.alerts;
            if (alerts != null && !alerts.isEmpty()) return new ArrayList<>(alerts.keySet());
        }

        return Collections.emptyList();
    }
}
