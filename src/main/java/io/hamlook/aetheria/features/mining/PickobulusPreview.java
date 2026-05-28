package io.hamlook.aetheria.features.mining;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.RaycastUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;

@RegisterEvents
public class PickobulusPreview {

    private static final int RADIUS = 3;
    private static final double REACH = 30.0;
    private static final String PICKOBULUS_LORE_MARKER = "Ability: Pickobulus";
    private static final double PICKOBULUS_EYE_OFFSET = 0.53625;

    private static final Color COLOR_READY = new Color(255, 100, 200, 160);
    private static final Color COLOR_FILL = new Color(255, 100, 200, 30);

    private AxisAlignedBB previewBox = null;

    private boolean cachedIsHoldingPickobulus = false;
    private int lastHeldItemHash = 0;
    private int tickCounter = 0;

    private boolean isEnabled() {
        return ATHRConfig.feature == null || !ATHRConfig.feature.mining.pickobulusPreview;
    }

    private boolean isHoldingPickobulus() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        int currentHash = System.identityHashCode(mc.thePlayer.getHeldItem());
        if (currentHash != lastHeldItemHash) {
            lastHeldItemHash = currentHash;
            if (mc.thePlayer.getHeldItem() != null) {
                for (String line : ItemUtils.getLoreLines(mc.thePlayer.getHeldItem())) {
                    if (ColorUtils.stripColor(line).contains(PICKOBULUS_LORE_MARKER)) {
                        cachedIsHoldingPickobulus = true;
                        return true;
                    }
                }
            }
            cachedIsHoldingPickobulus = false;
        }
        return cachedIsHoldingPickobulus;
    }

    private BlockPos raycast(EntityPlayer player) {
        double eyeY = player.posY + PICKOBULUS_EYE_OFFSET + (player.isSneaking() ? 1.54 : 1.62);
        Vec3 eyes = new Vec3(player.posX, eyeY, player.posZ);
        Vec3 look = player.getLookVec();
        return RaycastUtils.raycastBlock(eyes, look, REACH);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter % 2 != 0) return;

        if (isEnabled() || !isHoldingPickobulus()) {
            previewBox = null;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            previewBox = null;
            return;
        }

        BlockPos hit = raycast(mc.thePlayer);
        if (hit == null) {
            previewBox = null;
            return;
        }

        previewBox = new AxisAlignedBB(hit.getX() - RADIUS, hit.getY() - RADIUS, hit.getZ() - RADIUS, hit.getX() + RADIUS + 1, hit.getY() + RADIUS + 1, hit.getZ() + RADIUS + 1);
    }

    // TODO: UNCOMMENT WHEN FAKEPIXEL ADDS Pickobulus is now available MESSAGE
    // @SubscribeEvent
    // public void onChat(ClientChatReceivedEvent event) {
    //     if (!isEnabled()) return;
    //     String text = StringUtils.stripControlCodes(event.message.getFormattedText()).trim();
    //
    //     if ("You used your Pickobulus Pickaxe Ability!".equals(text) || text.startsWith("Your Pickaxe ability is on cooldown for ")) {
    //         onCooldown = true;
    //     } else if ("Pickobulus is now available!".equals(text)) {
    //         onCooldown = false;
    //     }
    // }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (isEnabled() || previewBox == null) return;

        WorldRenderUtils.drawSelectionBox(previewBox, COLOR_READY, 2f);
        WorldRenderUtils.drawFilledBlock(previewBox, COLOR_FILL);
    }
}