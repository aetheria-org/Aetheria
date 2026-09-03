package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.VisitorsConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.compat.InventoryCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.overlay.OverlayUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;

import java.util.List;

@RegisterEvents
public final class VisitorPanel extends VisitorPanelBase {

    @Getter
    private static VisitorPanel instance;

    public VisitorPanel() {
        instance = this;
    }

    private static VisitorsConfig config() {
        return ATHRConfig.feature == null ? null : ATHRConfig.feature.farming.visitors;
    }

    @Override
    protected boolean panelEnabled() {
        VisitorsConfig cfg = config();
        if (cfg == null) return blocked("config not loaded");
        if (!cfg.enabled) return blocked("visitors feature disabled");
        if (!cfg.panel.enabled) return blocked("main panel disabled in config");
        if (OverlayUtils.isStorageActive()) return blocked("storage overlay active");
        if (VisitorShoppingList.hiddenAt(cfg.panel.visible))
            return blocked("hidden here (mode " + cfg.panel.visible + ", loc=" + SkyblockData.getCurrentLocation() + ", skyblock=" + SkyblockData.isOnSkyblock() + ")");
        GuiScreen screen = MinecraftCompat.getMinecraft().currentScreen;
        if (screen == null) return blocked("no screen open");
        if (cfg.panel.onlyShowWithData && !FarmingApi.hasVisitorData()) return blocked("no visitor data learned yet");

        boolean signSurface = screen instanceof GuiEditSign;
        boolean inventorySurface = screen instanceof GuiInventory;
        String chestTitle = null;
        if (!signSurface && screen instanceof GuiContainer && InventoryCompat.getContainer((GuiContainer) screen) instanceof ContainerChest) {
            chestTitle = ContainerUtils.getTitle((ContainerChest) InventoryCompat.getContainer((GuiContainer) screen));
        }

        boolean visitorSurface = chestTitle != null && FarmingApi.getVisitorNeeds().containsKey(chestTitle);
        boolean bazaarSurface = false;
        if (chestTitle != null && chestTitle.startsWith("Bazaar")) {
            bazaarSurface = true;
        } else if (!signSurface && !inventorySurface && chestTitle != null) {
            // Product / instant-buy / confirm pages of an active ordering flow
            bazaarSurface = VisitorShoppingList.nameMatchesFlow(chestTitle);
        }

        switch (cfg.panel.showIn) {
            case 0:
                if (!visitorSurface) return blocked("show-in Visitors: not a recorded visitor menu");
                break;
            case 1:
                if (!bazaarSurface && !signSurface) return blocked("show-in Bazaar: not Bazaar/flow/sign");
                break;
            case 2:
                if (!inventorySurface) return blocked("show-in Inventory: not own inventory");
                break;
            case 4:
                if (!signSurface && !(screen instanceof GuiContainer))
                    return blocked("all menus: screen is not a container or sign");
                break;
            case 3:
            default:
                if (!visitorSurface && !bazaarSurface && !signSurface && !inventorySurface)
                    return blocked("relevant menus: '" + (chestTitle != null ? chestTitle : screen.getClass().getSimpleName()) + "' is not Bazaar/visitor/inventory/sign");
                break;
        }
        clearBlocked();
        return true;
    }

    @Override
    protected List<VisitorLine> lines() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        if (mc.currentScreen instanceof GuiEditSign && VisitorShoppingList.isOrderFlowActive()) {
            return VisitorShoppingList.buildSingleEntryLines();
        }
        return VisitorShoppingList.buildMainLines(false);
    }
}
