package io.hamlook.aetheria.utils;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class JsonCache {
    private final Gson gson;
    private final ConcurrentMap<String, String> raws = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<Type, TypedEntry>> parsed = new ConcurrentHashMap<>();

    public JsonCache(Gson gson) {
        this.gson = gson;
    }

    public void store(String key, String json) {
        raws.put(key, json);
    }

    public String retrieve(String key) {
        return raws.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(String key, Type type, Supplier<T> fallback) {
        String json = raws.get(key);
        if (json == null) return fallback != null ? fallback.get() : null;

        ConcurrentMap<Type, TypedEntry> byType = parsed.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        TypedEntry existing = byType.get(type);
        if (existing != null && existing.json.equals(json)) {
            return (T) existing.value;
        }

        try {
            T result = gson.fromJson(json, type);
            if (result != null) {
                byType.put(type, new TypedEntry(json, result));
                return result;
            }
        } catch (Exception e) {
            System.err.println("[ATHR] JSON error (" + key + "): " + e.getMessage());
        }
        return fallback != null ? fallback.get() : null;
    }

    public void drop(String key) {
        raws.remove(key);
        parsed.remove(key);
    }

    private static class TypedEntry {
        final String json;
        final Object value;

        TypedEntry(String json, Object value) {
            this.json = json;
            this.value = value;
        }
    }
}
