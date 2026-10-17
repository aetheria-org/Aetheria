package io.hamlook.aetheria.features.misc.party;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.utils.compat.EntityCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextCompat;
import io.hamlook.aetheria.utils.compat.WorldCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.EntityOutlineQueue;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a colored box around party members' player models.
 * <p>
 * This is intentionally NOT an ESP/X-ray effect: boxes render with depth
 * testing enabled, so a wall or block between you and the party member hides
 * the box exactly like it hides the player model itself. It only ever shows
 * on a party member you could already see normally.
 */
public class PartyMemberOutline {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    @HandleEvent
    public void onRenderWorldLast(ASMRenderWorldEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.misc.partyMemberOutline.enabled) return;
        if (ATHRConfig.feature.misc.partyMemberOutline.disableInDungeons && SkyblockData.isInDungeon()) return;
        if (MinecraftCompat.getLocalWorld() == null || MinecraftCompat.getLocalPlayer() == null) return;

        Color color = getColor();
        for (EntityPlayer player : collectMembers()) {
            WorldRenderUtils.drawSelectionBox(
                    player.getEntityBoundingBox().expand(0.1, 0.05, 0.1), color, 2.5f);
            EntityOutlineQueue.boxed(player, color);
        }
    }

    private static List<EntityPlayer> collectMembers() {
        List<EntityPlayer> members = new ArrayList<>();
        for (Entity entity : WorldCompat.getAllEntities(MinecraftCompat.getLocalWorld())) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == MinecraftCompat.getLocalPlayer() || entity.isInvisible()) continue;
            if (EntityCompat.isDead(entity)) continue;
            if (!PartyMemberTracker.isPartyMember(TextCompat.getUnformattedText(entity.getName()))) continue;
            members.add((EntityPlayer) entity);
        }
        return members;
    }

    private Color getColor() {
        int argb = ChromaColour.specialToChromaRGB(ATHRConfig.feature.misc.partyMemberOutline.outlineColor);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }
}
