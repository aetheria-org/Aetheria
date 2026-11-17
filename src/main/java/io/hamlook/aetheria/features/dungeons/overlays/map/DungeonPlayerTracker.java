package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.WorldCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
//#if MC < 1.21
import net.minecraft.util.Vec4b;
//#else
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.util.DefaultSkinHelper;
//$$ import net.minecraft.item.map.MapDecoration;
//$$ import net.minecraft.registry.Registries;
//#endif

import java.util.*;

public class DungeonPlayerTracker {

    @Getter
    public final List<String> playerNames = new ArrayList<>();
    private final List<EntityPlayer> players = new ArrayList<>();
    private final Map<String, float[]> currentPositions = new HashMap<>();
    //#if MC < 1.21
    private final List<Vec4b> decorationBuffer = new ArrayList<>();
    //#else
    //$$ private final List<MapDecoration> decorationBuffer = new ArrayList<>();
    //#endif

    private static boolean isPlausibleUsername(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) return false;
        boolean hasLetter = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) return false;
            if (Character.isLetter(c)) hasLetter = true;
        }
        return hasLetter;
    }

    public void clear() {
        players.clear();
        playerNames.clear();
        currentPositions.clear();
    }

    public void populate() {
        players.clear();
        playerNames.clear();

        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null || MinecraftCompat.getLocalWorld() == null) return;

        if (!isRunStarted()) return;

        String selfName = TextCompat.plainString(MinecraftCompat.getLocalPlayer().getName());
        playerNames.add(selfName);
        players.add(MinecraftCompat.getLocalPlayer());

        for (String username : getOrderedPartyUsernames()) {
            if (username.equalsIgnoreCase(selfName) || playerNames.contains(username)) continue;
            playerNames.add(username);
            players.add(WorldCompat.getPlayerByName(MinecraftCompat.getLocalWorld(), username));
        }
    }

    private boolean isRunStarted() {
        for (String line : SkyblockData.getCleanScoreboardLines()) {
            if (line.startsWith("Time Elapsed")) return true;
        }
        return false;
    }

    /**
     * Cross-references Tab List players against Scoreboard lines, in scoreboard
     * line order (the same order the server writes map decorations). Scoreboard
     * names can be cut off, so a full tab name is matched
     * against each scoreboard line. The isPlausibleUsername guard stops Skyblock tab
     * garbage contents from matching random scoreboard
     * contents like "Time Elapsed: 03:18s"
     */
    private List<String> getOrderedPartyUsernames() {
        List<String> members = new ArrayList<>();
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.getNetHandler() == null) return members;

        Collection<NetworkPlayerInfo> tabList = MinecraftCompat.getPlayerListEntries();
        List<String> scoreboardLines = SkyblockData.getCleanScoreboardLines();

        for (String line : scoreboardLines) {
            if (line.isEmpty()) continue;

            for (NetworkPlayerInfo info : tabList) {
                if (info == null || info.getGameProfile() == null) continue;
                String tabName = info.getGameProfile().getName();

                if (!isPlausibleUsername(tabName)) continue;

                if (line.contains(tabName) || (tabName.length() > 12 && line.contains(tabName.substring(0, 12)))) {
                    if (!members.contains(tabName)) {
                        members.add(tabName);
                    }
                }
            }
        }
        return members;
    }

    /**
     * Matches map decoration bytes to tracked players by index. Decorations carry
     * ust icon type + x/z + rotation nibble so they can only be
     * bound to usernames positionally. Decoration order == scoreboard/tablist order
     * (getOrderedPartyUsernames preserves it), so decoration lands on
     * playerNames[i]. Self's own marker (icon type 1) is located by index and kept
     * anchored to it: a later entity-position override for self (accurateSelfPosition)
     * only changes the drawn coordinates of that anchored slot, so it can never be
     * mis-assigned to a teammate.
     */
    //#if MC < 1.21
    public void matchDecorations(Map<String, Vec4b> mapDecorations) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (MinecraftCompat.getLocalPlayer() == null || playerNames.isEmpty()) return;
        if (mapDecorations == null || mapDecorations.isEmpty()) return;

        currentPositions.clear();

        decorationBuffer.clear();
        decorationBuffer.addAll(mapDecorations.values());
        List<Vec4b> decorations = decorationBuffer;

        // Self's own marker (local player = icon type 1), normally decorations[0].
        int selfDecoIndex = 0;
        for (int i = 0; i < decorations.size(); i++) {
            if (decorations.get(i).func_176110_a() == 1) {
                selfDecoIndex = i;
                break;
            }
        }

        int decoIndex = 0;
        for (String playerName : playerNames) {
            if (decoIndex >= decorations.size()) break;

            // Anchor self to its own decoration slot (swap only when the server
            // places it somewhere other than index 0).
            int slot = decoIndex;
            if (decoIndex == 0 && selfDecoIndex > 0) {
                slot = selfDecoIndex;
            } else if (decoIndex == selfDecoIndex && selfDecoIndex > 0) {
                slot = 0;
            }

            Vec4b deco = decorations.get(slot);
            decoIndex++;
            byte type = deco.func_176110_a();

            // Accept standard map marker types (0 = White, 1 = Green, 3 = Head)
            if (type != 0 && type != 1 && type != 3) continue;

            float x = (float) deco.func_176112_b() / 2.0F + 64.0F;
            float z = (float) deco.func_176113_c() / 2.0F + 64.0F;
            // Vanilla rotates decorations at rot*360/16 + 180; store the
            // render-ready angle so consumers can pass it straight through.
            float yaw = (float) (deco.func_176111_d() * 360) / 16.0F + 180F;

            currentPositions.put(playerName, new float[]{x, z, yaw});
        }
    }
    //#else
    //$$ public void matchDecorations(Iterable<MapDecoration> mapDecorations) {
    //$$     matchDecorationsModern(mapDecorations);
    //$$ }
    //#endif

    //#if MC < 1.21
    //#else
    //$$ private void matchDecorationsModern(Iterable<MapDecoration> mapDecorations) {
    //$$     MinecraftClient mc = MinecraftCompat.getMinecraft();
    //$$     if (MinecraftCompat.getLocalPlayer() == null || playerNames.isEmpty()) return;
    //$$     if (mapDecorations == null) return;
    //$$
    //$$     currentPositions.clear();
    //$$
    //$$     decorationBuffer.clear();
    //$$     for (MapDecoration deco : mapDecorations) decorationBuffer.add(deco);
    //$$     List<MapDecoration> decorations = decorationBuffer;
    //$$
    //$$     int selfDecoIndex = 0;
    //$$     for (int i = 0; i < decorations.size(); i++) {
    //$$         if (decoType(decorations.get(i)) == 1) {
    //$$             selfDecoIndex = i;
    //$$             break;
    //$$         }
    //$$     }
    //$$
    //$$     int decoIndex = 0;
    //$$     for (String playerName : playerNames) {
    //$$         if (decoIndex >= decorations.size()) break;
    //$$
    //$$         int slot = decoIndex;
    //$$         if (decoIndex == 0 && selfDecoIndex > 0) {
    //$$             slot = selfDecoIndex;
    //$$         } else if (decoIndex == selfDecoIndex && selfDecoIndex > 0) {
    //$$             slot = 0;
    //$$         }
    //$$
    //$$         MapDecoration deco = decorations.get(slot);
    //$$         decoIndex++;
    //$$         int type = decoType(deco);
    //$$
    //$$         if (type != 0 && type != 1 && type != 3) continue;
    //$$
    //$$         float x = (float) deco.x() / 2.0F + 64.0F;
    //$$         float z = (float) deco.z() / 2.0F + 64.0F;
    //$$         float yaw = (float) (deco.rotation() * 360) / 16.0F + 180F;
    //$$
    //$$         currentPositions.put(playerName, new float[]{x, z, yaw});
    //$$     }
    //$$ }
    //$$
    //$$ private static int decoType(MapDecoration deco) {
    //$$     return Registries.MAP_DECORATION_TYPE.getRawId(deco.type().value());
    //$$ }
    //#endif

    public float[] getPosition(String name) {
        return currentPositions.get(name);
    }

    public EntityPlayer getEntity(String name) {
        for (int i = 0; i < playerNames.size(); i++) {
            if (playerNames.get(i).equals(name) && i < players.size()) {
                return players.get(i);
            }
        }
        return null;
    }

    /** Final map position per player; null when the player has no position. */
    public static final class MarkerState {
        public final float px;
        public final float pz;
        public final float yaw;
        public final EntityPlayer entity;
        public final boolean isSelf;

        private MarkerState(float px, float pz, float yaw, EntityPlayer entity, boolean isSelf) {
            this.px = px;
            this.pz = pz;
            this.yaw = yaw;
            this.entity = entity;
            this.isSelf = isSelf;
        }
    }

    /** Decoration by default; live entity for self when calibrated and enabled. */
    public MarkerState getMarker(String name, String selfName, DungeonMapGrid grid, boolean mapCalibrated, boolean accurateSelfPosition) {
        EntityPlayer entity = getEntity(name);
        float[] pos = getPosition(name);
        if (pos == null) return null;
        float px = pos[0];
        float pz = pos[1];
        float yaw = pos[2];
        boolean isSelf = selfName != null && name.equalsIgnoreCase(selfName);
        // Stored entity can be stale post-respawn; revalidate liveness live.
        if (isSelf) {
            EntityPlayer live = MinecraftCompat.getLocalPlayer();
            if (live != null) entity = live;
        }
        // Uncalibrated runs draw self from its decoration until the latch completes.
        if (isSelf && mapCalibrated && accurateSelfPosition && entity != null && !EntityCompat.isDead(entity)) {
            px = grid.worldToPixelX(EntityCompat.getPosX(entity));
            pz = grid.worldToPixelZ(EntityCompat.getPosZ(entity));
            yaw = EntityCompat.getYaw(entity) + 180f;
        }
        return new MarkerState(px, pz, yaw, entity, isSelf);
    }

    public NetworkPlayerInfo getNetworkPlayerInfo(String name, Minecraft mc) {
        if (MinecraftCompat.getLocalPlayer() == null) return null;
        Collection<NetworkPlayerInfo> infos;
        try {
            infos = MinecraftCompat.getPlayerListEntries();
        } catch (Exception e) {
            return null;
        }
        for (NetworkPlayerInfo info : infos) {
            if (info.getGameProfile().getName().equalsIgnoreCase(name)) {
                return info;
            }
        }
        return null;
    }

    public ResourceLocation resolveSkin(String name, Minecraft mc) {
        EntityPlayer entity = getEntity(name);
        NetworkPlayerInfo info = null;
        if (entity != null) {
            //#if MC < 1.21
            info = mc.getNetHandler().getPlayerInfo(entity.getUniqueID());
            //#else
            //$$ info = MinecraftCompat.getMinecraft().getNetworkHandler().getPlayerListEntry(EntityCompat.getUniqueId(entity));
            //#endif
        }
        if (info == null) {
            info = getNetworkPlayerInfo(name, mc);
        }
        //#if MC < 1.21
        return (info != null && info.getLocationSkin() != null) ? info.getLocationSkin() : DefaultPlayerSkin.getDefaultSkinLegacy();
        //#else
        //$$ return (info != null && info.getSkinTextures().texture() != null) ? info.getSkinTextures().texture() : DefaultSkinHelper.getTexture();
        //#endif
    }
}