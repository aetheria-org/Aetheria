package io.hamlook.aetheria.core;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.diana.DianaStats;
import io.hamlook.aetheria.features.farming.farmingtracker.FarmingTrackerData;
import io.hamlook.aetheria.features.farming.organicmatter.OrganicMatterTrackerData;
import io.hamlook.aetheria.features.farming.pests.PestStats;
import io.hamlook.aetheria.features.fishing.trophy.TrophyFishStorage;
import io.hamlook.aetheria.features.mining.gold.GoldStats;
import io.hamlook.aetheria.features.mining.powder.PowderStats;
import io.hamlook.aetheria.features.mining.pristine.PristineStats;
import io.hamlook.aetheria.features.misc.ghosttracker.GhostStats;
import io.hamlook.aetheria.features.misc.invbuttons.InventoryButtonStorage;
import io.hamlook.aetheria.features.misc.pet.CurrentPetTracker;
import io.hamlook.aetheria.features.misc.pet.PetCache;
import io.hamlook.aetheria.features.misc.protect.ProtectedItemStorage;
import io.hamlook.aetheria.features.scoreboard.MaxwellPowerSync;
import io.hamlook.aetheria.features.storage.data.StorageData;
import io.hamlook.aetheria.features.waypoints.WaypointStorage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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
 */
public enum StorageManager {

    WAYPOINTS(WaypointStorage.getInstance()), INV_BUTTONS(InventoryButtonStorage.getInstance()), DIANA_STATS(DianaStats.getInstance()), POWDER_STATS(PowderStats.getInstance()), PRISTINE_STATS(PristineStats.getInstance()), MAXWELL_POWER(MaxwellPowerSync.getInstance()), PET_CACHE(PetCache.getInstance()), CURRENT_PET(CurrentPetTracker.getInstance()), TROPHY_FISH(TrophyFishStorage.getInstance()), FARMING_TRACKER(FarmingTrackerData.getInstance()), ORGANIC_MATTER_TRACKER(OrganicMatterTrackerData.getInstance()), GHOST_STATS(GhostStats.getInstance()), PEST_STATS(PestStats.getInstance()), GOLD_STATS(GoldStats.getInstance());

    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private final Managed instance;

    StorageManager(Managed instance) {
        this.instance = instance;
    }

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

    /** Calls save() on every profile-managed storage. */
    public static void saveAllProfileData() {
        for (StorageManager entry : values()) {
            if (entry.instance instanceof ProfileManagedStorage) {
                ((ProfileManagedStorage) entry.instance).save();
            }
        }
    }

    /** Calls load() on every profile-managed storage. */
    public static void loadAllProfileData() {
        for (StorageManager entry : values()) {
            if (entry.instance instanceof ProfileManagedStorage) {
                entry.instance.load();
            }
        }
    }

    /**
     * Saves every storage at shutdown so in-memory changes made since the last
     * 60s auto-save are not lost. The auto-save timer is daemon and does not run
     * during JVM shutdown, so this is the final flush for all tracked data.
     */
    public static void saveAll() {
        for (StorageManager entry : values()) {
            if (entry.instance instanceof AutoSaveable) {
                try {
                    ((AutoSaveable) entry.instance).autoSave();
                } catch (Exception e) {
                    Aetheria.logger.severe("[ATHR/Shutdown] Error saving " + entry.name() + ": " + e.getMessage());
                }
            }
        }
        try {
            PetCache.getInstance().save();
        } catch (Exception ignored) {
        }
        try {
            CurrentPetTracker.getInstance().save();
        } catch (Exception ignored) {
        }
        try {
            StorageData.saveContainers();
        } catch (Exception ignored) {
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
                            Aetheria.logger.severe("[ATHR/AutoSave] Error saving " + entry.name() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }, 60_000L, 60_000L);
    }

    /**
     * Loads a JSON file into the given class. On corruption, renames the bad
     * file to a dated .corrupted backup and returns {@code null} so callers
     * can fall back to a default instance.
     */
    public static <T> T loadSafe(File file, Class<T> clazz, Gson gson) {
        if (file == null || !file.exists()) return null;
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to read " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "loading", e.getMessage());
            backupCorrupted(file);
            return null;
        }
        boolean legacy;
        String json;
        try {
            json = decodeStrictUtf8(bytes);
            legacy = false;
        } catch (CharacterCodingException e) {
            json = new String(bytes, WINDOWS_1252);
            legacy = true;
        }
        try {
            T loaded = gson.fromJson(json, clazz);
            if (loaded == null && bytes.length > 0) {
                String msg = "parsed to null (blank or null content), treating as corrupted";
                Aetheria.logger.warning("[ATHR] " + file.getName() + " " + msg);
                CrashLog.report(file, "loading", msg);
                backupCorrupted(file);
                return null;
            }
            if (legacy && loaded != null) {
                saveAtomic(file, loaded, gson);
            }
            return loaded;
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to load " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "loading", e.getMessage());
            backupCorrupted(file);
            return null;
        }
    }

    /**
     * Variant for generic types (TypeToken). Same corruption handling.
     */
    public static <T> T loadSafe(File file, java.lang.reflect.Type type, Gson gson) {
        if (file == null || !file.exists()) return null;
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to read " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "loading", e.getMessage());
            backupCorrupted(file);
            return null;
        }
        boolean legacy;
        String json;
        try {
            json = decodeStrictUtf8(bytes);
            legacy = false;
        } catch (CharacterCodingException e) {
            json = new String(bytes, WINDOWS_1252);
            legacy = true;
        }
        try {
            T loaded = gson.fromJson(json, type);
            if (loaded == null && bytes.length > 0) {
                String msg = "parsed to null (blank or null content), treating as corrupted";
                Aetheria.logger.warning("[ATHR] " + file.getName() + " " + msg);
                CrashLog.report(file, "loading", msg);
                backupCorrupted(file);
                return null;
            }
            if (legacy && loaded != null) {
                saveAtomic(file, loaded, gson);
            }
            return loaded;
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to load " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "loading", e.getMessage());
            backupCorrupted(file);
            return null;
        }
    }

    /**
     * Atomically saves an object to disk using a .tmp → rename pattern.
     * Verifies the write before committing. On failure, leaves the original
     * file untouched and returns {@code false}. Serialized per target path so
     * concurrent saves of the same file cannot tear the shared .tmp.
     */
    public static boolean saveAtomic(File file, Object data, Gson gson) {
        if (file == null) return false;
        Object lock = FILE_LOCKS.computeIfAbsent(file.getAbsolutePath(), k -> new Object());
        synchronized (lock) {
            return saveAtomicLocked(file, data, gson);
        }
    }

    private static boolean saveAtomicLocked(File file, Object data, Gson gson) {
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer w = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(tmp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8))) {
            gson.toJson(data, w);
            w.flush();
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to write " + tmp.getName() + ": " + e.getMessage());
            CrashLog.report(file, "saving", "write failed: " + e.getMessage());
            tmp.delete();
            return false;
        }

        // verify the written tmp actually parses before committing
        try {
            String content = new String(Files.readAllBytes(tmp.toPath()), StandardCharsets.UTF_8);
            JsonParser.parseString(content);
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Refusing to commit " + tmp.getName() + " — write verification failed: " + e.getMessage());
            CrashLog.report(file, "saving", "write verification failed: " + e.getMessage());
            tmp.delete();
            return false;
        }

        // fsync so a hard crash cannot leave a zero-filled file behind the atomic rename
        try (FileChannel ch = FileChannel.open(tmp.toPath(), StandardOpenOption.WRITE)) {
            ch.force(true);
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] fsync failed for " + tmp.getName() + ", committing anyway: " + e.getMessage());
        }

        // atomic rename
        try {
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to commit " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "saving", "commit failed: " + e.getMessage());
            tmp.delete();
            return false;
        }
        return true;
    }

    /**
     * Atomically saves a raw JSON string (e.g. a cached remote data body) using the
     * same hardened tmp → verify → fsync → rename flow as {@link #saveAtomic}, but
     * takes a String body instead of a typed object.
     * <p>
     * This is the <b>caching primitive for remote data</b> (used by {@code RepoManager}
     * for repo bodies). Example:
     * <pre>
     *   File cache = new File(ATHRConfig.configDirectory, "repo/mykey.json");
     *   if (StorageManager.saveAtomicRaw(cache, json)) {   // commit a fetch (durable)
     *       ...
     *   }
     * </pre>
     * Write failures, write-verification failures and rename failures return
     * {@code false} and raise a CrashLog report (one aggregated report per launch).
     * Safe to call from any thread — a per-path lock serialises concurrent writers
     * of the same file.
     */
    public static boolean saveAtomicRaw(File file, String json) {
        if (file == null) return false;
        Object lock = FILE_LOCKS.computeIfAbsent(file.getAbsolutePath(), k -> new Object());
        synchronized (lock) {
            return saveAtomicRawLocked(file, json);
        }
    }

    private static boolean saveAtomicRawLocked(File file, String json) {
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer w = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(tmp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8))) {
            w.write(json);
            w.flush();
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to write " + tmp.getName() + ": " + e.getMessage());
            CrashLog.report(file, "saving", "write failed: " + e.getMessage());
            tmp.delete();
            return false;
        }

        try {
            String content = new String(Files.readAllBytes(tmp.toPath()), StandardCharsets.UTF_8);
            JsonParser.parseString(content);
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Refusing to commit " + tmp.getName() + " — write verification failed: " + e.getMessage());
            CrashLog.report(file, "saving", "write verification failed: " + e.getMessage());
            tmp.delete();
            return false;
        }

        try (FileChannel ch = FileChannel.open(tmp.toPath(), StandardOpenOption.WRITE)) {
            ch.force(true);
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] fsync failed for " + tmp.getName() + ", committing anyway: " + e.getMessage());
        }

        try {
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to commit " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "saving", "commit failed: " + e.getMessage());
            tmp.delete();
            return false;
        }
        return true;
    }

    /**
     * Atomically saves a {@link BufferedImage} as PNG using the same hardened
     * tmp → verify → fsync → rename flow as {@link #saveAtomicRaw}. The binary
     * counterpart for cached remote images (emoji sprite sheets, skins).
     * <p>
     * Write failures, read-back verification failures and rename failures return
     * {@code false} and raise a CrashLog report. Safe to call from any thread —
     * a per-path lock serialises concurrent writers of the same file.
     */
    public static boolean saveAtomicImage(File file, BufferedImage image) {
        if (file == null || image == null) return false;
        Object lock = FILE_LOCKS.computeIfAbsent(file.getAbsolutePath(), k -> new Object());
        synchronized (lock) {
            return saveAtomicImageLocked(file, image);
        }
    }

    private static boolean saveAtomicImageLocked(File file, BufferedImage image) {
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ImageIO.write(image, "png", os);
            os.flush();
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to write " + tmp.getName() + ": " + e.getMessage());
            CrashLog.report(file, "saving", "write failed: " + e.getMessage());
            tmp.delete();
            return false;
        }

        try {
            BufferedImage check = ImageIO.read(tmp);
            if (check == null || check.getWidth() != image.getWidth() || check.getHeight() != image.getHeight()) {
                throw new Exception("read-back mismatch (" + (check == null ? "null" : check.getWidth() + "x" + check.getHeight()) + ")");
            }
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Refusing to commit " + tmp.getName() + " — write verification failed: " + e.getMessage());
            CrashLog.report(file, "saving", "write verification failed: " + e.getMessage());
            tmp.delete();
            return false;
        }

        try (FileChannel ch = FileChannel.open(tmp.toPath(), StandardOpenOption.WRITE)) {
            ch.force(true);
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] fsync failed for " + tmp.getName() + ", committing anyway: " + e.getMessage());
        }

        try {
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to commit " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "saving", "commit failed: " + e.getMessage());
            tmp.delete();
            return false;
        }
        return true;
    }

    /**
     * Reads a raw JSON file as a String with the same corruption handling as
     * {@link #loadSafe}: missing file → {@code null}, unparseable content → backed
     * up to {@code *.corrupted} and {@code null} returned. Legacy windows-1252
     * bytes are re-decoded like {@code loadSafe}.
     * <p>
     * The read counterpart of {@link #saveAtomicRaw} for caching remote data:
     * <pre>
     *   File cache = new File(ATHRConfig.configDirectory, "repo/mykey.json");
     *   String cached = StorageManager.loadSafeRaw(cache);   // null if missing/corrupt
     * </pre>
     */
    public static String loadSafeRaw(File file) {
        if (file == null || !file.exists()) return null;
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            Aetheria.logger.severe("[ATHR] Failed to read " + file.getName() + ": " + e.getMessage());
            CrashLog.report(file, "loading", e.getMessage());
            backupCorrupted(file);
            return null;
        }
        String json;
        try {
            json = decodeStrictUtf8(bytes);
        } catch (CharacterCodingException e) {
            json = new String(bytes, WINDOWS_1252);
        }
        try {
            JsonParser.parseString(json);
        } catch (Exception e) {
            if (bytes.length > 0) {
                String msg = "does not parse as JSON, treating as corrupted";
                Aetheria.logger.warning("[ATHR] " + file.getName() + " " + msg);
                CrashLog.report(file, "loading", msg);
                backupCorrupted(file);
            }
            return null;
        }
        return json;
    }

    private static String decodeStrictUtf8(byte[] bytes) throws CharacterCodingException {
        CharsetDecoder strict = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String json = strict.decode(ByteBuffer.wrap(bytes)).toString();
        return !json.isEmpty() && json.charAt(0) == '\uFEFF' ? json.substring(1) : json;
    }

    private static void backupCorrupted(File file) {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backup = new File(file.getParentFile(), file.getName() + "." + stamp + ".corrupted");
        try {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Aetheria.logger.warning("[ATHR] Backed up corrupted file to " + backup.getName());
        } catch (Exception ignored) {
        }
    }

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
