package io.hamlook.aetheria.features.misc;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMGuiDrawPreEvent;
import io.hamlook.aetheria.events.ASMPlayerInteractEvent;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TessellatorCompat;
import io.hamlook.aetheria.utils.compat.VertexBuilder;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class BrewingStandHelper {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();
    private static final Pattern TIME_REGEX = Pattern.compile("§a(\\d+(?:\\.\\d)?)s");
    private final Map<BlockPos, Long> brewingStandToTimeMap = new HashMap<>();
    private TileEntityBrewingStand lastBrewingStand = null;
    private int tickCounter = 0;

    private static boolean isOnPrivateIsland() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.PRIVATE_ISLAND;
    }

    private static void drawFilledBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        GlStateManagerCompat.enableBlend();
        GlStateManagerCompat.disableTexture2D();
        GlStateManagerCompat.disableLighting();
        GlStateManagerCompat.disableCull();
        GlStateManagerCompat.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManagerCompat.depthMask(false);

        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.QUADS, TessellatorCompat.POSITION_COLOR);

        vb.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        vb.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        vb.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();

        vb.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        vb.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        vb.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();

        vb.draw();

        GlStateManagerCompat.depthMask(true);
        GlStateManagerCompat.enableCull();
        GlStateManagerCompat.enableLighting();
        GlStateManagerCompat.enableTexture2D();
        GlStateManagerCompat.disableBlend();
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (MinecraftCompat.getLocalWorld() == null) return;
        if (!isOnPrivateIsland()) return;
        if (++tickCounter % 100 != 0) return;

        Iterator<Map.Entry<BlockPos, Long>> it = brewingStandToTimeMap.entrySet().iterator();
        while (it.hasNext()) {
            if (!(MinecraftCompat.getLocalWorld().getTileEntity(it.next().getKey()) instanceof TileEntityBrewingStand)) it.remove();
        }
    }

    @HandleEvent
    public void onPlayerInteract(ASMPlayerInteractEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (!isOnPrivateIsland()) return;
        if (event.action != 1) return;
        if (MinecraftCompat.getLocalWorld() == null) return;
        if (MinecraftCompat.getLocalWorld().getTileEntity(event.pos) instanceof TileEntityBrewingStand)
            lastBrewingStand = (TileEntityBrewingStand) MinecraftCompat.getLocalWorld().getTileEntity(event.pos);
    }

    @HandleEvent
    public void onGuiDraw(ASMGuiDrawPreEvent event) {
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

    @HandleEvent
    public void onRenderWorld(ASMRenderWorldEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.qol.colorBrewingStands) return;
        if (!isOnPrivateIsland()) return;
        if (MinecraftCompat.getLocalWorld() == null || MinecraftCompat.getLocalPlayer() == null) return;

        float pt = event.partialTicks;
        double vx = MinecraftCompat.getLocalPlayer().lastTickPosX + (MinecraftCompat.getLocalPlayer().posX - MinecraftCompat.getLocalPlayer().lastTickPosX) * pt;
        double vy = MinecraftCompat.getLocalPlayer().lastTickPosY + (MinecraftCompat.getLocalPlayer().posY - MinecraftCompat.getLocalPlayer().lastTickPosY) * pt;
        double vz = MinecraftCompat.getLocalPlayer().lastTickPosZ + (MinecraftCompat.getLocalPlayer().posZ - MinecraftCompat.getLocalPlayer().lastTickPosZ) * pt;

        long now = System.currentTimeMillis();

        for (Map.Entry<BlockPos, Long> entry : brewingStandToTimeMap.entrySet()) {
            if (entry.getValue() <= now) continue;
            BlockPos pos = entry.getKey();
            AxisAlignedBB bb = new AxisAlignedBB(pos.getX() - vx, pos.getY() - vy, pos.getZ() - vz, pos.getX() + 1 - vx, pos.getY() + 1 - vy, pos.getZ() + 1 - vz);
            drawFilledBox(bb, 1f, 0f, 0f, 0.5f);
        }
    }
}