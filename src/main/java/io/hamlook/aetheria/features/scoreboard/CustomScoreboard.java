package io.hamlook.aetheria.features.scoreboard;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.config.editors.ChromaColour;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.features.mining.fetchur.FetchurData;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import io.hamlook.aetheria.utils.overlay.Overlay;
import io.hamlook.aetheria.utils.overlay.OverlayUtils;
import io.hamlook.aetheria.features.storage.StorageManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.regex.Pattern;

@RegisterEvents
public class CustomScoreboard extends Overlay {

    private static final int PAD_X = 4;
    private static final int PAD_Y = 4;
    private static final int LINE_GAP = 1;
    private static final int SUPERSAMPLE = 2;

    // Re-parse every 4 client ticks (~200ms at 20 tps)
    private static final int PARSE_INTERVAL = 4;

    // ── known line IDs ────────────────────────────────────────────────────────
    private static final int LINE_SERVER       = 0;
    private static final int LINE_TIME         = 1;
    private static final int LINE_PROFILE_TYPE = 2;
    private static final int LINE_SEASON       = 3;
    private static final int LINE_ISLAND       = 4;
    private static final int LINE_LOCATION     = 5;
    private static final int LINE_EMPTY1       = 6;
    private static final int LINE_PURSE        = 7;
    private static final int LINE_BANK         = 8;
    private static final int LINE_POWDER       = 9;
    private static final int LINE_HEAT         = 10;
    private static final int LINE_BITS         = 11;
    private static final int LINE_GEMS         = 12;
    private static final int LINE_NORTHSTARS   = 13;
    private static final int LINE_EVENT        = 14;
    private static final int LINE_POWER        = 15;
    private static final int LINE_COOKIE       = 16;
    private static final int LINE_EMPTY3       = 17;
    private static final int LINE_FETCHUR      = 18;
    private static final int LINE_SLAYER       = 19;
    private static final int LINE_EMPTY4       = 20;
    private static final int LINE_EMPTY5       = 21;
    private static final int LINE_EMPTY6       = 22;
    private static final int LINE_EMPTY7       = 23;
    private static final int LINE_EXTRA        = 24;
    private static final int LINE_EMPTY2       = 25;

    private static final String LOC_SYMBOL_NORMAL = "⏣";
    private static final String LOC_SYMBOL_RIFT   = "ф";

    private static final Pattern SERVER_PATTERN           = Pattern.compile("\\s*\\d{2}/\\d{2}/\\d{2}.*");
    private static final Pattern SEASON_PATTERN           = Pattern.compile("\\s*(?:(?:Late|Early) )?(?:Spring|Summer|Autumn|Winter) \\d+.*");
    private static final Pattern TIME_PATTERN             = Pattern.compile("\\s*\\d+:\\d+(?:am|pm).*");
    private static final Pattern PROFILE_TYPE_PATTERN     = Pattern.compile("Ironman|Stranded|Bingo|Classic");
    private static final Pattern PURSE_PATTERN            = Pattern.compile("(?:Piggy|Purse): [\\d,.]+");
    private static final Pattern BANK_PATTERN             = Pattern.compile("Bank: .+");
    private static final Pattern BITS_PATTERN             = Pattern.compile("Bits: [\\d,.]+");
    private static final Pattern EVENT_PATTERN            = Pattern.compile("(?:Fishing Festival|Mining Fiesta|Spooky Festival|Season of Jerry|Traveling Zoo|New Year Celebration|Election|Fallen Star|Festival of Gifts).*");
    private static final Pattern SLAYER_PATTERN           = Pattern.compile("Slayer Quest");
    private static final Pattern COOKIE_SUPPRESS_PATTERN = Pattern.compile("Cookie Buff.*|\\d+d\\s+\\d+h.*");
    private static final Pattern NORTHSTARS_PATTERN       = Pattern.compile("North Stars: [\\d,]+");
    private static final Pattern HEAT_PATTERN             = Pattern.compile("Heat: .+");
    private static final Pattern WEBSITE_PATTERN          = Pattern.compile(".*fakepixel.*");

    @Getter
    private static CustomScoreboard instance;

    private List<String> cachedLines = new ArrayList<>();

    // Reused every render frame — avoids allocating a new ArrayList per frame
    private final List<String> displayLines = new ArrayList<>();

    private List<Integer> cachedLineOrder    = null;
    private List<?>       lastLineOrderSource = null;

    private int     lastRawHash = 0;
    private boolean wasDown     = false;
    private int     tickCounter = 0;

    public CustomScoreboard() {
        super(130, 90);
        instance = this;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    public static boolean isActive() {
        return ATHRConfig.feature != null && ATHRConfig.feature.scoreboard.enabled;
    }

    private List<Integer> getLineOrder() {
        List<?> src = ATHRConfig.feature.scoreboard.scoreboardLines;
        if (src == lastLineOrderSource && cachedLineOrder != null) return cachedLineOrder;
        List<Integer> result = new ArrayList<>();
        if (src != null)
            for (Object o : src)
                if (o instanceof Number) result.add(((Number) o).intValue());
        lastLineOrderSource = src;
        cachedLineOrder = result;
        return result;
    }

    private static String formatPowder(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000.0);
        return Long.toString(v);
    }

    private String toTitleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.toLowerCase().split("_"))
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        return sb.toString().trim();
    }

    // ── Overlay contract ──────────────────────────────────────────────────────

    @Override public Position getPosition()    { return ATHRConfig.feature.scoreboard.position; }
    @Override public float   getScale()        { return ATHRConfig.feature.scoreboard.scale; }
    @Override public int     getBgColor()      { return ChromaColour.specialToChromaRGB(ATHRConfig.feature.scoreboard.scoreboardBg); }
    @Override public int     getCornerRadius() { return (int) ATHRConfig.feature.scoreboard.cornerRadius; }
    @Override protected boolean extraGuard()   { return isActive(); }
    @Override protected boolean isEnabled()    {
        return isActive()
                && !Minecraft.getMinecraft().gameSettings.showDebugInfo
                && !StorageManager.isOverlayActive();
    }

    /**
     * Returns body lines from cache.
     * Title is excluded — render prepends it fresh every frame for smooth chroma animation.
     * Preview mode always builds live so the config GUI shows current data.
     */
    @Override
    public List<String> getLines(boolean preview) {
        if (preview) return buildLines(SkyblockData.getScoreboardLines());
        return cachedLines;
    }

    // ── tick-driven parse ─────────────────────────────────────────────────────

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isActive()) return;
        if ((tickCounter = (tickCounter + 1) % PARSE_INTERVAL) != 0) return;

        List<String> raw = SkyblockData.getScoreboardLines();

        int hash = raw.hashCode();
        if (hash == lastRawHash) return;
        lastRawHash = hash;

        List<String> built = buildLines(raw);
        cachedLines = built;

        List<String> clean = new ArrayList<>(built.size());
        for (String line : built) clean.add(ColorUtils.stripColor(line));
        CustomScoreboardAPI.update(clean);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        cachedLines = new ArrayList<>();
        lastRawHash = 0;
        tickCounter = 0;
    }

    // ── parse + transform ─────────────────────────────────────────────────────

    /**
     * Pure parse+transform. Accepts already-read raw lines so onTick and preview
     * don't each make a redundant getScoreboardLines() call.
     * Title is NOT included in the output — render prepends it fresh every frame.
     */
    private List<String> buildLines(List<String> rawIn) {
        if (rawIn.isEmpty()) return new ArrayList<>();

        List<String> raw = new ArrayList<>(rawIn);
        Collections.reverse(raw);

        // Pre-strip all lines once so the parse loop never calls stripColor per-line
        String[] stripped = new String[raw.size()];
        for (int i = 0; i < raw.size(); i++)
            stripped[i] = ColorUtils.stripColor(raw.get(i)).trim();

        // Outside Skyblock — return vanilla lines as-is
        if (!SkyblockData.isOnSkyblock()) {
            return new ArrayList<>(raw);
        }

        boolean inDungeon = SkyblockData.isInDungeon();

        String serverRaw      = null;
        String seasonRaw      = null;
        String timeRaw        = null;
        String locationRaw    = null;
        String purseRaw       = null;
        String bankRaw        = null;
        String bitsRaw        = null;
        String profileTypeRaw = null;
        String northStarsRaw  = null;
        String heatRaw        = null;
        String websiteRaw     = null;
        List<String> eventLines  = new ArrayList<>();
        List<String> slayerLines = new ArrayList<>();
        Set<String>  claimed     = new LinkedHashSet<>();

        for (int i = 0; i < raw.size(); i++) {
            String l = raw.get(i);
            String c = stripped[i];
            if (c.isEmpty()) continue;

            if (locationRaw == null && (l.contains(LOC_SYMBOL_NORMAL) || l.contains(LOC_SYMBOL_RIFT))) {
                locationRaw = l; claimed.add(l); continue;
            }
            if (serverRaw == null && SERVER_PATTERN.matcher(c).matches()) {
                serverRaw = l; claimed.add(l); continue;
            }
            if (seasonRaw == null && SEASON_PATTERN.matcher(c).matches()) {
                seasonRaw = l; claimed.add(l); continue;
            }
            if (timeRaw == null && TIME_PATTERN.matcher(c).matches()) {
                timeRaw = l; claimed.add(l); continue;
            }
            if (purseRaw == null && PURSE_PATTERN.matcher(c).find()) {
                purseRaw = l; claimed.add(l); continue;
            }
            if (bankRaw == null && BANK_PATTERN.matcher(c).find()) {
                bankRaw = l; claimed.add(l); continue;
            }
            if (bitsRaw == null && BITS_PATTERN.matcher(c).find()) {
                bitsRaw = l; claimed.add(l); continue;
            }
            if (COOKIE_SUPPRESS_PATTERN.matcher(c).find()) {
                claimed.add(l); continue;
            }
            if (profileTypeRaw == null && PROFILE_TYPE_PATTERN.matcher(c).find()) {
                profileTypeRaw = l; claimed.add(l); continue;
            }
            if (EVENT_PATTERN.matcher(c).find()) {
                eventLines.add(l); claimed.add(l);
                if (i + 1 < raw.size() && !stripped[i + 1].isEmpty()) {
                    eventLines.add(raw.get(i + 1)); claimed.add(raw.get(i + 1)); i++;
                }
                continue;
            }
            if (slayerLines.isEmpty() && SLAYER_PATTERN.matcher(c).find()) {
                claimed.add(l); slayerLines.add(l);
                for (int off = 1; off <= 2 && (i + off) < raw.size(); off++) {
                    String next = raw.get(i + off);
                    if (!stripped[i + off].isEmpty()) { slayerLines.add(next); claimed.add(next); }
                }
                i += 2;
                continue;
            }
            if (websiteRaw == null && WEBSITE_PATTERN.matcher(c).find()) {
                websiteRaw = l; claimed.add(l); continue;
            }
            if (northStarsRaw == null && NORTHSTARS_PATTERN.matcher(c).find()) {
                northStarsRaw = l; claimed.add(l); continue;
            }
            if (heatRaw == null && HEAT_PATTERN.matcher(c).find()) {
                heatRaw = l; claimed.add(l);
            }
        }

        List<String> unknownLines = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String l = raw.get(i);
            if (claimed.contains(l)) continue;
            if (stripped[i].isEmpty()) continue;
            UnknownLinesHandler.handle(l);
            unknownLines.add(l);
        }

        List<String> lines = new ArrayList<>();

        for (int id : getLineOrder()) {
            switch (id) {
                case LINE_SERVER:
                    if (serverRaw != null) lines.add(serverRaw);
                    break;
                case LINE_SEASON:
                    if (seasonRaw != null) lines.add(seasonRaw);
                    break;
                case LINE_TIME:
                    if (timeRaw != null) lines.add(timeRaw);
                    break;
                case LINE_PROFILE_TYPE:
                    if (profileTypeRaw != null) lines.add(profileTypeRaw);
                    break;
                case LINE_ISLAND: {
                    SkyblockData.Location loc = SkyblockData.getCurrentLocation();
                    if (loc != SkyblockData.Location.NONE) {
                        String name;
                        if      (loc == SkyblockData.Location.CRIMSON_ISLE) name = "Crimson Isles";
                        else if (loc == SkyblockData.Location.HUB)          name = "Skyblock Hub";
                        else                                                  name = toTitleCase(loc.name());
                        lines.add("㋖ §b" + name);
                    }
                    break;
                }
                case LINE_LOCATION:
                    if (locationRaw != null) lines.add(locationRaw);
                    break;
                case LINE_PURSE:
                    if (purseRaw != null) lines.add(purseRaw);
                    break;
                case LINE_BANK:
                    if (!inDungeon) {
                        if (bankRaw != null) {
                            lines.add(bankRaw);
                        } else {
                            String bank = BankParser.getBank();
                            if (bank != null) lines.add("§fBank: §6" + bank);
                        }
                    }
                    break;
                case LINE_BITS:
                    if (bitsRaw != null) lines.add(bitsRaw);
                    break;
                case LINE_POWDER:
                    if (!inDungeon
                            && (SkyblockData.getCurrentLocation() == SkyblockData.Location.DWARVEN
                            || SkyblockData.getCurrentLocation() == SkyblockData.Location.CRYSTAL_HOLLOWS)) {
                        long mithril  = TablistParser.getMithrilPowder();
                        long gemstone = TablistParser.getGemstonePowder();
                        long glacite  = TablistParser.getGlacitePowder();
                        if (mithril > 0 || gemstone > 0 || glacite > 0) {
                            lines.add("§9§lPowder");
                            if (mithril  > 0) lines.add(" §7- §fMithril: §2"  + formatPowder(mithril));
                            if (gemstone > 0) lines.add(" §7- §fGemstone: §d" + formatPowder(gemstone));
                            if (glacite  > 0) lines.add(" §7- §fGlacite: §b"  + formatPowder(glacite));
                        }
                    }
                    break;
                case LINE_GEMS:
                    if (!inDungeon) {
                        String gems = TablistParser.readGems();
                        if (gems != null) lines.add("§fGems: §a" + gems);
                    }
                    break;
                case LINE_EVENT:
                    lines.addAll(eventLines);
                    break;
                case LINE_COOKIE:
                    if (!inDungeon) {
                        String cookie = TablistParser.readCookieBuff();
                        if (cookie != null && !cookie.toLowerCase().contains("not active"))
                            lines.add("§dCookie Buff: §f" + cookie);
                    }
                    break;
                case LINE_POWER: {
                    String power = MaxwellPowerSync.getPower();
                    if (power != null) lines.add("§fPower: §d" + power);
                    break;
                }
                case LINE_FETCHUR:
                    lines.add("§fFetchur: §e" + FetchurData.getTodaysItem());
                    break;
                case LINE_SLAYER:
                    if (!inDungeon) lines.addAll(slayerLines);
                    break;
                case LINE_EXTRA:
                    if (!inDungeon) lines.addAll(unknownLines);
                    break;
                case LINE_NORTHSTARS:
                    if (northStarsRaw != null) lines.add(northStarsRaw);
                    break;
                case LINE_HEAT:
                    if (heatRaw != null) lines.add(heatRaw);
                    break;
                case LINE_EMPTY1: case LINE_EMPTY2: case LINE_EMPTY3: case LINE_EMPTY4:
                case LINE_EMPTY5: case LINE_EMPTY6: case LINE_EMPTY7:
                    if (!inDungeon) lines.add("");
                    break;
            }
        }

        if (inDungeon && !unknownLines.isEmpty())
            lines.addAll(unknownLines);

        if (websiteRaw != null) lines.add(websiteRaw);

        return lines;
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(boolean preview) {
        if (!preview && !extraGuard()) return;
        if (!preview && ATHRConfig.feature.scoreboard.hideOnTab && OverlayUtils.shouldHide()) return;

        // Title fetched fresh every frame so chroma/rainbow animation plays smoothly.
        // Body lines come from the 4-tick cache.
        String title = SkyblockData.getScoreboardTitle();
        List<String> body = getLines(preview);

        // Reuse displayLines to avoid allocating a new ArrayList every frame
        displayLines.clear();
        if (title != null && !title.isEmpty()) displayLines.add(title);
        displayLines.addAll(body);

        if (displayLines.isEmpty()) return;

        boolean down = Keyboard.isKeyDown(ATHRConfig.feature.debug.scoreboardDebugConfig.scoreboardDebugKey);
        if (down && !wasDown && ATHRConfig.feature.debug.scoreboardDebugConfig.scoreboardDebug)
            ChatUtils.sendMessage(CustomScoreboardAPI.toJson());
        wasDown = down;

        Minecraft mc  = Minecraft.getMinecraft();
        float scale   = getScale();
        int lh        = LINE_HEIGHT + LINE_GAP;
        int ss        = SUPERSAMPLE;
        int alignment = ATHRConfig.feature.scoreboard.lineAlignment;
        int minWidth  = ATHRConfig.feature.scoreboard.minWidth;

        // Measure all lines once, cache widths for reuse during drawing
        int maxW = minWidth;
        int[] widths = new int[displayLines.size()];
        for (int i = 0; i < displayLines.size(); i++) {
            widths[i] = mc.fontRendererObj.getStringWidth(displayLines.get(i));
            if (widths[i] > maxW) maxW = widths[i];
        }

        int boxW = maxW + PAD_X * 2;
        int boxH = displayLines.size() * lh + PAD_Y * 2 - LINE_GAP;
        lastW = boxW;
        lastH = boxH;

        ScaledResolution sr = new ScaledResolution(mc);
        Position pos = getPosition();

        int x = pos.getAbsX(sr, (int) (boxW * scale));
        int y = pos.getAbsY(sr, (int) (boxH * scale));
        if (pos.isCenterX()) x -= (int) (boxW * scale / 2);
        if (pos.isCenterY()) y -= (int) (boxH * scale / 2);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale / ss, scale / ss, 1f);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int bgColor = getBgColor();
        if ((bgColor >>> 24) != 0)
            drawRoundedRect(0, 0, boxW * ss, boxH * ss, getCornerRadius() * ss, bgColor);

        GL11.glScalef(ss, ss, 1f);

        int textY = PAD_Y;
        // Line 0 is always the title — centered
        mc.fontRendererObj.drawStringWithShadow(displayLines.get(0), (boxW - widths[0]) / 2, textY, -1);
        textY += lh;

        for (int i = 1; i < displayLines.size(); i++) {
            int lx;
            switch (alignment) {
                case 1:  lx = PAD_X + (boxW - PAD_X * 2 - widths[i]) / 2; break;
                case 2:  lx = boxW - PAD_X - widths[i]; break;
                default: lx = PAD_X; break;
            }
            mc.fontRendererObj.drawStringWithShadow(displayLines.get(i), lx, textY, 0xFFFFFF);
            textY += lh;
        }

        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }
}