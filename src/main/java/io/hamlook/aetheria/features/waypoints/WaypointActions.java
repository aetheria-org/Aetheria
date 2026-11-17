package io.hamlook.aetheria.features.waypoints;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.waypoints.WaypointColorsConfig;
import io.hamlook.aetheria.features.waypoints.text.WaypointText;
import io.hamlook.aetheria.utils.compat.ClipboardCompat;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;

import java.util.*;

public final class WaypointActions {

    public static final double MIN_RANGE = 1.0;
    public static final double MAX_RANGE = 30.0;
    public static final long MIN_DELAY_MS = 250L;
    public static final long MAX_DELAY_MS = 10000L;
    public static final String[] COLOR_KEYS = {"box", "tracer", "label", "distance"};
    private static Runnable lastUndo;
    private static boolean applyingUndo;

    private WaypointActions() {
    }

    private static WaypointStorage storage() {
        return WaypointStorage.getInstance();
    }

    private static WaypointState state() {
        return WaypointState.getInstance();
    }

    public static boolean hasUndo() {
        return lastUndo != null;
    }

    public static void clearUndo() {
        lastUndo = null;
    }

    public static WaypointResult undo() {
        if (lastUndo == null) return WaypointResult.fail("Nothing to undo.");
        Runnable inverse = lastUndo;
        lastUndo = null;
        applyingUndo = true;
        try {
            inverse.run();
        } finally {
            applyingUndo = false;
        }
        return WaypointResult.ok("Undid the last action.");
    }

    private static void armUndo(Runnable inverse) {
        if (applyingUndo) return;
        lastUndo = inverse;
    }

    private static String freeCopyName(String base) {
        for (int i = 1; i < 1000; i++) {
            String candidate = i == 1 ? base : base + i;
            if (storage().getGroup(candidate) == null) return candidate;
        }
        return base + System.currentTimeMillis();
    }

    private static WaypointGroup snapshot(WaypointGroup g) {
        WaypointGroup copy = new WaypointGroup(g.name, g.description == null ? "" : g.description);
        for (WaypointPoint p : g.waypoints) {
            copy.waypoints.add(new WaypointPoint(p.x, p.y, p.z, p.name));
        }
        return copy;
    }

    private static void save() {
        WaypointStorage s = storage();
        s.markDirty();
        s.saveIfDirty();
    }

    public static String normalizeName(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(s.length());
        boolean lastDash = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-';
            if (ok) {
                sb.append(c);
                lastDash = c == '-';
            } else if (Character.isWhitespace(c) && !lastDash && sb.length() > 0) {
                sb.append('-');
                lastDash = true;
            }
        }
        String out = sb.toString();
        if (out.length() > WaypointText.MAX_GROUP_NAME) out = out.substring(0, WaypointText.MAX_GROUP_NAME);
        return out;
    }

    public static String uniqueName(String base) {
        if (storage().getGroup(base) == null) return base;
        for (int i = 2; i < 1000; i++) {
            String candidate = base + "-" + i;
            if (storage().getGroup(candidate) == null) return candidate;
        }
        return base + "-" + System.currentTimeMillis();
    }

    public static WaypointResult createRoute(String rawName, String description) {
        String name = normalizeName(rawName);
        if (name.isEmpty()) return WaypointResult.fail(Strings.NAME_EMPTY);
        if (storage().getGroup(name) != null) return WaypointResult.fail(Strings.NAME_TAKEN);
        storage().putGroup(new WaypointGroup(name, description == null ? "" : description));
        armUndo(() -> {
            storage().removeGroup(name);
            save();
        });
        save();
        return WaypointResult.ok("Created route " + name + ".");
    }

    public static WaypointResult renameRoute(String oldName, String rawNewName) {
        String newName = normalizeName(rawNewName);
        if (newName.isEmpty()) return WaypointResult.fail(Strings.NAME_EMPTY);
        WaypointGroup g = storage().getGroup(oldName);
        if (g == null) return WaypointResult.fail(Strings.ROUTE_NOT_FOUND_PREFIX + oldName + ".");
        if (newName.equalsIgnoreCase(oldName)) return WaypointResult.ok("Route is already called " + newName + ".");
        if (storage().getGroup(newName) != null) return WaypointResult.fail(Strings.NAME_TAKEN);
        storage().removeGroup(oldName);
        g.name = newName;
        storage().putGroup(g);
        armUndo(() -> renameRoute(newName, oldName));
        save();
        return WaypointResult.ok("Renamed " + oldName + " to " + newName + ".");
    }

    public static WaypointResult duplicateRoute(String name) {
        WaypointGroup src = storage().getGroup(name);
        if (src == null) return WaypointResult.fail(Strings.ROUTE_NOT_FOUND_PREFIX + name + ".");
        String copyName = freeCopyName(normalizeName(name + "-copy"));
        WaypointGroup copy = new WaypointGroup(copyName, src.description == null ? "" : src.description);
        for (WaypointPoint p : src.waypoints) copy.waypoints.add(new WaypointPoint(p.x, p.y, p.z, p.name));
        storage().putGroup(copy);
        armUndo(() -> {
            storage().removeGroup(copyName);
            save();
        });
        save();
        return WaypointResult.ok("Duplicated " + src.name + " as " + copyName + ".");
    }

    public static WaypointResult deleteRoute(String name) {
        WaypointGroup g = storage().getGroup(name);
        if (g == null) return WaypointResult.fail(Strings.ROUTE_NOT_FOUND_PREFIX + name + ".");
        WaypointGroup backup = snapshot(g);
        if (isActive(g)) state().unload();
        storage().removeGroup(g.name);
        armUndo(() -> {
            storage().putGroup(backup);
            save();
        });
        save();
        return WaypointResult.ok("Deleted route " + g.name + ".");
    }

    public static WaypointResult loadRoute(String name) {
        WaypointGroup g = storage().getGroup(name);
        if (g == null) return WaypointResult.fail(Strings.ROUTE_NOT_FOUND_PREFIX + name + ".");
        state().load(g);
        return WaypointResult.ok("Loaded route " + g.name + " with " + g.waypoints.size() + " waypoints.");
    }

    public static WaypointResult exportRoute(String name) {
        return exportRoute(name, false);
    }

    public static WaypointResult exportRoute(String name, boolean legacy) {
        WaypointGroup g = storage().getGroup(name);
        if (g == null) return WaypointResult.fail(Strings.ROUTE_NOT_FOUND_PREFIX + name + ".");
        ClipboardCompat.setClipboard(legacy ? WaypointText.writeSoopy(g) : WaypointText.write(g));
        return WaypointResult.ok("Copied " + g.waypoints.size() + " waypoints of " + g.name
                + " to clipboard (" + (legacy ? "soopy" : "aetheria") + ").");
    }

    public static WaypointText.Result readImport(String clipboardText) {
        return WaypointText.read(clipboardText);
    }

    public static WaypointResult importRoute(WaypointText.Result parsed, String rawName, boolean replaceExisting) {
        if (parsed == null || parsed.isFailed()) return WaypointResult.fail("The clipboard has no waypoint export.");
        String name = normalizeName(rawName);
        if (name.isEmpty()) return WaypointResult.fail(Strings.NAME_EMPTY);
        WaypointGroup existing = storage().getGroup(name);
        WaypointGroup backup = existing == null ? null : snapshot(existing);
        if (existing != null && !replaceExisting) return WaypointResult.fail(Strings.NAME_TAKEN);
        WaypointGroup g = existing != null ? existing : new WaypointGroup(name);
        g.name = name;
        g.waypoints = new ArrayList<>(parsed.getGroup().waypoints);
        if (g.description == null || g.description.isEmpty()) g.description = parsed.getGroup().description;
        storage().putGroup(g);
        armUndo(backup == null
                ? () -> {
            storage().removeGroup(name);
            save();
        }
                : () -> {
            storage().putGroup(backup);
            save();
        });
        save();
        String skipped = parsed.getSkipped() > 0 ? " Skipped " + parsed.getSkipped() + "." : "";
        return WaypointResult.ok("Imported " + g.waypoints.size() + " waypoints into " + name + "." + skipped);
    }

    public static boolean isActive(WaypointGroup g) {
        WaypointState st = state();
        if (st.loadedGroup == null || g == null) return false;
        return st.loadedGroup == g || (g.name != null && g.name.equalsIgnoreCase(st.loadedGroup.name));
    }

    public static WaypointResult unloadRoute() {
        state().unload();
        return WaypointResult.ok("Waypoints unloaded.");
    }

    public static WaypointResult skip(int n) {
        WaypointState st = state();
        if (!st.hasGroup()) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        st.skip(n);
        return WaypointResult.ok("Now at " + (st.currentIndex + 1) + " / " + st.size() + ".");
    }

    public static WaypointResult skipTo(int index) {
        WaypointState st = state();
        if (!st.hasGroup()) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (index < 0 || index >= st.size())
            return WaypointResult.fail("Position is out of range. Use 1 to " + st.size() + ".");
        st.skipTo(index);
        return WaypointResult.ok("Jumped to waypoint " + (st.currentIndex + 1) + ".");
    }

    public static WaypointResult restartRoute() {
        WaypointState st = state();
        if (!st.hasGroup()) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        st.reset();
        return WaypointResult.ok("Restarted at waypoint 1.");
    }

    public static WaypointResult setEnabled(boolean enabled) {
        state().enabled = enabled;
        return WaypointResult.ok(enabled ? "Waypoints enabled." : "Waypoints disabled.");
    }

    public static WaypointResult setSetupMode(boolean setup) {
        state().setupMode = setup;
        return WaypointResult.ok(setup ? "Setup mode is on." : "Setup mode is off.");
    }

    public static WaypointResult saveAll() {
        storage().saveForce();
        return WaypointResult.ok("Saved all routes.");
    }

    public static WaypointResult setDescription(WaypointGroup g, String text) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        String value = text == null ? "" : text.trim();
        if (value.length() > WaypointText.MAX_DESCRIPTION) value = value.substring(0, WaypointText.MAX_DESCRIPTION);
        String old = g.description;
        g.description = value;
        armUndo(() -> setDescription(g, old));
        save();
        return WaypointResult.ok("Saved the description.");
    }

    public static WaypointResult addHere(WaypointGroup g, String nameOrNull) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (MinecraftCompat.getLocalPlayer() == null) return WaypointResult.fail("No player found.");
        double x = Math.floor(EntityCompat.getPosX(MinecraftCompat.getLocalPlayer()));
        double y = Math.floor(EntityCompat.getPosY(MinecraftCompat.getLocalPlayer())) - 1;
        double z = Math.floor(EntityCompat.getPosZ(MinecraftCompat.getLocalPlayer()));
        return addAt(g, x, y, z, nameOrNull);
    }

    public static WaypointResult addAt(WaypointGroup g, double x, double y, double z, String nameOrNull) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (!coordOk(x) || !coordOk(y) || !coordOk(z)) return WaypointResult.fail("Coordinate is out of range.");
        String name = cleanName(nameOrNull, g.waypoints.size() + 1);
        int addedIndex = g.waypoints.size();
        g.waypoints.add(new WaypointPoint(x, y, z, name));
        armUndo(() -> removePoint(g, addedIndex));
        save();
        return WaypointResult.ok("Added " + name + " at " + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ".");
    }

    public static WaypointResult insertHere(WaypointGroup g, int index0, String nameOrNull) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (MinecraftCompat.getLocalPlayer() == null) return WaypointResult.fail("No player found.");
        if (index0 < 0 || index0 > g.waypoints.size())
            return WaypointResult.fail(Strings.POSITION_OUT_OF_RANGE);
        Set<WaypointPoint> defaults = defaultNamed(g);
        double x = Math.floor(EntityCompat.getPosX(MinecraftCompat.getLocalPlayer()));
        double y = Math.floor(EntityCompat.getPosY(MinecraftCompat.getLocalPlayer())) - 1;
        double z = Math.floor(EntityCompat.getPosZ(MinecraftCompat.getLocalPlayer()));
        String name = cleanName(nameOrNull, index0 + 1);
        g.waypoints.add(index0, new WaypointPoint(x, y, z, name));
        renumber(g, defaults);
        armUndo(() -> removePoint(g, index0));
        save();
        return WaypointResult.ok("Inserted " + name + " at " + (index0 + 1) + ".");
    }

    public static WaypointResult setHere(WaypointGroup g, int index0) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (index0 < 0 || index0 >= g.waypoints.size())
            return WaypointResult.fail(Strings.POSITION_OUT_OF_RANGE);
        if (MinecraftCompat.getLocalPlayer() == null) return WaypointResult.fail("No player found.");
        WaypointPoint p = g.waypoints.get(index0);
        WaypointPoint old = new WaypointPoint(p.x, p.y, p.z, p.name);
        p.x = Math.floor(EntityCompat.getPosX(MinecraftCompat.getLocalPlayer()));
        p.y = Math.floor(EntityCompat.getPosY(MinecraftCompat.getLocalPlayer())) - 1;
        p.z = Math.floor(EntityCompat.getPosZ(MinecraftCompat.getLocalPlayer()));
        armUndo(() -> editPoint(g, index0, null, old.x, old.y, old.z));
        save();
        return WaypointResult.ok("Moved " + pointName(p, index0) + " to " + fmt(p.x) + ", " + fmt(p.y) + ", " + fmt(p.z) + ".");
    }

    public static WaypointResult editPoint(WaypointGroup g, int index0, String name, Double x, Double y, Double z) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (index0 < 0 || index0 >= g.waypoints.size())
            return WaypointResult.fail(Strings.POSITION_OUT_OF_RANGE);
        if (x != null && !coordOk(x))
            return WaypointResult.fail("X is out of range. Use a number up to " + (long) WaypointText.MAX_COORDINATE + ".");
        if (y != null && !coordOk(y))
            return WaypointResult.fail("Y is out of range. Use a number up to " + (long) WaypointText.MAX_COORDINATE + ".");
        if (z != null && !coordOk(z))
            return WaypointResult.fail("Z is out of range. Use a number up to " + (long) WaypointText.MAX_COORDINATE + ".");
        WaypointPoint p = g.waypoints.get(index0);
        WaypointPoint old = new WaypointPoint(p.x, p.y, p.z, p.name);
        if (name != null) p.name = cleanName(name, index0 + 1);
        if (x != null) p.x = x;
        if (y != null) p.y = y;
        if (z != null) p.z = z;
        armUndo(() -> editPoint(g, index0, old.name, old.x, old.y, old.z));
        save();
        return WaypointResult.ok("Saved waypoint " + (index0 + 1) + ".");
    }

    public static WaypointResult movePoint(WaypointGroup g, int index0, int delta) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        int to = index0 + delta;
        if (index0 < 0 || index0 >= g.waypoints.size() || to < 0 || to >= g.waypoints.size()) {
            return WaypointResult.fail("Waypoint cannot move further.");
        }
        Set<WaypointPoint> defaults = defaultNamed(g);
        Collections.swap(g.waypoints, index0, to);
        renumber(g, defaults);
        armUndo(() -> movePoint(g, to, index0 - to));
        save();
        return WaypointResult.ok("Moved waypoint to " + (to + 1) + ".");
    }

    public static WaypointResult removePoint(WaypointGroup g, int index0) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (index0 < 0 || index0 >= g.waypoints.size())
            return WaypointResult.fail(Strings.POSITION_OUT_OF_RANGE);
        Set<WaypointPoint> defaults = defaultNamed(g);
        WaypointPoint removed = g.waypoints.remove(index0);
        renumber(g, defaults);
        armUndo(() -> restorePoint(g, index0, removed));
        save();
        return WaypointResult.ok("Removed " + pointName(removed, index0) + ".");
    }

    public static WaypointResult restorePoint(WaypointGroup g, int index0, WaypointPoint p) {
        if (g == null || p == null) return WaypointResult.fail("Nothing to restore.");
        Set<WaypointPoint> defaults = defaultNamed(g);
        int at = Math.max(0, Math.min(index0, g.waypoints.size()));
        g.waypoints.add(at, p);
        renumber(g, defaults);
        save();
        return WaypointResult.ok("Restored " + pointName(p, at) + ".");
    }

    public static WaypointResult duplicatePoint(WaypointGroup g, int index0) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (index0 < 0 || index0 >= g.waypoints.size())
            return WaypointResult.fail(Strings.POSITION_OUT_OF_RANGE);
        Set<WaypointPoint> defaults = defaultNamed(g);
        WaypointPoint src = g.waypoints.get(index0);
        WaypointPoint copy = new WaypointPoint(src.x, src.y, src.z, src.name);
        g.waypoints.add(index0 + 1, copy);
        renumber(g, defaults);
        armUndo(() -> removePoint(g, index0 + 1));
        save();
        return WaypointResult.ok("Duplicated waypoint " + (index0 + 1) + ".");
    }

    public static WaypointResult reverse(WaypointGroup g) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        if (g.waypoints.size() < 2) return WaypointResult.fail("Route needs two waypoints to reverse.");
        Set<WaypointPoint> defaults = defaultNamed(g);
        Collections.reverse(g.waypoints);
        renumber(g, defaults);
        armUndo(() -> reverse(g));
        save();
        return WaypointResult.ok("Reversed " + g.name + ".");
    }

    public static WaypointResult clear(WaypointGroup g) {
        if (g == null) return WaypointResult.fail(Strings.NO_ROUTE_LOADED);
        WaypointGroup backup = snapshot(g);
        g.waypoints.clear();
        armUndo(() -> {
            g.waypoints.clear();
            g.waypoints.addAll(backup.waypoints);
            save();
        });
        save();
        return WaypointResult.ok("Cleared all waypoints of " + g.name + ".");
    }

    private static Set<WaypointPoint> defaultNamed(WaypointGroup g) {
        Set<WaypointPoint> out = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < g.waypoints.size(); i++) {
            WaypointPoint p = g.waypoints.get(i);
            if (p == null) continue;
            String name = p.name == null ? "" : p.name.trim();
            if (name.isEmpty() || name.equals(String.valueOf(i + 1))) out.add(p);
        }
        return out;
    }

    private static void renumber(WaypointGroup g, Set<WaypointPoint> defaults) {
        for (int i = 0; i < g.waypoints.size(); i++) {
            WaypointPoint p = g.waypoints.get(i);
            if (p != null && defaults.contains(p)) p.name = String.valueOf(i + 1);
        }
    }

    private static String cleanName(String raw, int position) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) name = String.valueOf(position);
        if (name.length() > WaypointText.MAX_POINT_NAME) name = name.substring(0, WaypointText.MAX_POINT_NAME);
        return name;
    }

    private static String pointName(WaypointPoint p, int index0) {
        if (p == null || p.name == null || p.name.isEmpty()) return String.valueOf(index0 + 1);
        return p.name;
    }

    private static boolean coordOk(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && Math.abs(v) <= WaypointText.MAX_COORDINATE;
    }

    private static String fmt(double v) {
        return String.valueOf((int) v);
    }

    public static double getAdvanceRange() {
        if (ATHRConfig.feature != null) return ATHRConfig.feature.waypoints.autoAdvance.advanceRange;
        return state().advanceRange;
    }

    public static long getAdvanceDelayMs() {
        if (ATHRConfig.feature != null) return (long) ATHRConfig.feature.waypoints.autoAdvance.advanceDelayMs;
        return state().advanceDelayMs;
    }

    public static WaypointResult setAdvanceRange(double blocks) {
        double v = Math.max(MIN_RANGE, Math.min(MAX_RANGE, blocks));
        v = Math.round(v * 2.0) / 2.0;
        double old = getAdvanceRange();
        state().advanceRange = v;
        if (ATHRConfig.feature != null) {
            ATHRConfig.feature.waypoints.autoAdvance.advanceRange = (float) v;
            ATHRConfig.markConfigDirty();
        }
        armUndo(() -> setAdvanceRange(old));
        return WaypointResult.ok("Advance range set to " + trim(v) + " blocks.");
    }

    public static WaypointResult setAdvanceDelayMs(long ms) {
        long v = Math.max(MIN_DELAY_MS, Math.min(MAX_DELAY_MS, ms));
        v = Math.round(v / 250.0) * 250L;
        long old = getAdvanceDelayMs();
        state().advanceDelayMs = v;
        if (ATHRConfig.feature != null) {
            ATHRConfig.feature.waypoints.autoAdvance.advanceDelayMs = (float) v;
            ATHRConfig.markConfigDirty();
        }
        armUndo(() -> setAdvanceDelayMs(old));
        return WaypointResult.ok("Advance delay set to " + v + " ms.");
    }

    public static WaypointResult resetAdvanceDefaults() {
        double oldRange = getAdvanceRange();
        long oldDelay = getAdvanceDelayMs();
        Map<String, String> oldColors = new java.util.LinkedHashMap<>();
        WaypointColorsConfig oldConfig = ATHRConfig.feature != null ? ATHRConfig.feature.waypoints.colors : null;
        if (oldConfig != null) {
            oldColors.put("box", oldConfig.boxColour);
            oldColors.put("tracer", oldConfig.tracerColour);
            oldColors.put("label", oldConfig.labelColour);
            oldColors.put("distance", oldConfig.distanceLabelColour);
        }
        state().advanceRange = 5.0;
        state().advanceDelayMs = 2000L;
        if (ATHRConfig.feature != null) {
            ATHRConfig.feature.waypoints.autoAdvance.advanceRange = 5.0f;
            ATHRConfig.feature.waypoints.autoAdvance.advanceDelayMs = 2000f;
            Map<String, String> defaults = colorDefaults();
            WaypointColorsConfig c = ATHRConfig.feature.waypoints.colors;
            c.boxColour = defaults.get("box");
            c.tracerColour = defaults.get("tracer");
            c.labelColour = defaults.get("label");
            c.distanceLabelColour = defaults.get("distance");
            ATHRConfig.markConfigDirty();
        }
        armUndo(() -> {
            state().advanceRange = oldRange;
            state().advanceDelayMs = oldDelay;
            if (ATHRConfig.feature != null) {
                ATHRConfig.feature.waypoints.autoAdvance.advanceRange = (float) oldRange;
                ATHRConfig.feature.waypoints.autoAdvance.advanceDelayMs = (float) oldDelay;
                WaypointColorsConfig c = ATHRConfig.feature.waypoints.colors;
                if (!oldColors.isEmpty()) {
                    c.boxColour = oldColors.get("box");
                    c.tracerColour = oldColors.get("tracer");
                    c.labelColour = oldColors.get("label");
                    c.distanceLabelColour = oldColors.get("distance");
                }
                ATHRConfig.markConfigDirty();
            }
        });
        return WaypointResult.ok("Reset settings to defaults.");
    }

    public static String getColor(String key) {
        WaypointColorsConfig c = ATHRConfig.feature != null ? ATHRConfig.feature.waypoints.colors : null;
        return colorOf(c != null ? c : new WaypointColorsConfig(), key);
    }

    public static WaypointResult setColor(String key, String value) {
        if (ATHRConfig.feature == null) return WaypointResult.fail("Colors need the config to be loaded.");
        WaypointColorsConfig c = ATHRConfig.feature.waypoints.colors;
        if (c == null) return WaypointResult.fail("Colors are not available.");
        String old = getColor(key);
        if ("box".equals(key)) c.boxColour = value;
        else if ("tracer".equals(key)) c.tracerColour = value;
        else if ("label".equals(key)) c.labelColour = value;
        else if ("distance".equals(key)) c.distanceLabelColour = value;
        armUndo(() -> setColor(key, old));
        ATHRConfig.markConfigDirty();
        return WaypointResult.ok("Color saved.");
    }

    private static String colorOf(WaypointColorsConfig c, String key) {
        if ("box".equals(key)) return c.boxColour;
        if ("tracer".equals(key)) return c.tracerColour;
        if ("label".equals(key)) return c.labelColour;
        return c.distanceLabelColour;
    }

    public static String colorName(String key) {
        if ("box".equals(key)) return "Box";
        if ("tracer".equals(key)) return "Tracer";
        if ("label".equals(key)) return "Label";
        return "Distance label";
    }

    public static Map<String, String> colorDefaults() {
        WaypointColorsConfig d = new WaypointColorsConfig();
        Map<String, String> out = new java.util.LinkedHashMap<>();
        out.put("box", d.boxColour);
        out.put("tracer", d.tracerColour);
        out.put("label", d.labelColour);
        out.put("distance", d.distanceLabelColour);
        return out;
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
