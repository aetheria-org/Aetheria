package io.hamlook.aetheria.features.waypoints;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RegisterEvents
public class WaypointCommand {

    public static final String PREFIX = "§3[ATHRW]§b ";
    private static final Minecraft mc = MinecraftCompat.getMinecraft();
    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "load", "unload", "setup", "reset", "skip", "unskip", "skipto", "enable", "disable", "create", "delete", "add", "insert", "remove", "rename", "export", "import", "range", "time", "save", "info", "manage", "guide");

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("jw", builder -> {
            builder.setAliases(Arrays.asList("athrw", "waypoints", "asmw"));
            builder.description = "Waypoint commands";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(args -> {
                WaypointState state = WaypointState.getInstance();
                WaypointStorage storage = WaypointStorage.getInstance();

                if (args.length == 0) {
                    showGroupList();
                    return;
                }

                switch (args[0].toLowerCase()) {

                    case "load": {
                        if (args.length < 2) {
                            error("Usage: /athrw load <group>");
                            return;
                        }
                        WaypointGroup g = storage.getGroup(args[1]);
                        if (g == null) {
                            error("Group '" + args[1] + "' not found");
                            return;
                        }
                        state.load(g);
                        success("Loaded group: &e" + g.name + " &7(&e" + g.waypoints.size() + " waypoints&7)");
                        break;
                    }

                    case "unload":
                    case "clear":
                        state.unload();
                        success("Waypoints unloaded");
                        break;

                    case "skip": {
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        int n = args.length >= 2 ? parseIntSafe(args[1], 1) : 1;
                        state.skip(n);
                        success("Skipped " + n + " – now at &e" + (state.currentIndex + 1) + "&7/&e" + state.size());
                        break;
                    }

                    case "unskip": {
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        int n = args.length >= 2 ? parseIntSafe(args[1], 1) : 1;
                        state.skip(-n);
                        success("Went back " + n + " – now at &e" + (state.currentIndex + 1) + "&7/&e" + state.size());
                        break;
                    }

                    case "skipto": {
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        if (args.length < 2) {
                            error("Usage: /athrw skipto <number>");
                            return;
                        }
                        int n = parseIntSafe(args[1], -1);
                        if (n < 1 || n > state.size()) {
                            error("Index out of range (1–" + state.size() + ")");
                            return;
                        }
                        state.skipTo(n - 1);
                        success("Jumped to waypoint &e" + n);
                        break;
                    }

                    case "reset":
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        state.reset();
                        success("Reset to waypoint 1");
                        break;

                    case "list":
                        showGroupList();
                        break;

                    case "create": {
                        if (args.length < 2) {
                            error("Usage: /athrw create <n> [description]");
                            return;
                        }
                        String name = args[1].toLowerCase();
                        if (storage.getGroup(name) != null) {
                            error("Group '" + name + "' already exists");
                            return;
                        }
                        storage.putGroup(new WaypointGroup(name, args.length > 2 ? joinFrom(args, 2) : ""));
                        storage.saveIfDirty();
                        success("Created group: &e" + name);
                        break;
                    }

                    case "delete": {
                        if (args.length < 2) {
                            error("Usage: /athrw delete <group>");
                            return;
                        }
                        String name = args[1].toLowerCase();
                        if (state.loadedGroup != null && state.loadedGroup.name.equalsIgnoreCase(name)) state.unload();
                        if (storage.removeGroup(name)) {
                            storage.saveIfDirty();
                            success("Deleted group: &e" + name);
                        } else error("Group not found: " + name);
                        break;
                    }

                    case "rename": {
                        if (args.length < 3) {
                            error("Usage: /athrw rename <old> <new>");
                            return;
                        }
                        String oldName = args[1].toLowerCase(), newName = args[2].toLowerCase();
                        WaypointGroup g = storage.getGroup(oldName);
                        if (g == null) {
                            error("Group not found: " + oldName);
                            return;
                        }
                        storage.removeGroup(oldName);
                        g.name = newName;
                        storage.putGroup(g);
                        storage.saveIfDirty();
                        if (state.loadedGroup != null && state.loadedGroup.name.equalsIgnoreCase(oldName))
                            state.loadedGroup.name = newName;
                        success("Renamed &e" + oldName + " &a→ &e" + newName);
                        break;
                    }

                    case "add": {
                        WaypointGroup target = state.loadedGroup;
                        if (target == null) {
                            error("No group loaded. Use /athrw load <n> first");
                            return;
                        }
                        if (args.length >= 4 && isDouble(args[1]) && isDouble(args[2]) && isDouble(args[3])) {
                            double x = parseDoubleSafe(args[1], 0);
                            double y = parseDoubleSafe(args[2], 0);
                            double z = parseDoubleSafe(args[3], 0);
                            String name = args.length >= 5 ? joinFrom(args, 4) : String.valueOf(target.waypoints.size() + 1);
                            addWaypointAt(target, x, y, z, name);
                        } else {
                            String name = args.length >= 2 ? joinFrom(args, 1) : String.valueOf(target.waypoints.size() + 1);
                            addWaypoint(target, name);
                        }
                        storage.markDirty();
                        storage.saveIfDirty();
                        break;
                    }

                    case "insert": {
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        if (args.length < 2) {
                            error("Usage: /athrw insert <index> [name]");
                            return;
                        }
                        int idx = parseIntSafe(args[1], -1);
                        if (idx < 1 || idx > state.size() + 1) {
                            error("Index out of range (1–" + (state.size() + 1) + ")");
                            return;
                        }
                        String wpName = args.length >= 3 ? args[2] : String.valueOf(idx);
                        double bx = Math.floor(MinecraftCompat.getLocalPlayer().posX), by = Math.floor(MinecraftCompat.getLocalPlayer().posY) - 1, bz = Math.floor(MinecraftCompat.getLocalPlayer().posZ);
                        state.loadedGroup.waypoints.add(idx - 1, new WaypointPoint(bx, by, bz, wpName));
                        renumberNumericNames(state.loadedGroup, idx);
                        storage.markDirty();
                        storage.saveIfDirty();
                        success("Inserted &e" + wpName + " &aat index &e" + idx + " &7(" + (int) bx + ", " + (int) by + ", " + (int) bz + ")");
                        break;
                    }

                    case "remove": {
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        if (args.length < 2) {
                            error("Usage: /athrw remove <index>");
                            return;
                        }
                        int idx = parseIntSafe(args[1], -1);
                        if (idx < 1 || idx > state.size()) {
                            error("Index out of range (1–" + state.size() + ")");
                            return;
                        }
                        WaypointPoint removed = state.loadedGroup.waypoints.remove(idx - 1);
                        storage.markDirty();
                        storage.saveIfDirty();
                        success("Removed &e" + (removed.name != null ? removed.name : String.valueOf(idx)));
                        break;
                    }

                    case "export": {
                        if (args.length < 2) {
                            error("Usage: /athrw export <group>");
                            return;
                        }
                        WaypointGroup g = storage.getGroup(args[1]);
                        if (g == null) {
                            error("Group not found: " + args[1]);
                            return;
                        }
                        ClipboardCompat.setClipboard(exportSoopy(g));
                        success("Copied group '" + g.name + "' to clipboard");
                        break;
                    }

                    case "import": {
                        if (args.length < 2) {
                            error("Usage: /athrw import <groupname>");
                            return;
                        }
                        String name = args[1].toLowerCase();
                        String clip = ClipboardCompat.getClipboard();
                        if (clip == null || clip.trim().isEmpty()) {
                            error("Clipboard is empty");
                            return;
                        }
                        List<WaypointPoint> wps = parseSoopy(clip.trim());
                        if (wps == null) {
                            error("Could not parse clipboard as soopy waypoints");
                            return;
                        }
                        WaypointGroup g = storage.getGroup(name);
                        if (g == null) g = new WaypointGroup(name);
                        g.waypoints = wps;
                        storage.putGroup(g);
                        storage.saveIfDirty();
                        success("Imported &e" + wps.size() + " &awaypoints into &e" + name);
                        break;
                    }

                    case "setup":
                        state.setupMode = !state.setupMode;
                        success("Setup mode: " + (state.setupMode ? "&2ON" : "&4OFF"));
                        break;

                    case "enable":
                        state.enabled = true;
                        success("Waypoints enabled");
                        break;

                    case "disable":
                        state.enabled = false;
                        error("Waypoints disabled");
                        break;

                    case "range": {
                        if (args.length < 2) {
                            data("Advance range", state.advanceRange + " blocks");
                            return;
                        }
                        double r = parseDoubleSafe(args[1], -1);
                        if (r <= 0) {
                            error("Invalid range");
                            return;
                        }
                        state.advanceRange = r;
                        success("Advance range set to &e" + r + " blocks");
                        break;
                    }

                    case "time": {
                        if (args.length < 2) {
                            data("Advance delay", state.advanceDelayMs + "ms");
                            return;
                        }
                        long t = parseLongSafe(args[1]);
                        if (t <= 0) {
                            error("Invalid delay");
                            return;
                        }
                        state.advanceDelayMs = t;
                        success("Advance delay set to &e" + t + "ms");
                        break;
                    }

                    case "save":
                        storage.saveForce();
                        success("Saved all groups to config");
                        break;

                    case "info": {
                        if (!state.hasGroup()) {
                            error("No group loaded");
                            return;
                        }
                        WaypointGroup g = state.loadedGroup;
                        blank();
                        header("Group Information");
                        data("Name", g.name);
                        data("Position", (state.currentIndex + 1) + "/" + g.waypoints.size());
                        data("Setup mode", String.valueOf(state.setupMode));
                        data("Advance range", state.advanceRange + "m");
                        data("Delay", state.advanceDelayMs + "ms");
                        blank();
                        break;
                    }

                    case "manage":
                        ATHRConfig.openWaypointGroupGui();
                        break;

                    case "guide": {
                        blank();
                        header("Waypoints Guide");
                        line("/athrw list", "Show all waypoint groups");
                        line("/athrw create <n>", "Create a new group");
                        line("/athrw delete <n>", "Delete a group");
                        line("/athrw rename <old> <new>", "Rename a group");
                        line("/athrw load <n>", "Load a group");
                        line("/athrw add [name]", "Add waypoint at your position");
                        line("/athrw insert <index>", "Insert waypoint at index");
                        line("/athrw remove <index>", "Remove waypoint");
                        line("/athrw skip [n]", "Skip forward");
                        line("/athrw unskip [n]", "Go backward");
                        line("/athrw skipto <n>", "Jump to waypoint");
                        line("/athrw reset", "Reset to first waypoint");
                        line("/athrw setup", "Toggle setup mode");
                        line("/athrw enable / disable", "Toggle rendering");
                        line("/athrw export <n>", "Copy group to clipboard");
                        line("/athrw import <n>", "Import from clipboard");
                        line("/athrw range <blocks>", "Set auto-advance range");
                        line("/athrw time <ms>", "Set auto-advance delay");
                        line("/athrw manage", "Open group manager GUI");
                        blank();
                        break;
                    }

                    default:
                        error("Unknown subcommand '&e" + args[0] + "&c'. Try &e/athrw guide &cfor help.");
                }
            });
        });
    }

    private void showGroupList() {
        Map<String, WaypointGroup> groups = WaypointStorage.getInstance().getGroups();
        blank();
        header("Waypoint Groups");
        if (groups.isEmpty()) {
            error("No groups saved");
            blank();
            return;
        }
        for (WaypointGroup g : groups.values()) {
            IChatComponent root = TextCompat.createText("");

            IChatComponent name = TextCompat.createText(EnumChatFormatting.AQUA + g.name + EnumChatFormatting.GRAY + " (" + g.waypoints.size() + " wps)");
            if (g.description != null && !g.description.isEmpty())
                name.appendText(EnumChatFormatting.DARK_GRAY + " – " + g.description);
            TextCompat.appendSibling(root, name);

            IChatComponent load = TextCompat.createText(" " + EnumChatFormatting.YELLOW + EnumChatFormatting.BOLD + "[LOAD]");
            TextCompat.setClickRunCommand(TextCompat.getChatStyle(load), "/athrw load " + g.name);
            TextCompat.setHoverShowText(TextCompat.getChatStyle(load), "Load " + g.name);
            TextCompat.appendSibling(root, load);

            IChatComponent export = TextCompat.createText(" " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "[EXPORT]");
            TextCompat.setClickRunCommand(TextCompat.getChatStyle(export), "/athrw export " + g.name);
            TextCompat.setHoverShowText(TextCompat.getChatStyle(export), "Copy to clipboard");
            TextCompat.appendSibling(root, export);

            IChatComponent del = TextCompat.createText(" " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "[DEL]");
            TextCompat.setClickRunCommand(TextCompat.getChatStyle(del), "/athrw delete " + g.name);
            TextCompat.setHoverShowText(TextCompat.getChatStyle(del), "Delete " + g.name);
            TextCompat.appendSibling(root, del);

            ChatUtils.sendMessage(root);
        }
        blank();
    }

    private void addWaypoint(WaypointGroup group, String name) {
        double bx = Math.floor(MinecraftCompat.getLocalPlayer().posX), by = Math.floor(MinecraftCompat.getLocalPlayer().posY) - 1, bz = Math.floor(MinecraftCompat.getLocalPlayer().posZ);
        addWaypointAt(group, bx, by, bz, name);
    }

    private void addWaypointAt(WaypointGroup group, double x, double y, double z, String name) {
        group.waypoints.add(new WaypointPoint(x, y, z, name));
        success("Added &e" + name + " &aat (" + (int) x + ", " + (int) y + ", " + (int) z + ") to &e" + group.name + " &7(&e" + group.waypoints.size() + "&7 total)");
    }

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void renumberNumericNames(WaypointGroup group, int fromOneBasedIndex) {
        for (int i = fromOneBasedIndex; i < group.waypoints.size(); i++) {
            WaypointPoint waypoint = group.waypoints.get(i);

            try {
                if (Integer.parseInt(waypoint.name) == i) {
                    waypoint.name = String.valueOf(i + 1);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String exportSoopy(WaypointGroup group) {
        return WaypointGroupGui.exportSoopy(group);
    }

    private List<WaypointPoint> parseSoopy(String json) {
        return WaypointGroupGui.parseSoopy(json);
    }

    private void header(String text) {
        ChatUtils.sendMessage(color(PREFIX + "&6" + text));
    }

    private void line(String cmd, String desc) {
        ChatUtils.sendMessage(color(PREFIX + "&b" + cmd + " &7- " + desc));
    }

    private void data(String key, String value) {
        ChatUtils.sendMessage(color(PREFIX + "&b" + key + ": &e" + value));
    }

    private void success(String text) {
        ChatUtils.sendMessage(color(PREFIX + "&a" + text));
    }

    private void error(String text) {
        ChatUtils.sendMessage(color(PREFIX + "&c" + text));
    }

    private void blank() {
        ChatUtils.sendMessage("");
    }

    private String color(String s) {
        return s.replace("&", "§");
    }

    private int parseIntSafe(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return d;
        }
    }

    private double parseDoubleSafe(String s, double d) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return d;
        }
    }

    private long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return -1;
        }
    }

    private String joinFrom(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }
}
