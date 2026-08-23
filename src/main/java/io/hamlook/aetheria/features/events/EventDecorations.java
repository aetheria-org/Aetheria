package io.hamlook.aetheria.features.events;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static io.hamlook.aetheria.features.events.EventDecor.Anchor.*;
import static io.hamlook.aetheria.features.events.EventDecor.Layer.*;

/**
 * Hand-placed themed decorations per event type, rendered around/behind/in front of the
 * notification box (see {@link EventNotifierOverlay#render(boolean)}).
 * <p>
 * Every non-CENTER, non-BEHIND ("front") decoration lives inside a reserved strip on the box's
 * right edge ({@link #RIGHT_CLEARANCE}) — the box is widened by that amount specifically so these
 * can never reach into the text, no matter how long the toast's text gets. BEHIND decorations
 * need no such reservation: they're covered by the opaque box in the middle by construction, so
 * only the parts that stick out past the box's edges are ever visible, and text/badge are always
 * drawn after everything else — so nothing can ever cover them either.
 * <p>
 * Textures under {@code assets/aetheria/eventnotification/} are user-supplied renders (Cole, Foxy,
 * the Traveling Zoo pets, Ender Dragon, Yeti, the ruby gemstone, Tiger Shark), converted from
 * their original WebP source to real alpha-preserving PNGs via ffmpeg where needed. Dark Auction
 * intentionally has no decorations beyond its Sirius badge — the candidate weapon icons (Midas
 * Sword / Aspect of the End) carry vanilla's animated enchant-glint overlay, which looked bad at
 * this render scale. Farming Contest has none either — its actual crop icons render inline after
 * the text instead (see {@link EventNotifierOverlay}).
 */
public class EventDecorations {

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("aetheria", "eventnotification/" + name);
    }

    private static final ResourceLocation FOXY = tex("foxy.png");
    private static final ResourceLocation COLE = tex("cole.png");
    private static final ResourceLocation ELEPHANT = tex("elephant_pet.png");
    private static final ResourceLocation GIRAFFE = tex("giraffe_pet.png");
    private static final ResourceLocation TIGER = tex("tiger_pet.png");
    private static final ResourceLocation ENDER_DRAGON = tex("ender_dragon.png");
    private static final ResourceLocation YETI = tex("yeti.png");
    private static final ResourceLocation RUBY_GEMSTONE = tex("ruby_gemstone.png");
    private static final ResourceLocation SHARK = tex("tiger_shark.png");

    /** Extra box width reserved on the right for this event's front-layer decorations. */
    private static final Map<String, Integer> RIGHT_CLEARANCE = new HashMap<>();

    static {
        RIGHT_CLEARANCE.put("Election Booth Opens!", 32);
        RIGHT_CLEARANCE.put("Election Over!", 32);
        RIGHT_CLEARANCE.put("Traveling Zoo", 66);
        RIGHT_CLEARANCE.put("Spooky Festival", 20);
        RIGHT_CLEARANCE.put("Jerry Workshop Opens", 20);
        RIGHT_CLEARANCE.put("Mining Fiesta", 30);
        RIGHT_CLEARANCE.put("Fishing Festival", 28);
        RIGHT_CLEARANCE.put("New Year", 18);
    }

    private EventDecorations() {}

    public static int rightClearanceFor(String eventType) {
        return eventType == null ? 0 : RIGHT_CLEARANCE.getOrDefault(eventType, 0);
    }

    public static List<EventDecor> decorationsFor(String eventType) {
        if (eventType == null) return Collections.emptyList();
        List<EventDecor> list = new ArrayList<>();

        switch (eventType) {
            case "Election Booth Opens!":
            case "Election Over!":
                // Tiled in one row inside the reserved strip; RIGHT_MID clips top/bottom equally.
                list.add(EventDecor.texture(FRONT, FOXY, RIGHT_MID, -17, -6, 13, 30));
                list.add(EventDecor.texture(FRONT, COLE, RIGHT_MID, 0, -6, 14, 28));
                break;

            case "Traveling Zoo":
                // Same size on all three, cascading top-clip / center-clip / bottom-clip so they
                // read as one line — x positions unchanged. Giraffe's dy compensates for
                // RIGHT_MID's vertical center landing 1x PADDING below the box's true midpoint
                // (the same offset TOP_RIGHT/BOTTOM_RIGHT already measure from), so it lands
                // exactly halfway between Elephant's top-clip and Tiger's bottom-clip instead of
                // sitting visibly low/crooked between them.
                list.add(EventDecor.texture(FRONT, ELEPHANT, TOP_RIGHT, -44, -10, 20, 20));
                list.add(EventDecor.texture(FRONT, GIRAFFE, RIGHT_MID, -22, -3, 20, 20));
                list.add(EventDecor.texture(FRONT, TIGER, BOTTOM_RIGHT, 0, 10, 20, 20));
                break;

            case "Dark Auction":
                // Intentionally empty — see class doc.
                break;

            case "Farming Contest":
                // No border decorations — the contest's actual crop icons render inline after the
                // text instead (see EventNotifierOverlay), so nothing to place here.
                break;

            case "Spooky Festival":
                list.add(EventDecor.item(FRONT, deadBush(), TOP_RIGHT, -2, -6, 22));
                break;

            case "Jerry Workshop Opens":
                // Big and centered behind the box so a real chunk of dragon peeks past every edge
                // instead of a thin, hard-to-read sliver.
                list.add(EventDecor.texture(BEHIND, ENDER_DRAGON, CENTER, 0, 0, 100, 80));
                list.add(EventDecor.texture(FRONT, YETI, RIGHT_MID, 0, 0, 14, 30));
                break;

            case "Mining Fiesta":
                // Scaled up and vertically centered so it clips the top and bottom of the border
                // equally.
                list.add(EventDecor.texture(FRONT, RUBY_GEMSTONE, RIGHT_MID, 0, 0, 27, 36));
                break;

            case "Fishing Festival":
                list.add(EventDecor.texture(FRONT, SHARK, RIGHT_MID, -14, -6, 14, 30));
                list.add(EventDecor.item(FRONT, new ItemStack(Items.fish, 1, 3), TOP_RIGHT, 0, 4, 12));
                break;

            case "New Year":
                // A couple on top like before, inside the reserved strip...
                list.add(EventDecor.item(FRONT, new ItemStack(Items.fireworks), TOP_RIGHT, -13, 0, 12));
                list.add(EventDecor.item(FRONT, new ItemStack(Items.fireworks), TOP_RIGHT, 0, 3, 12));
                // ...plus several more behind the box, only their edges peeking out — safe from
                // the text/badge by construction since the opaque box covers the rest of them.
                list.add(EventDecor.item(BEHIND, new ItemStack(Items.fireworks), TOP_LEFT, 0, 0, 18));
                list.add(EventDecor.item(BEHIND, new ItemStack(Items.fireworks), BOTTOM_LEFT, 5, 0, 18));
                list.add(EventDecor.item(BEHIND, new ItemStack(Items.fireworks), BOTTOM_RIGHT, -5, 0, 18));
                list.add(EventDecor.item(BEHIND, new ItemStack(Items.fireworks), CENTER, 0, 0, 26));
                break;

            default:
                break;
        }

        return list;
    }

    private static ItemStack deadBush() {
        return new ItemStack(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.deadbush));
    }
}
