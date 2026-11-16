package io.hamlook.aetheria.repo;

import io.hamlook.aetheria.repo.data.RepoData;

public final class RepoLinks {

    private static final RepoData DEFAULTS = new RepoData();

    private RepoLinks() {
    }

    public static String site9() {
        return usable(data().site9);
    }

    public static String discord() {
        return orDefault(usable(data().discord), DEFAULTS.discord);
    }

    public static String skyatlas() {
        return orDefault(usable(data().skyatlas), DEFAULTS.skyatlas);
    }

    public static String github() {
        return orDefault(usable(data().github), DEFAULTS.github);
    }

    public static String modrinth() {
        return orDefault(usable(data().modrinth), DEFAULTS.modrinth);
    }

    public static String website() {
        return orDefault(usable(data().website), DEFAULTS.website);
    }

    private static RepoData data() {
        return RepoHandler.get(ATHRRepo.KEY_REPO, RepoData.class, DEFAULTS);
    }

    private static String orDefault(String repoValue, String fallback) {
        return repoValue != null ? repoValue : fallback;
    }

    private static String usable(String raw) {
        if (raw == null) return null;
        String url = raw.trim();
        boolean web = url.regionMatches(true, 0, "http://", 0, 7)
                || url.regionMatches(true, 0, "https://", 0, 8);
        return web ? url : null;
    }
}
