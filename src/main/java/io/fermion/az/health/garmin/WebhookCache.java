package io.fermion.az.health.garmin;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache to temporarily store recent Garmin webhook data.
 * Helps avoid duplicate logs and allows integration testing before database storage.
 */
public class WebhookCache {

    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    private static final Map<String, CachedItem> cache = new ConcurrentHashMap<>();

    public static void put(String key, Object value) {
        cache.put(key, new CachedItem(value));
    }

    public static Object get(String key) {
        CachedItem item = cache.get(key);
        if (item == null || item.isExpired()) {
            cache.remove(key);
            return null;
        }
        return item.value();
    }

    public static void cleanup() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private record CachedItem(Object value, long timestamp) {
        CachedItem(Object value) {
            this(value, Instant.now().toEpochMilli());
        }

        boolean isExpired() {
            return Instant.now().toEpochMilli() - timestamp > EXPIRY_MS;
        }
    }
}
