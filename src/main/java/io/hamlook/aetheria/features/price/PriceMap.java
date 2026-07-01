package io.hamlook.aetheria.features.price;

import com.google.gson.Gson;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.price.vars.AuctionEntry;
import io.hamlook.aetheria.features.price.vars.BazaarEntry;
import io.hamlook.aetheria.features.price.vars.PriceData;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PriceMap {

    private static final Gson gson = new Gson();
    @Getter
    private static final PriceData priceData = new PriceData();

    public static volatile int fetchFailCount = 0;
    public static final int MAX_RETRIES = 5;

    public static BazaarEntry getLatestBZPrice(String id) {
        List<BazaarEntry> prices = getBZPrice(id, 1);
        if (prices == null || prices.isEmpty()) return null;
        return prices.get(0);
    }

    public static AuctionEntry getLatestAHPrice(String id) {
        List<AuctionEntry> prices = getAHPrice(id, 1);
        if (prices == null || prices.isEmpty()) return null;
        return prices.get(0);
    }

    public static List<BazaarEntry> getBZPrice(String id, int entries) {
        List<BazaarEntry> prices = priceData.bazaar.get(id);
        if (prices == null) {
            return null;
        }
        prices.sort((c, c1) -> Long.compare(c1.timestamp, c.timestamp));
        int end = Math.min(entries, prices.size());
        return prices.subList(0, end);
    }

    public static List<AuctionEntry> getAHPrice(String id, int entries) {
        List<AuctionEntry> prices = priceData.auction.get(id);
        if (prices == null) {
            return null;
        }
        prices.sort((c, c1) -> Double.compare(c1.price, c.price));
        int count = (entries > 0) ? entries : prices.size();
        int end = Math.min(count, prices.size());
        return prices.subList(0, end);
    }

    public static void fetch() {
        if (ATHRConfig.feature != null && !NetworkGuard.apiAllowed()) return;
        new Thread(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("price"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-mod-secret", CapeAPI.getModSecret());
                conn.setRequestProperty("x-type", getDetailType());
                conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode <= 210) {
                    Aetheria.logger.info("[PriceDetector] Loaded entries items from DB");
                    fetchFailCount = 0;
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);

                        PriceData fetched = gson.fromJson(sb.toString(), PriceData.class);
                        if (fetched != null) {
                            Aetheria.logger.info("[PriceDetector] Loaded entries items from DB aren't null");
                            synchronized (priceData) {
                                priceData.bazaar.clear();
                                priceData.auction.clear();
                                if (fetched.bazaar != null) priceData.bazaar.putAll(fetched.bazaar);
                                if (fetched.auction != null) priceData.auction.putAll(fetched.auction);
                                Aetheria.logger.info("[PriceDetector] Loaded " + (priceData.bazaar.size() + priceData.auction.size()) + " items from DB");
                                priceData.bazaar.forEach((id, am) -> Aetheria.logger.info("[PriceDetector] Loaded " + am.size() + " entries of " + id));
                                priceData.auction.forEach((id, am) -> Aetheria.logger.info("[PriceDetector] Loaded " + am.size() + " entries of " + id));

                            }
                        }
                    }
                } else {
                    Aetheria.logger.info("[PriceDetector] Failed to load entries items from DB | " + responseCode);
                    fetchFailCount++;
                }
            } catch (Exception e) {
                Aetheria.logger.info("[PriceDetector] Failed to fetch prices: " + e.getMessage());
                fetchFailCount++;
            }
        }).start();
    }

    private static String getDetailType() {
        switch (ATHRConfig.feature.misc.itemPriceConfig.priceDetail) {
            case 0:
                return "latest";
            case 1:
                return "full_day";
            case 2:
                return "full_week";
            case 3:
                return "full_month";
        }
        return "full_month";
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

        public static BazaarEntry getLatestBZPrice(String id) {
            return getOrCache("bz_latest_" + id, () -> PriceMap.getLatestBZPrice(id));
        }

        public static AuctionEntry getLatestAHPrice(String id) {
            return getOrCache("ah_latest_" + id, () -> PriceMap.getLatestAHPrice(id));
        }

        public static List<BazaarEntry> getBZPrice(String id, int entries) {
            return getOrCache("bz_" + id + "_" + entries, () -> PriceMap.getBZPrice(id, entries));
        }

        public static List<AuctionEntry> getAHPrice(String id, int entries) {
            return getOrCache("ah_" + id + "_" + entries, () -> PriceMap.getAHPrice(id, entries));
        }

        public static double getPrice(String id) {
            if (id == null || id.isEmpty()) return -1;
            Double hc = HARDCODED_PRICES.get(id);
            if (hc != null) return hc;
            BazaarEntry entry = getLatestBZPrice(id);
            return entry != null && entry.oSell > 0 ? entry.oSell : -1;
        }

        public static double getAHPriceDouble(String id) {
            if (id == null || id.isEmpty()) return -1;
            Double hc = HARDCODED_PRICES.get(id);
            if (hc != null) return hc;
            AuctionEntry entry = getLatestAHPrice(id);
            return entry != null && entry.price > 0 ? entry.price : -1;
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