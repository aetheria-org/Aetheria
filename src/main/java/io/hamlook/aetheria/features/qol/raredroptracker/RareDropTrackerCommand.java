package io.hamlook.aetheria.features.qol.raredroptracker;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.qol.RareDropTrackerConfig;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.IChatComponent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /rdt (Rare Drop Tracker) - search the full item dataset (the same wiki-backed
 * dataset ItemRegistry loads for the item list / recipe viewer) by typing at least
 * the first 3 letters of an item's name, then track it so RareDropTracker can alert
 * you the moment your inventory update event shows you obtained it.
 */
@RegisterEvents
public class RareDropTrackerCommand {

    private static final String PREFIX = "§d[RareDrops] §7";
    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULTS = 15;

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("raredroptracker", builder -> {
            builder.setAliases(Arrays.asList("rdt", "rdtracker"));
            builder.description = "Rare drop tracker commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(args -> {
                RareDropTrackerConfig config = ATHRConfig.feature.qol.rareDropTracker;

                if (args.length == 0) {
                    ATHRConfig.openRareDropTrackerGui();
                    return;
                }

                switch (args[0].toLowerCase()) {
                    case "gui":
                        ATHRConfig.openRareDropTrackerGui();
                        break;

                    case "search": {
                        String query = joinArgs(args);
                        if (validateQuery(query)) return;
                        List<SkyblockItem> matches = search(query);
                        showResults(query, matches, false);
                        break;
                    }

                    case "add": {
                        String query = joinArgs(args);
                        if (validateQuery(query)) return;

                        Optional<SkyblockItem> exact = findExactId(query);
                        if (exact.isPresent()) {
                            track(config, exact.get());
                            return;
                        }

                        List<SkyblockItem> matches = search(query);
                        if (matches.isEmpty()) {
                            ChatUtils.sendMessage(PREFIX + "§cNo items found matching §f\"" + query + "\"§c.");
                        } else if (matches.size() == 1) {
                            track(config, matches.get(0));
                        } else {
                            showResults(query, matches, true);
                        }
                        break;
                    }

                    case "remove": {
                        if (args.length < 2) {
                            ChatUtils.sendMessage(PREFIX + "§cUsage: /rdt remove <skyblockId>");
                            return;
                        }
                        String id = args[1].toLowerCase();
                        RareDropTrackerConfig.TrackedItem removed = config.trackedItems.remove(id);
                        if (removed != null) {
                            ATHRConfig.saveConfig();
                            ChatUtils.sendMessage(PREFIX + "§aStopped tracking §f" + removed.displayName);
                        } else {
                            ChatUtils.sendMessage(PREFIX + "§cYou aren't tracking §f" + id);
                        }
                        break;
                    }

                    case "list":
                        showList(config);
                        break;

                    case "clear": {
                        int count = config.trackedItems.size();
                        config.trackedItems.clear();
                        ATHRConfig.saveConfig();
                        ChatUtils.sendMessage(PREFIX + "§aCleared §f" + count + " §atracked item" + (count == 1 ? "" : "s") + ".");
                        break;
                    }

                    default:
                        ChatUtils.sendMessage(PREFIX + "§cUnknown subcommand. Use: search, add, remove, list, clear");
                }
            });
        });
    }

    private boolean validateQuery(String query) {
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            ChatUtils.sendMessage(PREFIX + "§cType at least " + MIN_QUERY_LENGTH + " letters of the item's name.");
            return true;
        }
        if (!ItemRegistry.isLoaded) {
            ChatUtils.sendMessage(PREFIX + "§cThe item database is still loading, try again in a moment.");
            return true;
        }
        return false;
    }

    /**
     * Matches every item in the ItemRegistry dataset (the wiki-backed itemData.json)
     * whose display name or skyblockID contains the given text, case-insensitively.
     */
    private List<SkyblockItem> search(String query) {
        String needle = query.trim().toLowerCase();
        return ItemRegistry.getAllItems().stream().filter(i -> i.cleanNameLower != null && !i.cleanNameLower.isEmpty() && (i.cleanNameLower.contains(needle) || (i.idLower != null && i.idLower.contains(needle)))).sorted(Comparator.comparing(i -> i.cleanNameLower)).collect(Collectors.toList());
    }

    private Optional<SkyblockItem> findExactId(String query) {
        String needle = query.trim().toLowerCase();
        return ItemRegistry.getAllItems().stream().filter(i -> i.idLower != null && i.idLower.equals(needle)).findFirst();
    }

    private void track(RareDropTrackerConfig config, SkyblockItem item) {
        String id = item.skyblockID.toLowerCase();
        if (config.trackedItems.containsKey(id)) {
            ChatUtils.sendMessage(PREFIX + "§eAlready tracking §f" + item.displayName);
            return;
        }
        config.trackedItems.put(id, new RareDropTrackerConfig.TrackedItem(item.displayName != null ? item.displayName : item.skyblockID));
        ATHRConfig.saveConfig();
        ChatUtils.sendMessage(PREFIX + "§aNow tracking §f" + item.displayName + " §7(" + item.skyblockID + ")");
    }

    private void showResults(String query, List<SkyblockItem> matches, boolean addMode) {
        ChatUtils.sendMessage("");
        ChatUtils.sendMessage("§d§lItem Search: §f\"" + query + "\" §7(" + matches.size() + " match" + (matches.size() == 1 ? "" : "es") + ")");

        if (matches.isEmpty()) {
            ChatUtils.sendMessage(" §7No items found.");
        } else {
            List<SkyblockItem> shown = matches.size() > MAX_RESULTS ? matches.subList(0, MAX_RESULTS) : matches;
            for (SkyblockItem item : shown) {
                IChatComponent root = TextCompat.createText(" §7- §f" + item.displayName + " §8(" + item.skyblockID + ")");
                if (addMode) {
                    TextCompat.setClickRunCommand(TextCompat.getChatStyle(root), "/rdt add " + item.skyblockID);
                    TextCompat.setHoverShowText(TextCompat.getChatStyle(root), "§aClick to track this item");
                } else {
                    TextCompat.setClickSuggestCommand(TextCompat.getChatStyle(root), "/rdt add " + item.skyblockID);
                    TextCompat.setHoverShowText(TextCompat.getChatStyle(root), "§eClick to fill in the add command");
                }
                ChatUtils.sendMessage(root);
            }
            if (matches.size() > MAX_RESULTS) {
                ChatUtils.sendMessage(" §7...and " + (matches.size() - MAX_RESULTS) + " more. Narrow your search.");
            }
        }
        ChatUtils.sendMessage("");
    }

    private void showList(RareDropTrackerConfig config) {
        ChatUtils.sendMessage("");
        ChatUtils.sendMessage("§d§lRare Drop Tracker");

        if (config.trackedItems.isEmpty()) {
            ChatUtils.sendMessage(" §7Not tracking anything yet. Try §f/rdt add <name>");
        } else {
            for (Map.Entry<String, RareDropTrackerConfig.TrackedItem> e : config.trackedItems.entrySet()) {
                RareDropTrackerConfig.TrackedItem tracked = e.getValue();
                String progress = tracked.goal > 0 ? " §7(" + tracked.count + "/" + tracked.goal + ")" : (tracked.count > 0 ? " §7(" + tracked.count + ")" : "");
                IChatComponent root = TextCompat.createText(" §7- §f" + tracked.displayName + progress);
                IChatComponent del = TextCompat.createText(" §c§l[DEL]");
                TextCompat.setClickRunCommand(TextCompat.getChatStyle(del), "/rdt remove " + e.getKey());
                TextCompat.setHoverShowText(TextCompat.getChatStyle(del), "§cStop tracking " + tracked.displayName);
                TextCompat.appendSibling(root, del);
                ChatUtils.sendMessage(root);
            }
        }

        ChatUtils.sendMessage("");
        IChatComponent addNew = TextCompat.createText("§a§l[ADD NEW]");
        TextCompat.setClickSuggestCommand(TextCompat.getChatStyle(addNew), "/rdt add ");
        TextCompat.setHoverShowText(TextCompat.getChatStyle(addNew), "§aSearch for an item to track");
        ChatUtils.sendMessage(addNew);
        IChatComponent clear = TextCompat.createText(" §c§l[CLEAR ALL]");
        TextCompat.setClickRunCommand(TextCompat.getChatStyle(clear), "/rdt clear");
        TextCompat.setHoverShowText(TextCompat.getChatStyle(clear), "§cRemove all tracked items");
        ChatUtils.sendMessage(clear);
        ChatUtils.sendMessage("");
    }

    private String joinArgs(String[] args) {
        if (args.length <= 1) return "";
        return String.join(" ", Arrays.copyOfRange(args, 1, args.length));
    }
}
