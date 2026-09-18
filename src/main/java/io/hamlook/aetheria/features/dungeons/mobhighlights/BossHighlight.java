package io.hamlook.aetheria.features.dungeons.mobhighlights;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.EntityHighlight;
import io.hamlook.aetheria.utils.render.EntityOutlines;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StringUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RegisterEvents
public class BossHighlight {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();
    private volatile Map<EntityLivingBase, BossType> bossMobs = new HashMap<>();
    private int tickCounter = 0;

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        if (ATHRConfig.feature == null) return;

        boolean anyEnabled = ATHRConfig.feature.dungeons.bossHighlight.bonzoHighlight != 2 || ATHRConfig.feature.dungeons.bossHighlight.scarfHighlight != 2 || ATHRConfig.feature.dungeons.bossHighlight.scarfMinionHighlight != 2 || ATHRConfig.feature.dungeons.bossHighlight.professorHighlight != 2;
        if (!anyEnabled) return;

        if (++tickCounter < 4) return;
        tickCounter = 0;

        if (!SkyblockData.isInDungeon() || MinecraftCompat.getLocalWorld() == null) {
            bossMobs = new HashMap<>();
            return;
        }

        Map<EntityLivingBase, BossType> found = new HashMap<>();

        for (Entity entity : WorldCompat.getAllEntities(MinecraftCompat.getLocalWorld())) {
            if (!(entity instanceof EntityArmorStand)) continue;
            String raw = TextCompat.getUnformattedText(entity.getName());
            if (raw == null) continue;
            String name = StringUtils.stripControlCodes(raw).toLowerCase();

            BossType type = null;

            if (ATHRConfig.feature.dungeons.bossHighlight.bonzoHighlight != 2 && name.contains("bonzo")) {
                type = BossType.BONZO;
            } else if (ATHRConfig.feature.dungeons.bossHighlight.scarfMinionHighlight != 2 && (name.contains("undead mage") || name.contains("undead archer") || name.contains("undead warrior") || name.contains("undead priest"))) {
                type = BossType.SCARF_MINION;
            } else if (ATHRConfig.feature.dungeons.bossHighlight.scarfHighlight != 2 && name.contains("scarf")) {
                type = BossType.SCARF;
            } else if (ATHRConfig.feature.dungeons.bossHighlight.professorHighlight != 2 && (name.contains("healthy") || name.contains("chaos") || name.contains("laser") || name.contains("rogue") || name.contains("reinforced"))) {
                type = BossType.PROFESSOR_GUARDIAN;
            } else if (ATHRConfig.feature.dungeons.bossHighlight.professorHighlight != 2 && name.contains("professor")) {
                type = BossType.PROFESSOR;
            }

            if (type == null) continue;

            final BossType finalType = type;
            EntityLivingBase mob = MinecraftCompat.getLocalWorld().getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox().expand(1.0, 3.0, 1.0), e -> e != null && !(e instanceof EntityArmorStand) && e != MinecraftCompat.getLocalPlayer()).stream().findFirst().orElse(null);

            if (mob != null && !EntityCompat.isDead(mob) && mob.getHealth() > 0) found.put(mob, finalType);
        }

        bossMobs = found;
        for (Map.Entry<EntityLivingBase, BossType> entry : found.entrySet()) {
            if (highlightModeFor(entry.getValue()) == 1) EntityOutlines.xray(entry.getKey(), colorFor(entry.getValue()));
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (ATHRConfig.feature == null) return;
        EntityLivingBase entity = event.getEntity();
        BossType type = bossMobs.get(entity);
        if (type == null || entity.isInvisible()) return;
        if (highlightModeFor(type) != 1) return;
        EntityHighlight.renderEntityOutline(event, colorFor(type));
    }

    @HandleEvent
    public void onRenderWorldLast(ASMRenderWorldEvent event) {
        if (ATHRConfig.feature == null) return;
        Map<EntityLivingBase, BossType> snapshot = bossMobs;
        if (snapshot.isEmpty() || MinecraftCompat.getLocalPlayer() == null) return;

        Map<BossType, Set<EntityLivingBase>> byType = new HashMap<>();
        for (Map.Entry<EntityLivingBase, BossType> entry : snapshot.entrySet()) {
            EntityLivingBase mob = entry.getKey();
            BossType type = entry.getValue();
            if (highlightModeFor(type) != 0 || EntityCompat.isDead(mob) || mob.getHealth() <= 0) continue;
            byType.computeIfAbsent(type, k -> new HashSet<>()).add(mob);
        }
        if (byType.isEmpty()) return;

        double[] vp = WorldRenderUtils.viewerPos();
        double vx = vp[0], vy = vp[1], vz = vp[2];

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

        for (Map.Entry<BossType, Set<EntityLivingBase>> entry : byType.entrySet()) {
            Color c = colorFor(entry.getKey());
            float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f, a = c.getAlpha() / 255f;
            for (EntityLivingBase mob : entry.getValue())
                drawBox(mob.getEntityBoundingBox().expand(0.1, 0.05, 0.1), r, g, b, a);
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private int highlightModeFor(BossType type) {
        if (ATHRConfig.feature == null) return 2;
        switch (type) {
            case BONZO:
                return ATHRConfig.feature.dungeons.bossHighlight.bonzoHighlight;
            case SCARF:
                return ATHRConfig.feature.dungeons.bossHighlight.scarfHighlight;
            case SCARF_MINION:
                return ATHRConfig.feature.dungeons.bossHighlight.scarfMinionHighlight;
            case PROFESSOR:
                return ATHRConfig.feature.dungeons.bossHighlight.professorHighlight;
            case PROFESSOR_GUARDIAN:
                return ATHRConfig.feature.dungeons.bossHighlight.professorHighlight;
            default:
                return 2;
        }
    }

    private Color colorFor(BossType type) {
        String raw;
        switch (type) {
            case BONZO:
                raw = ATHRConfig.feature.dungeons.bossHighlight.bonzoColor;
                break;
            case SCARF:
                raw = ATHRConfig.feature.dungeons.bossHighlight.scarfColor;
                break;
            case SCARF_MINION:
                raw = ATHRConfig.feature.dungeons.bossHighlight.scarfMinionColor;
                break;
            case PROFESSOR:
            case PROFESSOR_GUARDIAN:
                raw = ATHRConfig.feature.dungeons.bossHighlight.professorColor;
                break;
            default:
                raw = "200:255:255:255:255";
                break;
        }
        int argb = ChromaColour.specialToChromaRGB(raw);
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    private void drawBox(AxisAlignedBB bb, float r, float g, float b, float a) {
        double[][] edges = {{bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.minZ}, {bb.minX, bb.minY, bb.maxZ, bb.maxX, bb.minY, bb.maxZ}, {bb.minX, bb.minY, bb.minZ, bb.minX, bb.minY, bb.maxZ}, {bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ}, {bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.minZ}, {bb.minX, bb.maxY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ}, {bb.minX, bb.maxY, bb.minZ, bb.minX, bb.maxY, bb.maxZ}, {bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ}, {bb.minX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.minZ}, {bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ}, {bb.minX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ}, {bb.maxX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ},};
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255), ai = (int) (a * 255);
        VertexBuilder vb = TessellatorCompat.beginDraw(TessellatorCompat.LINES, TessellatorCompat.POSITION_COLOR);
        for (double[] e : edges) {
            vb.pos(e[0], e[1], e[2]).color(ri, gi, bi, ai).endVertex();
            vb.pos(e[3], e[4], e[5]).color(ri, gi, bi, ai).endVertex();
        }
        vb.draw();
    }

    private enum BossType {BONZO, SCARF, SCARF_MINION, PROFESSOR, PROFESSOR_GUARDIAN}
}