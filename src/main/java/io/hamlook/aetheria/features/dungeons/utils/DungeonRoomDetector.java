package io.hamlook.aetheria.features.dungeons.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomOverlay;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RegisterEvents
public class DungeonRoomDetector {

    private static JsonObject roomsJson = null;
    private static JsonObject secretLocationsJson = null;
    private static int tickCount = 0;
    private static String lastRoomHash = null;
    private static JsonObject lastRoomJson = null;
    private final Executor executor = Executors.newFixedThreadPool(2);

    private static final Set<String> loadedSecretKeys = new HashSet<>();

    public static volatile BlockPos originBlock = null;
    public static volatile String originCorner = null;
    public static volatile int roomRotation = -1;
    public static volatile int playerRelX = Integer.MAX_VALUE;
    public static volatile int playerRelZ = Integer.MAX_VALUE;

    public static volatile int roomMinX = Integer.MAX_VALUE;
    public static volatile int roomMinZ = Integer.MAX_VALUE;
    public static volatile int roomMaxX = Integer.MIN_VALUE;
    public static volatile int roomMaxZ = Integer.MIN_VALUE;
    public static volatile int roomCeilingY = -1;
    public static volatile int roomFloorY = -1;
    public static volatile boolean roomBoundsValid = false;

    private void resetOrigin() {
        originBlock = null;
        originCorner = null;
        roomRotation = -1;
        playerRelX = Integer.MAX_VALUE;
        playerRelZ = Integer.MAX_VALUE;
        roomMinX = Integer.MAX_VALUE;
        roomMinZ = Integer.MAX_VALUE;
        roomMaxX = Integer.MIN_VALUE;
        roomMaxZ = Integer.MIN_VALUE;
        roomCeilingY = -1;
        roomFloorY = -1;
        roomBoundsValid = false;
        SecretRenderUtils.clearSecrets();
        loadedSecretKeys.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (ATHRConfig.feature == null) return;
        boolean overlayOn = ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlay;
        boolean sfOn = ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled;
        if (!overlayOn && !sfOn) {
            DungeonRoomOverlay.currentRoomName = null;
            DungeonRoomOverlay.currentRoomCategory = null;
            DungeonRoomOverlay.currentRoomNotes = null;
            lastRoomHash = null;
            lastRoomJson = null;
            resetOrigin();
            return;
        }
        if (!sfOn) {
            originBlock = null;
            originCorner = null;
            roomRotation = -1;
            playerRelX = Integer.MAX_VALUE;
            playerRelZ = Integer.MAX_VALUE;
            SecretRenderUtils.clearSecrets();
            loadedSecretKeys.clear();
            displayedSecretCount = -1;
        }
        if (SkyblockData.getCurrentLocation() != SkyblockData.Location.DUNGEON) {
            resetOrigin();
            return;
        }
        if (DungeonStats.isInBossFight()) {
            resetOrigin();
            return;
        }
        if (++tickCount % 30 != 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (roomsJson == null) loadRoomsJson();
        if (roomsJson == null) return;

        executor.execute(() -> {
            try {
                int x = (int) Math.floor(mc.thePlayer.posX);
                int y = (int) Math.floor(mc.thePlayer.posY);
                int z = (int) Math.floor(mc.thePlayer.posZ);

                int top = dungeonTop(x, z);
                String blockFreq = blockFrequency(x, top, z);
                if (blockFreq == null) return;

                String md5 = getMD5(blockFreq);
                String floorFreq = floorFrequency(x, top, z);
                String floorHash = getMD5(floorFreq);

                if ("16370f79b2cad049096f881d5294aee6".equals(md5) && !"94fb12c91c4b46bd0c254edadaa49a3d".equals(floorHash)) {
                    floorHash = "e617eff1d7b77faf0f8dd53ec93a220f";
                }
                md5 = resolveMD5(md5);
                if (Objects.equals(md5, lastRoomHash) && lastRoomJson != null) {
                    JsonElement jfh = lastRoomJson.get("floorhash");
                    if (jfh == null || (floorHash != null && floorHash.equals(jfh.getAsString()))) {
                        computeRoomBounds(x, top, z);
                        if (sfOn) processSecrets();
                        if (sfOn && originBlock != null && originCorner != null) {
                            BlockPos rel = actualToRelative(new BlockPos(x, y, z));
                            if (rel != null) {
                                playerRelX = rel.getX();
                                playerRelZ = rel.getZ();
                            }
                        }
                        return;
                    }
                }

                if (sfOn) {
                    originBlock = null;
                    originCorner = null;
                    roomRotation = -1;
                    playerRelX = Integer.MAX_VALUE;
                    playerRelZ = Integer.MAX_VALUE;
                }

                lastRoomHash = md5;

                if (!roomsJson.has(md5)) {
                    if (ATHRConfig.feature.debug.dungeonRoomDebug) {
                        DungeonRoomOverlay.currentRoomCategory = "Debug";
                        DungeonRoomOverlay.currentRoomName = "§cUnknown §7(" + (md5 != null ? md5.substring(0, 32) : "N/A") + ")";
                        DungeonRoomOverlay.currentRoomNotes = "§8Hash not in JSON";
                    } else {
                        DungeonRoomOverlay.currentRoomName = null;
                        DungeonRoomOverlay.currentRoomCategory = null;
                        DungeonRoomOverlay.currentRoomNotes = null;
                    }
                    lastRoomJson = null;
                    resetOrigin();
                    return;
                }

                JsonArray arr = roomsJson.get(md5).getAsJsonArray();

                if (arr.size() >= 2) {
                    JsonObject matched = null;
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        JsonElement jfh = obj.get("floorhash");
                        if (floorHash != null && jfh != null && floorHash.equals(jfh.getAsString())) {
                            matched = obj;
                            break;
                        }
                    }
                    if (matched != null) {
                        lastRoomJson = matched;
                        setOverlay(matched);
                    } else {
                        lastRoomJson = arr.get(0).getAsJsonObject();
                        setOverlay(lastRoomJson);
                    }
                } else {
                    lastRoomJson = arr.get(0).getAsJsonObject();
                    setOverlay(lastRoomJson);
                }

                computeRoomBounds(x, top, z);
                if (sfOn) processSecrets();

                if (sfOn && originBlock != null && originCorner != null) {
                    BlockPos rel = actualToRelative(new BlockPos(x, y, z));
                    if (rel != null) {
                        playerRelX = rel.getX();
                        playerRelZ = rel.getZ();
                    }
                    switch (originCorner) {
                        case "northwest": roomRotation = 0; break;
                        case "northeast": roomRotation = 90; break;
                        case "southeast": roomRotation = 180; break;
                        case "southwest": roomRotation = 270; break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static String resolveMD5(String md5) {
        switch (md5){
            // Cavern-8
            case "eb202d1d318396fc44bd1da3ab00b9cc":
            case "e94d4df3348b347eb6182ef6bd7cb26d": return "721bf13b2441c9269f8222f4e90f897c";
            // Entrance Room
            case "11ac182bc9abe2cbf21719733d4d58bc": return "74e45b213b3372fe91b0dd6a7474a588";
            // Trivia Room
            case "3e877ad473671a2767362a93348b9f7f": return "506f87f8b14643cfcccd3d2845c86e50";
            // Blood Room
            case "48c22ef4c10a0a5036f9f06c62f295e4":
            case "56ae7d302c9d835d187e001a93372463": return "710eb845c35f240667bb63f8edb754bf";
            // Bridges
            case "03c30fd33553f37b22b9c4b8bed33e1d": return "c979e9eda7361555ce2d75d63f5305bb";
            // Ice-Path
            case "1d104fa1f828f60074dfc345dcf35032": return "ac807d34afef330d7275836795c6f734";
            // Miniboss Room
            case "2002014fb9fbaa0f896aaadc3854fef4":
            case "4c118368fc6f08b29ee18717999590bd":
            case "aeeeb0546987de22e3c4a45f45d546f9": return "569c63a07c6ebfe1153d0738f0e44731";
            // Redstone-Warrior-3
            case "0d25288e91b3380442576b0c0b23fa31": return "6d788f8bd2fb147f71d1afa6e010a7b8";
            // Sanctuary
            case "2cfcecf71825b76faa4f97787da2e996": return "263a269d5c93255c60eb721e128f2d20";
            // Trap-Very-Hard
            case "d076f0391db006f2282c52ec7c63d520": return "fe4b5561b73fb082acb80d904ec82294";

        }
        return md5;
    }

    private void setOverlay(JsonObject room) {
        String name = room.get("name").getAsString();

        DungeonRoomOverlay.currentRoomCategory = room.get("category").getAsString();
        DungeonRoomOverlay.currentRoomName = name;
        JsonElement notes = room.get("notes");
        DungeonRoomOverlay.currentRoomNotes = (notes != null) ? notes.getAsString() : null;
    }

    static BlockPos relativeToActual(BlockPos relative) {
        if (originBlock == null || originCorner == null) return null;
        double x;
        double z;
        switch (originCorner) {
            case "northwest":
                x = relative.getX() + originBlock.getX();
                z = relative.getZ() + originBlock.getZ();
                break;
            case "northeast":
                x = -(relative.getZ() - originBlock.getX());
                z = relative.getX() + originBlock.getZ();
                break;
            case "southeast":
                x = -(relative.getX() - originBlock.getX());
                z = -(relative.getZ() - originBlock.getZ());
                break;
            case "southwest":
                x = relative.getZ() + originBlock.getX();
                z = -(relative.getX() - originBlock.getZ());
                break;
            default:
                return null;
        }
        return new BlockPos(x, relative.getY(), z);
    }

    private void computeRoomBounds(int x, int y, int z) {
        int nz = endOfRoom(x, y, z, "n");
        int sz = endOfRoom(x, y, z, "s");
        int ex = endOfRoom(x, y, z, "e");
        int wx = endOfRoom(x, y, z, "w");

        if (nz == -1 || sz == -1 || ex == -1 || wx == -1) {
            roomBoundsValid = false;
            return;
        }

        roomMinX = wx;
        roomMinZ = nz;
        roomMaxX = ex;
        roomMaxZ = sz;
        roomCeilingY = dungeonTop(x, z);
        roomFloorY = dungeonBottom(x, z);
        roomBoundsValid = true;
    }

    private void checkCorner(BlockPos blockPos) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        if (world.getBlockState(blockPos).getBlock() == Blocks.stained_hardened_clay) {
            Block northBlock = world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1)).getBlock();
            Block southBlock = world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1)).getBlock();
            Block eastBlock = world.getBlockState(new BlockPos(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ())).getBlock();
            Block westBlock = world.getBlockState(new BlockPos(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ())).getBlock();
            if (northBlock == Blocks.air && southBlock != Blocks.air && eastBlock != Blocks.air && westBlock == Blocks.air) {
                originCorner = "northwest";
                originBlock = blockPos;
            } else if (northBlock == Blocks.air && southBlock != Blocks.air && eastBlock == Blocks.air && westBlock != Blocks.air) {
                originCorner = "northeast";
                originBlock = blockPos;
            } else if (northBlock != Blocks.air && southBlock == Blocks.air && eastBlock == Blocks.air && westBlock != Blocks.air) {
                originCorner = "southeast";
                originBlock = blockPos;
            } else if (northBlock != Blocks.air && southBlock == Blocks.air && eastBlock != Blocks.air && westBlock == Blocks.air) {
                originCorner = "southwest";
                originBlock = blockPos;
            }
        }
    }

    public static BlockPos actualToRelative(BlockPos actual) {
        if (originBlock == null || originCorner == null) return null;
        double x;
        double z;
        switch (originCorner) {
            case "northwest":
                x = actual.getX() - originBlock.getX();
                z = actual.getZ() - originBlock.getZ();
                break;
            case "northeast":
                x = actual.getZ() - originBlock.getZ();
                z = -(actual.getX() - originBlock.getX());
                break;
            case "southeast":
                x = -(actual.getX() - originBlock.getX());
                z = -(actual.getZ() - originBlock.getZ());
                break;
            case "southwest":
                x = -(actual.getZ() - originBlock.getZ());
                z = actual.getX() - originBlock.getX();
                break;
            default:
                return null;
        }
        return new BlockPos(x, actual.getY(), z);
    }

    private InputStream getStreamWithFallback(String webUrl, ResourceLocation fallBack) {
        try {
            URL url = new URL(webUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Aetheria.logger.info("Successfully loaded data from URL: " + webUrl);
                return connection.getInputStream();
            } else {
                Aetheria.logger.info("Failed to fetch from URL (HTTP " + responseCode + "). Falling back to JAR.");
            }
        } catch (IOException e) {
            Aetheria.logger.info("Network error connecting to URL. Falling back to JAR. Error: " + e.getMessage());
        }
        try{
            Aetheria.logger.info("Loading data from fallback Path: " + fallBack.getResourcePath());
            return Minecraft.getMinecraft().getResourceManager().getResource(fallBack).getInputStream();
        }catch(IOException e){
            Aetheria.logger.info("Error loading from fallback");
            return null;
        }
    }

    private void loadSecretLocationsJson() {
        String webUrl = "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/refs/heads/main/data/secretlocations.json";
        ResourceLocation loc = Resources.SECRET_LOCATIONS_JSON;
        try {
            InputStream in = getStreamWithFallback(webUrl, loc);
            if(in == null) return;
            secretLocationsJson = new Gson().fromJson(new InputStreamReader(in), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int displayedSecretCount = -1;

    private void processSecrets() {
        if (secretLocationsJson == null) loadSecretLocationsJson();
        if (secretLocationsJson == null) { displayedSecretCount = -1; return; }
        if (originBlock == null || originCorner == null) { displayedSecretCount = -1; return; }
        if (lastRoomHash == null) { displayedSecretCount = -1; return; }

        String roomName = DungeonRoomOverlay.currentRoomName;
        if (roomName == null) { displayedSecretCount = -1; return; }

        String cacheKey = lastRoomHash + "|" + originBlock.getX() + "," + originBlock.getZ();
        if (!loadedSecretKeys.contains(cacheKey)) {
            loadedSecretKeys.clear();
            loadedSecretKeys.add(cacheKey);
            SecretRenderUtils.loadSecrets(roomName, secretLocationsJson);
            displayedSecretCount = SecretRenderUtils.getActiveSecretCount();
        }
    }

    private void loadRoomsJson() {
        String webUrl = "https://raw.githubusercontent.com/aetheria-org/Aetheria-REPO/refs/heads/main/data/dungeonrooms.json";
        ResourceLocation loc = Resources.DUNGEON_ROOMS_JSON;
        try {
            InputStream in = getStreamWithFallback(webUrl, loc);
            if(in == null) return;
            roomsJson = new Gson().fromJson(new InputStreamReader(in), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int dungeonTop(int x, int z) {
        World world = Minecraft.getMinecraft().theWorld;
        for (int i = 255; i >= 78; i--) {
            Block b = world.getBlockState(new BlockPos(x, i, z)).getBlock();
            if (b != Blocks.air && checkPlatform(x, i, z)) return i;
        }
        return -1;
    }

    private int dungeonBottom(int x, int z) {
        World world = Minecraft.getMinecraft().theWorld;
        for (int i = 0; i <= 68; i++) {
            Block b = world.getBlockState(new BlockPos(x, i, z)).getBlock();
            if (b == Blocks.bedrock || b == Blocks.stone) return i;
        }
        return -1;
    }

    private int dungeonHeight(int x, int z) {
        return dungeonTop(x, z) - dungeonBottom(x, z);
    }

    private boolean checkPlatform(int x, int y, int z) {
        World world = Minecraft.getMinecraft().theWorld;
        int n = 0, s = 0, e = 0, w = 0;
        for (int j = 0; j < 10; j++) {
            if (world.getBlockState(new BlockPos(x, y, z - j)).getBlock() != Blocks.air) n++;
            if (world.getBlockState(new BlockPos(x, y, z + j)).getBlock() != Blocks.air) s++;
            if (world.getBlockState(new BlockPos(x + j, y, z)).getBlock() != Blocks.air) e++;
            if (world.getBlockState(new BlockPos(x - j, y, z)).getBlock() != Blocks.air) w++;
        }
        return (n == 10 || s == 10 || e == 10 || w == 10);
    }

    private int endOfRoom(int x, int y, int z, String dir) {
        World world = Minecraft.getMinecraft().theWorld;
        for (int i = 1; i <= 200; i++) {
            BlockPos bp;
            int coord;
            switch (dir) {
                case "n":
                    bp = new BlockPos(x, y, z - i);
                    coord = z - i + 1;
                    if (world.getBlockState(bp).getBlock() == Blocks.air || checkPlatform(x, y + 1, z - i) || Math.abs(dungeonHeight(x, z - i) - dungeonHeight(x, z - i + 1)) > 3)
                        return coord;
                    break;
                case "s":
                    bp = new BlockPos(x, y, z + i);
                    coord = z + i - 1;
                    if (world.getBlockState(bp).getBlock() == Blocks.air || checkPlatform(x, y + 1, z + i) || Math.abs(dungeonHeight(x, z + i) - dungeonHeight(x, z + i - 1)) > 3)
                        return coord;
                    break;
                case "e":
                    bp = new BlockPos(x + i, y, z);
                    coord = x + i - 1;
                    if (world.getBlockState(bp).getBlock() == Blocks.air || checkPlatform(x + i, y + 1, z) || Math.abs(dungeonHeight(x + i, z) - dungeonHeight(x + i - 1, z)) > 3)
                        return coord;
                    break;
                case "w":
                    bp = new BlockPos(x - i, y, z);
                    coord = x - i + 1;
                    if (world.getBlockState(bp).getBlock() == Blocks.air || checkPlatform(x - i, y + 1, z) || Math.abs(dungeonHeight(x - i, z) - dungeonHeight(x - i + 1, z)) > 3)
                        return coord;
                    break;
            }
        }
        return -1;
    }

    private int northWidth(int x, int y, int z) {
        int nz = endOfRoom(x, y, z, "n");
        return endOfRoom(x, y, nz, "e") - endOfRoom(x, y, nz, "w");
    }

    private int southWidth(int x, int y, int z) {
        int sz = endOfRoom(x, y, z, "s");
        return endOfRoom(x, y, sz, "e") - endOfRoom(x, y, sz, "w");
    }

    private int eastWidth(int x, int y, int z) {
        int ex = endOfRoom(x, y, z, "e");
        return endOfRoom(ex, y, z, "s") - endOfRoom(ex, y, z, "n");
    }

    private int westWidth(int x, int y, int z) {
        int wx = endOfRoom(x, y, z, "w");
        return endOfRoom(wx, y, z, "s") - endOfRoom(wx, y, z, "n");
    }

    private String getSize(int x, int y, int z) {
        int n = northWidth(x, y, z), s = southWidth(x, y, z), e = eastWidth(x, y, z), w = westWidth(x, y, z);
        if (n == s && s == e && e == w) {
            if (n == 30) return "1x1";
            if (n == 62) return "2x2";
        } else if (n == s && e == w) {
            if ((n == 62 && e == 30) || (n == 30 && e == 62)) return "1x2";
            if ((n == 94 && e == 30) || (n == 30 && e == 94)) return "1x3";
            if ((n == 126 && e == 30) || (n == 30 && e == 126)) return "1x4";
        } else {
            int l62 = (n == 62 ? 1 : 0) + (s == 62 ? 1 : 0) + (e == 62 ? 1 : 0) + (w == 62 ? 1 : 0);
            int l30 = (n == 30 ? 1 : 0) + (s == 30 ? 1 : 0) + (e == 30 ? 1 : 0) + (w == 30 ? 1 : 0);
            if (l62 >= 2 && l30 == 4 - l62) return "L-shape";
        }
        return "error";
    }

    private String blockFrequency(int x, int y, int z) {
        if (y == -1) return null;
        World world = Minecraft.getMinecraft().theWorld;
        List<String> blockList = new ArrayList<>();

        int nw = northWidth(x, y, z), sw = southWidth(x, y, z), ew = eastWidth(x, y, z), ww = westWidth(x, y, z);

        if (nw == sw && ew == ww) {
            int nz = endOfRoom(x, y, z, "n"), nwx = endOfRoom(x, y, nz, "w");
            int sz = endOfRoom(x, y, z, "s"), sex = endOfRoom(x, y, sz, "e");
            for (BlockPos bp : BlockPos.getAllInBox(new BlockPos(nwx, y, nz), new BlockPos(sex, y, sz))) {
                if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled) checkCorner(bp);
                blockList.add(world.getBlockState(bp).toString());
            }
        } else if (getSize(x, y, z).equals("L-shape")) {
            if (nw == sw) {
                int startX = ew > ww ? endOfRoom(x, y, z, "e") : endOfRoom(x, y, z, "w");
                int nz = endOfRoom(startX, y, z, "n");
                int dx = ew > ww ? -1 : 1;
                for (int i = 0; i < 200; i++) {
                    int cz = nz + i;
                    if (world.getBlockState(new BlockPos(startX, y, cz)).getBlock() == Blocks.air || checkPlatform(startX, y + 1, cz) || (i > 0 && Math.abs(dungeonHeight(startX, cz) - dungeonHeight(startX, cz - 1)) > 3))
                        break;
                    for (int j = 0; j < 200; j++) {
                        BlockPos bp = new BlockPos(startX + dx * j, y, cz);
                        Block b = world.getBlockState(bp).getBlock();
                        if (b == Blocks.air || checkPlatform(startX + dx * j, y + 1, cz) || (j > 0 && Math.abs(dungeonHeight(startX + dx * j, cz) - dungeonHeight(startX + dx * (j - 1), cz)) > 3))
                            break;
                        if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled) checkCorner(bp);
                        blockList.add(b.toString());
                    }
                }
            } else {
                int startZ = nw > sw ? endOfRoom(x, y, z, "n") : endOfRoom(x, y, z, "s");
                int wx = endOfRoom(x, y, startZ, "w");
                int dz = nw > sw ? 1 : -1;
                for (int i = 0; i < 200; i++) {
                    int cx = wx + i;
                    if (world.getBlockState(new BlockPos(cx, y, startZ)).getBlock() == Blocks.air || checkPlatform(cx, y + 1, startZ) || (i > 0 && Math.abs(dungeonHeight(cx, startZ) - dungeonHeight(cx - 1, startZ)) > 3))
                        break;
                    for (int j = 0; j < 200; j++) {
                        BlockPos bp = new BlockPos(cx, y, startZ + dz * j);
                        Block b = world.getBlockState(bp).getBlock();
                        if (b == Blocks.air || checkPlatform(cx, y + 1, startZ + dz * j) || (j > 0 && Math.abs(dungeonHeight(cx, startZ + dz * j) - dungeonHeight(cx, startZ + dz * (j - 1))) > 3))
                            break;
                        if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled) checkCorner(bp);
                        blockList.add(b.toString());
                    }
                }
            }
        }

        if (blockList.isEmpty()) return null;
        Set<String> distinct = new HashSet<>(blockList);
        List<String> freqs = new ArrayList<>();
        for (String s : distinct) freqs.add(s + ":" + Collections.frequency(blockList, s));
        Collections.sort(freqs);
        return String.join(",", freqs);
    }

    private String floorFrequency(int x, int y, int z) {
        if (y == -1) return null;
        World world = Minecraft.getMinecraft().theWorld;
        List<String> blockList = new ArrayList<>();

        if (northWidth(x, y, z) == southWidth(x, y, z) && eastWidth(x, y, z) == westWidth(x, y, z)) {
            int nz = endOfRoom(x, y, z, "n"), nwx = endOfRoom(x, y, nz, "w");
            int sz = endOfRoom(x, y, z, "s"), sex = endOfRoom(x, y, sz, "e");
            for (BlockPos bp : BlockPos.getAllInBox(new BlockPos(nwx + 10, 68, nz + 10), new BlockPos(sex - 10, 68, sz - 10)))
                blockList.add(world.getBlockState(bp).getBlock().toString());
        }
        if (getSize(x, y, z).equals("L-shape")) blockList.add(String.valueOf(dungeonTop(x, z)));

        if (blockList.isEmpty()) return null;
        Set<String> distinct = new HashSet<>(blockList);
        List<String> freqs = new ArrayList<>();
        for (String s : distinct) freqs.add(s + ":" + Collections.frequency(blockList, s));
        Collections.sort(freqs);
        return String.join(",", freqs);
    }

    private String getMD5(String input) {
        try {
            if (input == null) return null;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, digest);
            StringBuilder hash = new StringBuilder(no.toString(16));
            while (hash.length() < 32) hash.insert(0, "0");
            return hash.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        resetOrigin();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!roomBoundsValid || roomMinX == Integer.MAX_VALUE) return;
        if (DungeonRoomOverlay.currentRoomName == null) return;
        if (roomCeilingY <= 0 || roomFloorY < 0) return;

        float tracerWidth = ATHRConfig.feature != null
            && ATHRConfig.feature.dungeons.dungeonSecretFinder != null
            ? ATHRConfig.feature.dungeons.dungeonSecretFinder.other.tracerWidth : 2.0f;
        WorldRenderUtils.drawSelectionBox(new AxisAlignedBB(
            roomMinX, roomFloorY, roomMinZ,
            roomMaxX + 1, roomCeilingY + 1, roomMaxZ + 1
        ), new Color(0, 200, 255, 120), tracerWidth);

        if (originBlock == null) return;

        double vx = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double vy = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double vz = Minecraft.getMinecraft().getRenderManager().viewerPosZ;

        drawEspBoxTranslated(originBlock.getX(), originBlock.getY(), originBlock.getZ(), new Color(180, 0, 255, 200), vx, vy, vz, tracerWidth);

        if (ATHRConfig.feature != null && ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled) {
            displayedSecretCount = SecretRenderUtils.getActiveSecretCount();
            SecretRenderUtils.renderSecrets(event.partialTicks);
        }
    }

    private void drawEspBoxTranslated(double x, double y, double z, Color color, double vx, double vy, double vz, float lineWidth) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);
        WorldRenderUtils.drawEspBox(x, y, z, color);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}

