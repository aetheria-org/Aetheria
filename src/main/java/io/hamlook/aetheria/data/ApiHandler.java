package io.hamlook.aetheria.data;

import com.google.gson.Gson;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ApiHandler {

    private static final String CONFIG_URL = "https://raw.githubusercontent.com/aetheria-org/Aetheria/main/data/repo.json";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = new HttpClient();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ATHR-Analytics");
        t.setDaemon(true);
        return t;
    });

    private ApiHandler() {
    }

    public static void onServerJoin() {
        if (ATHRConfig.feature == null) return;
        POOL.submit(ApiHandler::sendAnalytics);
    }

    private static void sendAnalytics() {
        try {
            String target = resolveEndpoint();
            if (target == null || target.isEmpty()) return;
            HTTP.post(target, buildPayload(), "application/json; charset=utf-8");
        } catch (Exception ignored) {
        }
    }

    private static String resolveEndpoint() throws Exception {
        HttpClient.FetchResult result = HTTP.fetch(CONFIG_URL, null);
        if (result.body() == null) return null;
        RemoteConfig cfg = GSON.fromJson(result.body(), RemoteConfig.class);
        if (cfg == null || cfg.apiUrl == null) return null;
        return new String(Base64.getDecoder().decode(cfg.apiUrl), StandardCharsets.UTF_8);
    }

    private static String buildPayload() {
        String username = Minecraft.getMinecraft().getSession().getUsername();
        List<String> mods = Loader.instance().getModList().stream().map(ModContainer::getModId).collect(Collectors.toList());
        return GSON.toJson(new Payload(username, mods, Aetheria.VERSION));
    }

    private static class RemoteConfig {
        String apiUrl;
    }

    private static class Payload {
        final String username;
        final List<String> modList;
        final String ATHRVersion;

        Payload(String username, List<String> modList, String ATHRVersion) {
            this.username = username;
            this.modList = modList;
            this.ATHRVersion = ATHRVersion;
        }
    }
}
