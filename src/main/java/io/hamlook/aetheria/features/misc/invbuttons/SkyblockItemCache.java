package io.hamlook.aetheria.features.misc.invbuttons;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.network.NetworkGuard;
import lombok.Getter;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SkyblockItemCache {

    private static final String REPO_ZIP_URL = "https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/archive/master.zip";

    private static final Pattern VALUE_PATTERN = Pattern.compile("Value\\s*:\\s*[\"']([A-Za-z0-9+/=\\s]+)[\"']");

    private static final LinkedHashMap<String, String> BUILTIN_SKULLS;

    static {
        BUILTIN_SKULLS = new LinkedHashMap<String, String>();
        BUILTIN_SKULLS.put("personal bank", "e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852");
        BUILTIN_SKULLS.put("skyblock hub", "d7cc6687423d0570d556ac53e0676cb563bbdd9717cd8269bdebed6f6d4e7bf8");
        BUILTIN_SKULLS.put("private island", "c9c8881e42915a9d29bb61a16fb26d059913204d265df5b439b3d792acd56");
        BUILTIN_SKULLS.put("castle", "f4559d75464b2e40a518e4de8e6cf3085f0a3ca0b1b7012614c4cd96fed60378");
        BUILTIN_SKULLS.put("sirius shack", "7ab83858ebc8ee85c3e54ab13aabfcc1ef2ad446d6a900e471c3f33b78906a5b");
        BUILTIN_SKULLS.put("crypts", "25d2f31ba162fe6272e831aed17f53213db6fa1c4cbe4fc827f3963cc98b9");
        BUILTIN_SKULLS.put("spiders den", "c754318a3376f470e481dfcd6c83a59aa690ad4b4dd7577fdad1c2ef08d8aee6");
        BUILTIN_SKULLS.put("top of the nest", "9d7e3b19ac4f3dee9c5677c135333b9d35a7f568b63d1ef4ada4b068b5a25");
        BUILTIN_SKULLS.put("the end", "7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5");
        BUILTIN_SKULLS.put("the end dragons nest", "a1cd6d2d03f135e7c6b5d6cdae1b3a68743db4eb749faf7341e9fb347aa283b");
        BUILTIN_SKULLS.put("the park", "a221f813dacee0fef8c59f76894dbb26415478d9ddfc44c2e708a6d3b7549b");
        BUILTIN_SKULLS.put("the park jungle", "79ca3540621c1c79c32bf42438708ff1f5f7d0af9b14a074731107edfeb691c");
        BUILTIN_SKULLS.put("the park howling cave", "1832d53997b451635c9cf9004b0f22bb3d99ab5a093942b5b5f6bb4e4de47065");
        BUILTIN_SKULLS.put("gold mines", "73bc965d579c3c6039f0a17eb7c2e6faf538c7a5de8e60ec7a719360d0a857a9");
        BUILTIN_SKULLS.put("deep caverns", "569a1f114151b4521373f34bc14c2963a5011cdc25a6554c48c708cd96ebfc");
        BUILTIN_SKULLS.put("the barn", "4d3a6bd98ac1833c664c4909ff8d2dc62ce887bdcf3cc5b3848651ae5af6b");
        BUILTIN_SKULLS.put("mushroom desert", "6b20b23c1aa2be0270f016b4c90d6ee6b8330a17cfef87869d6ad60b2ffbf3b5");
        BUILTIN_SKULLS.put("dungeon hub", "9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d");
        BUILTIN_SKULLS.put("dwarven mines", "51539dddf9ed255ece6348193cd75012c82c93aec381f05572cecf7379711b3b");
        BUILTIN_SKULLS.put("hotm heart of the mountain", "86f06eaa3004aeed09b3d5b45d976de584e691c0e9cade133635de93d23b9edb");
        BUILTIN_SKULLS.put("bazaar dude", "c232e3820897429157619b0ee099fec0628f602fff12b695de54aef11d923ad7");
        BUILTIN_SKULLS.put("museum", "438cf3f8e54afc3b3f91d20a49f324dca1486007fe545399055524c17941f4dc");
        BUILTIN_SKULLS.put("crystal hollows", "21dbe30b027acbceb612563bd877cd7ebb719ea6ed1399027dcee58bb9049d4a");
        BUILTIN_SKULLS.put("dwarven forge", "5cbd9f5ec1ed007259996491e69ff649a3106cf920227b1bb3a71ee7a89863f");
        BUILTIN_SKULLS.put("forgotton skull", "6becc645f129c8bc2faa4d8145481fab11ad2ee75749d628dcd999aa94e7");
        BUILTIN_SKULLS.put("crystal nucleus", "34d42f9c461cee1997b67bf3610c6411bf852b9e5db607bbf626527cfb42912c");
        BUILTIN_SKULLS.put("void sepulture", "eb07594e2df273921a77c101d0bfdfa1115abed5b9b2029eb496ceba9bdbb4b3");
        BUILTIN_SKULLS.put("crimson isle", "c3687e25c632bce8aa61e0d64c24e694c3eea629ea944f4cf30dcfb4fbce071");
        BUILTIN_SKULLS.put("trapper den", "6102f82148461ced1f7b62e326eb2db3a94a33cba81d4281452af4d8aeca4991");
        BUILTIN_SKULLS.put("arachne sanctuary", "35e248da2e108f09813a6b848a0fcef111300978180eda41d3d1a7a8e4dba3c3");
        BUILTIN_SKULLS.put("garden", "f4880d2c1e7b86e87522e20882656f45bafd42f94932b2c5e0d6ecaa490cb4c");
        BUILTIN_SKULLS.put("winter", "6dd663136cafa11806fdbca6b596afd85166b4ec02142c8d5ac8941d89ab7");
        BUILTIN_SKULLS.put("wizard tower", "838564e28aba98301dbda5fafd86d1da4e2eaeef12ea94dcf440b883e559311c");
        BUILTIN_SKULLS.put("dwarven mines base camp", "2461ec3bd654f62ca9a393a32629e21b4e497c877d3f3380bcf2db0e20fc0244");
    }

    private static final SkyblockItemCache INSTANCE = new SkyblockItemCache();

    private final TreeMap<String, JsonObject> itemMap = new TreeMap<>();
    private final LinkedHashMap<String, String> skullMap = new LinkedHashMap<>();
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ATHR-ItemCache");
        t.setDaemon(true);
        return t;
    });
    @Getter
    private volatile boolean loaded = false;

    private SkyblockItemCache() {
        skullMap.putAll(BUILTIN_SKULLS);
    }

    public static SkyblockItemCache getInstance() {
        return INSTANCE;
    }

    public void loadAsync() {
        if (loaded) return;
        loader.submit(this::loadSync);
    }

    private void loadSync() {
        if (!NetworkGuard.githubAllowed()) {
            System.out.println("[ATHR] GitHub calls disabled. Skipping NEU item repo download.");
            loaded = true;
            return;
        }
        try {
            System.out.println("[ATHR] Downloading NEU item repo...");
            HttpURLConnection conn = (HttpURLConnection) new URL(REPO_ZIP_URL).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "ATHR/1.0");

            int items = 0, skulls = 0, skullsFailed = 0;
            JsonParser parser = new JsonParser();

            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(conn.getInputStream()))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (!name.endsWith(".json") || !name.contains("/items/")) {
                        zip.closeEntry();
                        continue;
                    }

                    String filename = name.substring(name.lastIndexOf('/') + 1);
                    String internalName = filename.substring(0, filename.length() - 5);
                    byte[] bytes = readEntry(zip);

                    try {
                        JsonObject json = parser.parse(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                        if (!json.has("itemid")) {
                            zip.closeEntry();
                            continue;
                        }

                        synchronized (itemMap) {
                            itemMap.put(internalName, json);
                        }
                        items++;

                        String itemid = json.get("itemid").getAsString().toLowerCase();
                        if (itemid.contains("skull")) {
                            if (!json.has("nbttag")) {
                                skullsFailed++;
                            } else {
                                String hash = extractSkullHash(json.get("nbttag").getAsString());
                                if (hash != null) {
                                    synchronized (skullMap) {
                                        skullMap.put(internalName, hash);
                                    }
                                    skulls++;
                                } else {
                                    skullsFailed++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("[ATHR-PARSE-ERR] " + internalName + " | " + e.getMessage());
                    }
                    zip.closeEntry();
                }
            }

            loaded = true;
            System.out.println("[ATHR] Loaded " + items + " items, " + skulls + " repo skulls + " + BUILTIN_SKULLS.size() + " builtin island skulls (" + skullsFailed + " failed) from NEU repo.");
        } catch (Exception e) {
            System.err.println("[ATHR] Item cache failed: " + e.getMessage());
            loaded = true;
        }
    }

    private String extractSkullHash(String nbtString) {
        try {
            Matcher m = VALUE_PATTERN.matcher(nbtString);
            while (m.find()) {
                String b64 = m.group(1).replaceAll("\\s", "");
                try {
                    String decoded = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
                    int ti = decoded.indexOf("/texture/");
                    if (ti == -1) continue;
                    ti += "/texture/".length();
                    int hashEnd = ti;
                    while (hashEnd < decoded.length()) {
                        char c = decoded.charAt(hashEnd);
                        if (c == '"' || c == '\'' || c == '\\' || c == '?' || c == ' ') break;
                        hashEnd++;
                    }
                    String hash = decoded.substring(ti, hashEnd).trim();
                    if (!hash.isEmpty()) return hash;
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readEntry(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = zip.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    public JsonObject getItemJson(String internalName) {
        synchronized (itemMap) {
            return itemMap.get(internalName);
        }
    }

    public Set<String> getAllItemIds() {
        synchronized (itemMap) {
            return new LinkedHashSet<>(itemMap.keySet());
        }
    }

    public Map<String, String> getSkullItems() {
        synchronized (skullMap) {
            return new LinkedHashMap<>(skullMap);
        }
    }
}