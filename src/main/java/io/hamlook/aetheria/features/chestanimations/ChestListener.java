package io.hamlook.aetheria.features.chestanimations;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.DebugLogger;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chestanimations.caseopening.DungeonDropData;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.ContainerUtils;
import io.hamlook.aetheria.utils.RomanNumeralParser;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.data.DungeonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RegisterEvents
public class ChestListener {

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII"};
    public static GuiChest originalGui;
    private static WorldClient lastWorld = null;
    private final Map<Integer, Set<DungeonDropData.CaseMaterial>> crOpenedChests = new HashMap<>();
    private final Map<DungeonDropData.CaseMaterial, Boolean> openedChests = new HashMap<>();
    private boolean isCroesus = false;
    private boolean isCatacombsChestList = false;
    private int chestID = -1;
    private DungeonDropData.Floor curFloor;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (ATHRConfig.feature == null || !ATHRConfig.feature.dungeons.caseOpening.caseOpeningAnimation) return;
        if (!ContainerUtils.isChestOpen(event.gui)) return;

        originalGui = (GuiChest) event.gui;
        ContainerChest container = ContainerUtils.getOpenChest(event.gui);
        if (container == null) {
            Aetheria.logger.info("[ChestListener] GuiOpen: no ContainerChest found, not intercepting");
            return;
        }

        String name = ContainerUtils.getTitle(container);
        Aetheria.logger.info("[ChestListener] GuiOpen title=\"" + name + "\" isCroesus=" + isCroesus + " isCatacombsChestList=" + isCatacombsChestList + " chestID=" + chestID + " curFloor=" + curFloor);

        if (name.contains("Croesus")) {
            isCroesus = true;
            isCatacombsChestList = false;
            DebugLogger.log("[ChestListener] Croesus detected");
            return;
        }

        if (name.contains("Catacombs")) {
            if (!isCroesus) {
                DebugLogger.log("[ChestListener] Catacombs GUI seen but not in Croesus flow, ignoring");
                return;
            }
            isCatacombsChestList = true;

            boolean isMaster = name.contains("Master Mode") || name.contains("Master");
            DungeonDropData.Floor detected = null;
            for (String token : name.split("[\\s()]+")) {
                if (!RomanNumeralParser.isValid(token)) continue;
                try {
                    int num = RomanNumeralParser.parse(token);
                    if (num < 1 || num > 7) continue;
                    String key = (isMaster ? "M" : "") + ROMAN[num - 1];
                    detected = DungeonDropData.Floor.valueOf(key);
                    break;
                } catch (Exception ignored) {
                }
            }
            curFloor = detected;
            DebugLogger.log("[ChestListener] Catacombs chest list: floor=" + curFloor + ", master=" + isMaster + ", raw name=\"" + name + "\"");
            return;
        }

        if (!name.endsWith(" Chest")) return;

        String materialName = name.substring(0, name.length() - " Chest".length()).trim();
        DungeonDropData.CaseMaterial parsedMaterial = null;
        try {
            parsedMaterial = DungeonDropData.CaseMaterial.valueOf(materialName.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException ignored) {
        }

        if (parsedMaterial == null) {
            DebugLogger.log("[ChestListener] Chest \"" + name + "\" is not a supported material, skipping");
            return;
        }

        DungeonDropData.CaseMaterial curMaterial = parsedMaterial;
        String animation = ChestAnimations.getOption(curMaterial);
        if (ChestAnimations.NONE.equals(animation)) {
            Aetheria.logger.info("[ChestListener] Chest \"" + name + "\" has no animation selected (material=" + curMaterial + "), skipping");
            return;
        }
        Aetheria.logger.info("[ChestListener] Chest detected: material=" + curMaterial + ", animation=" + animation);

        if (isCroesus) {
            Set<DungeonDropData.CaseMaterial> opened = crOpenedChests.get(chestID);
            if (opened != null && opened.contains(curMaterial)) {
                Aetheria.logger.info("[ChestListener] Already opened chestID=" + chestID + " material=" + curMaterial + " (croesus), skipping animation");
                return;
            }
            crOpenedChests.computeIfAbsent(chestID, k -> new HashSet<>()).add(curMaterial);
        } else {
            if (Boolean.TRUE.equals(openedChests.get(curMaterial))) {
                Aetheria.logger.info("[ChestListener] Already opened " + curMaterial + " this run, skipping animation");
                return;
            }
            openedChests.put(curMaterial, true);
            curFloor = DungeonDropData.Floor.fromDungeonFloor(DungeonUtils.getFloorFromScoreboard());
            Aetheria.logger.info("[ChestListener] In-dungeon chest — scoreboard floor: " + curFloor);
        }

        if (curFloor == null) {
            Aetheria.logger.warning("[ChestListener] ERROR: floor is null! Cannot start animation. Make sure you navigate through Croesus → chest list first.");
            return;
        }

        Aetheria.logger.info("[ChestListener] Intercepting chest → floor=" + curFloor + ", material=" + curMaterial);
        event.gui = new GuiInterceptChest(container, curFloor, curMaterial, animation);
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ContainerUtils.isChestOpen()) return;
        if (!isCatacombsChestList) return;

        GuiChest chest = (GuiChest) Minecraft.getMinecraft().currentScreen;
        Slot hovered = chest.getSlotUnderMouse();
        if (hovered == null || !hovered.getHasStack()) return;

        String name = hovered.getStack().getDisplayName();

        name = StringUtils.cleanColour(name);
        name = StringUtils.clean(name);

        Set<DungeonDropData.CaseMaterial> opened = crOpenedChests.get(chestID);
        boolean isObChest = name.equalsIgnoreCase("Obsidian") && (opened == null || !opened.contains(DungeonDropData.CaseMaterial.OBSIDIAN));
        boolean isBrChest = name.equalsIgnoreCase("Bedrock") && (opened == null || !opened.contains(DungeonDropData.CaseMaterial.BEDROCK));
        if (!isObChest && !isBrChest) return;

        if (event.toolTip.size() > 3) {
            String first = event.toolTip.get(0);
            String last1 = event.toolTip.get(event.toolTip.size() - 1);
            String last2 = event.toolTip.get(event.toolTip.size() - 2);
            event.toolTip.clear();
            event.toolTip.add(first);
            event.toolTip.add("§7Hidden");
            event.toolTip.add(last2);
            event.toolTip.add(last1);
        }
    }

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!ContainerUtils.isChestOpen(event.gui)) return;
        Slot slot = ((GuiChest) event.gui).getSlotUnderMouse();
        if (slot != null && org.lwjgl.input.Mouse.getEventButtonState() && isCroesus && !isCatacombsChestList) {
            chestID = slot.slotNumber;
            DebugLogger.log("[ChestListener] Croesus slot clicked: chestID=" + chestID);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        WorldClient currentWorld = Minecraft.getMinecraft().theWorld;
        if (currentWorld != null && currentWorld != lastWorld) {
            lastWorld = currentWorld;
            isCroesus = false;
            isCatacombsChestList = false;
            chestID = -1;
            openedChests.clear();
            crOpenedChests.clear();
        }
    }
}