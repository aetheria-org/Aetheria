package io.hamlook.aetheria.features.storage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.storage.utils.SContainer;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class StorageSaving {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getStorageFolder() {
        String username = Minecraft.getMinecraft().getSession().getUsername();
        String profile = SkyblockData.getCurrentProfile();
        if (profile.isEmpty()) profile = "_unknown";
        return new File(ATHRConfig.configDirectory, "profiles/" + profile + "/storage/" + username);
    }

    public static LinkedHashMap<String, SContainer> loadStorageData() {
        TreeMap<String, SContainer> sorted = new TreeMap<>((a, b) -> {
            String[] partsA = a.split("-", 2);
            String[] partsB = b.split("-", 2);
            int prefixCmp = partsB[0].compareTo(partsA[0]);
            if (prefixCmp != 0) return prefixCmp;
            try {
                return Integer.compare(Integer.parseInt(partsA[1]), Integer.parseInt(partsB[1]));
            } catch (NumberFormatException e) {
                return partsA[1].compareTo(partsB[1]);
            }
        });
        File folder = getStorageFolder();
        if (!folder.exists()) {
            String username = Minecraft.getMinecraft().getSession().getUsername();
            File oldFolder = new File(ATHRConfig.configDirectory, "storage/" + username);
            if (oldFolder.exists()) {
                folder.mkdirs();
                File[] oldFiles = oldFolder.listFiles();
                if (oldFiles != null) {
                    for (File f : oldFiles) {
                        if (f.isFile()) f.renameTo(new File(folder, f.getName()));
                    }
                }
            } else {
                folder.mkdirs();
                return new LinkedHashMap<>();
            }
        }

        File[] files = folder.listFiles();
        if (files == null) return new LinkedHashMap<>();

        for (File file : files) {
            if (!file.isFile() || file.length() == 0) continue;

            try (FileReader fileReader = new FileReader(file); JsonReader jsonReader = new JsonReader(fileReader)) {

                jsonReader.setLenient(true);
                SContainer container = GSON.fromJson(jsonReader, SContainer.class);

                if (container != null && !container.empty) {
                    sorted.put(container.id, container);
                }
            } catch (JsonSyntaxException e) {
                Aetheria.logger.info("Malformed JSON in " + file.getName() + ": " + e.getMessage());
            } catch (IOException | JsonIOException e) {
                Aetheria.logger.info("Failed to read file " + file.getName());
            }
        }
        return new LinkedHashMap<>(sorted);
    }

    public static void saveStorageData(Collection<SContainer> containers) {
        for (SContainer container : containers) {
            saveContainer(container);
        }
    }

    public static void saveContainer(SContainer container) {
        if (container.empty) return;
        File folder = getStorageFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, container.id + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(GSON.toJson(container));
        } catch (IOException e) {
            Aetheria.logger.info("ERROR While Saving " + container.id + " ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}