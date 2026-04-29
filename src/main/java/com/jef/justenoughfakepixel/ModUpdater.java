package com.jef.justenoughfakepixel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ModUpdater {


    public static void updateAndRestart(boolean shutdown) {
        new Thread(() -> {
            try {
                // 1. Target the specific GitHub API for JustEnoughFakepixel
                URL url = new URL("https://api.github.com/repos/JustEnoughFakepixel/JustEnoughFakepixel/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "JustEnoughFakepixel-Updater");

                if (conn.getResponseCode() != 200) return;

                // 2. Parse the response
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
                reader.close();

                String latestVersion = response.get("tag_name").getAsString();

                if (JefMod.VERSION.replace("v", "").equalsIgnoreCase(latestVersion.replace("v", ""))) {
                    System.out.println("Already on Latest Version");
                    return;
                }

                // 4. Locate the JAR asset
                JsonArray assets = response.getAsJsonArray("assets");
                if (assets.size() == 0) return;
                JsonObject jarAsset = null;
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    if (asset.get("name").getAsString().endsWith(".jar")) {
                        jarAsset = asset;
                        break;
                    }
                }

                if (jarAsset == null) return;

                String downloadUrl = jarAsset.get("browser_download_url").getAsString();
                String newFileName = jarAsset.get("name").getAsString();

                // 5. Download the file
                File modsDir = new File(Minecraft.getMinecraft().mcDataDir, "mods");
                File newModFile = new File(modsDir, newFileName);

                URL downloadURL = new URL(downloadUrl);
                HttpURLConnection downloadConn = (HttpURLConnection) downloadURL.openConnection();
                downloadConn.setRequestProperty("User-Agent", "JustEnoughFakepixel-Updater");

                try (InputStream in = downloadConn.getInputStream();
                     FileOutputStream out = new FileOutputStream(newModFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // 6. Handle old file cleanup
                ModContainer myMod = Loader.instance().getIndexedModList().get(JefMod.MODID);
                if (myMod != null) {
                    File oldJar = myMod.getSource();
                    if (oldJar != null && oldJar.exists() && !oldJar.getName().equals(newFileName)) {
                        oldJar.deleteOnExit();
                    }
                }

                if(shutdown) {
                    // 7. Shutdown if needed
                    Minecraft.getMinecraft().shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
