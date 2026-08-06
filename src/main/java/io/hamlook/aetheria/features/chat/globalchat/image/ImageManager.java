package io.hamlook.aetheria.features.chat.globalchat.image;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImageManager {

    public static HashMap<String,GCImage> images = new HashMap<>();
    public static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /** URL → GCImage id cache shared by every screen (chat, image viewer, ...) so a URL is only downloaded/decoded once. */
    private static final Map<String, String> urlToImage = new ConcurrentHashMap<>();

    public static void initialise() {
        images.clear();
        urlToImage.clear();
        executor.scheduleAtFixedRate(() -> images.values().forEach(GCImage::updateFrame), 1, 1, TimeUnit.MILLISECONDS);
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