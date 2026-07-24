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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class ElectionUtils {

   public static String currentMayor;
   public static boolean check = true;
   public static Perks perks = null;
   public static long lastParse = 0;
   private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

        public void postLoad(){
            if(updated == null || updated.isEmpty()) return;
            updatedAt = Instant.parse(updated).toEpochMilli();
            updated = null;
        }
    }

   public static void initialise(){
       currentMayor = getMayor();
       perks = getPerks();
       if(perks != null){
           perks.postLoad();
           Aetheria.logger.info("[Perks] Loaded " + perks.perks.size() + " perks");
       }else{
           Aetheria.logger.info("[Perks] Could not load Perks.");
       }
       Aetheria.logger.info("[Current Mayor] " + currentMayor);
   }

    private static Perks getPerks() {
        if(!NetworkGuard.apiAllowed()) return null;
        try{
            URL url = new URL(CapeAPI.getAPIUrl("perks"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
            conn.setConnectTimeout(35000);
            conn.setReadTimeout(35000);
            int responseCode = conn.getResponseCode();
            if(responseCode == 200){
                String response = readResponse(conn);
                if(response.isEmpty()) return null;
                return gson.fromJson(response, Perks.class);
            }
            return null;
        } catch (Exception e) {
            Aetheria.logger.info("Error Getting Mayor Data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @SubscribeEvent
   public void onGuiOpen(GuiOpenEvent event) {
       check = true;
   }

   @SubscribeEvent
   public void onBackgroundDrawn(GuiScreenEvent.BackgroundDrawnEvent e){
       if(!NetworkGuard.apiAllowed()) return;
       if(!check) return;
       GuiScreen screen = e.gui;
       if(screen instanceof GuiContainer){
           GuiContainer container = (GuiContainer) screen;
           if(container.inventorySlots instanceof ContainerChest){
               ContainerChest chest =  (ContainerChest) container.inventorySlots;
               String title = ContainerUtils.getTitle(chest);
               if(ColorUtils.stripColor(title).trim().equals("Calendar and Events")){
                   if(System.currentTimeMillis() - lastParse < 1800000) return;
                   parseMayorPerks(chest);
               }
           }
       }
   }

    public static void parseMayorPerks(ContainerChest chest) {
       int slot = 36;
       Slot s = chest.getSlot(slot);
       if(s == null || !s.getHasStack()) return;
       ItemStack stack = s.getStack();
       if(stack == null) return;
       String title = stack.getDisplayName();
       String colorToCheck = title.substring(0,2);
       List<String> lore = ProfileParser.getLoreColored(stack);
       List<String> perks = new ArrayList<>();

       for(String line : lore){
           if(!line.startsWith(colorToCheck)) continue;
           perks.add(ColorUtils.stripColor(line).trim());
           Aetheria.logger.info("Added From Lore: " + line);
       }
       if(!perks.isEmpty()){
           check = false;
           lastParse = System.currentTimeMillis();
           uploadPerksToAPI(perks);
       }
   }

    private static void uploadPerksToAPI(List<String> perks) {
        if(!NetworkGuard.apiAllowed()) return;
        String mayor = currentMayor;
        PerkData data = new PerkData(mayor,perks);

        try{
            URL url = new URL(CapeAPI.getAPIUrl("set_perks"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(gson.toJson(data).getBytes(StandardCharsets.UTF_8));
            }
            if(conn.getResponseCode() == 200){
                Aetheria.logger.info("[Perks] Successfully Uploaded Data to API");
            }else {
                Aetheria.logger.severe("[Perks] Failed to Upload Data to API: "+ conn.getResponseCode());
            }
        }catch(Exception e){
            Aetheria.logger.severe("[Perks] Error While Uploading to API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getMayor() {
       if(!NetworkGuard.apiAllowed()) return null;
       try{
           URL url = new URL(CapeAPI.getAPIUrl("elections"));
           HttpURLConnection conn = (HttpURLConnection) url.openConnection();
           conn.setRequestMethod("GET");
           conn.setRequestProperty("User-Agent", "Aetheria/" + Aetheria.VERSION);
           conn.setConnectTimeout(35000);
           conn.setReadTimeout(35000);
           int responseCode = conn.getResponseCode();
           if(responseCode == 200){
               String response = readResponse(conn);
               if(response.isEmpty()) return null;
               JsonObject object = JsonParser.parseString(response).getAsJsonObject();
               if(object == null) return null;
               if(!object.has("current")) return null;
               return object.get("current").getAsString();
           }
           return null;
       } catch (Exception e) {
           Aetheria.logger.info("Error Getting Mayor Data: " + e.getMessage());
           e.printStackTrace();
           return null;
       }
   }

    public static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString().trim();
        }
    }
}
