package io.hamlook.aetheria.features.profile;

import io.hamlook.aetheria.features.profile.impl.skills.SkillModule;

import java.util.ArrayList;
import java.util.List;

public class ModulesManager {

    public static List<ProfileModule> modules = new ArrayList<>();
    public static boolean initialised = false;

    public static void init(){
        if(initialised) return;
        // Register Here
        modules.add(new SkillModule());
        initialised = true;
    }

}
