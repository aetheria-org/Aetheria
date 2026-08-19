package io.hamlook.aetheria.features.chestanimations;

import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.features.chestanimations.caseopening.DungeonDropData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;

public class GuiInterceptChest extends GuiContainer {

    private static final int SCAN_DELAY = 3;
    private final ContainerChest container;
    private final DungeonDropData.Floor floor;
    private final DungeonDropData.CaseMaterial material;
    private final String animation;
    private int tickCount = 0;

    public GuiInterceptChest(ContainerChest container, DungeonDropData.Floor floor, DungeonDropData.CaseMaterial material, String animation) {
        super(container);
        this.container = container;
        this.floor = floor;
        this.material = material;
        this.animation = animation;
        DebugLogger.log("[GuiInterceptChest] Initialized — floor=" + floor + ", material=" + material + ", animation=" + animation);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        tickCount++;

        if (tickCount < SCAN_DELAY) return;

        GuiScreen gui = ChestAnimations.create(animation, container, floor, material);
        if (gui != null) {
            DebugLogger.log("[GuiInterceptChest] Launching animation: " + animation);
            Minecraft.getMinecraft().displayGuiScreen(gui);
        } else {
            DebugLogger.log("[GuiInterceptChest] No animation found for \"" + animation + "\" — returning to chest GUI");
            Minecraft.getMinecraft().displayGuiScreen(ChestListener.originalGui);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public void handleMouseInput() {
    }
}