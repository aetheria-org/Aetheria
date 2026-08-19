package io.hamlook.aetheria.features.chat.globalchat.image;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ImageManager {

    public static ConcurrentHashMap<String,GCImage> images = new ConcurrentHashMap<>();
    /** Shared single-thread scheduler also used by ExpiringArrayList; GCImage animation is now render-driven. */
    public static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /** URL → GCImage id cache shared by every screen (chat, image viewer, ...) so a URL is only downloaded/decoded once. */
    private static final Map<String, String> urlToImage = new ConcurrentHashMap<>();

    public static void initialise() {
        images.clear();
        urlToImage.clear();
    }

    /** Returns the id of the GCImage for a URL, creating (and asynchronously loading) it the first time it's seen. */
    public static String getOrCreateImage(String url, boolean circularMask) {
        String id = urlToImage.get(url);
        if (id == null || !images.containsKey(id)) {
            id = GCImage.createGCImage(url, circularMask);
            urlToImage.put(url, id);
        }
        return id;
    }
}