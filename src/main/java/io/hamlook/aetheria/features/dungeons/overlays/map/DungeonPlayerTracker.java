package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.utils.data.SkyblockData;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec4b;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DungeonPlayerTracker {

    private final List<EntityPlayer> players = new ArrayList<>();
    @Getter
    public final List<String> playerNames = new ArrayList<>();

    private final Map<String, float[]> currentPositions = new HashMap<>();

    public void clear() {
        players.clear();
        playerNames.clear();
        currentPositions.clear();
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

    /**
     * Cross-references Tab List players against Scoreboard lines.
     * Prevents scoreboard truncation bugs and random word parsing.
     */
    private List<String> getOrderedPartyUsernames() {
        List<String> members = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return members;

        Collection<NetworkPlayerInfo> tabList = mc.getNetHandler().getPlayerInfoMap();
        List<String> scoreboardLines = SkyblockData.getCleanScoreboardLines();

        for (String line : scoreboardLines) {
            if (line.isEmpty()) continue;

            for (NetworkPlayerInfo info : tabList) {
                if (info == null || info.getGameProfile() == null) continue;
                String tabName = info.getGameProfile().getName();

                // Match tab name (or truncated tab name) inside the scoreboard line
                if (line.contains(tabName) || (tabName.length() > 12 && line.contains(tabName.substring(0, 12)))) {
                    if (!members.contains(tabName)) {
                        members.add(tabName);
                    }
                }
            }
        }
        return members;
    }

    public void matchDecorations(Map<String, Vec4b> mapDecorations) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || playerNames.isEmpty()) return;
        if (mapDecorations == null || mapDecorations.isEmpty()) return;

        currentPositions.clear();

        // Include ALL players in index order (Index 0 = Local Player, Index 1+ = Teammates)
        List<Vec4b> decorations = new ArrayList<>(mapDecorations.values());

        int decoIndex = 0;
        for (String playerName : playerNames) {
            if (decoIndex >= decorations.size()) break;

            Vec4b deco = decorations.get(decoIndex++);
            byte type = deco.func_176110_a();

            // Accept standard map marker types (0 = White, 1 = Green, 3 = Head)
            if (type != 0 && type != 1 && type != 3) continue;

            float x = (float) deco.func_176112_b() / 2.0F + 64.0F;
            float z = (float) deco.func_176113_c() / 2.0F + 64.0F;
            float yaw = (float) (deco.func_176111_d() * 360) / 16.0F;

            currentPositions.put(playerName, new float[]{x, z, yaw});
        }
    }

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