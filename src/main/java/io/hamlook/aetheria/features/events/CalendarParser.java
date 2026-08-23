package io.hamlook.aetheria.features.events;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.profile.GuiWaiter;
import io.hamlook.aetheria.features.profile.ProfileParser;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a real, accurate event schedule by walking the in-game SkyBlock Calendar's month pages,
 * instead of trusting capeapi's events endpoint — whose Farming Contest crop data turned out to
 * be wrong at the source (confirmed by comparing fetched crops against what actually appeared
 * in-game). Only ever runs when the user clicks the "Parse" button this class draws over the
 * calendar's day-grid page — never passively/automatically, so a heavier multi-page walk (clicking
 * through up to a full SkyBlock year) only happens on demand, as a deliberate safety measure.
 * <p>
 * Each day-slot's lore carries a live "time until" countdown, but precision drops the further out
 * an entry is (far pages show only e.g. "(1d 13h)", no minutes/seconds) — nowhere near tight
 * enough for 5-minute/1-minute pre-event thresholds, and defeats the whole point of parsing only
 * once a year instead of re-fetching for precision. Since one SkyBlock day is exactly 20 real
 * minutes on this server, every entry's exact start time is instead computed by pure day-offset
 * arithmetic from a single anchor (the first, most-precise mention seen). Longer events
 * (Spooky/Zoo/New Year/Jerry) get previewed on several consecutive day-slots before they happen —
 * each is a real, individually-correct day-offset computation, so the fix isn't in the arithmetic,
 * it's a same-type/near-time upsert in {@link #handleSegment} that collapses those re-mentions
 * back into the single occurrence they describe. Anything that computes to a moment already in
 * the past is dropped, per instructions.
 */
@RegisterEvents
public class CalendarParser {

    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int BACK_SLOT = 48;

    private static final Pattern DAY_SLOT_NAME = Pattern.compile("^Day (\\d+)$");
    private static final Pattern SEGMENT_START = Pattern.compile(
            "^(?:All day: |\\d{1,2}:\\d{2}(?:am|pm)(?:-\\d{1,2}:\\d{2}(?:am|pm))?: )(.*)$");
    private static final Pattern ORDINAL_PREFIX = Pattern.compile("^[\\d,]+(?:st|nd|rd|th)\\s+(.*)$");
    private static final Pattern COUNTDOWN = Pattern.compile("^(.*?)\\s*\\(([^)]+)\\)$");
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([dhms])");

    /** First empty filler slot right after Day 31 (slot 39) on the calendar's day-grid page —
     *  slots 40-44 are unused space in that layout, confirmed via recon before this shipped. */
    private static final int ANCHOR_SLOT = 40;
    /** Exactly 3 slot-cells wide, 1 slot-cell tall (18px each) — {@code -1} in {@link #buttonPos}
     *  on both axes lands on the anchor slot's cell border (matching how vanilla draws the 18x18
     *  slot-cell background a pixel before the 16x16 icon itself starts), so the button fully
     *  tiles the same grid area a real 1x3 row of slots would occupy. */
    private static final int BTN_W = 54, BTN_H = 18;

    private boolean parsing = false;
    private final List<SkyblockEvent> parsed = new ArrayList<>();
    private int currentPage = 1;

    // ---- Overlay button: drawn over the calendar's day-grid page, never touching real slots ----

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (parsing) return;
        if (!(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        Container inv = containerOf(gui);
        if (inv == null || !isCalendarDayGrid(inv)) return;

        int[] pos = buttonPos(gui, inv);
        int btnX = pos[0], btnY = pos[1];

        int mouseX = KeybindHelper.getScaledEventX(event.gui.width);
        int mouseY = KeybindHelper.getScaledEventY(event.gui.height);
        boolean hovered = mouseX >= btnX && mouseX < btnX + BTN_W && mouseY >= btnY && mouseY < btnY + BTN_H;

        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.color(1f, 1f, 1f, 1f);
        if (hovered) {
            GlStateManager.color(0.75f, 1f, 0.75f, 1f);
            mc.getTextureManager().bindTexture(Resources.button_white);
        } else {
            mc.getTextureManager().bindTexture(Resources.button_tex);
        }
        RenderUtils.drawTexturedRect(btnX, btnY, BTN_W, BTN_H);
        GlStateManager.color(1f, 1f, 1f, 1f);

        // Flat, no drop shadow — matches how GuiContainer draws this same screen's own
        // "Summer, Year 293" title text (vanilla's foreground-layer title color, 0x404040).
        String label = "Parse";
        mc.fontRendererObj.drawString(label,
                btnX + (BTN_W - mc.fontRendererObj.getStringWidth(label)) / 2,
                btnY + (BTN_H - mc.fontRendererObj.FONT_HEIGHT) / 2, 0x404040);
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (parsing) return;
        if (!(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        Container inv = containerOf(gui);
        if (inv == null || !isCalendarDayGrid(inv)) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) return;

        int[] pos = buttonPos(gui, inv);
        int mouseX = KeybindHelper.getScaledEventX(event.gui.width);
        int mouseY = KeybindHelper.getScaledEventY(event.gui.height);
        if (mouseX >= pos[0] && mouseX < pos[0] + BTN_W && mouseY >= pos[1] && mouseY < pos[1] + BTN_H) {
            event.setCanceled(true);
            startParse((ContainerChest) inv);
        }
    }

    private static int[] buttonPos(GuiContainer gui, Container inv) {
        Slot anchor = inv.getSlot(ANCHOR_SLOT);
        int x = gui.guiLeft + anchor.xDisplayPosition - 1;
        int y = gui.guiTop + anchor.yDisplayPosition - 1;
        return new int[]{x, y};
    }

    private static Container containerOf(GuiContainer gui) {
        Container inv = gui.inventorySlots;
        return inv instanceof ContainerChest ? inv : null;
    }

    private static boolean isCalendarDayGrid(Container inv) {
        for (Slot slot : inv.inventorySlots) {
            ItemStack stack = slot.getStack();
            if (stack != null && DAY_SLOT_NAME.matcher(ColorUtils.stripColor(stack.getDisplayName()).trim()).matches()) {
                return true;
            }
        }
        return false;
    }

    // ---- Parse orchestration — reuses GuiWaiter's existing auto-page-walk machinery ----

    private void startParse(ContainerChest chest) {
        parsing = true;
        anchorRealMs = null;
        parsed.clear();

        currentPage = readStartingPage(chest);
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Parsing SkyBlock Calendar...");
        GuiWaiter.waitForPaged("*", 5, NEXT_PAGE_SLOT, "Next Page", BACK_SLOT, null, this::parsePage, null);
    }

    /** The calendar may already be open on any page when "Parse" is clicked — "Previous Page"'s
     *  own lore prints "[Page N]" for the page it would take you to, so the current page is N+1;
     *  no "Previous Page" slot present at all means this already is page 1. */
    private static int readStartingPage(ContainerChest chest) {
        Slot prevSlot = chest.getSlot(PREV_PAGE_SLOT);
        if (prevSlot == null || !prevSlot.getHasStack()) return 1;
        for (String line : ProfileParser.getLore(prevSlot.getStack())) {
            Matcher m = Pattern.compile("\\[Page (\\d+)\\]").matcher(line);
            if (m.find()) return Integer.parseInt(m.group(1)) + 1;
        }
        return 1;
    }

    private void parsePage(ContainerChest chest) {
        if (chest == null) {
            finishParse();
            return;
        }

        long now = System.currentTimeMillis();
        for (Slot slot : chest.inventorySlots) {
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            Matcher dayMatch = DAY_SLOT_NAME.matcher(ColorUtils.stripColor(stack.getDisplayName()).trim());
            if (!dayMatch.matches()) continue;

            // One "Day N" slot's lore is a list of separate lines (one per event/crop mentioned
            // that day), not one comma-joined line — a crop line only makes sense attached to
            // whichever Farming Contest line immediately preceded it, so this has to persist
            // across the whole slot's lines, not reset every line.
            int dayInPage = Integer.parseInt(dayMatch.group(1));
            long absoluteSkyblockDay = (long) (currentPage - 1) * DAYS_PER_PAGE + dayInPage;

            SkyblockEvent lastEvent = null;
            for (String line : ProfileParser.getLore(stack)) {
                if (line == null || line.isEmpty() || "No events".equals(line)) continue;
                for (String rawSegment : line.split(", ")) {
                    Matcher segStart = SEGMENT_START.matcher(rawSegment);
                    if (segStart.matches()) {
                        String rest = segStart.group(1);
                        SkyblockEvent added = handleSegment(rest, absoluteSkyblockDay, now);
                        if (added != null) lastEvent = added;
                    } else if (lastEvent != null && "Farming Contest".equals(lastEvent.event.event)) {
                        String crop = rawSegment.replaceFirst("^[^a-zA-Z]*", "").trim();
                        if (!crop.isEmpty()) {
                            if (lastEvent.event.crops == null) lastEvent.event.crops = new ArrayList<>();
                            lastEvent.event.crops.add(crop);
                        }
                    }
                }
            }
        }

        boolean hasNextPage = false;
        Slot nextSlot = chest.getSlot(NEXT_PAGE_SLOT);
        if (nextSlot != null && nextSlot.getHasStack()) {
            hasNextPage = "Next Page".equals(ColorUtils.stripColor(nextSlot.getStack().getDisplayName()).trim());
        }

        if (hasNextPage) {
            currentPage++;
        } else {
            finishParse();
        }
    }

    /** One SkyBlock day is exactly 20 real minutes on this server — used to compute every
     *  entry's exact start time from its absolute day number, rather than trusting each mention's
     *  own printed countdown (which drops precision the further out it is — far pages show only
     *  e.g. "(1d 13h)", no minutes/seconds — nowhere near tight enough for 5-minute/1-minute
     *  pre-event thresholds, and the whole point is parsing once a year, not re-parsing to patch
     *  precision back in). The very first mention encountered (almost always an imminent hourly
     *  Farming Contest/Dark Auction) anchors real-time to an absolute SkyBlock day; every other
     *  entry, however far out, is then exact arithmetic from that one reference point. */
    private static final long SKYBLOCK_DAY_MS = 20 * 60_000L;
    private static final int DAYS_PER_PAGE = 31;
    private Long anchorRealMs = null;
    private long anchorSkyblockDay = 0;

    /**
     * Longer events (Spooky/Zoo/New Year/Jerry) get previewed on several consecutive day-slots
     * before they actually happen — each preview mention is its own real day-slot, so day-offset
     * arithmetic correctly computes a different (and each individually correct) time for every one
     * of them, 20-40 minutes apart; the fix isn't in the arithmetic, it's recognizing they're all
     * describing the same occurrence. Walking days in order means the last mention seen in a
     * cluster is always the closest to the true day, so dedup keeps whichever computes later.
     * 45 minutes is comfortably under Farming Contest's real 60-minute gap between genuine hourly
     * occurrences, so those are never merged into each other.
     */
    private static final long DEDUPE_WINDOW_MS = 45 * 60_000L;

    private SkyblockEvent handleSegment(String rest, long absoluteSkyblockDay, long now) {
        String withoutCountdown = rest;
        Long deltaMs = null;
        Matcher countdown = COUNTDOWN.matcher(rest);
        if (countdown.matches()) {
            withoutCountdown = countdown.group(1).trim();
            deltaMs = parseDuration(countdown.group(2));
        }
        if (deltaMs == null) return null; // no countdown at all => already past

        Matcher ordinal = ORDINAL_PREFIX.matcher(withoutCountdown);
        String typeName = normalizeType(ordinal.matches() ? ordinal.group(1) : withoutCountdown);
        if (typeName == null) return null; // unrecognized segment — ignore rather than guess

        if (anchorRealMs == null) {
            anchorRealMs = now + deltaMs;
            anchorSkyblockDay = absoluteSkyblockDay;
        }
        long startMs = anchorRealMs + (absoluteSkyblockDay - anchorSkyblockDay) * SKYBLOCK_DAY_MS;
        if (startMs <= now) return null; // already over

        for (SkyblockEvent existing : parsed) {
            if (!typeName.equals(existing.event.event)) continue;
            long existingStart = Instant.parse(existing.start).toEpochMilli();
            if (Math.abs(existingStart - startMs) >= DEDUPE_WINDOW_MS) continue;
            if (startMs > existingStart) {
                // A later day-slot mention of the same occurrence, closer to the true day —
                // refine in place rather than add a duplicate. Keep crops already collected.
                existing.start = Instant.ofEpochMilli(startMs).toString();
                existing.end = Instant.ofEpochMilli(startMs + durationForType(typeName)).toString();
            }
            return existing;
        }

        SkyblockEvent event = new SkyblockEvent();
        event.start = Instant.ofEpochMilli(startMs).toString();
        event.end = Instant.ofEpochMilli(startMs + durationForType(typeName)).toString();
        event.event = new EventInfo();
        event.event.event = typeName;
        parsed.add(event);
        return event;
    }

    private static String normalizeType(String raw) {
        switch (raw) {
            case "Farming Contest":
            case "Dark Auction":
            case "Traveling Zoo":
            case "Election Booth Opens!":
            case "Election Over!":
            case "Spooky Festival":
            case "Jerry Workshop Opens":
                return raw;
            default:
                return raw.endsWith("New Year's Celebration") ? "New Year" : null;
        }
    }

    /** Only Farming Contest/Dark Auction/Election durations are known precisely (they're short,
     *  fixed-length server mechanics); everything else states "Event lasts for 1h 0m 0s" in its
     *  own lore, which this mirrors directly rather than re-deriving from the day-length ratio. */
    private static long durationForType(String type) {
        switch (type) {
            case "Dark Auction": return 40_000L;
            case "Election Booth Opens!":
            case "Election Over!": return 60_000L;
            case "Farming Contest": return 20 * 60_000L;
            default: return 60 * 60_000L;
        }
    }

    private static Long parseDuration(String text) {
        Matcher m = DURATION_PART.matcher(text);
        long totalMs = 0;
        boolean found = false;
        while (m.find()) {
            found = true;
            long value = Long.parseLong(m.group(1));
            switch (m.group(2)) {
                case "d": totalMs += value * 86_400_000L; break;
                case "h": totalMs += value * 3_600_000L; break;
                case "m": totalMs += value * 60_000L; break;
                case "s": totalMs += value * 1_000L; break;
            }
        }
        return found ? totalMs : null;
    }

    private void finishParse() {
        parsing = false;
        EventUtils.adoptParsedEvents(new ArrayList<>(parsed));

        ChatUtils.sendMessage(EnumChatFormatting.GREEN
                + "Parsed " + parsed.size() + " event(s) from the calendar — cached for the notifier.");
        Aetheria.logger.info("[CalendarParser] Parsed " + parsed.size() + " events across " + (currentPage - 1) + " page(s)");
    }
}
