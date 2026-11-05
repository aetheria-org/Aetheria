package io.hamlook.aetheria.features.dungeons.rooms.secret;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum SecretCategory {
    ENTRANCE("entrance", false),
    CHEST("chest", true),
    ITEM("item", true),
    BAT("bat", true),
    LEVER("lever", true),
    SUPERBOOM("superboom", false),
    WITHER("wither", true),
    STONK("stonk", false),
    PUZZLE("puzzle", false),
    FAIRYSOUL("fairysoul", false),
    UNKNOWN("unknown", false);

    private static final Map<String, SecretCategory> BY_ID = new HashMap<>();

    static {
        for (SecretCategory category : values()) BY_ID.put(category.id, category);
    }

    private final String id;
    private final boolean secret;

    SecretCategory(String id, boolean secret) {
        this.id = id;
        this.secret = secret;
    }

    public String id() {
        return id;
    }

    public boolean isSecret() {
        return secret;
    }

    public static SecretCategory of(String raw) {
        if (raw == null) return UNKNOWN;
        SecretCategory category = BY_ID.get(raw.toLowerCase(Locale.ROOT));
        return category == null ? UNKNOWN : category;
    }
}
