package io.hamlook.aetheria.features.profile;

import java.util.HashMap;

public class PlayerProfile {

    public HashMap<String,ProfileModule> modules;
    public String playerName;

    public PlayerProfile(String playerName) {
        this.playerName = playerName;
        this.modules = new HashMap<>();
    }

    public void addModule(ProfileModule module) {
        this.modules.put(module.moduleID, module);
    }

}
