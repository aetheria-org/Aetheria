package io.hamlook.aetheria.features.custommenu.util;

import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.GuiScreenElementWrapper;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigEditor;
import io.hamlook.aetheria.features.custommenu.selector.CMMSelectorGUI;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.gui.*;

import java.util.HashMap;
import java.util.Map;

public class GuiHelper {

    private static final Map<String, GuiScreen> instanceCache = new HashMap<>();

    public static GuiScreen getMenu(String name, GuiScreen parentScreen) {
        return instanceCache.computeIfAbsent(name, key -> {
            switch (key) {
                case "Singleplayer Menu":
                    return new GuiSelectWorld(parentScreen);
                case "Multiplayer Menu":
                    return new GuiMultiplayer(parentScreen);
                case "Options Menu":
                    return new GuiOptions(parentScreen, MinecraftCompat.getMinecraft().gameSettings);
                case "Controls Menu":
                    return new GuiControls(parentScreen, MinecraftCompat.getMinecraft().gameSettings);
                case "Video Settings":
                    return new GuiVideoSettings(parentScreen, MinecraftCompat.getMinecraft().gameSettings);
                case "Language Menu":
                    return new GuiLanguage(parentScreen, MinecraftCompat.getMinecraft().gameSettings, MinecraftCompat.getMinecraft().getLanguageManager());
                case "Create World Menu":
                    return new GuiCreateWorld(parentScreen);
                case "ASM Config":
                    return new GuiScreenElementWrapper(new ConfigEditor(ATHRConfig.feature));
                case "ASM Options Menu":
                    return new OptionsMenu();
                case "CMM Editor":
                case "CMM Editor Menu":
                case "CMM Selector":
                    return new CMMSelectorGUI();
                default:
                    return null;
            }
        });
    }

}
