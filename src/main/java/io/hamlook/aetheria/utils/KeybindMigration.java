package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.Config;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class KeybindMigration {

    private static final Map<Integer, Integer> LWJGL2_TO_GLFW = buildTable();
    private static final Map<Integer, Integer> GLFW_TO_LWJGL2 = invertTable(LWJGL2_TO_GLFW);

    private static Map<Integer, Integer> invertTable(Map<Integer, Integer> forward) {
        HashMap<Integer, Integer> m = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : forward.entrySet()) {
            m.put(e.getValue(), e.getKey());
        }
        return Collections.unmodifiableMap(m);
    }

    private static void put(HashMap<Integer, Integer> m, int lwjgl2, int glfw) {
        m.put(lwjgl2, glfw);
    }

    private static Map<Integer, Integer> buildTable() {
        HashMap<Integer, Integer> m = new HashMap<>();
        int[] letters = {30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50, 49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44};
        for (int i = 0; i < 26; i++) put(m, letters[i], 65 + i);
        int[] digits = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        for (int i = 0; i < 9; i++) put(m, digits[i], 49 + i);
        put(m, 11, 48);
        int[] fkeys = {59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 87, 88};
        for (int i = 0; i < 12; i++) put(m, fkeys[i], 290 + i);
        put(m, 1, 256);
        put(m, 15, 258);
        put(m, 58, 280);
        put(m, 42, 340);
        put(m, 54, 344);
        put(m, 29, 341);
        put(m, 157, 345);
        put(m, 56, 342);
        put(m, 184, 346);
        put(m, 219, 343);
        put(m, 220, 347);
        put(m, 57, 32);
        put(m, 28, 257);
        put(m, 156, 335);
        put(m, 14, 259);
        put(m, 12, 45);
        put(m, 13, 61);
        put(m, 26, 91);
        put(m, 27, 93);
        put(m, 39, 59);
        put(m, 40, 39);
        put(m, 41, 96);
        put(m, 86, 92);
        put(m, 51, 44);
        put(m, 52, 46);
        put(m, 53, 47);
        put(m, 200, 265);
        put(m, 208, 264);
        put(m, 203, 263);
        put(m, 205, 262);
        put(m, 199, 268);
        put(m, 207, 269);
        put(m, 210, 260);
        put(m, 211, 261);
        put(m, 201, 266);
        put(m, 209, 267);
        int[] numpad = {82, 79, 80, 81, 75, 76, 77, 71, 72, 73};
        for (int i = 0; i < 10; i++) put(m, numpad[i], 320 + i);
        put(m, 181, 331);
        put(m, 55, 332);
        put(m, 74, 333);
        put(m, 78, 334);
        put(m, 83, 330);
        put(m, 197, 281);
        put(m, 183, 282);
        return Collections.unmodifiableMap(m);
    }

    public static boolean migrateIfNeeded(Config feature) {
        //#if MC < 1.21
        if (feature == null || feature.keybindMigrationVersion != 1) return false;
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Counter counter = new Counter();
        migrateObject(feature, "", seen, GLFW_TO_LWJGL2, counter);
        feature.keybindMigrationVersion = 0;
        Aetheria.logger.info("[ATHR] Keybind reverse-migration: checked " + counter.fields
                + " bind(s), updated " + counter.changed + ".");
        return counter.changed > 0;
        //#else
        //$$ if (feature == null || feature.keybindMigrationVersion >= 1) return false;
        //$$ Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        //$$ Counter counter = new Counter();
        //$$ migrateObject(feature, "", seen, LWJGL2_TO_GLFW, counter);
        //$$ feature.keybindMigrationVersion = 1;
        //$$ Aetheria.logger.info("[ATHR] Keybind migration: checked " + counter.fields
        //$$         + " bind(s), updated " + counter.changed + ".");
        //$$ return counter.changed > 0;
        //#endif
    }

    private static final class Counter {
        int fields;
        int changed;
    }

    private static int migrateObject(Object obj, String path, Set<Object> seen, Map<Integer, Integer> table, Counter counter) {
        if (obj == null || !seen.add(obj)) return 0;
        int changed = 0;
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.getType() == int.class
                    && field.isAnnotationPresent(ConfigAnnotations.ConfigEditorKeybind.class)) {
                counter.fields++;
                changed += migrateField(obj, field, path, table, counter);
            } else if (!field.getType().isPrimitive()
                    && !field.getType().getName().startsWith("java.")) {
                try {
                    field.setAccessible(true);
                    Object child = field.get(obj);
                    if (child != null) changed += migrateObject(child, path + field.getName() + ".", seen, table, counter);
                } catch (Exception ignored) {}
            }
        }
        return changed;
    }

    private static int migrateField(Object obj, Field field, String path, Map<Integer, Integer> table, Counter counter) {
        try {
            field.setAccessible(true);
            int value = field.getInt(obj);
            if (value < 0) return 0;
            if (value == 0) {
                field.setInt(obj, -1);
                counter.changed++;
                return 1;
            }
            Integer translated = table.get(value);
            if (translated != null) {
                if (translated != value) {
                    field.setInt(obj, translated);
                    counter.changed++;
                }
                return 1;
            }
            Aetheria.logger.log(Level.WARNING,
                    "Keybind migration: no equivalent for stored code " + value
                            + " at " + path + field.getName() + "; left as-is, please rebind in config.");
        } catch (Exception e) {
            Aetheria.logger.log(Level.WARNING, "Keybind migration failed at " + path + field.getName(), e);
        }
        return 0;
    }
}
