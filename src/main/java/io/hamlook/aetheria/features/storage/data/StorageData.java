package io.hamlook.aetheria.features.storage.data;

import io.hamlook.aetheria.features.storage.utils.SContainer;

import java.util.LinkedHashMap;
import java.util.Map;


public class StorageData {

    public static LinkedHashMap<String, SContainer> containers = new LinkedHashMap<>();

    /**
     * Containers mutated since the last persisted save, keyed by container id.
     * Holds the container reference so a container dropped from {@link #containers}
     * (e.g. its nav slot was empty on a later Storage-menu parse) can still be
     * written at close time. Cleared after every save.
     */
    private static final Map<String, SContainer> dirtyContainers = new LinkedHashMap<>();


    public static void loadContainers() {
        containers = StorageSaving.loadStorageData();
    }


    public static void markDirty(SContainer container) {
        if (container == null) return;
        dirtyContainers.put(container.id, container);
    }


    public static boolean isDirty() {
        return !dirtyContainers.isEmpty();
    }


    /**
     * Persists only the mutated containers. Used on overlay close, where disk
     * I/O on the render thread must stay proportional to actual changes.
     */
    public static void saveDirtyContainers() {
        if (dirtyContainers.isEmpty()) return;
        for (SContainer container : dirtyContainers.values()) {
            StorageSaving.saveContainer(container);
        }
        dirtyContainers.clear();
    }


    /**
     * Persists every loaded container plus any still-dirty references, then
     * clears the dirty set. Used at shutdown and profile/environment switches
     * (always before the storage path changes).
     */
    public static void saveContainers() {
        saveDirtyContainers();
        StorageSaving.saveStorageData(containers.values());
    }

}