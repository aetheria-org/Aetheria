package io.hamlook.aetheria.features.farming.visitors;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.features.farming.VisitorsConfig;
import io.hamlook.aetheria.features.farming.FarmingApi;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
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

    private static ContainerChest chestOf(GuiScreen screen) {
        if (!(screen instanceof GuiContainer)) return null;
        if (!(((GuiContainer) screen).inventorySlots instanceof ContainerChest)) return null;
        return (ContainerChest) ((GuiContainer) screen).inventorySlots;
    }

    @Override
    protected boolean panelEnabled() {
        VisitorsConfig cfg = config();
        if (cfg == null) return blocked("config not loaded");
        if (!cfg.enabled) return blocked("visitors feature disabled");
        if (!cfg.panel.enabled) return blocked("main panel disabled in config");
        if (VisitorShoppingList.hiddenAt(cfg.panel.visible))
            return blocked("hidden here (mode " + cfg.panel.visible + ", loc=" + SkyblockData.getCurrentLocation() + ", skyblock=" + SkyblockData.isOnSkyblock() + ")");
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null) return blocked("no screen open");
        if (screen instanceof GuiEditSign)
            return VisitorShoppingList.signFillMode() == 1 || blocked("sign open, Click-to-Fill mode off");
        if (!(screen instanceof GuiContainer))
            return blocked("screen not a container: " + screen.getClass().getSimpleName());
        if (screen instanceof GuiInventory) {
            clearBlocked();
            return true;
        }
        ContainerChest chest = chestOf(screen);
        if (chest == null) return blocked("container has no chest inventory");
        String title = ContainerUtils.getTitle(chest);
        // Bazaar menus only display the panel while on the Garden
        if (title.startsWith("Bazaar") && SkyblockData.getCurrentLocation() != SkyblockData.Location.GARDEN) {
            return blocked("bazaar chest outside the Garden");
        }
        if (title.startsWith("Bazaar")) {
            clearBlocked();
            return true;
        }
        if (FarmingApi.getVisitorNeeds().containsKey(title)) {
            clearBlocked();
            return true;
        }
        // Product / instant-buy / confirm pages of an active ordering flow
        if (VisitorShoppingList.nameMatchesFlow(title)) {
            clearBlocked();
            return true;
        }
        return blocked("chest '" + title + "' is not Bazaar/visitor/inventory/sign");
    }

    @Override
    protected List<VisitorLine> lines() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiEditSign && VisitorShoppingList.isOrderFlowActive()) {
            return VisitorShoppingList.buildSingleEntryLines();
        }
        return VisitorShoppingList.buildMainLines(false);
    }
}
