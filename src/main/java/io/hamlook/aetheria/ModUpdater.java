package io.hamlook.aetheria;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Deprecated
public class ModUpdater {


    public static void updateAndRestart(boolean shutdown) {
        ThreadUtils.run(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/aetheria-org/Aetheria/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);

                if (conn.getResponseCode() != 200) return;

                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                reader.close();

                String latestVersion = response.get("tag_name").getAsString();

                if (Aetheria.VERSION.replace("v", "").equalsIgnoreCase(latestVersion.replace("v", ""))) {
                    System.out.println("Already on Latest Version");
                    return;
                }

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

                File modsDir = new File(MinecraftCompat.getMinecraft().mcDataDir, "mods");
                File newModFile = new File(modsDir, newFileName);

                URL downloadURL = new URL(downloadUrl);
                HttpURLConnection downloadConn = (HttpURLConnection) downloadURL.openConnection();
                downloadConn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);

                try (InputStream in = downloadConn.getInputStream();
                     FileOutputStream out = new FileOutputStream(newModFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                ModContainer myMod = Loader.instance().getIndexedModList().get(Aetheria.MODID);
                if (myMod != null) {
                    File oldJar = myMod.getSource();
                    if (oldJar != null && oldJar.exists() && !oldJar.getName().equals(newFileName)) {
                        oldJar.deleteOnExit();
                    }
                }

                if(shutdown) {
                    MinecraftCompat.getMinecraft().shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
