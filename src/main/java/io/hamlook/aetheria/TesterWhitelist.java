package io.hamlook.aetheria;

import com.google.gson.JsonParser;
import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RegisterEvents
public class TesterWhitelist {
    private static final String API_URL = "https://whitelistapi.vercel.app/api/check";
    private static final String API_KEY = "Pcygv4+FwgbJOHtxLTo+g4IzikJo6fEHWD9q9L1fe3A=";
    private static final int CHECK_INTERVAL_TICKS = 6000;

    private static boolean isAlpha = false;
    private static int tickCounter = 0;
    private static boolean hasChecked = false;

    public static String getApiKey() {
        return API_KEY;
    }

    public static void init(String version) {
        isAlpha = version.toLowerCase().contains("alpha") || version.toLowerCase().contains("beta");
    }

    private static void checkWhitelist() {
        String playerName = Minecraft.getMinecraft().getSession().getProfile().getName().toLowerCase();

        new Thread(() -> {
            try {
                String urlString = API_URL + "?username=" + playerName;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("aetheria-auth", API_KEY);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    crashGame("Whitelist check failed: HTTP " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String jsonResponse = reader.readLine();
                reader.close();

                boolean whitelisted = JsonParser.parseString(jsonResponse).getAsJsonObject().get("whitelisted").getAsBoolean();
                if (!whitelisted) {
                    crashGame("You are not whitelisted for this alpha version.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void crashGame(String reason) {
        Aetheria.logger.severe("[ATHR] " + reason);
        FMLCommonHandler.instance().exitJava(1, true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isAlpha || event.phase != TickEvent.Phase.START) return;

        tickCounter++;
        if (!hasChecked || tickCounter >= CHECK_INTERVAL_TICKS) {
            checkWhitelist();
            tickCounter = 0;
            hasChecked = true;
        }
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!isAlpha) return;
        checkWhitelist();
    }
}