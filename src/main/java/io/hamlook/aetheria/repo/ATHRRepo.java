package io.hamlook.aetheria.repo;

public class ATHRRepo {

    public static final String KEY_UPDATE = "ASMVersion";
    public static final String KEY_PLAYERSIZES = "playersizes";
    public static final String KEY_ENCHANTS = "enchants";
    public static final String KEY_TIMERS = "timers";
    public static final String KEY_TAGS = "tags";
    public static final String KEY_REPO = "repo";
    public static final String KEY_OTHER = "other";
    public static final String BASE = "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/main/";

    private ATHRRepo() {
    }

    public static void init() {
        RepoHandler.register(KEY_UPDATE, BASE + "data/ASMVersion.json");
        RepoHandler.register(KEY_PLAYERSIZES, BASE + "data/playersizes.json");
        RepoHandler.register(KEY_ENCHANTS, BASE + "data/enchants.json");
        RepoHandler.register(KEY_TIMERS, BASE + "data/timers.json");
        RepoHandler.register(KEY_TAGS, BASE + "data/tags.json");
        RepoHandler.register(KEY_REPO, BASE + "data/repo.json");
        RepoHandler.register(KEY_OTHER, BASE + "data/other.json");
        RepoHandler.warmupAll();
    }
}
