package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DungeonPlayerTracker {

    private final List<EntityPlayer> players = new ArrayList<>();
    @Getter
    public final List<String> playerNames = new ArrayList<>();

    public void clear() {
        players.clear();
        playerNames.clear();
    }

    public void populate() {
        players.clear();
        playerNames.clear();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!isRunStarted()) return;

        String selfName = mc.thePlayer.getName();
        playerNames.add(selfName);
        players.add(mc.thePlayer);

        for (String username : getOrderedPartyUsernames()) {
            if (username.equalsIgnoreCase(selfName) || playerNames.contains(username)) continue;
            playerNames.add(username);
            players.add(mc.theWorld.getPlayerEntityByName(username));
        }
    }

    private boolean isRunStarted() {
        for (String line : SkyblockData.getCleanScoreboardLines()) {
            if (line.startsWith("Time Elapsed")) return true;
        }
        return false;
    }

    private List<String> getOrderedPartyUsernames() {
        List<String> members = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return members;

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null) continue;
            String tabName = info.getGameProfile().getName();
            if (isPlausibleUsername(tabName) && !members.contains(tabName)) {
                members.add(tabName);
            }
        }
        return members;
    }

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

    /**
     * Resolves a tracked player's marker position in the dungeon map's local pixel
     * space, using their actual world position/yaw rather than the map's own
     * decoration bytes (which carry no name/UUID field — just icon type + x/z +
     * rotation nibble, and so can't be reliably bound to a specific username; a
     * prior version of this class matched them to playerNames positionally, which
     * silently swapped self/teammate identity whenever the decoration map's
     * iteration order didn't match party order — i.e. constantly).
     * Returns null if the entity isn't currently loaded (e.g. out of render distance).
     */
    public float[] getPixelPosition(String name, DungeonMapGrid grid) {
        EntityPlayer entity = getEntity(name);
        if (entity == null || entity.isDead) return null;
        float px = grid.worldToPixelX(entity.posX);
        float pz = grid.worldToPixelZ(entity.posZ);
        return new float[]{px, pz, entity.rotationYaw};
    }

    public EntityPlayer getEntity(String name) {
        for (int i = 0; i < playerNames.size(); i++) {
            if (playerNames.get(i).equals(name) && i < players.size()) {
                return players.get(i);
            }
        }
        return null;
    }

    public NetworkPlayerInfo getNetworkPlayerInfo(String name, Minecraft mc) {
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return null;
        Collection<NetworkPlayerInfo> infos;
        try {
            infos = mc.thePlayer.sendQueue.getPlayerInfoMap();
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
}