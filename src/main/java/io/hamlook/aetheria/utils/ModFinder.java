package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.utils.compat.ModCompat;

public class ModFinder {

    public static boolean isModPresent(String modID){
        return ModCompat.isModLoaded(modID);
    }

    public static boolean isLabyModPresent() {
        try {
            Class.forName("net.labymod.core.asm.LabyModCoreMod");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
