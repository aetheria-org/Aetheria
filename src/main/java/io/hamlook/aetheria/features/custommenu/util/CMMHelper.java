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
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
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
    public static final List<String> incompatiblePresets = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static String selectedConfig = "Default";

    public static File CONFIG_FOLDER = new File(ATHRConfig.configDirectory, "cmmConfigs");
    public static Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(CMMElement.class, new CMMElementTypeAdapter())
            .create();


    public static boolean isModPreset(String presetID) {
        return "default".equalsIgnoreCase(presetID); // Add here for all default presets
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
                    selectedConfig = configList.containsKey("Default") ? "Default" : configList.keySet().iterator().next();
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
        if (preset == null) return;
        migrate(preset);
        ensureEditorButton(preset);
        configList.put(preset.configName, preset);
    }

    private static void ensureEditorButton(CustomMMConfig preset) {
        if (preset == null || preset.elements == null) return;
        for (CMMElement element : preset.elements) {
            if (element instanceof GuiButton && ("Custom Main Menu Editor".equals(((GuiButton) element).screen) || "CMM Editor".equals(((GuiButton) element).screen))) return;
        }
        preset.addElement(new GuiButton(new Position("CENTER", "CENTER", -100, -75), 200, 20, "Menu Editor", "CMM Editor"));
    }

    public static boolean createPreset(String name) {
        return createPreset(name, "Default");
    }

    public static boolean importPreset(CustomMMConfig imported) {
        if (imported == null) return false;
        String base = imported.configName == null || imported.configName.trim().isEmpty() ? "Imported Menu" : imported.configName.trim();
        String name = base; int suffix = 2;
        while (configList.containsKey(name)) name = base + " " + suffix++;
        imported.configName = name; registerPreset(imported); selectedConfig = name; save(); return true;
    }

    private static void migrate(CustomMMConfig preset) {
        if (preset.elements == null) preset.elements = new java.util.ArrayList<>();
        if (preset.formatVersion > CustomMMConfig.CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported future CMM preset version: " + preset.formatVersion);
        }
        if (preset.formatVersion <= 0) preset.formatVersion = 1;
        // Version 2 introduced shared element presentation fields; Gson already supplies their defaults.
        preset.formatVersion = CustomMMConfig.CURRENT_FORMAT_VERSION;
    }

    public static boolean createPreset(String name, String basePresetName) {
        if (name == null || name.trim().isEmpty()) return false;
        String id = name.trim().replaceAll("[^a-zA-Z0-9_\\- ]", "");
        if (id.isEmpty() || configList.containsKey(id)) return false;
        CustomMMConfig base = configList.get(basePresetName);
        if (base == null) base = configList.get("Default");
        CustomMMConfig copy = null;
        if (base != null) {
            try { copy = GSON.fromJson(GSON.toJson(base), CustomMMConfig.class); }
            catch (RuntimeException ex) { Aetheria.logger.warning("[CMM] Base preset was invalid; creating a clean preset instead."); }
        }
        if (copy == null && "Default".equalsIgnoreCase(basePresetName)) {
            try { copy = GSON.fromJson(GSON.toJson(new DefaultCMMPreset()), CustomMMConfig.class); }
            catch (RuntimeException ignored) { }
        }
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
        File file = new File(CONFIG_FOLDER, name + ".menu");
        if (file.exists() && !file.delete()) Aetheria.logger.warning("[CMM] Failed to delete preset: " + name);
        cleanupUnusedAssets();
        selectedConfig = configList.containsKey("Default") ? "Default" : configList.keySet().iterator().next();
        saveCMMConfig();
        return true;
    }

    public static void cleanupUnusedAssets() {
        File assets = new File(CONFIG_FOLDER, "assets");
        File[] files = assets.listFiles();
        if (files == null) return;
        java.util.Set<String> used = new java.util.HashSet<>();
        for (CustomMMConfig preset : configList.values()) {
            if (preset.background != null && preset.background.url != null) addAssetReference(used, preset.background.url);
            if (preset.elements != null) for (CMMElement element : preset.elements) {
                if (element instanceof Sprite) {
                    Sprite sprite = (Sprite) element;
                    if (sprite.image != null && sprite.image.url != null) addAssetReference(used, sprite.image.url);
                }
            }
        }
        for (File file : files) if (file.isFile() && !used.contains(assetPath(file))) file.delete();
    }

    private static void addAssetReference(java.util.Set<String> used, String path) {
        used.add(assetPath(new File(path)));
    }

    private static String assetPath(File file) {
        try { return file.getCanonicalPath(); } catch (Exception ignored) { return file.getAbsolutePath(); }
    }

    public static void savePreset(CustomMMConfig preset) {
        if (preset == null || isModPreset(preset.configName)) {
            saveCMMConfig();
            return;
        }
        if (!CONFIG_FOLDER.exists()) CONFIG_FOLDER.mkdirs();
        File file = new File(CONFIG_FOLDER, preset.configName + ".menu");
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
        cleanupUnusedAssets();
    }

    public static void loadConfigList() {
        incompatiblePresets.clear();
        if (!CONFIG_FOLDER.exists()) {
            CONFIG_FOLDER.mkdirs();
            return;
        }
        if (!CONFIG_FOLDER.isDirectory() || CONFIG_FOLDER.list() == null || Objects.requireNonNull(CONFIG_FOLDER.list()).length == 0)
            return;
        File[] files = Objects.requireNonNull(CONFIG_FOLDER.listFiles());
        List<File> filtered = Arrays.stream(files).filter(file -> file.getName().endsWith(".menu")).collect(Collectors.toList());
        for (File file : filtered) {
            try {
                CustomMMConfig config = GSON.fromJson(new FileReader(file), CustomMMConfig.class);
                if (config == null) continue;
                registerPreset(config);
            } catch (Exception e) {
                Aetheria.logger.warning("Failed to load config file: " + file.getName());
                if (e.getMessage() != null && e.getMessage().contains("future CMM preset version")) incompatiblePresets.add(file.getName());
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

    /** Clones using the concrete runtime class so class-specific fields are retained. */
    public static CMMElement copyElement(CMMElement element) {
        if (element == null) return null;
        return GSON.fromJson(GSON.toJson(element, element.getClass()), element.getClass());
    }
}
