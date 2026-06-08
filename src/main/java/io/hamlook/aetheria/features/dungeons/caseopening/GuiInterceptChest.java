package io.hamlook.aetheria.features.dungeons.caseopening;

import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class GuiInterceptChest extends GuiContainer {

    private static final int SCAN_DELAY = 3;
    private static final int REWARD_SLOT_START = 10;
    private static final int REWARD_SLOT_END = 16;
    private final ContainerChest container;
    private final DungeonDropData.Floor floor;
    private final DungeonDropData.CaseMaterial material;
    private int tickCount = 0;
    private boolean doneCollectingReward = false;
    private DungeonDropData.Rule rewardToOpen = null;

    public GuiInterceptChest(ContainerChest container, DungeonDropData.Floor floor, DungeonDropData.CaseMaterial material) {
        super(container);
        this.container = container;
        this.floor = floor;
        this.material = material;
        DebugLogger.log("[GuiInterceptChest] Initialized — floor=" + floor + ", material=" + material);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        tickCount++;

        if (tickCount < SCAN_DELAY) return;

        if (!doneCollectingReward) {
            doneCollectingReward = true;
            scanForReward();
        }

        if (rewardToOpen != null) {
            DebugLogger.log("[GuiInterceptChest] Launching animation for: " + rewardToOpen.item.name());
            Minecraft.getMinecraft().displayGuiScreen(new CustomDropAnimationGui(rewardToOpen, floor, material));
        } else {
            DebugLogger.log("[GuiInterceptChest] No matching reward found — returning to chest GUI");
            Minecraft.getMinecraft().displayGuiScreen(ChestListener.originalGui);
        }
    }

    private void scanForReward() {
        IInventory lower = ContainerUtils.getLowerInventory(container);
        int size = lower.getSizeInventory();
        int dropCount = DungeonDropData.getDrops(material, floor).size();
        DebugLogger.log("[GuiInterceptChest] Scanning — floor=" + floor + ", material=" + material + ", possible drops=" + dropCount + ", inventory size=" + size);

        if (ATHRConfig.feature.debug.enableDebug) {
            for (int i = 0; i < size; i++) {
                ItemStack s = lower.getStackInSlot(i);
                if (s == null || s.getItem() == null) continue;
                String id = ItemUtils.getEffectiveItemId(s);
                DebugLogger.log("[GuiInterceptChest] [ALL] Slot " + i + ": " + s.getDisplayName() + " (id=" + id + ")");
            }
        }

        for (int i = REWARD_SLOT_START; i <= REWARD_SLOT_END; i++) {
            ItemStack stack = lower.getStackInSlot(i);
            if (stack == null || stack.getItem() == null) continue;

            String itemId = ItemUtils.getEffectiveItemId(stack);
            if (itemId.isEmpty()) continue;
            DebugLogger.log("[GuiInterceptChest] Slot " + i + ": " + itemId);

            DungeonDropData.Rule found = DungeonDropData.getDrops(material, floor).stream().filter(r -> r.item.name().equals(itemId)).findFirst().orElse(null);

            if (found == null) continue;

            if (rewardToOpen == null || found.rarity < rewardToOpen.rarity || (found.rarity == rewardToOpen.rarity && found.item.name().compareTo(rewardToOpen.item.name()) < 0)) {
                rewardToOpen = found;
                DebugLogger.log("[GuiInterceptChest] New best reward: " + rewardToOpen.item.name() + " (rarity " + rewardToOpen.rarity + ")");
            }
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