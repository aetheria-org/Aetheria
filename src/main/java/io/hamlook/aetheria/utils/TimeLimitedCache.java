package io.hamlook.aetheria.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class TimeLimitedCache<K, V> {

    private final Cache<K, V> cache;

    private TimeLimitedCache(CacheBuilder<Object, Object> builder, long idleMs, long maximumSize) {
        this.cache = builder.expireAfterAccess(idleMs, TimeUnit.MILLISECONDS).maximumSize(maximumSize).build();
    }

    public static <K, V> TimeLimitedCache<K, V> weakKeys(long idleMs, long maximumSize) {
        return new TimeLimitedCache<>(CacheBuilder.newBuilder().weakKeys(), idleMs, maximumSize);
    }

    public V getOrCompute(K key, Callable<V> loader) {
        V present = cache.getIfPresent(key);
        if (present != null) return present;
        try {
            return cache.get(key, loader);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
