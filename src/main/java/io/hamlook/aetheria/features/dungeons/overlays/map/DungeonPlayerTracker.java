package io.hamlook.aetheria.features.dungeons.overlays.map;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec4b;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonPlayerTracker {

    public static final Pattern PLAYER_REGEX = Pattern.compile("^\\[\\d+]\\s+(?:\\[[^]]+]\\s+)?(\\w+)");

    private final List<EntityPlayer> players = new ArrayList<>();
    @Getter
    public final List<String> playerNames = new ArrayList<>();

    private final Map<String, float[]> currentPositions = new HashMap<>();
    private final Map<String, float[]> lastPositions = new HashMap<>();
    private boolean matched = false;

    public void clear() {
        players.clear();
        playerNames.clear();
        currentPositions.clear();
        lastPositions.clear();
        matched = false;
    }

    public void populate() {
        players.clear();
        playerNames.clear();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> infos;
        try {
            infos = mc.thePlayer.sendQueue.getPlayerInfoMap().stream().sorted((a, b) -> {
                String ta = a.getPlayerTeam() != null ? a.getPlayerTeam().getRegisteredName() : "";
                String tb = b.getPlayerTeam() != null ? b.getPlayerTeam().getRegisteredName() : "";
                int cmp = ta.compareTo(tb);
                return cmp != 0 ? cmp : a.getGameProfile().getName().compareTo(b.getGameProfile().getName());
            }).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            Aetheria.logger.warning("Failed to sort player info map: " + e.getMessage());
            infos = new ArrayList<>(mc.thePlayer.sendQueue.getPlayerInfoMap());
        }
        if (infos.isEmpty()) {
            Aetheria.logger.info("Empty Tab List??");
        }

        List<String> usersToCheck = new ArrayList<>();
        for (NetworkPlayerInfo info : infos) {
            String raw = tab.getPlayerName(info);
            String stripped = ColorUtils.stripColor(raw != null ? raw : "").trim();
            if (stripped.isEmpty()) continue;

            Matcher matcher = PLAYER_REGEX.matcher(stripped.trim());
            if (matcher.lookingAt()) {
                String username = matcher.group(1);
                usersToCheck.add(username);
            } else {
                if (stripped.contains("[") && stripped.contains("]")) {
                    if (ATHRConfig.feature.debug.dungeonMapDebug) {
                        ChatUtils.sendMessage("§7[§6DEBUG§7] §cCould not Find Username in: " + stripped);
                    }
                }
            }
        }

        List<EntityPlayer> playerEntities = mc.theWorld.getEntities(EntityPlayer.class, Objects::nonNull);

        for (String username : usersToCheck) {
            AtomicReference<EntityPlayer> player = new AtomicReference<>(mc.theWorld.getPlayerEntityByName(username));
            if (player.get() == null) {
                playerEntities.forEach(pl -> {
                    if (pl.getName().equalsIgnoreCase(username)) {
                        player.set(pl);
                    }
                });
            }
            if (player.get() != null) {
                players.add(player.get());
                playerNames.add(username);
            } else {
                playerNames.add(username);
            }
        }
    }

    public void matchDecorations(Map<String, Vec4b> mapDecorations) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || playerNames.isEmpty()) return;
        String selfName = mc.thePlayer.getName();

        if (mapDecorations == null || mapDecorations.isEmpty()) return;

        // Convert decorations to pixel positions, separate self (type 1) from others (type 3)
        List<float[]> otherDecos = new ArrayList<>();  // [pixelX, pixelZ, yaw]
        for (Vec4b deco : mapDecorations.values()) {
            byte type = deco.func_176110_a();
            if (type == 1 || type == 3) {
                float x = (float) deco.func_176112_b() / 2.0F + 64.0F;
                float z = (float) deco.func_176113_c() / 2.0F + 64.0F;
                float yaw = (float) (deco.func_176111_d() * 360) / 16.0F;
                if (type == 1) continue; // self marker, we use entity pos
                otherDecos.add(new float[]{x, z, yaw});
            }
        }

        if (otherDecos.isEmpty()) return;

        currentPositions.clear();

        List<String> otherNames = new ArrayList<>();
        for (String name : playerNames) {
            if (!name.equals(selfName)) {
                otherNames.add(name);
            }
        }

        if (otherNames.isEmpty()) return;

        // First match, assign in tab order
        if (!matched || lastPositions.isEmpty()) {
            matched = true;
            for (int i = 0; i < otherNames.size() && i < otherDecos.size(); i++) {
                currentPositions.put(otherNames.get(i), otherDecos.get(i));
            }
            syncLastPositions();
            return;
        }

        // Build distance matrix from lastPositions → otherDecos
        List<String> playerList = new ArrayList<>();
        List<float[]> refs = new ArrayList<>();
        for (String name : otherNames) {
            float[] last = lastPositions.get(name);
            if (last != null) {
                playerList.add(name);
                refs.add(last);
            }
        }

        if (playerList.isEmpty()) {
            // No reference positions, assign in tab order
            for (int i = 0; i < otherNames.size() && i < otherDecos.size(); i++) {
                currentPositions.put(otherNames.get(i), otherDecos.get(i));
            }
            syncLastPositions();
            return;
        }

        // Try all player permutations to find minimum total distance
        List<Integer> usedIndices = solvePermutations(refs, otherDecos, playerList);

        // Assign matched positions
        for (int i = 0; i < playerList.size(); i++) {
            int decoIdx = usedIndices.get(i);
            if (decoIdx >= 0 && decoIdx < otherDecos.size()) {
                currentPositions.put(playerList.get(i), otherDecos.get(decoIdx));
            }
        }

        // Assign unmatched decoration slots to unmatched players
        Set<Integer> used = new HashSet<>(usedIndices);
        List<Integer> leftoverDecos = new ArrayList<>();
        for (int i = 0; i < otherDecos.size(); i++) {
            if (!used.contains(i)) leftoverDecos.add(i);
        }
        for (String name : otherNames) {
            if (!currentPositions.containsKey(name) && !leftoverDecos.isEmpty()) {
                currentPositions.put(name, otherDecos.get(leftoverDecos.remove(0)));
            }
        }

        syncLastPositions();
    }

    private List<Integer> solvePermutations(List<float[]> refs, List<float[]> decos, List<String> playerList) {
        int n = playerList.size();
        int m = decos.size();

        // Generate all permutations of indices 0..n-1
        List<List<Integer>> perms = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        generatePermutations(perms, new ArrayList<>(), indices);

        List<Integer> bestAssignment = null;
        double bestDistance = Double.MAX_VALUE;

        for (List<Integer> perm : perms) {
            double totalDist = 0;
            boolean valid = true;
            Set<Integer> taken = new HashSet<>();
            List<Integer> assignment = new ArrayList<>();
            for (int pi = 0; pi < n; pi++) {
                float[] ref = refs.get(perm.get(pi));
                int bestDeco = -1;
                double bestDecoDist = Double.MAX_VALUE;
                for (int di = 0; di < m; di++) {
                    if (taken.contains(di)) continue;
                    float[] deco = decos.get(di);
                    double dx = ref[0] - deco[0];
                    double dz = ref[1] - deco[1];
                    double dist = dx * dx + dz * dz;
                    if (dist < bestDecoDist) {
                        bestDecoDist = dist;
                        bestDeco = di;
                    }
                }
                if (bestDeco < 0) {
                    valid = false;
                    break;
                }
                taken.add(bestDeco);
                assignment.add(bestDeco);
                totalDist += bestDecoDist;
            }
            if (valid && totalDist < bestDistance) {
                bestDistance = totalDist;
                bestAssignment = assignment;
            }
        }

        if (bestAssignment == null) {
            bestAssignment = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                bestAssignment.add(i < m ? i : -1);
            }
        }
        return bestAssignment;
    }

    private void generatePermutations(List<List<Integer>> result, List<Integer> current, List<Integer> remaining) {
        if (remaining.isEmpty()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = 0; i < remaining.size(); i++) {
            int val = remaining.get(i);
            current.add(val);
            List<Integer> nextRemaining = new ArrayList<>(remaining.subList(0, i));
            nextRemaining.addAll(remaining.subList(i + 1, remaining.size()));
            generatePermutations(result, current, nextRemaining);
            current.remove(current.size() - 1);
        }
    }

    private void syncLastPositions() {
        // Keep old lastPositions for players without a current position (no decoration this frame)
        // This prevents their head from jumping to a random leftover decoration when temporarily missing
        Set<String> active = new HashSet<>(playerNames);

        for (Map.Entry<String, float[]> entry : currentPositions.entrySet()) {
            float[] pos = entry.getValue();
            lastPositions.put(entry.getKey(), new float[]{pos[0], pos[1], pos[2]});
        }
        // Prune players no longer in the party
        lastPositions.keySet().retainAll(active);
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
        Collection<NetworkPlayerInfo> infos;
        try {
            infos = mc.thePlayer.sendQueue.getPlayerInfoMap();
        } catch (Exception e) {
            return null;
        }
        for (NetworkPlayerInfo info : infos) {
            if (info.getGameProfile().getName().equals(name)) {
                return info;
            }
        }
        return null;
    }
}
