package io.hamlook.aetheria.utils.chat;

import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ExpiringArrayList<E extends ExpiringArrayList.Trackable> extends ArrayList<E> {

    public interface Trackable {
        boolean isExpired();
    }

    public ExpiringArrayList() {

        ImageManager.executor.scheduleAtFixedRate(this::activePurge, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void activePurge() {
        super.removeIf(Trackable::isExpired);
    }
}
