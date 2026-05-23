package io.hamlook.aetheria.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public final class RaycastUtils {

    private RaycastUtils() {}


    public static BlockPos raycastBlock(Vec3 eyes, Vec3 look, double reach) {
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        double x = Math.floor(eyes.xCoord);
        double y = Math.floor(eyes.yCoord);
        double z = Math.floor(eyes.zCoord);

        double dx = end.xCoord - eyes.xCoord;
        double dy = end.yCoord - eyes.yCoord;
        double dz = end.zCoord - eyes.zCoord;

        double stepX = Math.signum(dx);
        double stepY = Math.signum(dy);
        double stepZ = Math.signum(dz);

        double invDx = dx != 0 ? 1.0 / dx : Double.MAX_VALUE;
        double invDy = dy != 0 ? 1.0 / dy : Double.MAX_VALUE;
        double invDz = dz != 0 ? 1.0 / dz : Double.MAX_VALUE;

        double tDeltaX = Math.abs(invDx * stepX);
        double tDeltaY = Math.abs(invDy * stepY);
        double tDeltaZ = Math.abs(invDz * stepZ);

        double tMaxX = Math.abs((x + Math.max(stepX, 0) - eyes.xCoord) * invDx);
        double tMaxY = Math.abs((y + Math.max(stepY, 0) - eyes.yCoord) * invDy);
        double tMaxZ = Math.abs((z + Math.max(stepZ, 0) - eyes.zCoord) * invDz);

        double endX = Math.floor(end.xCoord);
        double endY = Math.floor(end.yCoord);
        double endZ = Math.floor(end.zCoord);

        for (int i = 0; i < 1000; i++) {
            BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
            if (Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                return pos;
            }
            if (x == endX && y == endY && z == endZ) return null;

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) { tMaxX += tDeltaX; x += stepX; }
            else if (tMaxY <= tMaxZ)               { tMaxY += tDeltaY; y += stepY; }
            else                                   { tMaxZ += tDeltaZ; z += stepZ; }
        }
        return null;
    }

    public static BlockPos raycastBlock(net.minecraft.entity.player.EntityPlayer player,
                                        float partialTicks, double reach) {
        Vec3 eyes = player.getPositionEyes(partialTicks);
        Vec3 look = player.getLookVec();
        return raycastBlock(eyes, look, reach);
    }
}
