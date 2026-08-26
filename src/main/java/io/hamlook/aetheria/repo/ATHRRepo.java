package io.hamlook.aetheria.repo;

import io.hamlook.aetheria.core.ATHRConfig;

import java.io.File;

public class ATHRRepo {

    public static final String KEY_UPDATE = "ASMVersion";
    public static final String KEY_PLAYERSIZES = "playersizes";
    public static final String KEY_ENCHANTS = "enchants";
    public static final String KEY_TIMERS = "timers";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_REPO = "repo";
    public static final String KEY_OTHER = "other";
    public static final String KEY_DUNGEONROOMS = "dungeonrooms";
    public static final String KEY_SECRETLOCATIONS = "secretlocations";
    public static final String KEY_WATERSOLUTIONS = "waterSolutions";
    public static final String KEY_INV_PRESETS = "presets";
    public static final String KEY_INV_EXTRA_ICONS = "extraicons";
    public static final String KEY_EMOJIS = "emojis";
    public static final String KEY_EMOJI_DATA = "emojidata";
    public static final String KEY_CAPES = "capes";
    public static final String KEY_ITEMDATA = "itemdata";
    public static final String BASE = "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/main/";

    private ATHRRepo() {
    }

    public static void init() {
        RepoHandler.registerAlwaysFetch(KEY_UPDATE, BASE + "data/ASMVersion.json");
        RepoHandler.registerAlwaysFetch(KEY_PLAYERSIZES, BASE + "data/playersizes.json");
        RepoHandler.registerEtagFetch(KEY_ENCHANTS, BASE + "data/enchants.json");
        RepoHandler.registerAlwaysFetch(KEY_TIMERS, BASE + "data/timers.json");
        RepoHandler.registerAlwaysFetch(KEY_TAGS, BASE + "data/tags.json");
        RepoHandler.registerAlwaysFetch(KEY_REPO, BASE + "data/repo.json");
        RepoHandler.registerAlwaysFetch(KEY_OTHER, BASE + "data/other.json");
        RepoHandler.registerEtagFetch(KEY_DUNGEONROOMS, BASE + "data/dungeonrooms.json");
        RepoHandler.registerEtagFetch(KEY_SECRETLOCATIONS, BASE + "data/secretlocations.json");
        RepoHandler.registerParallel(KEY_WATERSOLUTIONS, BASE + "data/waterSolutions.json");
        RepoHandler.registerEtagFetch(KEY_INV_PRESETS, BASE + "data/presets.json");
        RepoHandler.registerEtagFetch(KEY_INV_EXTRA_ICONS, BASE + "data/extraicons.json");
        RepoHandler.registerVersionOnly(KEY_EMOJIS);
        RepoHandler.registerEtagFetch(KEY_EMOJI_DATA, BASE + "emojis/emoji.json");
        RepoHandler.registerVersionOnly(KEY_CAPES);
        RepoHandler.registerEtagFetch(KEY_ITEMDATA, BASE + "itemData/itemData.json",
                new File(ATHRConfig.configDirectory, "itemData.json"),
                new File(ATHRConfig.configDirectory, "itemData_version.txt"));
        RepoHandler.warmupAll();
    }
}
