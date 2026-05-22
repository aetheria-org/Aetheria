package io.hamlook.aetheria.features.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.command.SimpleCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Type;
import java.util.*;

@RegisterCommand
public class WaypointCommand extends SimpleCommand {

    public static final String PREFIX = "§3[ATHRW]§b ";

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "load", "unload", "setup", "reset", "skip", "unskip", "skipto", "enable", "disable", "create", "delete", "add", "insert", "remove", "rename", "export", "import", "range", "time", "save", "info", "manage", "guide");

    @Override
    public String getName() {
        return "jw";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("athrw", "asmw");
    }

    @Override
    public String getUsage() {
        return "/athrw<subcommand>";
    }

    // execute

    @Override
    public void execute(ICommandSender sender, String[] args) throws net.minecraft.command.CommandException {
        WaypointState state = WaypointState.getInstance();
        WaypointStorage storage = WaypointStorage.getInstance();

        if (args.length == 0) {
            showGroupList(sender);
            return;
        }

        switch (args[0].toLowerCase()) {

            // navigation

            case "load": {
                if (args.length < 2) {
                    error(sender, "Usage: /athrwload <group>");
                    return;
                }
                WaypointGroup g = storage.getGroup(args[1]);
                if (g == null) {
                    error(sender, "Group '" + args[1] + "' not found");
                    return;
                }
                state.load(g);
                success(sender, "Loaded group: &e" + g.name + " &7(&e" + g.waypoints.size() + " waypoints&7)");
                break;
            }

            case "unload":
            case "clear":
                state.unload();
                success(sender, "Waypoints unloaded");
                break;

            case "skip": {
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                int n = args.length >= 2 ? parseIntSafe(args[1], 1) : 1;
                state.skip(n);
                success(sender, "Skipped " + n + " – now at &e" + (state.currentIndex + 1) + "&7/&e" + state.size());
                break;
            }

            case "unskip": {
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                int n = args.length >= 2 ? parseIntSafe(args[1], 1) : 1;
                state.skip(-n);
                success(sender, "Went back " + n + " – now at &e" + (state.currentIndex + 1) + "&7/&e" + state.size());
                break;
            }

            case "skipto": {
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                if (args.length < 2) {
                    error(sender, "Usage: /athrwskipto <number>");
                    return;
                }
                int n = parseIntSafe(args[1], -1);
                if (n < 1 || n > state.size()) {
                    error(sender, "Index out of range (1–" + state.size() + ")");
                    return;
                }
                state.skipTo(n - 1);
                success(sender, "Jumped to waypoint &e" + n);
                break;
            }

            case "reset":
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                state.reset();
                success(sender, "Reset to waypoint 1");
                break;

            //  group management

            case "list":
                showGroupList(sender);
                break;

            case "create": {
                if (args.length < 2) {
                    error(sender, "Usage: /athrwcreate <n> [description]");
                    return;
                }
                String name = args[1].toLowerCase();
                if (storage.getGroup(name) != null) {
                    error(sender, "Group '" + name + "' already exists");
                    return;
                }
                storage.putGroup(new WaypointGroup(name, args.length > 2 ? joinFrom(args, 2) : ""));
                storage.saveIfDirty();
                success(sender, "Created group: &e" + name);
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    error(sender, "Usage: /athrwdelete <group>");
                    return;
                }
                String name = args[1].toLowerCase();
                if (state.loadedGroup != null && state.loadedGroup.name.equalsIgnoreCase(name)) state.unload();
                if (storage.removeGroup(name)) {
                    storage.saveIfDirty();
                    success(sender, "Deleted group: &e" + name);
                } else error(sender, "Group not found: " + name);
                break;
            }

            case "rename": {
                if (args.length < 3) {
                    error(sender, "Usage: /athrwrename <old> <new>");
                    return;
                }
                String oldName = args[1].toLowerCase(), newName = args[2].toLowerCase();
                WaypointGroup g = storage.getGroup(oldName);
                if (g == null) {
                    error(sender, "Group not found: " + oldName);
                    return;
                }
                storage.removeGroup(oldName);
                g.name = newName;
                storage.putGroup(g);
                storage.saveIfDirty();
                if (state.loadedGroup != null && state.loadedGroup.name.equalsIgnoreCase(oldName))
                    state.loadedGroup.name = newName;
                success(sender, "Renamed &e" + oldName + " &a→ &e" + newName);
                break;
            }

            //  waypoint editing

            case "add": {
                WaypointGroup target = state.loadedGroup;
                if (target == null) {
                    error(sender, "No group loaded. Use /athrwload <n> first");
                    return;
                }
                // detect: /athrwadd x y z [name]  vs  /athrwadd [name]
                if (args.length >= 4 && isDouble(args[1]) && isDouble(args[2]) && isDouble(args[3])) {
                    double x = parseDoubleSafe(args[1], 0);
                    double y = parseDoubleSafe(args[2], 0);
                    double z = parseDoubleSafe(args[3], 0);
                    String name = args.length >= 5 ? joinFrom(args, 4) : String.valueOf(target.waypoints.size() + 1);
                    addWaypointAt(sender, target, x, y, z, name);
                } else {
                    String name = args.length >= 2 ? joinFrom(args, 1) : String.valueOf(target.waypoints.size() + 1);
                    addWaypoint(sender, target, name);
                }
                storage.markDirty();
                storage.saveIfDirty();
                break;
            }

            case "insert": {
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                if (args.length < 2) {
                    error(sender, "Usage: /athrwinsert <index> [name]");
                    return;
                }
                int idx = parseIntSafe(args[1], -1);
                if (idx < 1 || idx > state.size() + 1) {
                    error(sender, "Index out of range (1–" + (state.size() + 1) + ")");
                    return;
                }
                String JWName = args.length >= 3 ? args[2] : String.valueOf(idx);
                double bx = Math.floor(mc.thePlayer.posX), by = Math.floor(mc.thePlayer.posY) - 1, bz = Math.floor(mc.thePlayer.posZ);
                state.loadedGroup.waypoints.add(idx - 1, new WaypointPoint(bx, by, bz, JWName));
                renumberNumericNames(state.loadedGroup, idx);
                storage.markDirty();
                storage.saveIfDirty();
                success(sender, "Inserted &e" + JWName + " &aat index &e" + idx + " &7(" + (int) bx + ", " + (int) by + ", " + (int) bz + ")");
                break;
            }

            case "remove": {
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                if (args.length < 2) {
                    error(sender, "Usage: /athrwremove <index>");
                    return;
                }
                int idx = parseIntSafe(args[1], -1);
                if (idx < 1 || idx > state.size()) {
                    error(sender, "Index out of range (1–" + state.size() + ")");
                    return;
                }
                WaypointPoint removed = state.loadedGroup.waypoints.remove(idx - 1);
                storage.markDirty();
                storage.saveIfDirty();
                success(sender, "Removed &e" + (removed.name != null ? removed.name : String.valueOf(idx)));
                break;
            }

            //  import / export

            case "export": {
                if (args.length < 2) {
                    error(sender, "Usage: /athrwexport <group>");
                    return;
                }
                WaypointGroup g = storage.getGroup(args[1]);
                if (g == null) {
                    error(sender, "Group not found: " + args[1]);
                    return;
                }
                GuiScreen.setClipboardString(exportSoopy(g));
                success(sender, "Copied group '" + g.name + "' to clipboard");
                break;
            }

            case "import": {
                if (args.length < 2) {
                    error(sender, "Usage: /athrwimport <groupname>");
                    return;
                }
                String name = args[1].toLowerCase();
                String clip = GuiScreen.getClipboardString();
                if (clip == null || clip.trim().isEmpty()) {
                    error(sender, "Clipboard is empty");
                    return;
                }
                List<WaypointPoint> JWs = parseSoopy(clip.trim());
                if (JWs == null) {
                    error(sender, "Could not parse clipboard as soopy waypoints");
                    return;
                }
                WaypointGroup g = storage.getGroup(name);
                if (g == null) g = new WaypointGroup(name);
                g.waypoints = JWs;
                storage.putGroup(g);
                storage.saveIfDirty();
                success(sender, "Imported &e" + JWs.size() + " &awaypoints into &e" + name);
                break;
            }

            // settings

            case "setup":
                state.setupMode = !state.setupMode;
                success(sender, "Setup mode: " + (state.setupMode ? "&2ON" : "&4OFF"));
                break;

            case "enable":
                state.enabled = true;
                success(sender, "Waypoints enabled");
                break;

            case "disable":
                state.enabled = false;
                error(sender, "Waypoints disabled");
                break;

            case "range": {
                if (args.length < 2) {
                    data(sender, "Advance range", state.advanceRange + " blocks");
                    return;
                }
                double r = parseDoubleSafe(args[1], -1);
                if (r <= 0) {
                    error(sender, "Invalid range");
                    return;
                }
                state.advanceRange = r;
                success(sender, "Advance range set to &e" + r + " blocks");
                break;
            }

            case "time": {
                if (args.length < 2) {
                    data(sender, "Advance delay", state.advanceDelayMs + "ms");
                    return;
                }
                long t = parseLongSafe(args[1]);
                if (t <= 0) {
                    error(sender, "Invalid delay");
                    return;
                }
                state.advanceDelayMs = t;
                success(sender, "Advance delay set to &e" + t + "ms");
                break;
            }

            case "save":
                storage.saveForce();
                success(sender, "Saved all groups to config");
                break;

            // info / gui

            case "info": {
                if (!state.hasGroup()) {
                    error(sender, "No group loaded");
                    return;
                }
                WaypointGroup g = state.loadedGroup;
                blank(sender);
                header(sender, "Group Information");
                data(sender, "Name", g.name);
                data(sender, "Position", (state.currentIndex + 1) + "/" + g.waypoints.size());
                data(sender, "Setup mode", String.valueOf(state.setupMode));
                data(sender, "Advance range", state.advanceRange + "m");
                data(sender, "Delay", state.advanceDelayMs + "ms");
                blank(sender);
                break;
            }

            case "manage":
                ATHRConfig.openWaypointGroupGui();
                break;

            case "guide": {
                blank(sender);
                header(sender, "Waypoints Guide");
                line(sender, "/athrwlist", "Show all waypoint groups");
                line(sender, "/athrwcreate <n>", "Create a new group");
                line(sender, "/athrwdelete <n>", "Delete a group");
                line(sender, "/athrwrename <old> <new>", "Rename a group");
                line(sender, "/athrwload <n>", "Load a group");
                line(sender, "/athrwadd [name]", "Add waypoint at your position");
                line(sender, "/athrwinsert <index>", "Insert waypoint at index");
                line(sender, "/athrwremove <index>", "Remove waypoint");
                line(sender, "/athrwskip [n]", "Skip forward");
                line(sender, "/athrwunskip [n]", "Go backward");
                line(sender, "/athrwskipto <n>", "Jump to waypoint");
                line(sender, "/athrwreset", "Reset to first waypoint");
                line(sender, "/athrwsetup", "Toggle setup mode");
                line(sender, "/athrwenable / disable", "Toggle rendering");
                line(sender, "/athrwexport <n>", "Copy group to clipboard");
                line(sender, "/athrwimport <n>", "Import from clipboard");
                line(sender, "/athrwrange <blocks>", "Set auto-advance range");
                line(sender, "/athrwtime <ms>", "Set auto-advance delay");
                line(sender, "/athrwmanage", "Open group manager GUI");
                blank(sender);
                break;
            }

            default:
                error(sender, "Unknown subcommand '&e" + args[0] + "&c'. Try &e/athrwguide &cfor help.");
        }
    }

    //group list

    private void showGroupList(ICommandSender sender) {
        Map<String, WaypointGroup> groups = WaypointStorage.getInstance().getGroups();
        blank(sender);
        header(sender, "Waypoint Groups");
        if (groups.isEmpty()) {
            error(sender, "No groups saved");
            blank(sender);
            return;
        }
        for (WaypointGroup g : groups.values()) {
            ChatComponentText root = new ChatComponentText("");

            ChatComponentText name = new ChatComponentText(EnumChatFormatting.AQUA + g.name + EnumChatFormatting.GRAY + " (" + g.waypoints.size() + " JWs)");
            if (g.description != null && !g.description.isEmpty())
                name.appendText(EnumChatFormatting.DARK_GRAY + " – " + g.description);
            root.appendSibling(name);

            ChatComponentText load = new ChatComponentText(" " + EnumChatFormatting.YELLOW + EnumChatFormatting.BOLD + "[LOAD]");
            load.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/athrwload " + g.name)).setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Load " + g.name)));
            root.appendSibling(load);

            ChatComponentText export = new ChatComponentText(" " + EnumChatFormatting.GOLD + EnumChatFormatting.BOLD + "[EXPORT]");
            export.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/athrwexport " + g.name)).setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Copy to clipboard")));
            root.appendSibling(export);

            ChatComponentText del = new ChatComponentText(" " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "[DEL]");
            del.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/athrwdelete " + g.name)).setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Delete " + g.name)));
            root.appendSibling(del);

            sender.addChatMessage(root);
        }
        blank(sender);
    }

    //  helpers

    private void addWaypoint(ICommandSender sender, WaypointGroup group, String name) {
        double bx = Math.floor(mc.thePlayer.posX), by = Math.floor(mc.thePlayer.posY) - 1, bz = Math.floor(mc.thePlayer.posZ);
        addWaypointAt(sender, group, bx, by, bz, name);
    }

    private void addWaypointAt(ICommandSender sender, WaypointGroup group, double x, double y, double z, String name) {
        group.waypoints.add(new WaypointPoint(x, y, z, name));
        success(sender, "Added &e" + name + " &aat (" + (int) x + ", " + (int) y + ", " + (int) z + ") to &e" + group.name + " &7(&e" + group.waypoints.size() + "&7 total)");
    }

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void renumberNumericNames(WaypointGroup g, int fromOneBasedIndex) {
        for (int i = fromOneBasedIndex; i < g.waypoints.size(); i++) {
            WaypointPoint ATHRW= g.waypoints.get(i);
            try {
                if (Integer.parseInt(JW.name) == i) JW.name = String.valueOf(i + 1);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String exportSoopy(WaypointGroup g) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WaypointPoint ATHRW: g.waypoints) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", JW.x);
            m.put("y", JW.y);
            m.put("z", JW.z);
            m.put("r", 0);
            m.put("g", 1);
            m.put("b", 0);
            Map<String, Object> opts = new LinkedHashMap<>();
            opts.put("name", JW.name != null ? JW.name : "");
            m.put("options", opts);
            list.add(m);
        }
        return GSON.toJson(list);
    }

    private List<WaypointPoint> parseSoopy(String json) {
        try {
            if (json.startsWith("[")) {
                Type type = new TypeToken<List<Map<String, Object>>>() {
                }.getType();
                List<Map<String, Object>> raw = GSON.fromJson(json, type);
                List<WaypointPoint> JWs = new ArrayList<>();
                for (int i = 0; i < raw.size(); i++) {
                    Map<String, Object> m = raw.get(i);
                    double x = toDouble(m.get("x")), y = toDouble(m.get("y")), z = toDouble(m.get("z"));
                    String name = String.valueOf(i + 1);
                    if (m.containsKey("options")) {
                        @SuppressWarnings("unchecked") Map<String, Object> opts = (Map<String, Object>) m.get("options");
                        if (opts != null && opts.containsKey("name")) name = String.valueOf(opts.get("name"));
                    }
                    JWs.add(new WaypointPoint(x, y, z, name));
                }
                JWs.sort((a, b) -> {
                    try {
                        return Integer.compare(Integer.parseInt(a.name), Integer.parseInt(b.name));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
                return JWs;
            }
            if (json.matches("(?s).*\\d.*")) {
                List<WaypointPoint> JWs = new ArrayList<>();
                String[] rows = json.split("[\\r\\n]+");
                for (int i = 0; i < rows.length; i++) {
                    String[] parts = rows[i].trim().split("\\s+");
                    if (parts.length >= 3)
                        JWs.add(new WaypointPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), String.valueOf(i + 1)));
                }
                return JWs.isEmpty() ? null : JWs;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }

    //  output

    private void header(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText(color(PREFIX + "&6" + text)));
    }

    private void line(ICommandSender s, String cmd, String desc) {
        s.addChatMessage(new ChatComponentText(color(PREFIX + "&b" + cmd + " &7- " + desc)));
    }

    private void data(ICommandSender s, String key, String value) {
        s.addChatMessage(new ChatComponentText(color(PREFIX + "&b" + key + ": &e" + value)));
    }

    private void success(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText(color(PREFIX + "&a" + text)));
    }

    private void error(ICommandSender s, String text) {
        s.addChatMessage(new ChatComponentText(color(PREFIX + "&c" + text)));
    }

    private void blank(ICommandSender s) {
        s.addChatMessage(new ChatComponentText(""));
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
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }


    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("load") || sub.equals("delete") || sub.equals("export") || sub.equals("rename") || sub.equals("import"))
                return getListOfStringsMatchingLastWord(args, new ArrayList<>(WaypointStorage.getInstance().getGroups().keySet()));
        }
        return Collections.emptyList();
    }
}