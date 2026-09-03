package io.hamlook.aetheria.features.misc.ghosttracker;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.misc.GhostTrackerConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.Overlay;
import io.hamlook.aetheria.utils.time.TimeFormatter;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class GhostOverlay extends Overlay {

    private static final int ICON_SIZE = 8;
    private static final int ICON_PAD = 2;
    private static final int COLOR_BLUE = 0x5555FF;
    private static final int COLOR_GOLD = 0xFFAA00;
    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_AQUA = 0x55FFFF;
    @Getter
    private static GhostOverlay instance;
    private final GhostStats stats = GhostStats.getInstance();

    public GhostOverlay() {
        super(140, 30);
        instance = this;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.misc.ghostTrackerConfig.ghostOverlayPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.misc.ghostTrackerConfig.ghostScale;
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.misc.ghostTrackerConfig.ghostCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature != null && ATHRConfig.feature.misc.ghostTrackerConfig.ghostTrackerEnabled && SkyblockData.isOnSkyblock() && SkyblockData.getCurrentLocation() == SkyblockData.Location.DWARVEN
                && SkyblockData.isInMist();
    }

    @Override
    protected boolean hideOnChat()   { return ATHRConfig.feature.misc.ghostTrackerConfig.hideOnChat; }
    @Override
    protected boolean hideOnTab()    { return ATHRConfig.feature.misc.ghostTrackerConfig.hideOnTab; }
    @Override
    protected boolean hideOnDebug()  { return ATHRConfig.feature.misc.ghostTrackerConfig.hideOnDebug; }

    @Override
    public int getBgColor() {
        try {
            String[] parts = ATHRConfig.feature.misc.ghostTrackerConfig.ghostBgColor.split(":");
            int a = Integer.parseInt(parts[1]);
            int r = Integer.parseInt(parts[2]);
            int g = Integer.parseInt(parts[3]);
            int b = Integer.parseInt(parts[4]);
            return (a << 24) | (r << 16) | (g << 8) | b;
        } catch (Exception e) {
            return 0x88000000;
        }
    }

    @Override
    public List<String> getLines(boolean preview) {
        return new ArrayList<>();
    }

    @Override
    public void render(boolean preview) {
        if (!preview && !extraGuard()) return;

        Minecraft mc = MinecraftCompat.getMinecraft();
        float scale = getScale();
        GhostTrackerConfig cfg = ATHRConfig.feature.misc.ghostTrackerConfig;

        double kph = stats.getKillsPerHour();
        double xph = stats.getXpPerHour();

        HudEntry[] allLines = {
                // 0  Kills
                new HudEntry("Kills: ", Utils.shortNumberFormat(stats.totalKills, 0), Resources.GHOSTTRACKER_KILLS, COLOR_RED),
                // 1  Kills/h
                new HudEntry("Kills/h: ", kph > 0 ? Utils.shortNumberFormat((long) kph, 0) : "-", Resources.GHOSTTRACKER_KILLS, COLOR_RED),
                // 2  XP
                new HudEntry("XP: ", Utils.shortNumberFormat(stats.totalXp, 0), Resources.GHOSTTRACKER_KILLS, COLOR_GOLD),
                // 3  XP/h
                new HudEntry("XP/h: ", xph > 0 ? Utils.shortNumberFormat((long) xph, 0) : "-", Resources.GHOSTTRACKER_KILLS, COLOR_GOLD),
                // 4  Sorrows
                new HudEntry("Sorrows: ", stats.totalSorrow + " §7(" + stats.killsSinceLastSorrow + ")", Resources.GHOSTTRACKER_SORROW, COLOR_BLUE),
                // 5  Voltas
                new HudEntry("Voltas: ", stats.totalVolta + " §7(" + stats.killsSinceLastVolta + ")", Resources.GHOSTTRACKER_VOLTA, COLOR_BLUE),
                // 6  Plasmas
                new HudEntry("Plasmas: ", stats.totalPlasma + " §7(" + stats.killsSinceLastPlasma + ")", Resources.GHOSTTRACKER_PLASMA, COLOR_BLUE),
                // 7  Boots
                new HudEntry("Boots: ", stats.totalBoots + " §7(" + stats.killsSinceLastBoots + ")", Resources.GHOSTTRACKER_BOOTS, COLOR_BLUE),
                // 8  Bag of Cash
                new HudEntry("Bag of Cash: ", stats.totalBagOfCash + " §7(" + stats.killsSinceLastBagOfCash + ")", Resources.GHOSTTRACKER_COINS, COLOR_BLUE),
                // 9  Scavenger
                new HudEntry("Scavenger: ", Utils.shortNumberFormat(stats.totalScavenger, 0), Resources.GHOSTTRACKER_COINS, COLOR_GOLD),
                // 10 Avg MF
                new HudEntry("Avg MF: ", String.format("%.0f", stats.avgMagicFind()), Resources.GHOSTTRACKER_MAGIC_FIND, COLOR_AQUA),
                // 11 Best MF
                new HudEntry("Best MF: ", stats.highestMagicFind > 0 ? String.valueOf(stats.highestMagicFind) : "-", Resources.GHOSTTRACKER_MAGIC_FIND, COLOR_AQUA),
                // 12 Session Time
                new HudEntry("Time: ", TimeFormatter.formatCountdown(stats.getSessionDurationMs()), Resources.GHOSTTRACKER_TIME, COLOR_GOLD),
                // 13 Estimated Profit
                new HudEntry("Estimated Profit: ", Utils.shortNumberFormat(stats.getEstimatedProfit(), 0), Resources.GHOSTTRACKER_MONEY, COLOR_GOLD),};

        List<HudEntry> entries = new ArrayList<>();
        for (int idx : cfg.ghostTrackerLines) {
            if (idx >= 0 && idx < allLines.length) {
                entries.add(allLines[idx]);
            }
        }

        if (entries.isEmpty()) return;

        int maxW = 0;
        for (HudEntry e : entries) {
            String plain = e.label + e.value.replaceAll("§.", "");
            int w = mc.fontRendererObj.getStringWidth(plain) + ICON_SIZE + ICON_PAD * 3 + 4;
            maxW = Math.max(maxW, w);
        }

        String header = "§8Ghost Tracker";
        int headerW = mc.fontRendererObj.getStringWidth(header) + 6;
        int w = Math.max(maxW + 6, headerW);
        int h = entries.size() * LINE_HEIGHT + LINE_HEIGHT + 4; // header + entries
        lastW = w;
        lastH = h;

        Position pos = getPosition();
        int x = pos.getAbsX(sr, (int) (w * scale));
        int y = pos.getAbsY(sr, (int) (h * scale));
        if (pos.isCenterX()) x -= (int) (w * scale / 2);
        if (pos.isCenterY()) y -= (int) (h * scale / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1f);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0) drawRoundedRect(-3, -3, w, h - 3, getCornerRadius(), bgColor);

        GlStateManagerCompat.enableAlpha();
        GlStateManagerCompat.enableBlend();

        mc.fontRendererObj.drawStringWithShadow(header, 3, 3, 0xFFFFFF);

        int dy = 3 + LINE_HEIGHT;
        for (HudEntry e : entries) {
            mc.getTextureManager().bindTexture(e.icon);
            Utils.drawTexturedRect(ICON_PAD, dy + 1, ICON_SIZE, ICON_SIZE);
            int textX = ICON_SIZE + ICON_PAD * 2;
            mc.fontRendererObj.drawStringWithShadow(e.label, textX, dy, 0xFFFFFF);
            String[] parts = e.value.split(" §7\\(");
            String mainVal = parts[0];
            int labelEnd = textX + mc.fontRendererObj.getStringWidth(e.label);
            mc.fontRendererObj.drawStringWithShadow(mainVal, labelEnd, dy, e.color);
            if (parts.length > 1) {
                int mainEnd = labelEnd + mc.fontRendererObj.getStringWidth(mainVal);
                mc.fontRendererObj.drawStringWithShadow(" §7(" + parts[1], mainEnd, dy, 0xFFFFFF);
            }
            dy += LINE_HEIGHT;
        }

        GL11.glPopMatrix();
    }

    private static class HudEntry {
        String label, value;
        ResourceLocation icon;
        int color;

        HudEntry(String label, String value, ResourceLocation icon, int color) {
            this.label = label;
            this.value = value;
            this.icon = icon;
            this.color = color;
        }
    }
}
