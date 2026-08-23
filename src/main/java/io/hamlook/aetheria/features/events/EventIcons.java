package io.hamlook.aetheria.features.events;

import io.hamlook.aetheria.features.farming.data.Crop;
import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Icon per SkyBlock event type; Farming Contest additionally resolves its crop names through
 *  the existing Farming Tracker crop-icon lookup ({@link Crop#findByDropName}) instead of a
 *  second table. */
public class EventIcons {

    /** Verified-correct fallback for Sirius/Oringo's heads, used only until {@link ItemRegistry}
     *  finishes its async load — the mod's own itemData.json already has SIRIUS_NPC/ORINGO_NPC
     *  entries with these exact same texture values, so that's the live source of truth
     *  (see {@link #resolveTypeIcon}); these constants just cover the gap before it's loaded. */
    private static final String SIRIUS_TEXTURE_FALLBACK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdhYjgzODU4ZWJjOGVlODVjM2U1NGFiMTNhYWJmY2MxZWYyYWQ0NDZkNmE5MDBlNDcxYzNmMzNiNzg5MDZhNWIifX19";
    private static final String ORINGO_TEXTURE_FALLBACK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTQ5ZmU1OTVjNmEwOGFkZWM4YjlkYWIwOTg2ODUzMjcxYzZiODdkODk3ZDczMThiMWJhZGFkMmMzNGJkNWEwZSIsIm1ldGFkYXRhIjp7fX19LCJwcm9maWxlSWQiOiJkYmQyNWU5YzM5MTcyZTU0YWM2YjA2M2U1OGE5ZTk5MyIsInByb2ZpbGVOYW1lIjoibmVhODkiLCJpc1B1YmxpYyI6dHJ1ZSwidGltZXN0YW1wIjoxNzcyNDI3NTg2MjkyfQ==";

    private static final Map<String, ItemStack> TYPE_ICONS = new HashMap<>();

    /** The events API's crop names don't always match {@link Crop}'s own
     *  {@code displayName}/{@code enchantedChatName}/{@code blockChatName} strings (built for the
     *  Farming Tracker's own naming, not this feed) — "Melon Slice" vs. {@code Crop}'s "Melon"
     *  being the confirmed case so far. Aliased here rather than in {@code Crop} itself, since
     *  that class is shared with the Farming Tracker and changing its displayName would affect
     *  that feature's own UI text too. */
    private static final Map<String, String> API_CROP_ALIASES = new HashMap<>();

    static {
        API_CROP_ALIASES.put("melon slice", "Melon");
    }

    static {
        TYPE_ICONS.put("Election Booth Opens!", new ItemStack(Item.getItemFromBlock(Blocks.jukebox)));
        TYPE_ICONS.put("Election Over!", new ItemStack(Item.getItemFromBlock(Blocks.jukebox)));
        TYPE_ICONS.put("Spooky Festival", new ItemStack(Item.getItemFromBlock(Blocks.pumpkin)));
        TYPE_ICONS.put("Jerry Workshop Opens", new ItemStack(Item.getItemFromBlock(Blocks.snow)));
        TYPE_ICONS.put("Farming Contest", new ItemStack(Items.iron_hoe));
        TYPE_ICONS.put("Mining Fiesta", new ItemStack(Items.iron_pickaxe));
        TYPE_ICONS.put("Fishing Festival", new ItemStack(Items.fishing_rod));
        // API type-name unconfirmed — flagged to the user, easy to correct in one line if wrong.
        TYPE_ICONS.put("New Year", new ItemStack(Items.cake));
        // Dark Auction / Traveling Zoo are resolved dynamically in resolveTypeIcon(), not cached
        // here, so they pick up ItemRegistry's data as soon as it finishes loading.
    }

    private EventIcons() {}

    public static List<ItemStack> iconsFor(EventInfo event) {
        List<ItemStack> icons = new ArrayList<>();
        if (event == null) return icons;

        ItemStack typeIcon = resolveTypeIcon(event.event);
        if (typeIcon != null) icons.add(typeIcon);

        if (event.crops != null) {
            for (String cropName : event.crops) {
                Crop crop = Crop.findByDropName(cropName);
                if (crop == null) {
                    String alias = API_CROP_ALIASES.get(cropName.toLowerCase());
                    if (alias != null) crop = Crop.findByDropName(alias);
                }
                ItemStack icon = crop != null ? crop.getIcon() : null;
                if (icon != null) icons.add(icon);
            }
        }
        return icons;
    }

    private static ItemStack resolveTypeIcon(String type) {
        if ("Dark Auction".equals(type)) return skullFor("SIRIUS_NPC", SIRIUS_TEXTURE_FALLBACK);
        if ("Traveling Zoo".equals(type)) return skullFor("ORINGO_NPC", ORINGO_TEXTURE_FALLBACK);
        return TYPE_ICONS.get(type);
    }

    /** Prefers the live {@link ItemRegistry} entry (same texture, but stays in sync automatically
     *  if the repo data ever changes); falls back to the verified constant if the registry hasn't
     *  loaded yet. */
    private static ItemStack skullFor(String skyblockId, String fallbackTexture) {
        SkyblockItem item = ItemRegistry.getItem(skyblockId);
        if (item != null) {
            ItemStack stack = item.getStack();
            if (stack != null) return stack;
        }
        return ItemUtils.createSkullWithTexture(fallbackTexture);
    }
}
