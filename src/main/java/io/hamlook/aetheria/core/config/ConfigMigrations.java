package io.hamlook.aetheria.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.config.migrations.EnchantParserNested;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class ConfigMigrations {

    public static final String VERSION_KEY = "configVersion";

    private static final List<ConfigMigrationHandler> STEPS = Collections.unmodifiableList(Arrays.asList(
            new EnchantParserNested()      // v1
    ));

    public static final int CURRENT = STEPS.size();

    static {
        checkContiguity(STEPS);
    }

    static void checkContiguity(List<ConfigMigrationHandler> steps) {
        for (int i = 0; i < steps.size(); i++) {
            int expected = i + 1;
            int actual = steps.get(i).version();
            if (actual != expected) {
                throw new IllegalStateException("Config migration at index " + i
                        + " declares v" + actual + ", expected v" + expected + " (append only)");
            }
        }
    }

    private ConfigMigrations() {}

    public static int readVersion(JsonObject root) {
        JsonElement element = root.get(VERSION_KEY);
        if (element == null || !element.isJsonPrimitive()) return 0;
        try {
            return element.getAsInt();
        } catch (NumberFormatException e) {
            Aetheria.logger.warning("[ATHR] config.json has a non-numeric " + VERSION_KEY + "; treating it as 0");
            return 0;
        }
    }

    public static boolean isIncomplete(int from, int reached) {
        return from < CURRENT && reached < CURRENT;
    }

    public static JsonObject apply(JsonObject root) {
        return apply(root, STEPS);
    }

    static JsonObject apply(JsonObject root, List<ConfigMigrationHandler> steps) {
        int from = readVersion(root);
        int target = steps.size();
        if (from >= target) {
            if (from > target) {
                Aetheria.logger.warning("[ATHR] config.json is from a newer build (v" + from + " > v" + target
                        + "); leaving its version untouched");
            }
            return root;
        }

        JsonObject committed = root;
        int reached = from;
        for (int version = from + 1; version <= target; version++) {
            ConfigMigrationHandler step = steps.get(version - 1);
            JsonObject candidate = committed.deepCopy();
            try {
                step.apply(new ConfigFixContext(candidate));
            } catch (Throwable t) {
                Aetheria.logger.log(Level.SEVERE, "[ATHR] config migration v" + version + " ("
                        + step.getClass().getSimpleName() + ") failed; keeping v" + reached, t);
                break;
            }
            committed = candidate;
            reached = version;
        }
        if (reached == from) return root;
        committed.addProperty(VERSION_KEY, reached);
        return committed;
    }
}
