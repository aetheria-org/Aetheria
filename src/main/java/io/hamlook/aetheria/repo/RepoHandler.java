package io.hamlook.aetheria.repo;

import java.lang.reflect.Type;

public class RepoHandler {
    private static final RepoManager MANAGER = new RepoManager();

    public static void register(String key, String url)     { MANAGER.register(key, url); }
    public static void addListener(String key, Runnable cb) { MANAGER.listen(key, cb); }
    public static void warmupAll()                          { MANAGER.refreshAll(); }
    public static void refresh(String key)                  { MANAGER.refresh(key); }
    public static String getJson(String key)                { return MANAGER.raw(key); }
    public static <T> T get(String key, Class<T> t, T fb)   { return MANAGER.get(key, t, fb); }
    public static <T> T get(String key, Type t, T fb)       { return MANAGER.get(key, t, fb); }
    public static void shutdown()                           { MANAGER.shutdown(); }
}
