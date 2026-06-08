package io.hamlook.aetheria.features.dungeons.utils.dung;

import net.minecraft.util.BlockPos;

/**
 * Simple immutable holder for a visited dungeon room.
 * Stores the room name, its hash, the origin (minimum corner), the centre block position,
 * and the dimensions in blocks.
 */
public class DungeonRoom {
    public final String name;
    public final String hash;
    public final BlockPos origin; // minimum corner (minX, floorY, minZ)
    public final BlockPos center; // centre block of the room (averaged on X/Z)
    public final int width; // X size (blocks)
    public final int height; // Z size (blocks)

    public DungeonRoom(String name, String hash, BlockPos origin, BlockPos center, int width, int height) {
        this.name = name;
        this.hash = hash;
        this.origin = origin;
        this.center = center;
        this.width = width;
        this.height = height;
    }
}
