package io.hamlook.aetheria.features.waypoints;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.waypoints.text.WaypointText;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.Arrays;
import java.util.Map;

@RegisterEvents
public class WaypointCommand {

    public static final String PREFIX = "§3[ASM]§b ";

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("jw", builder -> {
            builder.setAliases(Arrays.asList("athrw", "waypoints", "asmw"));
            builder.description = "Waypoint routes";
            builder.setCategory(CommandCategory.USERS_ACTIVE);

            builder.legacyCallbackArgs(this::handle);
        });
    }

    private void handle(String[] args) {
        if (args.length == 0) {
            ATHRConfig.openWaypointGroupGui();
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                showGroupList();
                return;
            case "guide":
            case "help":
                showGuide();
                return;
            case "manage":
            case "menu":
                ATHRConfig.openWaypointGroupGui();
                return;
            case "unload":
            case "clear":
                report(WaypointActions.unloadRoute());
                return;
            case "skip": {
                Integer n = args.length >= 2 ? intArg(args[1], "Usage: /jw skip [n]") : Integer.valueOf(1);
                if (n == null) return;
                report(WaypointActions.skip(n));
                return;
            }
            case "unskip": {
                Integer n = args.length >= 2 ? intArg(args[1], "Usage: /jw unskip [n]") : Integer.valueOf(1);
                if (n == null) return;
                report(WaypointActions.skip(-n));
                return;
            }
            case "skipto": {
                if (args.length < 2) {
                    error("Usage: /jw skipto <n>");
                    return;
                }
                Integer n = intArg(args[1], "Usage: /jw skipto <n>");
                if (n == null) return;
                report(WaypointActions.skipTo(n - 1));
                return;
            }
            case "reset":
                report(WaypointActions.restartRoute());
                return;
            case "setup":
                report(WaypointActions.setSetupMode(!WaypointState.getInstance().setupMode));
                return;
            case "enable":
                report(WaypointActions.setEnabled(true));
                return;
            case "disable":
                report(WaypointActions.setEnabled(false));
                return;
            case "save":
                report(WaypointActions.saveAll());
                return;
            case "range": {
                if (args.length < 2) {
                    data("Advance range", WaypointActions.getAdvanceRange() + " blocks");
                    return;
                }
                Double blocks = doubleArg(args[1], "Usage: /jw range <blocks>");
                if (blocks == null) return;
                report(WaypointActions.setAdvanceRange(blocks));
                return;
            }
            case "time": {
                if (args.length < 2) {
                    data("Advance delay", WaypointActions.getAdvanceDelayMs() + " ms");
                    return;
                }
                Double ms = doubleArg(args[1], "Usage: /jw time <ms>");
                if (ms == null || ms.longValue() <= 0) {
                    error("Delay must be a positive number of milliseconds.");
                    return;
                }
                report(WaypointActions.setAdvanceDelayMs(ms.longValue()));
                return;
            }
            case "info":
                showInfo();
                return;
            case "load": {
                if (args.length < 2) {
                    error("Usage: /jw load <route>");
                    return;
                }
                WaypointGroup g = find(args, 1);
                if (g == null) {
                    error("Route not found: " + joinFrom(args, 1));
                    return;
                }
                report(WaypointActions.loadRoute(g.name));
                return;
            }
            case "create": {
                if (args.length < 2) {
                    error("Usage: /jw create <route> [description]");
                    return;
                }
                report(WaypointActions.createRoute(args[1], args.length > 2 ? joinFrom(args, 2) : ""));
                return;
            }
            case "delete": {
                if (args.length < 2) {
                    error("Usage: /jw delete <route>");
                    return;
                }
                WaypointGroup g = find(args, 1);
                if (g == null) {
                    error("Route not found: " + joinFrom(args, 1));
                    return;
                }
                report(WaypointActions.deleteRoute(g.name));
                return;
            }
            case "rename": {
                if (args.length < 3) {
                    error("Usage: /jw rename <route> <new>");
                    return;
                }
                WaypointGroup g = find(args, 1);
                if (g == null) {
                    error("Route not found: " + args[1]);
                    return;
                }
                report(WaypointActions.renameRoute(g.name, joinFrom(args, 2)));
                return;
            }
            case "duplicate": {
                if (args.length < 2) {
                    error("Usage: /jw duplicate <route>");
                    return;
                }
                WaypointGroup g = find(args, 1);
                if (g == null) {
                    error("Route not found: " + joinFrom(args, 1));
                    return;
                }
                report(WaypointActions.duplicateRoute(g.name));
                return;
            }
            case "add": {
                WaypointGroup g = loaded();
                if (g == null) {
                    error("No route is loaded. Use /jw load <route> first.");
                    return;
                }
                if (args.length >= 4 && isDouble(args[1]) && isDouble(args[2]) && isDouble(args[3])) {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    report(WaypointActions.addAt(g, x, y, z, args.length >= 5 ? joinFrom(args, 4) : null));
                } else {
                    report(WaypointActions.addHere(g, args.length >= 2 ? joinFrom(args, 1) : null));
                }
                return;
            }
            case "insert": {
                WaypointGroup g = loaded();
                if (g == null) {
                    error("No route is loaded.");
                    return;
                }
                if (args.length < 2) {
                    error("Usage: /jw insert <n> [name]");
                    return;
                }
                Integer n = intArg(args[1], "Usage: /jw insert <n> [name]");
                if (n == null) return;
                report(WaypointActions.insertHere(g, n - 1,
                        args.length >= 3 ? joinFrom(args, 2) : null));
                return;
            }
            case "here": {
                WaypointGroup g = loaded();
                if (g == null) {
                    error("No route is loaded.");
                    return;
                }
                if (args.length < 2) {
                    error("Usage: /jw here <n>");
                    return;
                }
                Integer n = intArg(args[1], "Usage: /jw here <n>");
                if (n == null) return;
                report(WaypointActions.setHere(g, n - 1));
                return;
            }
            case "remove": {
                WaypointGroup g = loaded();
                if (g == null) {
                    error("No route is loaded.");
                    return;
                }
                if (args.length < 2) {
                    error("Usage: /jw remove <n>");
                    return;
                }
                Integer n = intArg(args[1], "Usage: /jw remove <n>");
                if (n == null) return;
                report(WaypointActions.removePoint(g, n - 1));
                return;
            }
            case "export": {
                if (args.length < 2) {
                    error("Usage: /jw export <route> [legacy]");
                    return;
                }
                boolean legacy = args.length >= 3 && "legacy".equalsIgnoreCase(args[args.length - 1]);
                String[] routeArgs = legacy ? Arrays.copyOf(args, args.length - 1) : args;
                WaypointGroup g = find(routeArgs, 1);
                if (g == null) {
                    error("Route not found: " + joinFrom(routeArgs, 1));
                    return;
                }
                report(WaypointActions.exportRoute(g.name, legacy));
                return;
            }
            case "import": {
                if (args.length < 2) {
                    error("Usage: /jw import <route>");
                    return;
                }
                String clipboard = ClipboardCompat.getClipboard();
                if (clipboard == null || clipboard.trim().isEmpty()) {
                    error("The clipboard is empty.");
                    return;
                }
                WaypointText.Result parsed = WaypointActions.readImport(clipboard);
                report(WaypointActions.importRoute(parsed, joinFrom(args, 1), false));
                return;
            }
            default:
                error("Unknown subcommand '" + args[0] + "'. Try /jw help.");
        }
    }

    private WaypointGroup loaded() {
        return WaypointState.getInstance().loadedGroup;
    }

    private WaypointGroup find(String[] args, int from) {
        if (from >= args.length) return null;
        WaypointStorage storage = WaypointStorage.getInstance();
        WaypointGroup g = storage.getGroup(joinFrom(args, from));
        if (g != null) return g;
        for (int i = args.length - 1; i > from; i--) {
            g = storage.getGroup(String.join(" ", Arrays.copyOfRange(args, from, i)));
            if (g != null) return g;
        }
        return null;
    }

    private void report(WaypointResult result) {
        if (result == null) return;
        if (result.ok) success(result.message);
        else error(result.message);
    }

    private void showInfo() {
        WaypointState state = WaypointState.getInstance();
        WaypointGroup g = state.loadedGroup;
        blank();
        header("Waypoint State");
        if (g == null) {
            data("Route", "none");
        } else {
            data("Route", g.name);
            data("Position", (state.currentIndex + 1) + "/" + g.waypoints.size());
        }
        data("Visible", state.enabled ? "yes" : "no");
        data("Setup mode", state.setupMode ? "yes" : "no");
        data("Advance range", WaypointActions.getAdvanceRange() + " blocks");
        data("Advance delay", WaypointActions.getAdvanceDelayMs() + " ms");
        data("Saved routes", String.valueOf(WaypointStorage.getInstance().getGroups().size()));
        blank();
    }

    private void showGuide() {
        blank();
        header(Strings.GUIDE_TITLE);
        for (String[] row : Strings.COMMAND_ROWS) {
            line(row[0], row[1]);
        }
        line("/jw manage", "open the waypoint menu");
        line("Aliases", "/athrw, /waypoints, /asmw");
        blank();
    }

    private void showGroupList() {
        Map<String, WaypointGroup> groups = WaypointStorage.getInstance().getGroups();
        blank();
        header("Waypoint Routes");
        if (groups.isEmpty()) {
            error("No routes saved. Use /jw create <route> or the menu.");
            blank();
            return;
        }
        for (WaypointGroup g : groups.values()) {
            IChatComponent root = TextCompat.createText("");

            IChatComponent name = TextCompat.fromFormatted(EnumChatFormatting.AQUA + g.name
                    + EnumChatFormatting.GRAY + " (" + g.waypoints.size() + " wps)");
            if (g.description != null && !g.description.isEmpty()) {
                TextCompat.appendSibling(name,
                        TextCompat.fromFormatted(EnumChatFormatting.DARK_GRAY + " - " + g.description));
            }
            TextCompat.appendSibling(root, name);

            IChatComponent load = TextCompat.fromFormatted(" " + EnumChatFormatting.YELLOW
                    + EnumChatFormatting.BOLD + "[LOAD]");
            TextCompat.setChatStyle(load, TextCompat.setClickRunCommand(TextCompat.getChatStyle(load),
                    "/jw load " + g.name));
            TextCompat.setChatStyle(load, TextCompat.setHoverShowText(TextCompat.getChatStyle(load),
                    "Load " + g.name));
            TextCompat.appendSibling(root, load);

            IChatComponent export = TextCompat.fromFormatted(" " + EnumChatFormatting.GOLD
                    + EnumChatFormatting.BOLD + "[EXPORT]");
            TextCompat.setChatStyle(export, TextCompat.setClickRunCommand(TextCompat.getChatStyle(export),
                    "/jw export " + g.name));
            TextCompat.setChatStyle(export, TextCompat.setHoverShowText(TextCompat.getChatStyle(export),
                    "Copy to clipboard"));
            TextCompat.appendSibling(root, export);

            IChatComponent del = TextCompat.fromFormatted(" " + EnumChatFormatting.RED
                    + EnumChatFormatting.BOLD + "[DEL]");
            TextCompat.setChatStyle(del, TextCompat.setClickRunCommand(TextCompat.getChatStyle(del),
                    "/jw delete " + g.name));
            TextCompat.setChatStyle(del, TextCompat.setHoverShowText(TextCompat.getChatStyle(del),
                    "Delete " + g.name));
            TextCompat.appendSibling(root, del);

            ChatUtils.sendMessage(root);
        }
        blank();
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

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Integer intArg(String s, String usage) {
        if (s == null) {
            error(usage);
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            error(usage);
            return null;
        }
    }

    private Double doubleArg(String s, String usage) {
        if (s == null) {
            error(usage);
            return null;
        }
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException e) {
            error(usage);
            return null;
        }
    }

    private String joinFrom(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }
}
