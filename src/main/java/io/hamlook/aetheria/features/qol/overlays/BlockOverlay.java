package io.hamlook.aetheria.features.qol.overlays;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMBlockHighlightEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.awt.*;

@RegisterEvents
public class BlockOverlay {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    @HandleEvent
    public void onDrawBlockHighlight(ASMBlockHighlightEvent event) {
        if (!ATHRConfig.feature.qol.blockSelection.blockSelectionOverlay) return;
        if (event.target == null || !BlockCompat.isBlockHit(event.target)) return;

        event.cancel();

        Color color = ChromaColour.specialToColor(ATHRConfig.feature.qol.blockSelection.blockSelectionColor);

        BlockPos pos = BlockCompat.getHitBlockPos(event.target);
        AxisAlignedBB aabb = BlockCompat.getSelectionAABB(pos);

        if (ATHRConfig.feature.qol.blockSelection.blockSelectionMode == 0) {
            WorldRenderUtils.drawFilledBlock(aabb, color);
        } else {
            WorldRenderUtils.drawSelectionBox(aabb, color, ATHRConfig.feature.qol.blockSelection.blockSelectionThickness);
        }
    }
}