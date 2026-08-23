package io.hamlook.aetheria.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.JsonCache;
import io.hamlook.aetheria.utils.ThreadUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manifest-driven repo data engine (see {@link RepoHandler} for the usage guide).
 * <p>
 * Sources are registered by key with a URL and a fetch policy. Fetched bodies
 * land in an in-memory {@code JsonCache} that consumers read synchronously.
 * Versioned sources are additionally mirrored to
 * {@code config/Aetheria/repo/<key>.json} (via
 * {@code StorageManager.saveAtomicRaw}) and their versions tracked in
 * {@code config/Aetheria/repo/versions.json}, gated by the remote
 * {@code ASMDataVersions.json} manifest ({@code {key: version}}). ETag-gated
 * sources skip the manifest and instead re-validate with a conditional GET on
 * every refresh, persisting validators in {@code repo/etags.json}. Always-fetch
 * sources skip the manifest, the version file and disk persistence entirely.
 * <p>
 * Boot: {@link #warmupAll()} loads cached bodies + versions + etags from disk on
 * the calling thread (consumers can read immediately), then refreshes everything
 * in the background. {@link #refresh(String)} re-checks a single key and
 * re-fetches the manifest at most once per 10s ({@link #MANIFEST_TTL_MS}) so
 * concurrent refreshes (multiple keys on server join) coalesce into one manifest
 * fetch; etag and always-fetch keys skip the manifest entirely.
 * <p>
 * A version is only persisted after a successful body fetch — a failed/404 fetch
 * leaves the old version so the key retries next launch. Registration must
 * complete before {@link #warmupAll()} (register everything in one init method);
 * fetch/listen/read paths are concurrency-safe.
 */
public class RepoManager {
    private static final String MANIFEST_URL = ATHRRepo.BASE + "data/ASMDataVersions.json";
    private static final File REPO_DIR = new File(ATHRConfig.configDirectory, "repo");
    private static final File VERSIONS_FILE = new File(REPO_DIR, "versions.json");
    private static final File ETAGS_FILE = new File(REPO_DIR, "etags.json");
    private static final Type VERSIONS_TYPE = new TypeToken<Map<String, Integer>>() {
    }.getType();
    private static final Type ETAGS_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final long MANIFEST_TTL_MS = 10_000L;
    private final HttpClient http = new HttpClient();
    private final JsonCache cache = new JsonCache(new GsonBuilder().create());
    private final Gson gson = new Gson();
    private final ConcurrentMap<String, Source> sources = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Runnable>> listeners = new ConcurrentHashMap<>();
    private final Map<String, Integer> localVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> etags = new ConcurrentHashMap<>();
    private volatile JsonObject manifest;
    private volatile long manifestFetchedAt = 0L;

    public void register(String key, String url) {
        sources.put(key, new Source(url, false, false, false));
    }

    public void registerParallel(String key, String url) {
        sources.put(key, new Source(url, true, false, false));
    }

    public void registerAlwaysFetch(String key, String url) {
        sources.put(key, new Source(url, true, false, true));
    }

    /**
     * ETag-gated source: re-validated with a conditional GET on every launch and
     * server join. A 304 keeps the cached body (zero transfer); a 200 updates the
     * in-memory cache and the atomic disk copy at {@code repo/<key>.json}.
     * Auto-propagates content changes without any manifest entry.
     */
    public void registerEtagFetch(String key, String url) {
        sources.put(key, new Source(url, true, false, false, true, null, null));
    }

    /** Same as {@link #registerEtagFetch(String, String)} plus one-time migration of a legacy cache file. */
    public void registerEtagFetch(String key, String url, File legacyBody, File legacyEtag) {
        sources.put(key, new Source(url, true, false, false, true, legacyBody, legacyEtag));
    }

    public void registerVersionOnly(String key) {
        sources.put(key, new Source(null, false, true, false));
    }

    public void listen(String key, Runnable callback) {
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void warmupAll() {
        loadFromDisk();
        ThreadUtils.run(this::refreshAll);
    }

    public void refreshAll() {
        if (!NetworkGuard.githubAllowed()) {
            Aetheria.logger.info("[ATHR] Skipping repo refresh: GitHub calls disabled");
            return;
        }
        JsonObject m = ensureManifest();
        List<String> serial = new ArrayList<>();
        List<String> parallel = new ArrayList<>();
        for (String key : sources.keySet()) {
            Source src = sources.get(key);
            if (src.versionOnly) continue;
            if (src.alwaysFetch || src.etagFetch) {
                (src.parallel ? parallel : serial).add(key);
                continue;
            }
            if (m == null) continue;
            if (!shouldFetch(key, m)) continue;
            (src.parallel ? parallel : serial).add(key);
        }
        if (!serial.isEmpty()) {
            ThreadUtils.run(() -> {
                for (String key : serial) fetchBody(key);
            });
        }
        for (String key : parallel) {
            ThreadUtils.run(() -> fetchBody(key));
        }
    }

    public void refresh(String key) {
        if (!sources.containsKey(key)) return;
        if (!NetworkGuard.githubAllowed()) return;
        ThreadUtils.run(() -> {
            Source src = sources.get(key);
            if (src != null && (src.alwaysFetch || src.etagFetch)) {
                fetchBody(key);
                return;
            }
            JsonObject m = fetchManifest(true);
            if (m != null && !shouldFetch(key, m)) return;
            fetchBody(key);
        });
    }

    public String raw(String key) {
        return cache.retrieve(key);
    }

    public <T> T get(String key, Class<T> type, T fallback) {
        return cache.resolve(key, type, () -> fallback);
    }

    public <T> T get(String key, Type type, T fallback) {
        return cache.resolve(key, type, () -> fallback);
    }

    public boolean isUpdateNeeded(String key) {
        JsonObject m = manifest != null ? manifest : ensureManifest();
        Integer remote = manifestVersion(m, key);
        if (remote == null) return getLocalVersion(key) == 0;
        return remote != getLocalVersion(key);
    }

    public void saveVersion(String key) {
        Integer remote = manifestVersion(manifest, key);
        if (remote == null) return;
        localVersions.put(key, remote);
        persistVersions();
    }

    public int getLocalVersion(String key) {
        return localVersions.getOrDefault(key, 0);
    }

    private void loadFromDisk() {
        Map<String, String> storedEtags = StorageManager.loadSafe(ETAGS_FILE, ETAGS_TYPE, gson);
        if (storedEtags != null) etags.putAll(storedEtags);
        for (String key : sources.keySet()) {
            Source src = sources.get(key);
            if (src.versionOnly) continue;
            if (src.alwaysFetch) {
                File f = bodyFile(key);
                if (f.exists()) f.delete();
                continue;
            }
            if (src.etagFetch) migrateLegacyCache(key, src);
            File f = bodyFile(key);
            String json = StorageManager.loadSafeRaw(f);
            if (json != null) cache.store(key, json);
        }
        Map<String, Integer> versions = StorageManager.loadSafe(VERSIONS_FILE, VERSIONS_TYPE, gson);
        if (versions != null) localVersions.putAll(versions);
    }

    private void migrateLegacyCache(String key, Source src) {
        if (src.legacyBody == null || !src.legacyBody.isFile()) return;
        try {
            File target = bodyFile(key);
            boolean sameFile = src.legacyBody.toPath().toAbsolutePath()
                    .equals(target.toPath().toAbsolutePath());
            if (!target.exists() && !sameFile) {
                java.nio.file.Files.move(src.legacyBody.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Aetheria.logger.info("[ATHR] Migrated legacy cache " + src.legacyBody.getName()
                        + " -> " + target.getName());
            }
            if (src.legacyEtag != null && src.legacyEtag.isFile()) {
                String value = new String(java.nio.file.Files.readAllBytes(src.legacyEtag.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) {
                    if (!value.startsWith("\"")) value = "\"" + value + "\"";
                    etags.put(key, value);
                    persistEtags();
                }
                src.legacyEtag.delete();
            }
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] Legacy cache migration failed (" + key + "): " + e.getMessage());
        }
    }

    private synchronized void persistEtags() {
        StorageManager.saveAtomicRaw(ETAGS_FILE, gson.toJson(new HashMap<>(etags)));
    }

    private synchronized JsonObject ensureManifest() {
        return fetchManifest(false);
    }

    private synchronized JsonObject fetchManifest(boolean force) {
        long now = System.currentTimeMillis();
        if (manifest != null && (!force || now - manifestFetchedAt < MANIFEST_TTL_MS)) return manifest;
        if (!NetworkGuard.githubAllowed()) return manifest;
        try {
            HttpClient.FetchResult res = http.fetch(MANIFEST_URL, null);
            if (res.modified() && res.body() != null) {
                manifest = JsonParser.parseString(res.body()).getAsJsonObject();
                manifestFetchedAt = now;
            }
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] Failed to fetch repo manifest: " + e.getMessage());
        }
        return manifest;
    }

    private boolean shouldFetch(String key, JsonObject m) {
        Integer remote = manifestVersion(m, key);
        if (remote == null) return false;
        if (remote != getLocalVersion(key)) return true;
        return cache.retrieve(key) == null;
    }

    private void fetchBody(String key) {
        Source src = sources.get(key);
        if (src == null || src.versionOnly || !src.claim()) return;
        try {
            if (!NetworkGuard.githubAllowed()) return;
            if (src.etagFetch) {
                fetchEtagBody(key, src);
                return;
            }
            HttpClient.FetchResult res = http.fetch(src.url, null);
            if (!res.modified() || res.body() == null) return;
            cache.store(key, res.body());
            if (!src.alwaysFetch) {
                StorageManager.saveAtomicRaw(bodyFile(key), res.body());
                Integer remote = manifestVersion(manifest, key);
                if (remote != null) {
                    localVersions.put(key, remote);
                    persistVersions();
                }
            }
            notifyListeners(key);
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] Fetch failed (" + key + "): " + e.getMessage());
        } finally {
            src.release();
        }
    }

    private void fetchEtagBody(String key, Source src) throws Exception {
        String conditional = cache.retrieve(key) != null ? etags.get(key) : null;
        HttpClient.FetchResult res = http.fetch(src.url, conditional);
        if (!res.modified() || res.body() == null) {
            Aetheria.logger.info("[ATHR] " + key + " up-to-date (etag)");
            return;
        }
        cache.store(key, res.body());
        StorageManager.saveAtomicRaw(bodyFile(key), res.body());
        String etag = res.etag();
        if (etag != null && !etag.equals(etags.get(key))) {
            etags.put(key, etag);
            persistEtags();
        }
        notifyListeners(key);
    }

    /**
     * Drops the cached body (memory + disk) and the stored ETag for a key, so the
     * next refresh performs an unconditional full download. Used by consumers whose
     * parse of the body failed — the recovery path for corrupt/unusable data.
     */
    public void invalidateBody(String key) {
        cache.drop(key);
        bodyFile(key).delete();
        etags.remove(key);
        persistEtags();
    }

    private Integer manifestVersion(JsonObject m, String key) {
        if (m == null || !m.has(key)) return null;
        return m.get(key).getAsInt();
    }

    private void persistVersions() {
        StorageManager.saveAtomic(VERSIONS_FILE, new HashMap<>(localVersions), gson);
    }

    public File cacheFile(String key) {
        return bodyFile(key);
    }

    private File bodyFile(String key) {
        return new File(REPO_DIR, key + ".json");
    }

    private void notifyListeners(String key) {
        List<Runnable> cbs = listeners.get(key);
        if (cbs == null) return;
        for (Runnable cb : cbs) {
            try {
                cb.run();
            } catch (Exception e) {
                Aetheria.logger.warning("[ATHR] Listener error (" + key + "): " + e.getMessage());
            }
        }
    }

    private static class Source {
        final String url;
        final boolean parallel;
        final boolean versionOnly;
        final boolean alwaysFetch;
        final boolean etagFetch;
        final File legacyBody;
        final File legacyEtag;
        private final AtomicBoolean loading = new AtomicBoolean();

        Source(String url, boolean parallel, boolean versionOnly, boolean alwaysFetch) {
            this(url, parallel, versionOnly, alwaysFetch, false, null, null);
        }

        Source(String url, boolean parallel, boolean versionOnly, boolean alwaysFetch,
               boolean etagFetch, File legacyBody, File legacyEtag) {
            this.url = url;
            this.parallel = parallel;
            this.versionOnly = versionOnly;
            this.alwaysFetch = alwaysFetch;
            this.etagFetch = etagFetch;
            this.legacyBody = legacyBody;
            this.legacyEtag = legacyEtag;
        }

        boolean claim() {
            return loading.compareAndSet(false, true);
        }

        void release() {
            loading.set(false);
        }
    }
}