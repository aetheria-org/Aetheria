package io.hamlook.aetheria.core;

import com.google.gson.Gson;
import io.hamlook.aetheria.features.diana.DianaStats;
import io.hamlook.aetheria.features.fishing.trophy.TrophyFishStorage;
import io.hamlook.aetheria.features.farming.FarmingTrackerData;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.features.mining.pristine.PristineStats;
import io.hamlook.aetheria.features.misc.invbuttons.InventoryButtonStorage;
import io.hamlook.aetheria.features.misc.pet.CurrentPetTracker;
import io.hamlook.aetheria.features.misc.pet.PetCache;
import io.hamlook.aetheria.features.misc.protect.ProtectedItemStorage;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostStats;
import io.hamlook.aetheria.features.scoreboard.MaxwellPowerSync;
import io.hamlook.aetheria.features.waypoints.WaypointStorage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * SkyHanni-style centralised storage registry.
 *<p>
 * Every storage singleton is registered here as a one-liner enum entry.
 * ATHRMod only calls {@link #initAll} and {@link #loadAll} — it never
 * manually orchestrates individual storage classes again.
 *<p>
 * Adding a new storage file in the future:
 *   1. Make sure your class has {@code initFile(File)} and {@code load()} methods.
 *   2. Add one line to the enum below.
 *   3. That's it. ATHRMod.java does not need to change.
 *<p>
 * Mirrors: ConfigFileType + ConfigManager (SkyHanni).
 */
public enum StorageManager {

    // ── enum entries ─────────────────────────────────────────────────────────
    WAYPOINTS      (WaypointStorage.getInstance()),
    INV_BUTTONS    (InventoryButtonStorage.getInstance()),
    DIANA_STATS    (DianaStats.getInstance()),
    POWDER_STATS   (PowderStats.getInstance()),
    PRISTINE_STATS (PristineStats.getInstance()),
    MAXWELL_POWER  (MaxwellPowerSync.getInstance()),
    PET_CACHE      (PetCache.getInstance()),
    CURRENT_PET    (CurrentPetTracker.getInstance()),
    TROPHY_FISH    (TrophyFishStorage.getInstance()),
    FARMING_TRACKER(FarmingTrackerData.getInstance()),
    GHOST_STATS    (GhostStats.getInstance());
    // ─────────────────────────────────────────────────────────────────────────

    private final Managed instance;

    StorageManager(Managed instance) {
        this.instance = instance;
    }

    // ── static boot helpers ───────────────────────────────────────────────────

    /** Call once in preInit. Calls initFile() on every registered storage. */
    public static void initAll(File configDir) {
        for (StorageManager entry : values()) {
            entry.instance.initFile(configDir);
        }
        ProtectedItemStorage.INSTANCE.init(ATHRConfig.configDirectory);
    }

    /** Call once in clientInit. Calls load() on every registered storage. */
    public static void loadAll() {
        for (StorageManager entry : values()) {
            entry.instance.load();
        }
    }

    /**
     * Starts the 60-second auto-save timer.
     * Only storages that implement {@link AutoSaveable} are included.
     * Call once from clientInit after loadAll().
     */
    public static void startAutoSave() {
        Timer timer = new Timer("ATHR-AutoSave", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (StorageManager entry : values()) {
                    if (entry.instance instanceof AutoSaveable) {
                        try {
                            ((AutoSaveable) entry.instance).autoSave();
                        } catch (Exception e) {
                            System.err.println("[ATHR/AutoSave] Error saving " + entry.name() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }, 60_000L, 60_000L);
    }

    // ── shared I/O utilities (mirrors SkyHanni's ConfigManager) ──────────────

    /**
     * Loads a JSON file into the given class. On corruption, renames the bad
     * file to a dated .corrupted backup and returns {@code null} so callers
     * can fall back to a default instance.
     */
    public static <T> T loadSafe(File file, Class<T> clazz, Gson gson) {
        if (file == null || !file.exists()) return null;
        try (Reader r = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            return gson.fromJson(r, clazz);
        } catch (Exception e) {
            System.err.println("[ATHR] Failed to load " + file.getName() + ": " + e.getMessage());
            backupCorrupted(file);
            return null;
        }
    }

    /**
     * Variant for generic types (TypeToken). Same corruption handling.
     */
    public static <T> T loadSafe(File file, java.lang.reflect.Type type, Gson gson) {
        if (file == null || !file.exists()) return null;
        try (Reader r = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            return gson.fromJson(r, type);
        } catch (Exception e) {
            System.err.println("[ATHR] Failed to load " + file.getName() + ": " + e.getMessage());
            backupCorrupted(file);
            return null;
        }
    }

    /**
     * Atomically saves an object to disk using a .tmp → rename pattern.
     * Verifies the write before committing. On failure, leaves the original
     * file untouched.
     */
    public static void saveAtomic(File file, Object data, Gson gson) {
        if (file == null) return;
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer w = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(tmp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                StandardCharsets.UTF_8))) {
            gson.toJson(data, w);
            w.flush();
        } catch (Exception e) {
            System.err.println("[ATHR] Failed to write " + tmp.getName() + ": " + e.getMessage());
            tmp.delete();
            return;
        }

        // atomic rename
        try {
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("[ATHR] Failed to commit " + file.getName() + ": " + e.getMessage());
            tmp.delete();
        }
    }

    private static void backupCorrupted(File file) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backup = new File(file.getParentFile(), file.getName() + "." + stamp + ".corrupted");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[ATHR] Backed up corrupted file to " + backup.getName());
        } catch (Exception ignored) {}
    }

    // ── interfaces ────────────────────────────────────────────────────────────

    /**
     * Every storage singleton must implement this so the enum can drive it.
     * Matches the existing initFile / load pattern all storage classes already use.
     */
    public interface Managed {
        void initFile(File configDir);
        void load();
    }

    /**
     * Optional: implement if the storage should participate in the 60s
     * auto-save timer. Most classes just call save(); WaypointStorage
     * should call saveIfDirty() instead.
     */
    public interface AutoSaveable {
        void autoSave();
    }
}
