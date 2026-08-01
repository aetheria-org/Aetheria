package io.hamlook.aetheria.utils.data;

import java.io.File;

public final class DataPaths {

    private DataPaths() {
    }

    public static File baseDir(File configDir, String envKey, String profile) {
        String ign = SkyblockData.getIgn();
        if (ign.isEmpty()) ign = "_unknown";
        return new File(new File(new File(configDir, "data"), ign), envKey + "/" + profile);
    }

    public static File profileFile(File configDir, String envKey, String profile, String fileName) {
        return new File(baseDir(configDir, envKey, profile), fileName);
    }

    public static File storageDir(File configDir, String envKey, String profile) {
        return new File(baseDir(configDir, envKey, profile), "storage");
    }

    public static void migrate(File target, File... candidates) {
        if (target.exists()) return;
        for (File candidate : candidates) {
            if (candidate.exists()) {
                target.getParentFile().mkdirs();
                candidate.renameTo(target);
                break;
            }
        }
    }
}
