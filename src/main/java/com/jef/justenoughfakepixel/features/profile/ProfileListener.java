package com.jef.justenoughfakepixel.features.profile;

import com.jef.justenoughfakepixel.JefMod;
import com.jef.justenoughfakepixel.init.RegisterEvents;
import com.jef.justenoughfakepixel.utils.ColorUtils;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

@RegisterEvents
public class ProfileListener {

    private static final List<String> PROFILE_TITLES = Arrays.asList(
            "Select Profile", "View Profile", "View Inventory",
            "View Skills","View HOTM","View Dungeon Stats","View Storage",
            "View Slayers","View Wardrobe","View Pets","View Bags","Show Contents"
    );

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui == null) {
            if(!ProfileParser.parsing && !ProfileParser.lastCachedProfile.isEmpty()) {
                JefMod.logger.info("Refreshing Cache");
                ProfileParser.lastCachedProfile = "";
                return;
            }
        }

        if (event.gui instanceof GuiChest) {
            GuiChest chest       = (GuiChest) event.gui;
            ContainerChest ch    = (ContainerChest) chest.inventorySlots;
            IInventory lowerInv  = ch.getLowerChestInventory();
            String title         = ColorUtils.stripColor(
                    lowerInv.getDisplayName().getUnformattedText()
            ).trim();

            if (!PROFILE_TITLES.contains(title)) {
                ProfileParser.lastCachedProfile = "";
                ProfileParser.parsing = false;
            }
        }
    }
}