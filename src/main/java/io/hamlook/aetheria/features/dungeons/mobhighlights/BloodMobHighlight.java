package io.hamlook.aetheria.features.dungeons.mobhighlights;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IChatComponent;
import io.hamlook.aetheria.api.event.HandleEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import io.hamlook.aetheria.events.ASMTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;

@RegisterEvents
public class BloodMobHighlight {

    private static final Pattern MOB_NAME = Pattern.compile(".*(?:Putrid|Reaper|Vader|Frost|Cannibal|Revoker|Tear|Mr\\.? Dead|Skull|Walker|Psycho|Ooze|Freak|Flamer|Mute|Leech|Parasite).*");
    private static final Pattern DYING1 = Pattern.compile("^§.\\[§.Lv\\d+§.\\] §.+ (?:§.)+0§f/.+§c❤$");
    private static final Pattern DYING2 = Pattern.compile("^.+ (?:§.)+0§c❤$");

    private static final Minecraft mc = Minecraft.getMinecraft();
    private volatile Set<EntityLivingBase> bloodMobs = new HashSet<>();
    private int tickCounter = 0;


    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight == 2) return;

        if (++tickCounter < 4) return;
        tickCounter = 0;

        if (!SkyblockData.isInDungeon() || mc.theWorld == null) {
            bloodMobs = new HashSet<>();
            return;
        }

        Set<EntityLivingBase> found = new HashSet<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityArmorStand)) continue;
            String name = entity.getName();
            if (name == null || !MOB_NAME.matcher(name).matches()) continue;

            EntityLivingBase mob = mc.theWorld.getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox().expand(0.5, 3.0, 0.5), e -> e != null && !(e instanceof EntityArmorStand) && e != mc.thePlayer).stream().findFirst().orElse(null);

            if (mob != null && !isDying(mob)) found.add(mob);
        }
        bloodMobs = found;
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight != 1) return;
        EntityLivingBase entity = event.getEntity();
        if (!bloodMobs.contains(entity) || isDying(entity) || entity.isInvisible()) return;
        renderCleanOutline(event, getColor());
    }

    @HandleEvent
    public void onRenderWorldLast(ASMRenderWorldEvent event) {
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight != 0) return;
        Set<EntityLivingBase> snapshot = bloodMobs;
        if (snapshot.isEmpty() || mc.thePlayer == null) return;

        Color c = getColor();
        float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f, a = c.getAlpha() / 255f;

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.5f);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);

        for (EntityLivingBase mob : snapshot) {
            if (mob.isDead || mob.getHealth() <= 0) continue;
            drawBox(mob.getEntityBoundingBox().expand(0.1, 0.05, 0.1), r, g, b, a);
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private Color getColor() {
        int argb = ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.bloodMobColor);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    private boolean isDying(EntityLivingBase entity) {
        if (entity == null || entity.isDead || entity.getHealth() <= 0.1f) return true;
        IChatComponent name = entity.getDisplayName();
        if (name == null) return false;
        String text = name.getUnformattedText();
        return DYING1.matcher(text).matches() || DYING2.matcher(text).matches();
    }

    private void renderCleanOutline(RenderEntityModelEvent event, Color color) {
        EntityLivingBase entity = event.getEntity();
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.22f);
        GlStateManager.scale(1.04f, 1.04f, 1.04f);
        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.88f);
        float shrink = 1.025f / 1.04f;
        GlStateManager.scale(shrink, shrink, shrink);
        event.getModel().render(entity, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(), event.getHeadYaw(), event.getHeadPitch(), event.getScaleFactor());

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void drawBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        double[][] edges = {{bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.minZ}, {bb.minX, bb.minY, bb.maxZ, bb.maxX, bb.minY, bb.maxZ}, {bb.minX, bb.minY, bb.minZ, bb.minX, bb.minY, bb.maxZ}, {bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ}, {bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.minZ}, {bb.minX, bb.maxY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ}, {bb.minX, bb.maxY, bb.minZ, bb.minX, bb.maxY, bb.maxZ}, {bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ}, {bb.minX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.minZ}, {bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ}, {bb.minX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ}, {bb.maxX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ},};
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255), ai = (int) (a * 255);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (double[] e : edges) {
            wr.pos(e[0], e[1], e[2]).color(ri, gi, bi, ai).endVertex();
            wr.pos(e[3], e[4], e[5]).color(ri, gi, bi, ai).endVertex();
        }
        tess.draw();
    }
}