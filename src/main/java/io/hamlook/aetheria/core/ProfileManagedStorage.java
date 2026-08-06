package io.hamlook.aetheria.core;

import io.hamlook.aetheria.utils.data.DataPaths;
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

    public abstract void save();

    protected File resolveFile() {
        String profile = SkyblockData.getCurrentProfile();
        if (profile.isEmpty()) return null;
        DataPaths.migrate(DataPaths.profileFile(configDir, "normal", profile, fileName), new File(configDir, "profiles/" + profile + "/" + fileName), new File(configDir, fileName));
        return DataPaths.profileFile(configDir, SkyblockData.getEnvironmentKey(), profile, fileName);
    }
}
