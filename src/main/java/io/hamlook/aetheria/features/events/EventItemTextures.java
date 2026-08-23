package io.hamlook.aetheria.features.events;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Hand-drawn replacements for vanilla items whose stock inventory icon looked wrong at this
 * overlay's render scale/style — badges (Jukebox, Pumpkin, Snow Block, Iron Hoe, Iron Pickaxe,
 * Fishing Rod), Spooky's Dead Bush, Fishing Festival's Pufferfish, and New Year's firework rocket
 * decorations, and most of Farming Contest's crop icons. Checked first by
 * {@link EventNotifierOverlay#renderEventIcon}, ahead of both the flat-atlas-sprite renderer and
 * the shrink workaround, since a real bundled texture always fades better than either. Keyed by
 * raw item+metadata identity (not event type) since the same lookup has to serve badges, inline
 * crop icons, and decorations alike. Anything not listed here just falls through to vanilla
 * rendering as before — currently that's New Year's cake badge and Farming Contest's Seeds crop
 * (never actually a real contest crop, so this shouldn't matter in practice).
 */
public class EventItemTextures {

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("aetheria", "eventnotification/" + name);
    }

    private static final Map<String, ResourceLocation> MAP = new HashMap<>();

    /** Textures whose drawn content sits low within its own square canvas — read
     *  {@link #nudgesUpQuarter} for how this gets applied at render time. */
    private static final Set<String> NUDGE_UP_QUARTER = new HashSet<>();

    private static void put(Item item, int meta, String textureName) {
        MAP.put(Item.getIdFromItem(item) + ":" + meta, tex(textureName));
    }

    private static void nudgeUpQuarter(Item item, int meta) {
        NUDGE_UP_QUARTER.add(Item.getIdFromItem(item) + ":" + meta);
    }

    static {
        put(Item.getItemFromBlock(Blocks.jukebox), 0, "jukebox.png");
        put(Item.getItemFromBlock(Blocks.pumpkin), 0, "pumpkin.png");
        put(Item.getItemFromBlock(Blocks.snow), 0, "snow_block.png");
        put(Items.iron_hoe, 0, "iron_hoe.png");
        put(Items.iron_pickaxe, 0, "iron_pickaxe.png");
        put(Items.fishing_rod, 0, "fishing_rod.png");
        put(Item.getItemFromBlock(Blocks.deadbush), 0, "dead_bush.png");
        put(Items.fish, 3, "pufferfish.png");
        put(Items.fireworks, 0, "firework_rocket.png");

        put(Items.wheat, 0, "wheat.png");
        put(Items.carrot, 0, "carrot.png");
        put(Items.potato, 0, "potato.png");
        put(Items.melon, 0, "melon_slice.png");
        put(Items.reeds, 0, "sugar_cane.png");
        put(Items.dye, 3, "cocoa_beans.png");
        put(Item.getItemFromBlock(Blocks.cactus), 0, "cactus.png");
        put(Item.getItemFromBlock(Blocks.red_mushroom), 0, "red_mushroom.png");
        // Brown Mushroom isn't a distinct Farming Contest crop — contests only ever call it out
        // as "Mushroom" and should always show the same red mushroom art, never a separate icon.
        put(Item.getItemFromBlock(Blocks.brown_mushroom), 0, "red_mushroom.png");
        // The mushroom art sits low in its own canvas, so it reads as hanging below the other
        // crop icons even once vertically centered on the box — nudge it up to compensate.
        nudgeUpQuarter(Item.getItemFromBlock(Blocks.red_mushroom), 0);
        nudgeUpQuarter(Item.getItemFromBlock(Blocks.brown_mushroom), 0);
        put(Items.nether_wart, 0, "nether_wart.png");
        put(Item.getItemFromBlock(Blocks.double_plant), 4, "rose_bush.png");
        put(Item.getItemFromBlock(Blocks.red_flower), 1, "blue_orchid.png");
        put(Item.getItemFromBlock(Blocks.double_plant), 0, "sunflower.png");
    }

    private EventItemTextures() {}

    public static ResourceLocation forStack(ItemStack stack) {
        if (stack == null) return null;
        return MAP.get(Item.getIdFromItem(stack.getItem()) + ":" + stack.getMetadata());
    }

    public static boolean nudgesUpQuarter(ItemStack stack) {
        if (stack == null) return false;
        return NUDGE_UP_QUARTER.contains(Item.getIdFromItem(stack.getItem()) + ":" + stack.getMetadata());
    }
}
