package com.jef.justenoughfakepixel.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Single source of truth for every Gson instance in JEF.
 * Mirrors SkyHanni's BaseGsonBuilder.
 *
 * Previously every storage class had its own:
 *   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
 *
 * Now they all reference JefGsonBuilder.GSON. To add a new type adapter for
 * the whole mod, add it to build() once — every storage benefits automatically.
 */
public final class JefGsonBuilder {

    /**
     * Shared Gson for all storage classes.
     * Pretty-printed, lenient (unknown fields are silently skipped).
     */
    public static final Gson GSON = build().create();

    /**
     * Strict variant used by JefConfig / Config.
     * Strips fields that don't have @Expose, matching the existing
     * ConfigProcessor behaviour.
     */
    public static final Gson GSON_STRICT = buildStrict().create();

    private JefGsonBuilder() {}

    /**
     * Returns a pre-configured GsonBuilder.
     * Use this when you need a TypeToken for generics, e.g.:
     *   Type t = new TypeToken<Map<String, Foo>>(){}.getType();
     *   Map<String, Foo> data = JefGsonBuilder.build().create().fromJson(reader, t);
     */
    public static GsonBuilder build() {
        return new GsonBuilder()
                .setPrettyPrinting();
                // Add shared type adapters here when needed, e.g.:
                // .registerTypeAdapter(Position.class, new PositionAdapter())
    }

    public static GsonBuilder buildStrict() {
        return build().excludeFieldsWithoutExposeAnnotation();
    }
}
