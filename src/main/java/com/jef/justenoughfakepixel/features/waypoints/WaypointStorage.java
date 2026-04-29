package com.jef.justenoughfakepixel.features.waypoints;

import com.google.gson.reflect.TypeToken;
import com.jef.justenoughfakepixel.core.JefGsonBuilder;
import com.jef.justenoughfakepixel.core.JefStorageManager;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


public class WaypointStorage implements JefStorageManager.Managed, JefStorageManager.AutoSaveable {

    private static final WaypointStorage INSTANCE = new WaypointStorage();

    private final Map<String, WaypointGroup> groups = new LinkedHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private File file;

    private WaypointStorage() {
    }

    public static WaypointStorage getInstance() {
        return INSTANCE;
    }

    @Override
    public void initFile(File configDir) {
        if (file != null) return;
        configDir.mkdirs();
        file = new File(configDir, "waypoints_groups.json");
    }

    @Override
    public synchronized void load() {
        if (file == null || !file.exists()) return;
        Type type = new TypeToken<Map<String, WaypointGroup>>() {}.getType();
        Map<String, WaypointGroup> loaded = JefStorageManager.loadSafe(file, type, JefGsonBuilder.GSON);
        if (loaded != null) {
            groups.clear();
            for (Map.Entry<String, WaypointGroup> entry : loaded.entrySet()) {
                WaypointGroup g = entry.getValue();
                if (g == null) continue;
                if (g.waypoints == null) g.waypoints = new ArrayList<>();
                if (g.name == null) g.name = entry.getKey();
                groups.put(entry.getKey().toLowerCase(), g);
            }
        }
        dirty.set(false);
    }

    public synchronized Map<String, WaypointGroup> getGroups() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(groups));
    }

    public synchronized WaypointGroup getGroup(String name) {
        return name == null ? null : groups.get(name.toLowerCase());
    }

    public synchronized void putGroup(WaypointGroup group) {
        if (group == null || group.name == null) return;
        groups.put(group.name.toLowerCase(), group);
        dirty.set(true);
    }

    public synchronized boolean removeGroup(String name) {
        if (name == null) return false;
        boolean removed = groups.remove(name.toLowerCase()) != null;
        if (removed) dirty.set(true);
        return removed;
    }

    public void markDirty() {
        dirty.set(true);
    }

    public synchronized void saveIfDirty() {
        if (!dirty.get()) return;
        saveForce();
    }

    public synchronized void saveForce() {
        if (file == null) return;
        JefStorageManager.saveAtomic(file, groups, JefGsonBuilder.GSON);
        dirty.set(false);
    }

    /** Called by the 60s auto-save timer; only flushes if dirty. */
    @Override
    public void autoSave() {
        saveIfDirty();
    }
}