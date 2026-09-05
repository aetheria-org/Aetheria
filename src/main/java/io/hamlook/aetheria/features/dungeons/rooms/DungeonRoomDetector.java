package io.hamlook.aetheria.features.dungeons.rooms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.overlays.DungeonMapOverlay;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.WorldCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RegisterEvents
public class DungeonRoomDetector {

    private static final Executor executor = Executors.newFixedThreadPool(2);
    private static final ConcurrentMap<String, DungeonRoom> visitedRooms = new ConcurrentHashMap<>();
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
    public static int displayedSecretCount = -1;
    private static JsonObject roomsJson = null;
    private static JsonObject secretLocationsJson = null;
    private static int tickCount = 0;
    private static String lastRoomHash = null;
    private static JsonObject lastRoomJson = null;
    private final Map<Long, Integer> dungeonTopCache = new ConcurrentHashMap<>();
    private final Map<Long, Integer> dungeonBottomCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> endOfRoomCache = new ConcurrentHashMap<>();

    public static java.util.Collection<DungeonRoom> getVisitedRooms() {
        return visitedRooms.values();
    }

    public static String resolveMD5(String md5) {
        switch (md5) {
            // Cavern-8
            case "eb202d1d318396fc44bd1da3ab00b9cc":
            case "e94d4df3348b347eb6182ef6bd7cb26d":
                return "721bf13b2441c9269f8222f4e90f897c";
            // Entrance Room
            case "11ac182bc9abe2cbf21719733d4d58bc":
                return "74e45b213b3372fe91b0dd6a7474a588";
            // Trivia Room
            case "3e877ad473671a2767362a93348b9f7f":
                return "506f87f8b14643cfcccd3d2845c86e50";
            // Blood Room
            case "48c22ef4c10a0a5036f9f06c62f295e4":
            case "56ae7d302c9d835d187e001a93372463":
                return "710eb845c35f240667bb63f8edb754bf";
            // Bridges
            case "03c30fd33553f37b22b9c4b8bed33e1d":
                return "c979e9eda7361555ce2d75d63f5305bb";
            // Ice-Path
            case "1d104fa1f828f60074dfc345dcf35032":
                return "ac807d34afef330d7275836795c6f734";
            // Miniboss Room
            case "2002014fb9fbaa0f896aaadc3854fef4":
            case "4c118368fc6f08b29ee18717999590bd":
            case "aeeeb0546987de22e3c4a45f45d546f9":
                return "569c63a07c6ebfe1153d0738f0e44731";
            // Redstone-Warrior-3
            case "0d25288e91b3380442576b0c0b23fa31":
                return "6d788f8bd2fb147f71d1afa6e010a7b8";
            // Sanctuary
            case "2cfcecf71825b76faa4f97787da2e996":
                return "263a269d5c93255c60eb721e128f2d20";
            // Trap-Very-Hard
            case "d076f0391db006f2282c52ec7c63d520":
                return "fe4b5561b73fb082acb80d904ec82294";

        }
        return md5;
    }

    public static BlockPos relativeToActual(BlockPos relative) {
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

    // Store a visited room in the static cache. Called after the room has been identified
    private static void addVisitedRoom(JsonObject roomJson) {
        if (roomJson == null) return;
        // Use the already‑computed bounds (roomMinX/Y etc.) to derive centre and size
        String name = roomJson.get("name").getAsString();
        String alias = getRoomAlias(roomJson, name);
        String hash = lastRoomHash;
        // Origin is the minimum corner at floor level
        BlockPos origin = new BlockPos(roomMinX, roomFloorY, roomMinZ);
        BlockPos center = new BlockPos((roomMinX + roomMaxX) / 2, roomFloorY, (roomMinZ + roomMaxZ) / 2);
        int width = Math.abs(roomMaxX - roomMinX) + 1;
        int height = Math.abs(roomMaxZ - roomMinZ) + 1;
        DungeonRoom dr = new DungeonRoom(name, alias, hash, origin, center, width, height);
        visitedRooms.putIfAbsent(roomKey(hash, origin), dr);
    }

    /**
     * Returns the currently detected dungeon room based on the latest detection.
     * Returns null if no room has been detected or bounds are invalid.
     */
    public static DungeonRoom getCurrentRoom() {
        if (lastRoomHash == null || lastRoomJson == null || !roomBoundsValid) return null;
        String name = lastRoomJson.get("name").getAsString();
        String alias = getRoomAlias(lastRoomJson, name);
        BlockPos origin = new BlockPos(roomMinX, roomFloorY, roomMinZ);
        BlockPos center = new BlockPos((roomMinX + roomMaxX) / 2, roomFloorY, (roomMinZ + roomMaxZ) / 2);
        int width = Math.abs(roomMaxX - roomMinX) + 1;
        int height = Math.abs(roomMaxZ - roomMinZ) + 1;
        return new DungeonRoom(name, alias, lastRoomHash, origin, center, width, height);
    }

    private static String roomKey(String hash, BlockPos origin) {
        return hash + "|" + origin.getX() + "," + origin.getZ();
    }

    private static String getRoomAlias(JsonObject roomJson, String name) {
        JsonElement aliasEl = roomJson.get("alias");
        return (aliasEl != null && aliasEl.isJsonPrimitive()) ? aliasEl.getAsString() : name;
    }

    // Helper to determine if a player is inside a given DungeonRoom
    private static boolean isPlayerInRoom(EntityPlayer player, DungeonRoom room) {
        int px = (int) Math.floor(player.posX);
        int pz = (int) Math.floor(player.posZ);
        int minX = room.origin.getX();
        int minZ = room.origin.getZ();
        int maxX = minX + room.width - 1;
        int maxZ = minZ + room.height - 1;
        return px >= minX && px <= maxX && pz >= minZ && pz <= maxZ;
    }

    /**
     * Scans all players and ensures known rooms are tracked. If a player is not inside any
     * visited room, attempts to add the current detected room for them.
     */
    public static void updateAllPlayerRooms() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalWorld() == null) return;
        for (EntityPlayer player : WorldCompat.getLoadedPlayers(MinecraftCompat.getLocalWorld())) {
            boolean known = false;
            for (DungeonRoom dr : visitedRooms.values()) {
                if (isPlayerInRoom(player, dr)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                DungeonRoom dr = getCurrentRoom();
                if (dr != null) {
                    visitedRooms.putIfAbsent(roomKey(dr.hash, dr.origin), dr);
                    if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled && secretLocationsJson != null) {
                        SecretRenderUtils.loadSecrets(dr.name, secretLocationsJson);
                    }
                }
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

    public static List<String> getSecretNamesForRoom(String roomName) {
        List<String> names = new ArrayList<>();
        if (secretLocationsJson == null || roomName == null || !secretLocationsJson.has(roomName)) return names;
        try {
            JsonArray secrets = secretLocationsJson.get(roomName).getAsJsonArray();
            for (int i = 0; i < secrets.size(); i++) {
                JsonObject secret = secrets.get(i).getAsJsonObject();
                if (secret.has("category") && "fairysoul".equals(secret.get("category").getAsString())) {
                    continue;
                }
                if (secret.has("secretName")) {
                    String secretName = secret.get("secretName").getAsString();
                    if (!names.contains(secretName)) {
                        names.add(secretName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }

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

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (ATHRConfig.feature == null) return;
        boolean overlayOn = ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlay;
            boolean sfOn = ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled;
                boolean dmOn = ATHRConfig.feature.dungeons.dungeonMapConfig.rooms.showVisitedRoomNames;
                boolean needOrigin = sfOn || ATHRConfig.feature.dungeons.dungeonMapConfig.enabled;

                if (!overlayOn && !sfOn && !dmOn && !ATHRConfig.feature.dungeons.dungeonMapConfig.enabled) {
            DungeonRoomOverlay.currentRoomName = null;
            DungeonRoomOverlay.currentRoomCategory = null;
            DungeonRoomOverlay.currentRoomNotes = null;
            DungeonRoomOverlay.currentRoomAlias = null;
            lastRoomHash = null;
            lastRoomJson = null;
            visitedRooms.clear();
            resetOrigin();
            return;
        }
        if (!needOrigin) {
            originBlock = null;
            originCorner = null;
            roomRotation = -1;
            playerRelX = Integer.MAX_VALUE;
            playerRelZ = Integer.MAX_VALUE;
        }
        if (!sfOn) {
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

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null || MinecraftCompat.getLocalWorld() == null) return;

        if (roomsJson == null) loadRoomsJson();
        if (roomsJson == null) return;

        executor.execute(() -> {
            try {
                dungeonTopCache.clear();
                dungeonBottomCache.clear();
                endOfRoomCache.clear();

                int x = (int) Math.floor(MinecraftCompat.getLocalPlayer().posX);
                int y = (int) Math.floor(MinecraftCompat.getLocalPlayer().posY);
                int z = (int) Math.floor(MinecraftCompat.getLocalPlayer().posZ);

                // If player is still inside the last detected room, skip full scan.
                // Fall through if secrets need origin detection (first tick after room enter clears it)
                if (roomBoundsValid && lastRoomHash != null && x >= roomMinX && x <= roomMaxX && z >= roomMinZ && z <= roomMaxZ
                    && !(needOrigin && originBlock == null)) {
                    if (dmOn) updateAllPlayerRooms();
                    if (needOrigin && originBlock != null && originCorner != null) {
                        BlockPos rel = actualToRelative(new BlockPos(x, y, z));
                        if (rel != null) {
                            playerRelX = rel.getX();
                            playerRelZ = rel.getZ();
                        }
                        if (sfOn) processSecrets();
                    }
                    return;
                }

                int top = dungeonTop(x, z);
                String blockFreq = blockFrequency(x, top, z);
                if (blockFreq == null) return;

                String md5 = getMD5(blockFreq);
                String floorFreq = floorFrequency(x, top, z);
                String floorHash = getMD5(floorFreq);

                if ("16370f79b2cad049096f881d5294aee6".equals(md5) && !"94fb12c91c4b46bd0c254edadaa49a3d".equals(floorHash)) {
                    floorHash = "e617eff1d7b77faf0f8dd53ec93a220f";
                }
                if (md5 == null) return;
                md5 = resolveMD5(md5);
                if (Objects.equals(md5, lastRoomHash) && lastRoomJson != null) {
                    JsonElement jfh = lastRoomJson.get("floorhash");
                    if (jfh == null || (floorHash != null && floorHash.equals(jfh.getAsString()))) {
                        computeRoomBounds(x, top, z);
                        addVisitedRoom(lastRoomJson);
                        updateAllPlayerRooms();
                        if (sfOn) processSecrets();
                        if (needOrigin && originBlock != null && originCorner != null) {
                            BlockPos rel = actualToRelative(new BlockPos(x, y, z));
                            if (rel != null) {
                                playerRelX = rel.getX();
                                playerRelZ = rel.getZ();
                            }
                        }
                        return;
                    }
                }

                if (needOrigin) {
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
                        DungeonRoomOverlay.currentRoomAlias = null;
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
                addVisitedRoom(lastRoomJson);
                updateAllPlayerRooms();
                if (sfOn) processSecrets();

                if (needOrigin && originBlock != null && originCorner != null) {
                    BlockPos rel = actualToRelative(new BlockPos(x, y, z));
                    if (rel != null) {
                        playerRelX = rel.getX();
                        playerRelZ = rel.getZ();
                    }
                    switch (originCorner) {
                        case "northwest":
                            roomRotation = 0;
                            break;
                        case "northeast":
                            roomRotation = 90;
                            break;
                        case "southeast":
                            roomRotation = 180;
                            break;
                        case "southwest":
                            roomRotation = 270;
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setOverlay(JsonObject room) {
        String name = room.get("name").getAsString();

        DungeonRoomOverlay.currentRoomCategory = room.get("category").getAsString();
        DungeonRoomOverlay.currentRoomName = name;
        DungeonRoomOverlay.currentRoomAlias = getRoomAlias(room, name);
        JsonElement notes = room.get("notes");
        DungeonRoomOverlay.currentRoomNotes = (notes != null) ? notes.getAsString() : null;
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
        roomCeilingY = y;
        roomFloorY = dungeonBottom(x, z);
        roomBoundsValid = true;
    }

    private void checkCorner(BlockPos blockPos) {
        World world = MinecraftCompat.getLocalWorld();
        if (world == null) return;
        if (BlockCompat.isStainedHardenedClay(world.getBlockState(blockPos).getBlock())) {
            Block northBlock = world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() - 1)).getBlock();
            Block southBlock = world.getBlockState(new BlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ() + 1)).getBlock();
            Block eastBlock = world.getBlockState(new BlockPos(blockPos.getX() + 1, blockPos.getY(), blockPos.getZ())).getBlock();
            Block westBlock = world.getBlockState(new BlockPos(blockPos.getX() - 1, blockPos.getY(), blockPos.getZ())).getBlock();
            if (BlockCompat.isAir(northBlock) && !BlockCompat.isAir(southBlock) && !BlockCompat.isAir(eastBlock) && BlockCompat.isAir(westBlock)) {
                originCorner = "northwest";
                originBlock = blockPos;
            } else if (BlockCompat.isAir(northBlock) && !BlockCompat.isAir(southBlock) && BlockCompat.isAir(eastBlock) && !BlockCompat.isAir(westBlock)) {
                originCorner = "northeast";
                originBlock = blockPos;
            } else if (!BlockCompat.isAir(northBlock) && BlockCompat.isAir(southBlock) && BlockCompat.isAir(eastBlock) && !BlockCompat.isAir(westBlock)) {
                originCorner = "southeast";
                originBlock = blockPos;
            } else if (!BlockCompat.isAir(northBlock) && BlockCompat.isAir(southBlock) && !BlockCompat.isAir(eastBlock) && BlockCompat.isAir(westBlock)) {
                originCorner = "southwest";
                originBlock = blockPos;
            }
        }
    }

    private void loadSecretLocationsJson() {
        secretLocationsJson = RepoHandler.get(ATHRRepo.KEY_SECRETLOCATIONS, JsonObject.class, null);
    }

    private void processSecrets() {
        if (secretLocationsJson == null) loadSecretLocationsJson();
        if (secretLocationsJson == null) {
            displayedSecretCount = -1;
            return;
        }
        if (originBlock == null || originCorner == null) {
            displayedSecretCount = -1;
            return;
        }
        if (lastRoomHash == null) {
            displayedSecretCount = -1;
            return;
        }

        String roomName = DungeonRoomOverlay.currentRoomName;
        if (roomName == null) {
            displayedSecretCount = -1;
            return;
        }

        String cacheKey = lastRoomHash + "|" + originBlock.getX() + "," + originBlock.getZ();
        if (!loadedSecretKeys.contains(cacheKey)) {
            loadedSecretKeys.clear();
            loadedSecretKeys.add(cacheKey);
            SecretRenderUtils.loadSecrets(roomName, secretLocationsJson);
            displayedSecretCount = SecretRenderUtils.getActiveSecretCount();
        }
    }

    private void loadRoomsJson() {
        roomsJson = RepoHandler.get(ATHRRepo.KEY_DUNGEONROOMS, JsonObject.class, null);
    }

    private int dungeonTop(int x, int z) {
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        Integer cached = dungeonTopCache.get(key);
        if (cached != null) return cached;
        World world = MinecraftCompat.getLocalWorld();
        for (int i = 255; i >= 78; i--) {
            Block b = world.getBlockState(new BlockPos(x, i, z)).getBlock();
            if (!BlockCompat.isAir(b) && checkPlatform(x, i, z)) {
                dungeonTopCache.put(key, i);
                return i;
            }
        }
        dungeonTopCache.put(key, -1);
        return -1;
    }

    private int dungeonBottom(int x, int z) {
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        Integer cached = dungeonBottomCache.get(key);
        if (cached != null) return cached;
        World world = MinecraftCompat.getLocalWorld();
        for (int i = 0; i <= 68; i++) {
            Block b = world.getBlockState(new BlockPos(x, i, z)).getBlock();
            if (b == Blocks.bedrock || b == Blocks.stone) {
                dungeonBottomCache.put(key, i);
                return i;
            }
        }
        dungeonBottomCache.put(key, -1);
        return -1;
    }

    private int dungeonHeight(int x, int z) {
        return dungeonTop(x, z) - dungeonBottom(x, z);
    }

    private boolean checkPlatform(int x, int y, int z) {
        World world = MinecraftCompat.getLocalWorld();
        int n = 0, s = 0, e = 0, w = 0;
        for (int j = 0; j < 10; j++) {
            if (!BlockCompat.isAir(world.getBlockState(new BlockPos(x, y, z - j)).getBlock())) n++;
            if (!BlockCompat.isAir(world.getBlockState(new BlockPos(x, y, z + j)).getBlock())) s++;
            if (!BlockCompat.isAir(world.getBlockState(new BlockPos(x + j, y, z)).getBlock())) e++;
            if (!BlockCompat.isAir(world.getBlockState(new BlockPos(x - j, y, z)).getBlock())) w++;
        }
        return (n == 10 || s == 10 || e == 10 || w == 10);
    }

    private int endOfRoom(int x, int y, int z, String dir) {
        String key = x + "," + y + "," + z + "," + dir;
        Integer cached = endOfRoomCache.get(key);
        if (cached != null) return cached;
        World world = MinecraftCompat.getLocalWorld();
        int result = -1;
        for (int i = 1; i <= 200; i++) {
            switch (dir) {
                case "n":
                    if (BlockCompat.isAir(world.getBlockState(new BlockPos(x, y, z - i)).getBlock()) || checkPlatform(x, y + 1, z - i) || Math.abs(dungeonHeight(x, z - i) - dungeonHeight(x, z - i + 1)) > 3) {
                        result = z - i + 1;
                        endOfRoomCache.put(key, result);
                        return result;
                    }
                    break;
                case "s":
                    if (BlockCompat.isAir(world.getBlockState(new BlockPos(x, y, z + i)).getBlock()) || checkPlatform(x, y + 1, z + i) || Math.abs(dungeonHeight(x, z + i) - dungeonHeight(x, z + i - 1)) > 3) {
                        result = z + i - 1;
                        endOfRoomCache.put(key, result);
                        return result;
                    }
                    break;
                case "e":
                    if (BlockCompat.isAir(world.getBlockState(new BlockPos(x + i, y, z)).getBlock()) || checkPlatform(x + i, y + 1, z) || Math.abs(dungeonHeight(x + i, z) - dungeonHeight(x + i - 1, z)) > 3) {
                        result = x + i - 1;
                        endOfRoomCache.put(key, result);
                        return result;
                    }
                    break;
                case "w":
                    if (BlockCompat.isAir(world.getBlockState(new BlockPos(x - i, y, z)).getBlock()) || checkPlatform(x - i, y + 1, z) || Math.abs(dungeonHeight(x - i, z) - dungeonHeight(x - i + 1, z)) > 3) {
                        result = x - i + 1;
                        endOfRoomCache.put(key, result);
                        return result;
                    }
                    break;
            }
        }
        endOfRoomCache.put(key, -1);
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
        World world = MinecraftCompat.getLocalWorld();
        Map<String, Integer> freqMap = new HashMap<>();

        int nw = northWidth(x, y, z), sw = southWidth(x, y, z), ew = eastWidth(x, y, z), ww = westWidth(x, y, z);

        if (nw == sw && ew == ww) {
            int nz = endOfRoom(x, y, z, "n"), nwx = endOfRoom(x, y, nz, "w");
            int sz = endOfRoom(x, y, z, "s"), sex = endOfRoom(x, y, sz, "e");
            for (BlockPos bp : BlockPos.getAllInBox(new BlockPos(nwx, y, nz), new BlockPos(sex, y, sz))) {
                if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled || ATHRConfig.feature.dungeons.dungeonMapConfig.enabled) checkCorner(bp);
                freqMap.merge(world.getBlockState(bp).toString(), 1, Integer::sum);
            }
        } else if (getSize(x, y, z).equals("L-shape")) {
            if (nw == sw) {
                int startX = ew > ww ? endOfRoom(x, y, z, "e") : endOfRoom(x, y, z, "w");
                int nz = endOfRoom(startX, y, z, "n");
                int dx = ew > ww ? -1 : 1;
                for (int i = 0; i < 200; i++) {
                    int cz = nz + i;
                    if (BlockCompat.isAir(world.getBlockState(new BlockPos(startX, y, cz)).getBlock()) || checkPlatform(startX, y + 1, cz) || (i > 0 && Math.abs(dungeonHeight(startX, cz) - dungeonHeight(startX, cz - 1)) > 3))
                        break;
                    for (int j = 0; j < 200; j++) {
                        BlockPos bp = new BlockPos(startX + dx * j, y, cz);
                        Block b = world.getBlockState(bp).getBlock();
                        if (BlockCompat.isAir(b) || checkPlatform(startX + dx * j, y + 1, cz) || (j > 0 && Math.abs(dungeonHeight(startX + dx * j, cz) - dungeonHeight(startX + dx * (j - 1), cz)) > 3))
                            break;
                        if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled || ATHRConfig.feature.dungeons.dungeonMapConfig.enabled) checkCorner(bp);
                        freqMap.merge(b.toString(), 1, Integer::sum);
                    }
                }
            } else {
                int startZ = nw > sw ? endOfRoom(x, y, z, "n") : endOfRoom(x, y, z, "s");
                int wx = endOfRoom(x, y, startZ, "w");
                int dz = nw > sw ? 1 : -1;
                for (int i = 0; i < 200; i++) {
                    int cx = wx + i;
                    if (BlockCompat.isAir(world.getBlockState(new BlockPos(cx, y, startZ)).getBlock()) || checkPlatform(cx, y + 1, startZ) || (i > 0 && Math.abs(dungeonHeight(cx, startZ) - dungeonHeight(cx - 1, startZ)) > 3))
                        break;
                    for (int j = 0; j < 200; j++) {
                        BlockPos bp = new BlockPos(cx, y, startZ + dz * j);
                        Block b = world.getBlockState(bp).getBlock();
                        if (BlockCompat.isAir(b) || checkPlatform(cx, y + 1, startZ + dz * j) || (j > 0 && Math.abs(dungeonHeight(cx, startZ + dz * j) - dungeonHeight(cx, startZ + dz * (j - 1))) > 3))
                            break;
                        if (ATHRConfig.feature.dungeons.dungeonSecretFinder.enabled || ATHRConfig.feature.dungeons.dungeonMapConfig.enabled) checkCorner(bp);
                        freqMap.merge(b.toString(), 1, Integer::sum);
                    }
                }
            }
        }

        if (freqMap.isEmpty()) return null;
        List<String> freqs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : freqMap.entrySet()) freqs.add(e.getKey() + ":" + e.getValue());
        Collections.sort(freqs);
        return String.join(",", freqs);
    }

    private String floorFrequency(int x, int y, int z) {
        if (y == -1) return null;
        World world = MinecraftCompat.getLocalWorld();
        Map<String, Integer> freqMap = new HashMap<>();

        if (northWidth(x, y, z) == southWidth(x, y, z) && eastWidth(x, y, z) == westWidth(x, y, z)) {
            int nz = endOfRoom(x, y, z, "n"), nwx = endOfRoom(x, y, nz, "w");
            int sz = endOfRoom(x, y, z, "s"), sex = endOfRoom(x, y, sz, "e");
            for (BlockPos bp : BlockPos.getAllInBox(new BlockPos(nwx + 10, 68, nz + 10), new BlockPos(sex - 10, 68, sz - 10)))
                freqMap.merge(world.getBlockState(bp).getBlock().toString(), 1, Integer::sum);
        }
        if (getSize(x, y, z).equals("L-shape")) freqMap.merge(String.valueOf(dungeonTop(x, z)), 1, Integer::sum);

        if (freqMap.isEmpty()) return null;
        List<String> freqs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : freqMap.entrySet()) freqs.add(e.getKey() + ":" + e.getValue());
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

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        resetOrigin();
        DungeonMapOverlay.clearPlayers();

    }

    @HandleEvent
    public void onRenderWorld(ASMRenderWorldEvent event) {
        if (!roomBoundsValid || roomMinX == Integer.MAX_VALUE) return;
        if (DungeonRoomOverlay.currentRoomName == null) return;
        if (roomCeilingY <= 0 || roomFloorY < 0) return;

        float tracerWidth = ATHRConfig.feature != null && ATHRConfig.feature.dungeons.dungeonSecretFinder != null ? ATHRConfig.feature.dungeons.dungeonSecretFinder.other.tracerWidth : 2.0f;

        if (ATHRConfig.feature != null && ATHRConfig.feature.debug.dungeonRoomHighlight) {
            WorldRenderUtils.drawSelectionBox(new AxisAlignedBB(roomMinX, roomFloorY, roomMinZ, roomMaxX + 1, roomCeilingY + 1, roomMaxZ + 1), new Color(0, 200, 255, 120), tracerWidth);

            if (originBlock != null) {
                double vx = MinecraftCompat.getMinecraft().getRenderManager().viewerPosX;
                double vy = MinecraftCompat.getMinecraft().getRenderManager().viewerPosY;
                double vz = MinecraftCompat.getMinecraft().getRenderManager().viewerPosZ;

                drawEspBoxTranslated(originBlock.getX(), originBlock.getY(), originBlock.getZ(), new Color(180, 0, 255, 200), vx, vy, vz, tracerWidth);
            }
        }

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