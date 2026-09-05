package io.hamlook.aetheria.features.misc.itemlog;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.misc.ItemLogAlertsConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;

import java.util.*;

@RegisterEvents
public class ItemLogAlertsCommand {

    private static final String PREFIX = "§b[ItemAlert] §7";

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("itemlogalert", builder -> {
            builder.setAliases(Arrays.asList("ila", "itemalert"));
            builder.description = "Item log alert management";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(args -> {
                ItemLogAlertsConfig config = ATHRConfig.feature.misc.itemLogAlerts;
                Map<String, ItemLogAlertsConfig.AlertEntry> alerts = config.alerts;
                if (alerts == null) return;

                if (args.length == 0) {
                    showAlertList(alerts);
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
                        showAlertList(alerts);
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
            });
        });
    }

    private void showAlertList(Map<String, ItemLogAlertsConfig.AlertEntry> alerts) {
        ChatUtils.sendMessage("");
        ChatUtils.sendMessage("§b§lItem Log Alerts");

        if (alerts.isEmpty()) {
            ChatUtils.sendMessage(" §7No alerts configured.");
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

                ChatUtils.sendMessage(root);
            }
        }

        ChatUtils.sendMessage("");
        IChatComponent addNew = TextCompat.createText("§a§l[ADD NEW]");
        TextCompat.setClickSuggestCommand(TextCompat.getChatStyle(addNew), "/ila add ");
        TextCompat.setHoverShowText(TextCompat.getChatStyle(addNew), "§aAdd a new alert");
        ChatUtils.sendMessage(addNew);
        IChatComponent clear = TextCompat.createText(" §c§l[CLEAR ALL]");
        TextCompat.setClickRunCommand(TextCompat.getChatStyle(clear), "/ila clear");
        TextCompat.setHoverShowText(TextCompat.getChatStyle(clear), "§cRemove all alerts");
        ChatUtils.sendMessage(clear);
        ChatUtils.sendMessage("");
    }
}
