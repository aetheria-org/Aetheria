package io.hamlook.aetheria.features.chat.globalchat.image;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImageManager {

    public static HashMap<String,GCImage> images = new HashMap<>();
    public static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static boolean reduceAnimations = false;

    public static void initialise() {
        images.clear();
        executor.scheduleAtFixedRate(() -> images.values().forEach(GCImage::updateFrame), 1, 1, TimeUnit.MILLISECONDS);
    }
}