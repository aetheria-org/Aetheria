package io.hamlook.aetheria.features.qol.overlays;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMBlockHighlightEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

import java.awt.*;

@RegisterEvents
public class BlockOverlay {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    private static AxisAlignedBB getSelectionAABB(BlockPos pos) {
        if (mc.theWorld == null) {
            return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        AxisAlignedBB aabb = block.getSelectedBoundingBox(mc.theWorld, pos);
        return aabb != null ? aabb : new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    @HandleEvent
    public void onDrawBlockHighlight(ASMBlockHighlightEvent event) {
        if (!ATHRConfig.feature.qol.blockSelection.blockSelectionOverlay) return;
        if (event.target == null || event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        event.cancel();

        int argb = ChromaColour.specialToChromaRGB(ATHRConfig.feature.qol.blockSelection.blockSelectionColor);
        Color color = new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);

        BlockPos pos = event.target.getBlockPos();
        AxisAlignedBB aabb = getSelectionAABB(pos);

        if (ATHRConfig.feature.qol.blockSelection.blockSelectionMode == 0) {
            WorldRenderUtils.drawFilledBlock(aabb, color);
        } else {
            WorldRenderUtils.drawSelectionBox(aabb, color, ATHRConfig.feature.qol.blockSelection.blockSelectionThickness);
        }
    }
}