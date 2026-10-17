package io.hamlook.aetheria.features.dungeons.mobhighlights;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.dungeons.BossHighlightConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.RenderEntityModelEvent;
import io.hamlook.aetheria.events.RenderEntityOutlineEvent;
import io.hamlook.aetheria.events.TickPhase;
import io.hamlook.aetheria.features.dungeons.DungeonStats;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.EntityRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@RegisterEvents
public class BossHighlight {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();
    private volatile Map<EntityLivingBase, BossType> bossMobs = new HashMap<>();
    private int tickCounter = 0;

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        if (ATHRConfig.feature == null) return;

        BossHighlightConfig cfg = ATHRConfig.feature.dungeons.bossHighlight;
        boolean anyEnabled = cfg.bonzoHighlight != 2 || cfg.scarfHighlight != 2
                || cfg.scarfMinionHighlight != 2 || cfg.professorHighlight != 2;
        if (!anyEnabled) return;

        if (++tickCounter < 4) return;
        tickCounter = 0;

        if (!SkyblockData.isInDungeon() || MinecraftCompat.getLocalWorld() == null || !DungeonStats.isInBossFight()) {
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

                if (cfg.bonzoHighlight != 2 && name.contains("bonzo")) {
                    type = BossType.BONZO;
                } else if (cfg.scarfMinionHighlight != 2 && isScarfMinion(name)) {
                    type = BossType.SCARF_MINION;
                } else if (cfg.scarfHighlight != 2 && name.contains("scarf")) {
                    type = BossType.SCARF;
                } else if (cfg.professorHighlight != 2 && isProfessorGuardian(name)) {
                    type = BossType.PROFESSOR_GUARDIAN;
                } else if (cfg.professorHighlight != 2 && name.contains("professor")) {
                    type = BossType.PROFESSOR;
                }

                if (type == null) continue;

                final BossType finalType = type;
                EntityLivingBase mob = EntityCompat.findMobNearStand(MinecraftCompat.getLocalWorld(), entity, 1.0, 3.0, 1.0);

                if (mob != null && !EntityCompat.isDead(mob) && mob.getHealth() > 0) found.put(mob, finalType);
            }

            bossMobs = found;
    }

    private static boolean isScarfMinion(String name) {
        return name.contains("undead mage") || name.contains("undead archer")
                || name.contains("undead warrior") || name.contains("undead priest");
    }

    private static boolean isProfessorGuardian(String name) {
        return name.contains("healthy") || name.contains("chaos") || name.contains("laser")
                || name.contains("rogue") || name.contains("reinforced");
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (ATHRConfig.feature == null) return;
        EntityLivingBase entity = event.getEntity();
        BossType type = bossMobs.get(entity);
        if (type == null) return;
        if (highlightModeFor(type) != 1) return;
        EntityRenderUtils.renderOutline(event, colorFor(type));
    }

    @HandleEvent
    public void onRenderOutline(RenderEntityOutlineEvent event) {
        if (ATHRConfig.feature == null) return;
        for (Map.Entry<EntityLivingBase, BossType> entry : bossMobs.entrySet()) {
            if (highlightModeFor(entry.getValue()) != 1) continue;
            EntityLivingBase mob = entry.getKey();
            if (EntityCompat.isDead(mob)) continue;
            event.queue(mob, colorFor(entry.getValue()).getRGB());
        }
    }

    @HandleEvent
    public void onRenderWorldLast(ASMRenderWorldEvent event) {
        if (ATHRConfig.feature == null) return;
        Map<EntityLivingBase, Color> mobs = new HashMap<>();
        for (Map.Entry<EntityLivingBase, BossType> entry : bossMobs.entrySet()) {
            if (highlightModeFor(entry.getValue()) != 0) continue;
            mobs.put(entry.getKey(), colorFor(entry.getValue()));
        }
        EntityRenderUtils.renderBoxes(mobs);
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

    private enum BossType {BONZO, SCARF, SCARF_MINION, PROFESSOR, PROFESSOR_GUARDIAN}
}