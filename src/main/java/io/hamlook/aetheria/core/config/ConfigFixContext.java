package io.hamlook.aetheria.core.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.Aetheria;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ConfigFixContext {

    private final JsonObject root;

    public ConfigFixContext(JsonObject root) {
        this.root = root;
    }

    public JsonElement get(String path) {
        String[] parts = split(path);
        JsonObject parent = objectAt(parts, parts.length - 1);
        return parent == null ? null : parent.get(parts[parts.length - 1]);
    }

    public boolean has(String path) {
        return get(path) != null;
    }

    public JsonObject object(String path) {
        JsonElement element = get(path);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public JsonArray array(String path) {
        JsonElement element = get(path);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    public boolean transform(String path, Function<JsonElement, JsonElement> fn) {
        String[] parts = split(path);
        JsonObject parent = objectAt(parts, parts.length - 1);
        if (parent == null) return false;
        String key = parts[parts.length - 1];
        JsonElement old = parent.get(key);
        if (old == null) return false;
        JsonElement next = fn.apply(old);
        if (next == null) parent.remove(key);
        else parent.add(key, next);
        return true;
    }

    public boolean add(String path, Supplier<JsonElement> value) {
        if (has(path)) return false;
        String[] parts = split(path);
        JsonObject parent = ensureObjectAt(parts, parts.length - 1);
        if (parent == null) return false;
        parent.add(parts[parts.length - 1], value.get());
        return true;
    }

    public boolean move(String from, String to) {
        return move(from, to, element -> element);
    }

    public boolean move(String from, String to, Function<JsonElement, JsonElement> fn) {
        if (from.equals(to) || to.startsWith(from + ".")) return false;
        JsonElement value = get(from);
        if (value == null) return false;
        if (has(to)) {
            Aetheria.logger.warning("[ATHR] config migration skipped " + from + " -> " + to + ": destination occupied");
            return false;
        }
        JsonElement next = fn.apply(value);
        if (next == null) {
            Aetheria.logger.warning("[ATHR] config migration aborted " + from + " -> " + to + ": transform returned null");
            return false;
        }
        String[] fromParts = split(from);
        String[] toParts = split(to);
        JsonObject fromParent = objectAt(fromParts, fromParts.length - 1);
        JsonObject toParent = ensureObjectAt(toParts, toParts.length - 1);
        if (fromParent == null || toParent == null) return false;
        toParent.add(toParts[toParts.length - 1], next);
        fromParent.remove(fromParts[fromParts.length - 1]);
        return true;
    }

    public boolean remove(String path) {
        String[] parts = split(path);
        JsonObject parent = objectAt(parts, parts.length - 1);
        return parent != null && parent.remove(parts[parts.length - 1]) != null;
    }

    public boolean addAfter(String arrayPath, JsonElement value, JsonElement anchor) {
        JsonArray array = array(arrayPath);
        if (array == null || containsElement(array, value)) return false;
        JsonArray rebuilt = new JsonArray();
        boolean inserted = false;
        for (JsonElement element : array) {
            rebuilt.add(element);
            if (!inserted && element.equals(anchor)) {
                rebuilt.add(value);
                inserted = true;
            }
        }
        if (!inserted) rebuilt.add(value);
        return replaceArray(arrayPath, rebuilt);
    }

    public boolean addAllAfter(String arrayPath, Iterable<JsonElement> values, JsonElement anchor) {
        boolean changed = false;
        for (JsonElement value : values) changed |= addAfter(arrayPath, value, anchor);
        return changed;
    }

    public boolean removeFromArray(String arrayPath, Predicate<JsonElement> match) {
        JsonArray array = array(arrayPath);
        if (array == null) return false;
        JsonArray rebuilt = new JsonArray();
        boolean changed = false;
        for (JsonElement element : array) {
            if (match.test(element)) {
                changed = true;
            } else {
                rebuilt.add(element);
            }
        }
        return changed && replaceArray(arrayPath, rebuilt);
    }

    public boolean replaceInArray(String arrayPath, Predicate<JsonElement> match, Function<JsonElement, JsonElement> fn) {
        JsonArray array = array(arrayPath);
        if (array == null) return false;
        JsonArray rebuilt = new JsonArray();
        boolean changed = false;
        for (JsonElement element : array) {
            if (!match.test(element)) {
                rebuilt.add(element);
                continue;
            }
            changed = true;
            JsonElement next = fn.apply(element);
            if (next != null) rebuilt.add(next);
        }
        return changed && replaceArray(arrayPath, rebuilt);
    }

    private boolean replaceArray(String arrayPath, JsonArray rebuilt) {
        String[] parts = split(arrayPath);
        JsonObject parent = objectAt(parts, parts.length - 1);
        if (parent == null) return false;
        parent.add(parts[parts.length - 1], rebuilt);
        return true;
    }

    private static boolean containsElement(JsonArray array, JsonElement value) {
        for (JsonElement element : array) {
            if (element.equals(value)) return true;
        }
        return false;
    }

    private static String[] split(String path) {
        return path.split("\\.");
    }

    private JsonObject objectAt(String[] parts, int length) {
        JsonObject current = root;
        for (int i = 0; i < length; i++) {
            JsonElement next = current.get(parts[i]);
            if (!(next instanceof JsonObject)) return null;
            current = (JsonObject) next;
        }
        return current;
    }

    private JsonObject ensureObjectAt(String[] parts, int length) {
        JsonObject probe = root;
        int i = 0;
        for (; i < length; i++) {
            JsonElement next = probe.get(parts[i]);
            if (next == null) break;
            if (!next.isJsonObject()) return null;
            probe = next.getAsJsonObject();
        }
        JsonObject current = probe;
        for (; i < length; i++) {
            JsonObject created = new JsonObject();
            current.add(parts[i], created);
            current = created;
        }
        return current;
    }
}
