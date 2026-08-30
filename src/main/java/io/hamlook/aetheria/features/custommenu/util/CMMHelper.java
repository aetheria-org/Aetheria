package io.hamlook.aetheria.features.custommenu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.custommenu.CMMElementTypeAdapter;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.presets.DefaultCMMPreset;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.utils.ModFinder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CMMHelper {

    public static Map<String, CustomMMConfig> configList = new ConcurrentHashMap<>();
    public static String selectedConfig = "default";

    public static File CONFIG_FOLDER = new File(ATHRConfig.configDirectory, "cmmConfigs");
    public static Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(CMMElement.class, new CMMElementTypeAdapter())
            .create();


    public static boolean isModPreset(String presetID) {
        return "default".equals(presetID); // Add here for all default presets
    }

    public static void initialise() {
        if (!modCheckPasses()) {
            Aetheria.logger.warning("Essential Mod & Labymod is present, disabling Custom Main Menu.");
            return;
        } else {
            Aetheria.logger.info("Mod Check Passed Successfully, Custom Main Menu can be Used.");
        }
        loadConfigList();
        loadPresets();
        loadCMMConfig();
    }

    public static void save() {
        saveConfigList();
        saveCMMConfig();
    }

    public static void saveCMMConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("selected", selectedConfig);
        try {
            FileWriter writer = new FileWriter(new File(CONFIG_FOLDER, "config.json"));
            writer.write(GSON.toJson(config));
            writer.close();
        } catch (Exception e) {
            Aetheria.logger.warning("[CMM] Failed to Save CMM Config.");
        }
    }

    public static void loadCMMConfig() {
        if (configList.isEmpty()) return;
        File cmmConfig = new File(CONFIG_FOLDER, "config.json");
        if (!cmmConfig.exists()) return;
        try {
            JsonObject object = JsonParser.parseReader(new FileReader(cmmConfig)).getAsJsonObject();
            if (object.has("selected")) {
                String configName = object.get("selected").getAsString();
                if (configList.containsKey(configName)) {
                    selectedConfig = configName;
                } else {
                    selectedConfig = "default";
                }
            }
        } catch (Exception e) {
            Aetheria.logger.warning("Failed to load CMM config file: " + cmmConfig.getName());
        }

    }

    public static void loadPresets() {
        registerPreset(new DefaultCMMPreset());
    }

    public static void registerPreset(CustomMMConfig preset) {
        ensureEditorButton(preset);
        configList.put(preset.configName, preset);
    }

    private static void ensureEditorButton(CustomMMConfig preset) {
        if (preset == null || preset.elements == null) return;
        for (CMMElement element : preset.elements) {
            if (element instanceof GuiButton && ("Custom Main Menu Editor".equals(((GuiButton) element).screen) || "CMM Editor".equals(((GuiButton) element).screen))) return;
        }
        preset.addElement(new GuiButton(new Position("CENTER", "CENTER", -100, -75), 200, 20, "Menu Editor", "Custom Main Menu Editor"));
    }

    public static boolean createPreset(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String id = name.trim().replaceAll("[^a-zA-Z0-9_\\- ]", "");
        if (id.isEmpty() || configList.containsKey(id)) return false;
        CustomMMConfig copy = GSON.fromJson(GSON.toJson(configList.get("default")), CustomMMConfig.class);
        if (copy == null) copy = new CustomMMConfig(id);
        copy.configName = id;
        registerPreset(copy);
        selectedConfig = id;
        save();
        return true;
    }

    public static void selectPreset(String name) {
        if (name != null && configList.containsKey(name)) {
            selectedConfig = name;
            saveCMMConfig();
        }
    }

    public static boolean deletePreset(String name) {
        if (name == null || isModPreset(name) || !configList.containsKey(name)) return false;
        configList.remove(name);
        File file = new File(CONFIG_FOLDER, name + ".cmm");
        if (file.exists() && !file.delete()) Aetheria.logger.warning("[CMM] Failed to delete preset: " + name);
        selectedConfig = "default";
        saveCMMConfig();
        return true;
    }

    public static void savePreset(CustomMMConfig preset) {
        if (preset == null || isModPreset(preset.configName)) {
            saveCMMConfig();
            return;
        }
        if (!CONFIG_FOLDER.exists()) CONFIG_FOLDER.mkdirs();
        File file = new File(CONFIG_FOLDER, preset.configName + ".cmm");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(GSON.toJson(preset));
        } catch (Exception e) {
            Aetheria.logger.warning("[CMM] Failed to Save CMM Preset: " + preset.configName);
        }
        saveCMMConfig();
    }

    public static void saveConfigList() {
        if (!CONFIG_FOLDER.exists()) CONFIG_FOLDER.mkdir();
        for (CustomMMConfig preset : configList.values()) {
            if (isModPreset(preset.configName)) continue;
            savePreset(preset);
        }
    }

    public static void loadConfigList() {
        if (!CONFIG_FOLDER.exists()) {
            CONFIG_FOLDER.mkdirs();
            return;
        }
        if (!CONFIG_FOLDER.isDirectory() || CONFIG_FOLDER.list() == null || Objects.requireNonNull(CONFIG_FOLDER.list()).length == 0)
            return;
        File[] files = Objects.requireNonNull(CONFIG_FOLDER.listFiles());
        List<File> filtered = Arrays.stream(files).filter(file -> file.getName().endsWith(".cmm")).collect(Collectors.toList());
        for (File file : filtered) {
            try {
                CustomMMConfig config = GSON.fromJson(new FileReader(file), CustomMMConfig.class);
                if (config == null) return;
                registerPreset(config);
            } catch (Exception e) {
                Aetheria.logger.warning("Failed to load config file: " + file.getName());
            }
        }
    }

    public static boolean isEnabled() {
        return ATHRConfig.feature.cosmetics.customMenu.enabled && modCheckPasses();
    }

    public static boolean modCheckPasses() {
        return !ModFinder.isModPresent("essential") && !ModFinder.isLabyModPresent();
    }

    public static CustomMMConfig getCMMConfig() {
        return configList.get(selectedConfig);
    }
}
