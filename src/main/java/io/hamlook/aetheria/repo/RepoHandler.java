package io.hamlook.aetheria.repo;

import java.lang.reflect.Type;

/**
 * Static facade over the {@link RepoManager} engine — this is what feature code
 * uses to read repo-hosted data.
 * <p>
 * Data files (JSON bodies) are downloaded from the {@code Aetheria-REPO}
 * repository, held in an in-memory {@code JsonCache}, and (for versioned keys)
 * mirrored to {@code config/Aetheria/repo/} with versions tracked against the
 * remote {@code ASMDataVersions.json} manifest. All downloads run on
 * {@code ThreadUtils}; every network call is gated on
 * {@code NetworkGuard.githubAllowed()}.
 * <p>
 * <b>Add a new repo data source</b> — register it in {@link ATHRRepo#init()}:
 * <pre>
 *   RepoHandler.register("mykey", ATHRRepo.BASE + "data/mykey.json");
 * </pre>
 * Pick the registration mode that matches the file:
 * <ul>
 *   <li>{@link #register(String, String)} — serial download, version-checked
 *       against the manifest and cached to disk under
 *       {@code config/Aetheria/repo/mykey.json}.</li>
 *   <li>{@link #registerParallel(String, String)} — same, but each key downloads
 *       on its own background task; use for the large files.</li>
 *   <li>{@link #registerAlwaysFetch(String, String)} — fetched fresh (in parallel)
 *       on every launch and server join; never versioned and never written to
 *       disk; use for small, frequently-changed data (player sizes, timers, tags,
 *       update check, API urls).</li>
 *   <li>{@link #registerVersionOnly(String)} — a manifest entry with no body of
 *       its own (the emoji sheets manage their own download).</li>
 * </ul>
 * <b>Read the data</b> (any thread, synchronous, from the in-memory cache):
 * <pre>
 *   MyData data = RepoHandler.get(ATHRRepo.KEY_MYKEY, MyData.class, FALLBACK);
 *   String raw  = RepoHandler.getJson(ATHRRepo.KEY_MYKEY);
 * </pre>
 * If the body has not been fetched yet you get the fallback / {@code null} —
 * retry on your own tick loop, or use {@link #addListener(String, Runnable)} to
 * be notified when a fetch completes.
 * <p>
 * <b>Freshness:</b> {@link #refresh(String)} re-checks a single key (server join,
 * {@code /athr reload}); versioned keys consult the manifest first (10s TTL),
 * always-fetch keys download unconditionally.
 */
public class RepoHandler {
    private static final RepoManager MANAGER = new RepoManager();

    /**
     * Register a versioned, serial-download source. See the class javadoc for usage.
     */
    public static void register(String key, String url) {
        MANAGER.register(key, url);
    }

    /**
     * Register a versioned source fetched on its own background task (large files).
     */
    public static void registerParallel(String key, String url) {
        MANAGER.registerParallel(key, url);
    }

    /**
     * Register an always-fetch source: downloaded fresh in parallel on every
     * launch and server join, never versioned and never written to disk.
     */
    public static void registerAlwaysFetch(String key, String url) {
        MANAGER.registerAlwaysFetch(key, url);
    }

    /**
     * Register a manifest-only key (no body of its own) — used to gate downloads
     * the feature manages itself, e.g. the emoji sheets.
     */
    public static void registerVersionOnly(String key) {
        MANAGER.registerVersionOnly(key);
    }

    /**
     * Register a callback fired on the background thread after {@code key}'s
     * body is fetched successfully.
     */
    public static void addListener(String key, Runnable cb) {
        MANAGER.listen(key, cb);
    }

    /**
     * Load cached bodies + versions from disk, then refresh everything in the
     * background. Called once at startup by {@link ATHRRepo#init()}.
     */
    public static void warmupAll() {
        MANAGER.warmupAll();
    }

    /**
     * Re-check a single key in the background: versioned keys re-fetch the
     * manifest first (10s TTL) and only download when changed/missing;
     * always-fetch keys download unconditionally.
     */
    public static void refresh(String key) {
        MANAGER.refresh(key);
    }

    /**
     * True when {@code key}'s manifest version differs from the locally stored
     * version (used by features that manage their own body download, e.g. emojis).
     * Unknown manifest → {@code local == 0}; key missing from manifest → false.
     */
    public static boolean isUpdateNeeded(String key) {
        return MANAGER.isUpdateNeeded(key);
    }

    /**
     * Persist the current manifest version for {@code key} after a successful
     * self-managed download. No-ops while the manifest is unknown.
     */
    public static void saveVersion(String key) {
        MANAGER.saveVersion(key);
    }

    /**
     * The locally stored manifest version for {@code key} (0 when unknown).
     */
    public static int getLocalVersion(String key) {
        return MANAGER.getLocalVersion(key);
    }

    /**
     * The raw cached JSON string for {@code key}, or {@code null} if not fetched.
     */
    public static String getJson(String key) {
        return MANAGER.raw(key);
    }

    /**
     * Parse {@code key}'s cached body as {@code type}, or return {@code fb} if
     * it is missing/unparseable. Thread-safe; cheap (in-memory only).
     */
    public static <T> T get(String key, Class<T> t, T fb) {
        return MANAGER.get(key, t, fb);
    }

    /**
     * Like {@link #get(String, Class, Object)} but for a generic {@link Type}
     * (e.g. a {@code List<PlayerSizeData>}).
     */
    public static <T> T get(String key, Type t, T fb) {
        return MANAGER.get(key, t, fb);
    }
}
