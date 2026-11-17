package io.hamlook.aetheria.features.dungeons.mobhighlights;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
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
import net.minecraft.util.IChatComponent;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RegisterEvents
public class BloodMobHighlight {

    private static final Pattern MOB_NAME = Pattern.compile(".*(?:Putrid|Reaper|Vader|Frost|Cannibal|Revoker|Tear|Mr\\.? Dead|Skull|Walker|Psycho|Ooze|Freak|Flamer|Mute|Leech|Parasite).*");
    private static final Pattern DYING1 = Pattern.compile("^§.\\[§.Lv\\d+§.\\] §.+ (?:§.)+0§f/.+§c❤$");
    private static final Pattern DYING2 = Pattern.compile("^.+ (?:§.)+0§c❤$");

    private static final Minecraft mc = MinecraftCompat.getMinecraft();
    private volatile Set<EntityLivingBase> bloodMobs = new HashSet<>();
    private int tickCounter = 0;


    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickPhase.END) return;
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight == 2) {
            bloodMobs = new HashSet<>();
            return;
        }

        if (++tickCounter < 4) return;
        tickCounter = 0;

        if (!SkyblockData.isInDungeon() || MinecraftCompat.getLocalWorld() == null || !DungeonStats.isBloodActive()) {
            bloodMobs = new HashSet<>();
            return;
        }

        Set<EntityLivingBase> found = new HashSet<>();
        for (Entity entity : WorldCompat.getAllEntities(MinecraftCompat.getLocalWorld())) {
            if (!(entity instanceof EntityArmorStand)) continue;
            String name = TextCompat.getUnformattedText(entity.getName());
            if (name == null || !MOB_NAME.matcher(name).matches()) continue;

            EntityLivingBase mob = EntityCompat.findMobNearStand(MinecraftCompat.getLocalWorld(), entity, 0.5, 3.0, 0.5);

            if (mob != null && !isDying(mob)) found.add(mob);
        }
        bloodMobs = found;
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight != 1) return;
        EntityLivingBase entity = event.getEntity();
        if (!bloodMobs.contains(entity) || isDying(entity)) return;
        EntityRenderUtils.renderOutline(event, getColor());
    }

    @HandleEvent
    public void onRenderOutline(RenderEntityOutlineEvent event) {
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight != 1) return;
        int argb = getColor().getRGB();
        for (EntityLivingBase mob : bloodMobs) {
            if (EntityCompat.isDead(mob) || isDying(mob)) continue;
            event.queue(mob, argb);
        }
    }

    @HandleEvent
    public void onRenderWorldLast(ASMRenderWorldEvent event) {
        if (ATHRConfig.feature == null || ATHRConfig.feature.dungeons.bloodMobHighlight != 0) return;
        Map<EntityLivingBase, Color> mobs = new HashMap<>();
        Color color = getColor();
        for (EntityLivingBase mob : bloodMobs) mobs.put(mob, color);
        EntityRenderUtils.renderBoxes(mobs);
    }

    private Color getColor() {
        return ChromaColour.specialToColor(ATHRConfig.feature.dungeons.bloodMobColor);
    }

    private boolean isDying(EntityLivingBase entity) {
        if (entity == null || EntityCompat.isDead(entity) || entity.getHealth() <= 0.1f) return true;
        IChatComponent name = entity.getDisplayName();
        if (name == null) return false;
        String text = TextCompat.getFormattedText(name);
        return DYING1.matcher(text).matches() || DYING2.matcher(text).matches();
    }
}