package io.hamlook.aetheria.features.profile;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ColorUtils;
import io.hamlook.aetheria.utils.ContainerUtils;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

@RegisterEvents
public class ProfileListener {

    public static final List<String> PROFILE_TITLES = Arrays.asList(
            "Select Profile", "View Profile", "View Inventory",
            "View Skills","View HOTM","View Dungeon Stats","View Storage",
            "View Slayers","View Wardrobe","View Pets","View Bags","Show Contents",
            "View Farming Collections","View Mining Collections","View Combat Collections",
            "View Foraging Collections","View Fishing Collections","View Boss Collections"
    );

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui == null) {
            if(!ProfileParser.parsing && !ProfileParser.lastCachedProfile.isEmpty()) {
                Aetheria.logger.info("Refreshing Cache");
                ProfileParser.lastCachedProfile = "";
                return;
            }
        }

        ContainerChest ch = ContainerUtils.getOpenChest(event.gui);
        if (ch != null) {
            String title = ContainerUtils.getTitle(ch);

            if (!PROFILE_TITLES.contains(title)) {
                ProfileParser.lastCachedProfile = "";
                ProfileParser.parsing = false;
            }
        }
    }
}