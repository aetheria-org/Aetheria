package io.hamlook.aetheria.core;

import com.google.gson.Gson;

/**
 * Single source of truth for every Gson instance in ATHR.
 * Mirrors SkyHanni's BaseGsonBuilder.
 * Previously every storage class had its own:
 *   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
 * Now they all reference ATHRGsonBuilder.GSON. To add a new type adapter for
 * the whole mod, add it to build() once — every storage benefits automatically.
 */
public final class GsonBuilder {

    /**
     * Shared Gson for all storage classes.
     * Pretty-printed, lenient (unknown fields are silently skipped).
     */
    public static final Gson GSON = build().create();

    /**
     * Strict variant used by ATHRConfig / Config.
     * Strips fields that don't have @Expose, matching the existing
     * ConfigProcessor behaviour.
     */
    public static final Gson GSON_STRICT = buildStrict().create();

    private GsonBuilder() {
    }

    /**
     * Returns a pre-configured GsonBuilder.
     * Use this when you need a TypeToken for generics, e.g.:
     *   Type t = new TypeToken<Map<String, Foo>>(){}.getType();
     *   Map<String, Foo> data = ATHRGsonBuilder.build().create().fromJson(reader, t);
     */
    public static com.google.gson.GsonBuilder build() {
        return new com.google.gson.GsonBuilder().setPrettyPrinting();
        // Add shared type adapters here when needed, e.g.:
        // .registerTypeAdapter(Position.class, new PositionAdapter())
    }

    public static com.google.gson.GsonBuilder buildStrict() {
        return build().excludeFieldsWithoutExposeAnnotation();
    }
}
