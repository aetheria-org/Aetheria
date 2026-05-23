package io.hamlook.aetheria.features.mining;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.RaycastUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
    private static final Color COLOR_COOLDOWN = new Color(255, 60, 60, 160);
    private static final Color COLOR_FILL = new Color(255, 100, 200, 30);
    private static final Color COLOR_FILL_CD = new Color(255, 60, 60, 30);

    private boolean onCooldown = false;

    private AxisAlignedBB previewBox = null;
    private int totalBlocks = 0;
    private int glassBlocks = 0;
    private int iceBlocks = 0;

    private static boolean isHoldingPickobulus() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;
        return ItemUtils.getLoreLines(mc.thePlayer.getHeldItem()).stream().anyMatch(line -> ColorUtils.stripColor(line).contains(PICKOBULUS_LORE_MARKER));
    }

    private static BlockPos raycast(EntityPlayer player) {
        double eyeY = player.posY + PICKOBULUS_EYE_OFFSET + (player.isSneaking() ? 1.54 : 1.62);
        Vec3 eyes = new Vec3(player.posX, eyeY, player.posZ);
        Vec3 look = player.getLookVec();
        return RaycastUtils.raycastBlock(eyes, look, REACH);
    }

    private boolean isEnabled() {
        return ATHRConfig.feature == null || !ATHRConfig.feature.mining.pickobulusPreview;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (isEnabled() || !isHoldingPickobulus() || onCooldown) {
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

        // 6×6×6 blast box centred on the hit block
        previewBox = new AxisAlignedBB(hit.getX() - RADIUS, hit.getY() - RADIUS, hit.getZ() - RADIUS, hit.getX() + RADIUS, hit.getY() + RADIUS, hit.getZ() + RADIUS);

        totalBlocks = 0;
        glassBlocks = 0;
        iceBlocks = 0;

        int x0 = hit.getX() - (RADIUS - 1), x1 = hit.getX() + (RADIUS - 1);
        int y0 = hit.getY() - (RADIUS - 1), y1 = hit.getY() + (RADIUS - 1);
        int z0 = hit.getZ() - (RADIUS - 1), z1 = hit.getZ() + (RADIUS - 1);

        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    net.minecraft.block.Block block = mc.theWorld.getBlockState(new BlockPos(bx, by, bz)).getBlock();
                    if (block == Blocks.air) continue;
                    totalBlocks++;
                    String id = block.getUnlocalizedName();
                    if (id.endsWith("glass") || id.endsWith("glass_pane")) glassBlocks++;
                    else if (id.endsWith("ice")) iceBlocks++;
                }
            }
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (isEnabled()) return;
        String text = StringUtils.stripControlCodes(event.message.getFormattedText()).trim();

        if ("You used your Pickobulus Pickaxe Ability!".equals(text) || text.startsWith("Your Pickaxe ability is on cooldown for ")) {
            onCooldown = true;
        } else if ("Pickobulus is now available!".equals(text)) {
            onCooldown = false;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (isEnabled() || previewBox == null) return;

        Color outline = onCooldown ? COLOR_COOLDOWN : COLOR_READY;
        Color fill = onCooldown ? COLOR_FILL_CD : COLOR_FILL;

        WorldRenderUtils.drawSelectionBox(previewBox, outline, 2f);
        WorldRenderUtils.drawFilledBlock(previewBox, fill);

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        double cx = (previewBox.minX + previewBox.maxX) / 2.0;
        double cy = previewBox.maxY + 0.3;
        double cz = (previewBox.minZ + previewBox.maxZ) / 2.0;

        String label = "§f" + totalBlocks + " blocks";
        if (glassBlocks > 0) label += " §b(" + glassBlocks + " glass)";
        if (iceBlocks > 0) label += " §3(" + iceBlocks + " ice)";

        WorldRenderUtils.drawTextInWorld(label, cx, cy, cz);
    }
}
