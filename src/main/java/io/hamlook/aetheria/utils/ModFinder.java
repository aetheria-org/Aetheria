package io.hamlook.aetheria.utils;

import net.minecraftforge.fml.common.Loader;

public class ModFinder {

    public static boolean isModPresent(String modID){
        return Loader.isModLoaded(modID);
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
