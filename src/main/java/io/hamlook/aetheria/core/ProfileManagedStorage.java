package io.hamlook.aetheria.core;

import io.hamlook.aetheria.utils.data.SkyblockData;

import java.io.File;

public abstract class ProfileManagedStorage implements StorageManager.Managed {
    private final String fileName;
    protected File configDir;

    protected ProfileManagedStorage(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void initFile(File configDir) {
        this.configDir = configDir;
    }

    protected File resolveFile() {
        String profile = SkyblockData.getCurrentProfile();
        if (profile.isEmpty()) return null;
        File target = new File(new File(configDir, "profiles/" + profile), fileName);
        if (!target.exists()) {
            File legacy = new File(configDir, fileName);
            if (legacy.exists()) {
                target.getParentFile().mkdirs();
                legacy.renameTo(target);
            }
        }
        return target;
    }
}
