package io.hamlook.aetheria.features.dungeons.overlays;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.Position;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.ArrayNormalizationKt;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.item.ItemUtils;
import io.hamlook.aetheria.utils.overlay.Overlay;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RegisterEvents
public class DungeonBreakerOverlay extends Overlay {

    private static final String ITEM_ID = "DUNGEONBREAKER";

    private static final Pattern CHARGES = Pattern.compile("Charges:\\s*(\\d+)/(\\d+)");

    private static final String C_LABEL = EnumChatFormatting.RED.toString();
    private static final String C_FULL = EnumChatFormatting.GREEN.toString();
    private static final String C_SPENT = EnumChatFormatting.RED.toString();
    private static final String C_VAL = EnumChatFormatting.GREEN.toString();
    private static final String C_SEP = EnumChatFormatting.GRAY.toString();

    @Getter
    private static DungeonBreakerOverlay instance;

    public DungeonBreakerOverlay() {
        super(90, 20);
        instance = this;
    }

    private static ItemStack findBreakerInHotbar() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.thePlayer == null) return null;
        ItemStack[] hotbar = ArrayNormalizationKt.normalizeAsArray(mc.thePlayer.inventory.mainInventory);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = hotbar[i];
            if (stack != null && ITEM_ID.equals(ItemUtils.getInternalName(stack))) return stack;
        }
        return null;
    }

    private static int[] parseCharges(ItemStack item) {
        for (String line : ItemUtils.getLoreLines(item)) {
            Matcher m = CHARGES.matcher(ColorUtils.stripColor(line));
            if (!m.find()) continue;
            try {
                return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override
    public Position getPosition() {
        return ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerPos;
    }

    @Override
    public float getScale() {
        return ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerScale;
    }

    @Override
    public int getBgColor() {
        return ChromaColour.specialToChromaRGB(ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerBgColor);
    }

    @Override
    public int getCornerRadius() {
        return ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerCornerRadius;
    }

    @Override
    protected boolean isEnabled() {
        return ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerOverlay;
    }

    @Override
    protected boolean extraGuard() {
        return SkyblockData.getCurrentLocation() == SkyblockData.Location.DUNGEON;
    }

    @Override
    public List<String> getLines(boolean preview) {
        List<String> out = new ArrayList<>();

        if (preview) {
            out.add(C_LABEL + "Dungeon Breaker");
            out.add(C_LABEL + "Charges: " + C_FULL + "20" + C_SEP + "/" + C_VAL + "20");
            return out;
        }

        ItemStack breaker = findBreakerInHotbar();
        if (breaker == null) return out;

        int[] charges = parseCharges(breaker);
        if (charges == null) return out;

        int current = charges[0];
        int max = charges[1];
        String chargeColor = current == max ? C_FULL : current == 0 ? C_SPENT : C_VAL;

        out.add(C_LABEL + "Dungeonbreaker");
        out.add(C_SEP + "Charges: " + chargeColor + current + C_SEP + "/" + C_VAL + max);
        return out;
    }
}