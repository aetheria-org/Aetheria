package io.hamlook.aetheria.features.price;

import com.google.gson.Gson;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.price.vars.PriceData;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PriceMap {

    private static final Gson gson = new Gson();
    @Getter
    private static final PriceData priceData = new PriceData();

    public static void fetch() {
        if (ATHRConfig.feature != null && !NetworkGuard.apiAllowed()) return;
        new Thread(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("price"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);

                        PriceData fetched = gson.fromJson(sb.toString(), PriceData.class);
                        if (fetched != null) {
                            synchronized (priceData) {
                                priceData.bazaar.clear();
                                priceData.auction.clear();
                                if (fetched.bazaar != null) priceData.bazaar.putAll(fetched.bazaar);
                                if (fetched.auction != null) priceData.auction.putAll(fetched.auction);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Aetheria.logger.info("[PriceDetector] Failed to fetch prices: " + e.getMessage());
            }
        }).start();
    }

}