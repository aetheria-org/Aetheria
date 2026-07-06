package io.hamlook.aetheria.features.price;

import com.google.gson.Gson;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.price.vars.recieve.PriceEntry;
import io.hamlook.aetheria.features.price.vars.recieve.PriceReceiveData;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import lombok.Getter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PriceMap {

    private static final Gson gson = new Gson();
    @Getter
    private static final PriceReceiveData newPriceData = new PriceReceiveData();

    public static int fetchFailCount = 0;
    public static final int MAX_RETRIES = 5;


    public static PriceEntry getPrice(String id){
        return newPriceData.get(id);
    }

    public static void fetch() {
        if (ATHRConfig.feature != null && !NetworkGuard.apiAllowed()) return;
        new Thread(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("price"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
                conn.setConnectTimeout(35000);
                conn.setReadTimeout(35000);

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode <= 210) {
                    Aetheria.logger.info("[PriceDetector] Loaded entries items from DB");
                    fetchFailCount = 0;
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        PriceReceiveData fetchedData = gson.fromJson(sb.toString(), PriceReceiveData.class);
                        if(fetchedData != null) {
                            Aetheria.logger.info("[PriceDetector] Loaded entries items from new DB aren't null");
                            writeToJson(fetchedData,"fetchedData.json");
                            writeToJson(sb.toString(),"fetchedDataString.txt");
                            synchronized (newPriceData) {
                                newPriceData.clear();
                                newPriceData.putAll(fetchedData);
                                Aetheria.logger.info("[PriceDetector] Loaded " + newPriceData.size() + " entries from DB.");
                            }
                        }
                    }
                } else {
                    Aetheria.logger.info("[PriceDetector] Failed to load entries items from DB | " + responseCode);
                    fetchFailCount++;
                }
            } catch (Exception e) {
                Aetheria.logger.info("[PriceDetector] Failed to fetch prices: " + e.getMessage());
                Aetheria.logger.info(Arrays.toString(e.getStackTrace()));
                fetchFailCount++;
            }
        }).start();
    }

    private static void writeToJson(Object obj,String fileName) {
        File file = new File(ATHRConfig.configDirectory, fileName);
        if (!file.exists()) {
            try { file.createNewFile(); }
            catch (IOException e) { Aetheria.logger.info("Error creating " + fileName); return; }
        }
        if(obj instanceof String){
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(((String)obj).getBytes());
            } catch (Exception e) {
                Aetheria.logger.info("Error writing to profile.bin");
            }
        }else {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(new Gson().toJson(obj).getBytes());
            } catch (Exception e) {
                Aetheria.logger.info("Error writing to profile.bin");
            }
        }
    }


    public static class Cached {

        private static final Map<String, Double> HARDCODED_PRICES = new HashMap<>();
        private static final Map<String, CachedValue> CACHE = new HashMap<>();
        private static final long TTL_MS = 30_000L;
        private static final long NOT_FOUND_TTL_MS = 300_000L;

        static {
            HARDCODED_PRICES.put("GHOSTLY_BOOTS", 77_000.0);
            HARDCODED_PRICES.put("BAG_OF_CASH", 1_000_000.0);
            HARDCODED_PRICES.put("CROWN_OF_GREED", 1_000_000.0);
        }

        public static PriceEntry getPrice(String id){
            return getOrCache(id,() -> PriceMap.getPrice(id));
        }

        public static void invalidate() {
            CACHE.clear();
        }

        @SuppressWarnings("unchecked")
        private static <T> T getOrCache(String key, Supplier<T> fetcher) {
            CachedValue cv = CACHE.get(key);
            if (cv != null && System.currentTimeMillis() < cv.expiry) {
                return (T) cv.data;
            }
            T value = fetcher.get();
            long ttl = value != null ? TTL_MS : NOT_FOUND_TTL_MS;
            CACHE.put(key, new CachedValue(value, System.currentTimeMillis() + ttl));
            return value;
        }

        public static double getDPrice(String id) {
            if (id == null || id.isEmpty()) return 0;
            Double hc = HARDCODED_PRICES.get(id);
            if (hc != null) return hc;
            PriceEntry entry = getPrice(id);
            if(entry == null) return 0;
            double price = entry.price.getOrDefault("iSell",0.0);
            if(price > 0) return price;
            return entry.price.getOrDefault("avgBin",0.0);
        }

        private static class CachedValue {
            Object data;
            long expiry;

            CachedValue(Object data, long expiry) {
                this.data = data;
                this.expiry = expiry;
            }
        }
    }
}