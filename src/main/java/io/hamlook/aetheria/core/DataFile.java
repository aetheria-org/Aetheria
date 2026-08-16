package io.hamlook.aetheria.core;

import com.google.gson.Gson;
import lombok.Getter;

import java.io.File;
import java.lang.reflect.Type;

/**
 * Thin wrapper over a single JSON-backed data file. Bundles the file + Gson
 * pair and routes through the central persistence primitives (loadSafe /
 * saveAtomic), so callers never hand-roll file I/O.
 */
public class DataFile {

    @Getter
    private final File file;
    private final Gson gson;

    public DataFile(File file, Gson gson) {
        this.file = file;
        this.gson = gson;
    }

    public <T> T load(Class<T> clazz) {
        return StorageManager.loadSafe(file, clazz, gson);
    }

    public <T> T load(Type type) {
        return StorageManager.loadSafe(file, type, gson);
    }

    public boolean save(Object data) {
        return StorageManager.saveAtomic(file, data, gson);
    }

}