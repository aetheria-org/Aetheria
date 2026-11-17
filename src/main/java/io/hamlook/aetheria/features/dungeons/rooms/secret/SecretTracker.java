package io.hamlook.aetheria.features.dungeons.rooms.secret;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.features.dungeons.rooms.DungeonRoomDetector;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class SecretTracker {

    public enum CounterResult {
        IGNORED, BASELINE, INCREMENT
    }

    private static final int ROOM_LEAVE_TICKS = 10;

    // every room seen this run, so walking back in reuses its waypoints and collected state
    private static final Map<String, SecretRoom> rooms = new ConcurrentHashMap<>();

    private static final Set<String> clearedAnnounced = ConcurrentHashMap.newKeySet();
    private static final Set<String> secretsAnnounced = ConcurrentHashMap.newKeySet();

    private static volatile SecretRoom current;
    private static volatile int found;
    private static volatile int total;
    private static volatile String counterRoom;
    private static int counterFound;
    private static int counterTotal;
    private static int outsideTicks;
    private static double lastX = Double.NaN;
    private static double lastZ = Double.NaN;

    private SecretTracker() {}

    public static void onRoomChanged(String name, String key) {
        if (key == null) return;
        JsonObject json = locations();
        SecretRoom room;
        synchronized (rooms) {
            room = rooms.get(key);
            if (room == null) {
                room = new SecretRoom(key, load(name, json), expectedSecrets(name));
                if (json != null) rooms.put(key, room);
            }
        }
        if (DungeonRoomDetector.roomBoundsValid) {
            room.setBounds(DungeonRoomDetector.roomMinX, DungeonRoomDetector.roomMinZ,
                    DungeonRoomDetector.roomMaxX, DungeonRoomDetector.roomMaxZ);
        }
        if (!room.hasBounds() || Double.isNaN(lastX) || room.contains(lastX, lastZ)) switchTo(room);
    }

    public static void activateAt(double x, double z) {
        lastX = x;
        lastZ = z;

        SecretRoom room = current;
        if (room != null && room.contains(x, z)) {
            outsideTicks = 0;
            return;
        }
        for (SecretRoom cached : rooms.values()) {
            if (cached.contains(x, z)) {
                switchTo(cached);
                return;
            }
        }
        if (room == null || !room.hasBounds()) return;
        if (++outsideTicks < ROOM_LEAVE_TICKS) return;
        switchTo(null);
    }

    private static void switchTo(SecretRoom room) {
        if (room == current) return;
        current = room;
        outsideTicks = 0;
        found = 0;
        total = 0;
        counterRoom = null;
        counterFound = 0;
        counterTotal = 0;
    }

    public static void clear() {
        rooms.clear();
        clearedAnnounced.clear();
        secretsAnnounced.clear();
        current = null;
        outsideTicks = 0;
        lastX = Double.NaN;
        lastZ = Double.NaN;
        found = 0;
        total = 0;
        counterRoom = null;
        counterFound = 0;
        counterTotal = 0;
    }

    public static SecretRoom current() {
        return current;
    }

    public static List<SecretWaypoint> secrets() {
        SecretRoom room = current;
        return room == null ? Collections.emptyList() : room.secrets();
    }

    public static boolean isLoaded() {
        return current != null;
    }

    public static boolean allSecretsCleared() {
        SecretRoom room = current;
        return room != null && room.allSecretsCleared();
    }

    // latest map reading wins, so a wrong tick corrects itself instead of sticking
    public static boolean updateSecretsCleared(SecretRoom room, boolean allSecretsCleared) {
        if (room == null) return false;
        room.setAllSecretsCleared(allSecretsCleared);
        return allSecretsCleared && secretsAnnounced.add(room.key());
    }

    public static boolean updateCleared(SecretRoom room, boolean cleared) {
        if (room == null) return false;
        return cleared && clearedAnnounced.add(room.key());
    }

    public static CounterResult acceptCounters(int foundCount, int totalCount) {
        found = foundCount;
        total = totalCount;

        SecretRoom room = current;
        if (room == null) return CounterResult.IGNORED;

        int bound = Math.max(room.secretCount(), room.expectedSecrets());
        if (bound <= 0 || totalCount > bound) return CounterResult.IGNORED;

        if (!room.key().equals(counterRoom) || totalCount != counterTotal || foundCount < counterFound) {
            adopt(room.key(), foundCount, totalCount);
            return CounterResult.BASELINE;
        }
        if (foundCount == counterFound) return CounterResult.IGNORED;

        adopt(room.key(), foundCount, totalCount);
        return CounterResult.INCREMENT;
    }

    private static void adopt(String key, int foundCount, int totalCount) {
        counterRoom = key;
        counterFound = foundCount;
        counterTotal = totalCount;
    }

    public static int found() {
        return found;
    }

    public static int total() {
        return total;
    }

    public static List<String> secretNamesForRoom(String name) {
        Set<String> names = new LinkedHashSet<>();
        forEachEntry(name, locations(), (category, entry) -> names.add(entry.get("secretName").getAsString()));
        return new ArrayList<>(names);
    }

    private static int expectedSecrets(String name) {
        JsonObject json = RepoHandler.get(ATHRRepo.KEY_DUNGEONROOMS, JsonObject.class, null);
        if (json == null || name == null) return 0;

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonArray()) continue;
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject room = element.getAsJsonObject();
                JsonElement roomName = room.get("name");
                if (roomName == null || !name.equals(roomName.getAsString())) continue;
                JsonElement secrets = room.get("secrets");
                return secrets == null ? 0 : secrets.getAsInt();
            }
        }
        return 0;
    }

    private static JsonObject locations() {
        return RepoHandler.get(ATHRRepo.KEY_SECRETLOCATIONS, JsonObject.class, null);
    }

    private static List<SecretWaypoint> load(String name, JsonObject json) {
        List<SecretWaypoint> loaded = new ArrayList<>();
        forEachEntry(name, json, (category, entry) -> {
            BlockPos pos = DungeonRoomDetector.relativeToActual(new BlockPos(
                    entry.get("x").getAsInt(),
                    entry.get("y").getAsInt(),
                    entry.get("z").getAsInt()));
            if (pos == null) return;
            loaded.add(new SecretWaypoint(category, entry.get("secretName").getAsString(), pos));
        });
        return Collections.unmodifiableList(loaded);
    }

    private static void forEachEntry(String name, JsonObject json, BiConsumer<SecretCategory, JsonObject> consumer) {
        if (json == null || name == null || !json.has(name)) return;

        JsonArray array;
        try {
            array = json.getAsJsonArray(name);
        } catch (Exception e) {
            Aetheria.logger.warning("Secret data for room " + name + " is not an array: " + e);
            return;
        }

        for (JsonElement element : array) {
            try {
                JsonObject entry = element.getAsJsonObject();
                SecretCategory category = SecretCategory.of(entry.get("category").getAsString());
                if (category == SecretCategory.FAIRYSOUL || category == SecretCategory.UNKNOWN) continue;
                consumer.accept(category, entry);
            } catch (Exception e) {
                Aetheria.logger.warning("Skipped malformed secret entry for room " + name + ": " + e);
            }
        }
    }
}
