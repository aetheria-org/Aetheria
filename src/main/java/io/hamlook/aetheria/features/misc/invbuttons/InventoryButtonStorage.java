package io.hamlook.aetheria.features.misc.invbuttons;

import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.core.GsonBuilder;
import io.hamlook.aetheria.core.StorageManager;
import lombok.Getter;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class InventoryButtonStorage implements StorageManager.Managed, StorageManager.AutoSaveable {

    private static final InventoryButtonStorage INSTANCE = new InventoryButtonStorage();
    private static final Type TYPE = new TypeToken<List<InventoryButton>>() {}.getType();

    private File storageFile;
    @Getter
    private List<InventoryButton> buttons = new ArrayList<>();

    private InventoryButtonStorage() {
    }

    public static InventoryButtonStorage getInstance() {
        return INSTANCE;
    }

    private static List<InventoryButton> createDefaults() {
        List<InventoryButton> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) list.add(new InventoryButton(87 + 21 * i, 63, true, false, false, 0, "", ""));
        for (int i = 0; i < 4; i++) list.add(new InventoryButton(87 + 21 * i, 5, true, false, false, 0, "", ""));
        list.add(new InventoryButton(87, 25, true, false, false, 0, "", ""));
        list.add(new InventoryButton(87 + 18, 25, true, false, false, 0, "", ""));
        list.add(new InventoryButton(87, 25 + 18, true, false, false, 0, "", ""));
        list.add(new InventoryButton(87 + 18, 25 + 18, true, false, false, 0, "", ""));
        list.add(new InventoryButton(143, 35, true, false, false, 0, "", ""));
        list.add(new InventoryButton(60, 8, true, false, false, 0, "", ""));
        list.add(new InventoryButton(60, 60, true, false, false, 0, "", ""));
        list.add(new InventoryButton(26, 8, true, false, false, 0, "", ""));
        list.add(new InventoryButton(26, 60, true, false, false, 0, "", ""));
        return list;
    }

    @Override
    public void initFile(File configDir) {
        this.storageFile = new File(configDir, "invbuttons.json");
    }

    @Override
    public void load() {
        if (storageFile == null || !storageFile.exists()) {
            buttons = createDefaults();
            save();
            return;
        }
        List<InventoryButton> loaded = StorageManager.loadSafe(storageFile, TYPE, GsonBuilder.GSON_STRICT);
        buttons = loaded != null ? loaded : createDefaults();
    }

    public void save() {
        StorageManager.saveAtomic(storageFile, buttons, GsonBuilder.GSON_STRICT);
    }

    @Override
    public void autoSave() {
        save();
    }

    public void setButtons(List<InventoryButton> b) {
        this.buttons = b;
        save();
    }
}