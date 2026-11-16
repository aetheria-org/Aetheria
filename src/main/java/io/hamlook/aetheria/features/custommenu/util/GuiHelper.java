package io.hamlook.aetheria.features.custommenu.util;

import io.hamlook.aetheria.OptionsMenu;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.GuiScreenElementWrapper;
import io.hamlook.aetheria.features.custommenu.selector.CMMSelectorGUI;
import io.hamlook.aetheria.utils.compat.GuiScreenUtils;
import net.minecraft.client.gui.GuiScreen;

import java.util.HashMap;
import java.util.Map;

public class GuiHelper {

    private static final Map<String, GuiScreen> instanceCache = new HashMap<>();

    public static GuiScreen getMenu(String name, GuiScreen parentScreen) {
        if ("ASM Config".equals(name)) {
            return new GuiScreenElementWrapper(ATHRConfig.primedConfigEditor());
        }
        return instanceCache.computeIfAbsent(name, key -> {
            switch (key) {
                case "Singleplayer Menu":
                    return GuiScreenUtils.createSingleplayerScreen(parentScreen);
                case "Multiplayer Menu":
                    return GuiScreenUtils.createMultiplayerScreen(parentScreen);
                case "Options Menu":
                    return GuiScreenUtils.createOptionsScreen(parentScreen);
                case "Controls Menu":
                    return GuiScreenUtils.createControlsScreen(parentScreen);
                case "Video Settings":
                    return GuiScreenUtils.createVideoSettingsScreen(parentScreen);
                case "Language Menu":
                    return GuiScreenUtils.createLanguageScreen(parentScreen);
                case "Create World Menu":
                    // Side-effect open: modern has no public constructor, the
                    // factory opens the screen itself. Null signals already-opened.
                    GuiScreenUtils.openCreateWorldScreen(parentScreen);
                    return null;
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
