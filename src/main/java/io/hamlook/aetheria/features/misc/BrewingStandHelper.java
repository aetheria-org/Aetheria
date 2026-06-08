package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class BrewingStandHelper {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Pattern TIME_REGEX = Pattern.compile("§a(\\d+(?:\\.\\d)?)s");
    private final Map<BlockPos, Long> brewingStandToTimeMap = new HashMap<>();
    private TileEntityBrewingStand lastBrewingStand = null;
    private int tickCounter = 0;

    private static boolean isOnPrivateIsland() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.PRIVATE_ISLAND;
    }

    private static void drawFilledBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        wr.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        tess.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (mc.theWorld == null) return;
        if (!isOnPrivateIsland()) return;
        if (++tickCounter % 100 != 0) return;

        Iterator<Map.Entry<BlockPos, Long>> it = brewingStandToTimeMap.entrySet().iterator();
        while (it.hasNext()) {
            if (!(mc.theWorld.getTileEntity(it.next().getKey()) instanceof TileEntityBrewingStand)) it.remove();
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (!isOnPrivateIsland()) return;
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (mc.theWorld == null) return;
        if (mc.theWorld.getTileEntity(event.pos) instanceof TileEntityBrewingStand)
            lastBrewingStand = (TileEntityBrewingStand) mc.theWorld.getTileEntity(event.pos);
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (!isOnPrivateIsland()) return;
        if (lastBrewingStand == null) return;
        if (!ContainerUtils.isInContainer(event.gui, "Brewing Stand")) return;

        ContainerChest container = ContainerUtils.getOpenChest(event.gui);
        if (container == null) return;

        BlockPos pos = lastBrewingStand.getPos();
        double time = 0.0;
        boolean found = false;

        for (int i = 0; i < container.inventorySlots.size(); i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (stack == null) continue;
            Matcher matcher = TIME_REGEX.matcher(stack.getDisplayName());
            if (matcher.find()) {
                try {
                    time = Double.parseDouble(matcher.group(1));
                    found = true;
                } catch (NumberFormatException ignored) {
                }
                break;
            }
        }

        if (!found) {
            brewingStandToTimeMap.remove(pos);
            return;
        }

        brewingStandToTimeMap.put(pos, System.currentTimeMillis() + (long) (time * 1000L));
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (!isOnPrivateIsland()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        float pt = event.partialTicks;
        double vx = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * pt;
        double vy = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * pt;
        double vz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * pt;

        long now = System.currentTimeMillis();

        for (Map.Entry<BlockPos, Long> entry : brewingStandToTimeMap.entrySet()) {
            if (entry.getValue() <= now) continue;
            BlockPos pos = entry.getKey();
            AxisAlignedBB bb = new AxisAlignedBB(pos.getX() - vx, pos.getY() - vy, pos.getZ() - vz, pos.getX() + 1 - vx, pos.getY() + 1 - vy, pos.getZ() + 1 - vz);
            drawFilledBox(bb, 1f, 0f, 0f, 0.5f);
        }
    }
}