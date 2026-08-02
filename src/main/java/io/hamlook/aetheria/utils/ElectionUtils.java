package io.hamlook.aetheria.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.repo.CapeAPI;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class ElectionUtils {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static volatile String currentMayor;
    public static boolean check = true;
    public static volatile Perks perks = null;
    public static long lastParse = 0;
    private static boolean mayorFromApi = false;

    public static void initialise() {
        new Thread(() -> {
            currentMayor = fetchCurrentMayor();
            mayorFromApi = currentMayor != null;
            Aetheria.logger.info("[Current Mayor] " + currentMayor);
            perks = fetchPerks();
            if (perks != null) {
                perks.postLoad();
                Aetheria.logger.info("[Perks] Loaded " + perks.perks.size() + " perks");
            } else {
                Aetheria.logger.info("[Perks] Could not load Perks.");
            }
        }, "Aetheria-Mayor-Fetch").start();
    }

    private static Perks fetchPerks() {
        if (!NetworkGuard.apiAllowed()) return null;
        try {
            String response = new HttpClient().fetch(CapeAPI.getAPIUrl("perks"), null).body();
            if (response == null || response.isEmpty()) return null;
            return gson.fromJson(response, Perks.class);
        } catch (Exception e) {
            Aetheria.logger.info("Error Getting Mayor Data: " + e.getMessage());
            return null;
        }
    }

    public static void parseMayorPerks(ContainerChest chest) {
        int slot = 36;
        Slot s = chest.getSlot(slot);
        if (s == null || !s.getHasStack()) return;
        ItemStack stack = s.getStack();
        if (stack == null) return;
        String title = stack.getDisplayName();
        String colorToCheck = title.substring(0, 2);
        List<String> lore = ProfileParser.getLoreColored(stack);
        List<String> perks = new ArrayList<>();

        for (String line : lore) {
            if (!line.startsWith(colorToCheck)) continue;
            perks.add(ColorUtils.stripColor(line).trim());
            Aetheria.logger.info("Added From Lore: " + line);
        }
        if (!perks.isEmpty()) {
            check = false;
            lastParse = System.currentTimeMillis();
            uploadPerksToAPI(perks);
        }
    }

    private static void uploadPerksToAPI(List<String> perks) {
        if (!NetworkGuard.apiAllowed()) return;
        PerkData data = new PerkData(currentMayor, perks);

        try {
            int code = new HttpClient().post(CapeAPI.getAPIUrl("set_perks"), gson.toJson(data), "application/json");
            if (code == 200) {
                Aetheria.logger.info("[Perks] Successfully Uploaded Data to API");
            } else {
                Aetheria.logger.severe("[Perks] Failed to Upload Data to API: " + code);
            }
        } catch (Exception e) {
            Aetheria.logger.severe("[Perks] Error While Uploading to API: " + e.getMessage());
        }
    }

    private static String fetchCurrentMayor() {
        if (!NetworkGuard.apiAllowed()) return null;
        try {
            String response = new HttpClient().fetch(CapeAPI.getAPIUrl("elections"), null).body();
            if (response == null || response.isEmpty()) return null;
            JsonObject object = JsonParser.parseString(response).getAsJsonObject();
            if (object == null) return null;
            if (!object.has("current")) return null;
            return object.get("current").getAsString();
        } catch (Exception e) {
            Aetheria.logger.info("Error Getting Mayor Data: " + e.getMessage());
            return null;
        }
    }

    public static boolean isDianaMayor() {
        return "Diana".equals(currentMayor);
    }

    public static void updateMayorFromTablist(String mayor) {
        if (mayorFromApi) return;
        if (mayor == null || mayor.isEmpty()) return;
        currentMayor = mayor;
    }

    public static void clearTablistMayor() {
        if (!mayorFromApi) currentMayor = null;
    }

    public static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString().trim();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        check = true;
    }

    @SubscribeEvent
    public void onBackgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent e) {
        if (!NetworkGuard.apiAllowed()) return;
        if (!check) return;
        GuiScreen screen = e.gui;
        if (screen instanceof GuiContainer) {
            GuiContainer container = (GuiContainer) screen;
            if (container.inventorySlots instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) container.inventorySlots;
                String title = ContainerUtils.getTitle(chest);
                if (ColorUtils.stripColor(title).trim().equals("Calendar and Events")) {
                    if (System.currentTimeMillis() - lastParse < 1800000) return;
                    parseMayorPerks(chest);
                }
            }
        }
    }

    public static class PerkData {
        public String mayor;
        public List<String> perks;

        public PerkData(String mayor, List<String> perks) {
            this.mayor = mayor;
            this.perks = perks;
        }
    }

    public static class Perks {
        public long updatedAt;
        public String updated;
        public List<String> perks;

        public Perks(long updatedAt, List<String> perks) {
            this.updatedAt = updatedAt;
            this.perks = perks;
        }

        public void postLoad() {
            if (updated == null || updated.isEmpty()) return;
            updatedAt = Instant.parse(updated).toEpochMilli();
            updated = null;
        }
    }
}
