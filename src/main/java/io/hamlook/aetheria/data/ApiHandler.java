package io.hamlook.aetheria.data;

import com.google.gson.Gson;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.TesterWhitelist;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.data.NoPriceData;
import io.hamlook.aetheria.utils.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RegisterEvents
public class ApiHandler {

    private static final String CONFIG_URL = "https://raw.githubusercontent.com/aetheria-org/Aetheria/main/data/repo.json";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = new HttpClient();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ATHR-Analytics");
        t.setDaemon(true);
        return t;
    });

    private static String baseUrl = null;
    private static volatile Set<String> effectiveIds = null;
    private boolean noPriceFetched = false;

    private ApiHandler() {
    }

    public static String getBaseUrl() {
        if (baseUrl != null) return baseUrl;
        try {
            String target = resolveEndpoint();
            if (target != null) baseUrl = target;
        } catch (Exception ignored) {
        }
        return baseUrl;
    }

    public static Set<String> getNoPriceIds() {
        return effectiveIds;
    }

    public static void onServerJoin() {
        if (ATHRConfig.feature == null) return;
        if (!NetworkGuard.telemetryAllowed()) return;
        POOL.submit(ApiHandler::sendAnalytics);
    }

    private static void fetchNoPriceAsync() {
        POOL.submit(() -> {
            if (!NetworkGuard.apiAllowed()) {
                effectiveIds = Collections.emptySet();
                return;
            }
            try {
                String base = getBaseUrl();
                if (base == null) {
                    effectiveIds = Collections.emptySet();
                    return;
                }
                URL parsed = new URL(base);
                URL nopriceUrl = new URL(parsed.getProtocol(), parsed.getHost(), "/api/noprice");
                HttpURLConnection conn = (HttpURLConnection) nopriceUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("aetheria-auth", TesterWhitelist.getApiKey());
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    effectiveIds = GSON.fromJson(new InputStreamReader(conn.getInputStream()), NoPriceData.class)
                            .effectiveIds.stream().map(String::toLowerCase).collect(Collectors.toSet());
                } else {
                    effectiveIds = Collections.emptySet();
                }
            } catch (Exception e) {
                effectiveIds = Collections.emptySet();
            }
        });
    }

    private static void sendAnalytics() {
        try {
            String target = getBaseUrl();
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
        List<String> mods = NetworkGuard.modListInTelemetryAllowed() ? Loader.instance().getModList().stream().map(ModContainer::getModId).collect(Collectors.toList()) : null;
        return GSON.toJson(new Payload(username, mods, Aetheria.VERSION));
    }

    @SubscribeEvent
    public void onFirstTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (noPriceFetched) return;
        noPriceFetched = true;
        ApiHandler.fetchNoPriceAsync();
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

