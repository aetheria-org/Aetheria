package io.hamlook.aetheria.repo;

import com.google.gson.GsonBuilder;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.JsonCache;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepoManager {
    private final HttpClient http = new HttpClient();
    private final JsonCache cache = new JsonCache(new GsonBuilder().create());
    private final ConcurrentMap<String, Source> sources = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Runnable>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService pool = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32), r -> {
        Thread t = new Thread(r, "ATHR-IO");
        t.setDaemon(true);
        return t;
    }, new ThreadPoolExecutor.DiscardOldestPolicy());

    public void register(String key, String url) {
        sources.put(key, new Source(url));
    }

    public void listen(String key, Runnable callback) {
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void refreshAll() {
        for (String key : sources.keySet()) {
            System.out.println("[ATHR] Fetching: " + sources.get(key).url);
            doFetch(key);
        }
    }

    public void refresh(String key) {
        if (sources.containsKey(key)) doFetch(key);
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

    public void shutdown() {
        pool.shutdownNow();
    }

    private void doFetch(String key) {
        Source src = sources.get(key);
        if (src == null || !src.claim()) return;
        pool.execute(() -> {
            try {
                HttpClient.FetchResult result = http.fetch(src.url, src.etag);
                if (result.modified() && result.body() != null) {
                    src.etag = result.etag();
                    cache.store(key, result.body());
                    notifyListeners(key);
                }
            } catch (Exception e) {
                System.err.println("[ATHR] Fetch failed (" + key + "): " + e.getMessage());
            } finally {
                src.release();
            }
        });
    }

    private void notifyListeners(String key) {
        List<Runnable> cbs = listeners.get(key);
        if (cbs == null) return;
        for (Runnable cb : cbs) {
            try {
                cb.run();
            } catch (Exception e) {
                System.err.println("[ATHR] Listener error (" + key + "): " + e.getMessage());
            }
        }
    }

    private static class Source {
        final String url;
        private final AtomicBoolean loading = new AtomicBoolean();
        volatile String etag;

        Source(String url) {
            this.url = url;
        }

        boolean claim() {
            return loading.compareAndSet(false, true);
        }

        void release() {
            loading.set(false);
        }
    }
}
