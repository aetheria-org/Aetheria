package io.hamlook.aetheria.features.dungeons.rooms;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.features.dungeons.rooms.secret.SecretTracker;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class DungeonRoomOverlay extends Overlay {

    public static String currentRoomName = null;
    public static String currentRoomAlias = null;
    public static String currentRoomCategory = null;
    public static String currentRoomNotes = null;
    @Getter
    private static DungeonRoomOverlay instance;

    public DungeonRoomOverlay() {
        super(130, 20);
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlay;
    }

    @Override
    protected boolean extraGuard() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.DUNGEON && !DungeonStats.isInBossFight();
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> out = new ArrayList<>();

        if (preview) {
            out.add(EnumChatFormatting.GRAY + "Category  " + EnumChatFormatting.LIGHT_PURPLE + "Puzzle");
            out.add(EnumChatFormatting.WHITE + "❖  " + EnumChatFormatting.AQUA + "Box" + EnumChatFormatting.DARK_PURPLE + "  ✿");
            out.add(EnumChatFormatting.GRAY + "Rot  " + EnumChatFormatting.GREEN + "0°  " + EnumChatFormatting.GRAY + "Origin " + EnumChatFormatting.GREEN + "northwest");
            out.add(EnumChatFormatting.GRAY + "Rel  " + EnumChatFormatting.WHITE + "(5, 12)");
            out.add(EnumChatFormatting.YELLOW + "✎  " + EnumChatFormatting.WHITE + "Example note about this room");
            return out;
        }

        if (currentRoomName != null) {
            // Line 1: category with colour-coded label
            String categoryColor = getCategoryColor(currentRoomCategory);
            out.add(EnumChatFormatting.GRAY + "Category  " + categoryColor + (currentRoomCategory != null ? currentRoomCategory : "Unknown"));

            // Line 2: room name
            String nameLine = EnumChatFormatting.WHITE + "❖  " + EnumChatFormatting.AQUA + currentRoomName;
            out.add(nameLine);

            // Line 3: short alias — only when enabled and present
            if (ATHRConfig.feature.dungeons.dungeonRoomOverlayConfig.dungeonRoomOverlayShowAlias && currentRoomAlias != null) {
                out.add(EnumChatFormatting.GRAY + "Alias  " + EnumChatFormatting.AQUA + currentRoomAlias);
            }


            // Line 4: relative player coordinates — only when detected
            if (DungeonRoomDetector.playerRelX != Integer.MAX_VALUE) {
                out.add(EnumChatFormatting.GRAY + "Rel  " + EnumChatFormatting.WHITE +
                    "(" + DungeonRoomDetector.playerRelX + ", " + DungeonRoomDetector.playerRelZ + ")");
            }

            // Line 5: notes — only shown when present
            if (currentRoomNotes != null) {
                out.add(EnumChatFormatting.YELLOW + "✎  " + EnumChatFormatting.WHITE + currentRoomNotes);
            }

            if (ATHRConfig.feature.dungeons.secrets.enabled && DungeonRoomDetector.originBlock != null) {
                int total = SecretTracker.total();
                if (SecretTracker.allSecretsCleared()) {
                    out.add(EnumChatFormatting.GREEN + "✦  " + EnumChatFormatting.WHITE + "All secrets found");
                } else if (total > 0) {
                    int found = SecretTracker.found();
                    EnumChatFormatting prefix = found >= total ? EnumChatFormatting.GREEN : EnumChatFormatting.YELLOW;
                    out.add(prefix + "✦  " + EnumChatFormatting.WHITE + found + "/" + total + " secrets");
                }
            }
        }

        return out;
    }

    private String getCategoryColor(String category) {
        if (category == null) return EnumChatFormatting.WHITE.toString();
        switch (category.toLowerCase()) {
            case "puzzle":
                return EnumChatFormatting.LIGHT_PURPLE.toString();
            case "trap":
            case "champion":
                return EnumChatFormatting.RED.toString();
            case "miniboss":
                return EnumChatFormatting.GOLD.toString();
            case "fairy":
                return EnumChatFormatting.DARK_PURPLE.toString();
            case "rare":
                return EnumChatFormatting.YELLOW.toString();
            default:
                return EnumChatFormatting.GREEN.toString();
        }
    }
}