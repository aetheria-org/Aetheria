package io.hamlook.aetheria.features.dungeons.puzzles;

import com.google.gson.JsonObject;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMRenderWorldEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.compat.BlockCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.render.WorldRenderUtils;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

@Deprecated
public class WaterSolver {

    private static final Minecraft mc = MinecraftCompat.getMinecraft();

    private static final String[] LEVER_KEYS = {"minecraft:quartz_block", "minecraft:diamond_block", "minecraft:gold_block", "minecraft:emerald_block", "minecraft:coal_block", "minecraft:hardened_clay"};


    private static final Map<EnumDyeColor, Integer> WOOL_ORDINAL = new HashMap<>();
    private static final List<EnumDyeColor> WOOL_ORDER = Arrays.asList(EnumDyeColor.LIME, EnumDyeColor.BLUE, EnumDyeColor.RED, EnumDyeColor.PURPLE, EnumDyeColor.ORANGE);
    private static JsonObject solutionsJson = null;

    static {
        WOOL_ORDINAL.put(EnumDyeColor.PURPLE, 0);
        WOOL_ORDINAL.put(EnumDyeColor.ORANGE, 1);
        WOOL_ORDINAL.put(EnumDyeColor.BLUE, 2);
        WOOL_ORDINAL.put(EnumDyeColor.LIME, 3);
        WOOL_ORDINAL.put(EnumDyeColor.RED, 4);
    }

    private final Map<String, Integer> clickCounts = new HashMap<>();
    private final Map<String, Boolean> prevPowered = new HashMap<>();
    private boolean inWaterRoom = false;
    private int patternId = -1;
    private String extendedSlots = null;
    private boolean lastOptimized = false;
    private volatile Map<String, int[]> solutions = new HashMap<>();
    private volatile Map<String, BlockPos> leverPositions = new HashMap<>();
    private volatile BlockPos waterLeverPos = null;
    private boolean prevWaterPowered = false;

    private int openedWaterTick = -1;

    private int tickCounter = 0;

    private int scanTick = 0;

    private static int[] toTickArray(com.google.gson.JsonArray arr) {
        int[] out = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            out[i] = (int) (arr.get(i).getAsDouble() * 20);
        return out;
    }

    private static BlockPos stepInDirection(BlockPos pos, EnumFacing facing) {
        switch (facing) {
            case NORTH:
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
            case EAST:
                return new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
            case SOUTH:
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
            case WEST:
                return new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
            default:
                return null;
        }
    }

    private static BlockPos stepOpposite(BlockPos pos, EnumFacing facing) {
        switch (facing) {
            case NORTH:
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
            case EAST:
                return new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
            case SOUTH:
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
            case WEST:
                return new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
            case UP:
                return new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
            case DOWN:
                return new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
            default:
                return null;
        }
    }

    private static boolean isTargetBlock(String name) {
        return name.equals("minecraft:quartz_block") || name.equals("minecraft:gold_block") || name.equals("minecraft:coal_block") || name.equals("minecraft:diamond_block") || name.equals("minecraft:emerald_block") || name.equals("minecraft:hardened_clay");
    }

    @HandleEvent
    public void onTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerOverlay) return;
        if (!SkyblockData.isInDungeon()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        tickCounter++;

        if (waterLeverPos != null && openedWaterTick == -1) {
            boolean powered = isLeverPowered(waterLeverPos);
            if (powered && !prevWaterPowered) openedWaterTick = tickCounter;
            prevWaterPowered = powered;
        }

        for (Map.Entry<String, BlockPos> entry : leverPositions.entrySet()) {
            String key = entry.getKey();
            BlockPos pos = entry.getValue();
            if (!BlockCompat.isLever(mc.theWorld.getBlockState(pos).getBlock())) continue;
            boolean powered = isLeverPowered(pos);
            Boolean prev = prevPowered.get(key);
            if (prev != null && powered != prev) clickCounts.merge(key, 1, Integer::sum);
            prevPowered.put(key, powered);
        }

        if (++scanTick % 20 != 0) return;
        new Thread(this::detectWaterRoom).start();
    }

    @HandleEvent
    public void onRenderWorld(ASMRenderWorldEvent event) {
        if (!ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerOverlay) return;
        if (!inWaterRoom || solutions.isEmpty()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double vx = mc.getRenderManager().viewerPosX;
        double vy = mc.getRenderManager().viewerPosY;
        double vz = mc.getRenderManager().viewerPosZ;

        List<Object[]> pending = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : solutions.entrySet()) {
            String key = entry.getKey();
            int[] times = entry.getValue();
            if (key.equals("water")) {
                if (openedWaterTick == -1 && waterLeverPos != null) {
                    for (int t : times)
                        pending.add(new Object[]{waterLeverPos, t});
                }
            } else {
                BlockPos pos = leverPositions.get(key);
                if (pos == null) continue;
                int done = clickCounts.getOrDefault(key, 0);
                for (int i = done; i < times.length; i++)
                    pending.add(new Object[]{pos, times[i]});
            }
        }
        pending.sort(Comparator.comparingInt(a -> (int) a[1]));

        WorldRenderUtils.beginWorldRender(2f);
        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);
        for (int i = 0; i < pending.size(); i++) {
            BlockPos pos = (BlockPos) pending.get(i)[0];
            WorldRenderUtils.drawEspBox(pos.getX(), pos.getY(), pos.getZ(), i == 0 ? java.awt.Color.GREEN : java.awt.Color.YELLOW);
        }
        GL11.glPopMatrix();
        WorldRenderUtils.endWorldRender();

        if (!pending.isEmpty()) {
            BlockPos nextPos = (BlockPos) pending.get(0)[0];
            WorldRenderUtils.drawTracer(new Vec3(nextPos.getX() + 0.5, nextPos.getY() + 0.5, nextPos.getZ() + 0.5), event.partialTicks, java.awt.Color.GREEN);
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);
        for (Map.Entry<String, int[]> entry : solutions.entrySet()) {
            String key = entry.getKey();
            if (key.equals("water")) continue;
            BlockPos pos = leverPositions.get(key);
            if (pos == null) continue;
            int done = clickCounts.getOrDefault(key, 0);
            int[] times = entry.getValue();
            for (int i = 0; i < times.length - done; i++) {
                String label = buildLabel(times[done + i], done + i);
                WorldRenderUtils.drawTextInWorld(label, pos.getX() + 0.5, pos.getY() + 1.5 + (done + i) * 0.5, pos.getZ() + 0.5);
            }
        }
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    private String buildLabel(int targetTick, int rank) {
        if (openedWaterTick == -1) {
            return targetTick == 0 ? "§a§lCLICK ME!" : "§e" + String.format("%.1fs", targetTick / 20f);
        }
        int remaining = (openedWaterTick + targetTick) - tickCounter;
        if (remaining <= 0) return "§a§lCLICK ME!";
        return "§e" + String.format("%.1fs", remaining / 20f);
    }

    private int indexOf(String key) {
        for (int i = 0; i < LEVER_KEYS.length; i++)
            if (LEVER_KEYS[i].equals(key)) return i;
        return 0;
    }

    private void detectWaterRoom() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (checkForBlock(Blocks.sticky_piston, 20, 57) == null) {
            if (inWaterRoom) reset();
            return;
        }

        BlockPos pistonHead = checkForBlock(Blocks.piston_head, 18, 57);
        if (pistonHead == null) return;

        inWaterRoom = true;

        List<EnumDyeColor> extendedColors = detectExtendedWools(pistonHead);
        StringBuilder sb = new StringBuilder();
        for (EnumDyeColor c : extendedColors) {
            Integer ord = WOOL_ORDINAL.get(c);
            if (ord != null) sb.append(ord);
        }
        String newSlots = sb.length() == 3 ? sb.toString() : null;
        int newPattern = detectPattern();
        boolean optimized = ATHRConfig.feature.dungeons.dungeonBreaker.dungeonBreakerOverlay;
        if (newPattern == patternId && Objects.equals(newSlots, extendedSlots) && !leverPositions.isEmpty() && optimized == lastOptimized)
            return;
        lastOptimized = optimized;

        patternId = newPattern;
        extendedSlots = newSlots;

        leverPositions = detectLeverPositions(new BlockPos(pistonHead.getX(), pistonHead.getY() + 4, pistonHead.getZ()));

        if (patternId != -1 && extendedSlots != null) loadSolutions();
    }

    private int detectPattern() {
        int px = (int) mc.thePlayer.posX, pz = (int) mc.thePlayer.posZ;
        for (int x = px - 25; x <= px + 25; x++) {
            for (int z = pz - 25; z <= pz + 25; z++) {
                Block b77 = mc.theWorld.getBlockState(new BlockPos(x, 77, z)).getBlock();
                Block b78 = mc.theWorld.getBlockState(new BlockPos(x, 78, z)).getBlock();
                if (BlockCompat.isHardenedClay(b77)) return 0;
                if (b78 == Blocks.emerald_block) return 1;
                if (b78 == Blocks.diamond_block) return 2;
                if (b78 == Blocks.quartz_block) return 3;
            }
        }
        return -1;
    }

    private List<EnumDyeColor> detectExtendedWools(BlockPos origin) {
        Set<EnumDyeColor> found = new LinkedHashSet<>();
        BlockPos scan1 = new BlockPos(origin.getX() + 5, origin.getY(), origin.getZ() + 5);
        BlockPos scan2 = new BlockPos(origin.getX() - 5, origin.getY(), origin.getZ() - 5);

        for (BlockPos pos : BlockPos.getAllInBox(scan1, scan2)) {
            IBlockState state = mc.theWorld.getBlockState(pos);
            if (state.getBlock() != Blocks.piston_head) continue;
            EnumFacing facing = state.getValue(BlockPistonExtension.FACING);
            BlockPos woolPos = stepInDirection(pos, facing);
            if (woolPos != null) found.add(mc.theWorld.getBlockState(woolPos).getValue(BlockColored.COLOR));
        }

        List<EnumDyeColor> sorted = new ArrayList<>();
        for (EnumDyeColor c : new EnumDyeColor[]{EnumDyeColor.PURPLE, EnumDyeColor.ORANGE, EnumDyeColor.BLUE, EnumDyeColor.LIME, EnumDyeColor.RED})
            if (found.contains(c)) sorted.add(c);
        return sorted;
    }

    private Map<String, BlockPos> detectLeverPositions(BlockPos origin) {
        Map<String, BlockPos> result = new HashMap<>();
        BlockPos scan1 = new BlockPos(origin.getX() + 16, origin.getY(), origin.getZ() + 16);
        BlockPos scan2 = new BlockPos(origin.getX() - 16, origin.getY() - 1, origin.getZ() - 16);

        for (BlockPos pos : BlockPos.getAllInBox(scan1, scan2)) {
            IBlockState state = mc.theWorld.getBlockState(pos);
            if (!BlockCompat.isLever(state.getBlock())) continue;

            EnumFacing facing = state.getValue(BlockLever.FACING).getFacing();
            BlockPos behind = stepOpposite(pos, facing);
            if (behind == null) continue;

            String name = BlockCompat.getBlockName(mc.theWorld.getBlockState(behind).getBlock());

            if (isTargetBlock(name)) {
                result.put(name, pos);
            } else if (facing == EnumFacing.UP) {
                IBlockState belowState = mc.theWorld.getBlockState(behind);
                if (BlockCompat.isSmoothAndesite(belowState)) {
                    waterLeverPos = pos;
                }
            }
        }
        return result;
    }

    private void loadSolutions() {
        if (solutionsJson == null) {
            solutionsJson = RepoHandler.get(ATHRRepo.KEY_WATERSOLUTIONS, JsonObject.class, null);
            if (solutionsJson == null) return;
        }

        try {
            boolean optimized = false;
            JsonObject bySlots = solutionsJson.getAsJsonObject(String.valueOf(optimized)).getAsJsonObject(String.valueOf(patternId)).getAsJsonObject(extendedSlots);

            Map<String, int[]> loaded = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : bySlots.entrySet()) {
                String leverName = entry.getKey();
                String leverKey = "minecraft:" + leverName;
                if (leverName.equals("water")) {
                    loaded.put("water", toTickArray(entry.getValue().getAsJsonArray()));
                    continue;
                }
                if (!isTargetBlock(leverKey)) continue;
                loaded.put(leverKey, toTickArray(entry.getValue().getAsJsonArray()));
            }
            solutions = loaded;
        } catch (Exception e) {
            solutions = new HashMap<>();
        }
    }

    private BlockPos checkForBlock(Block target, int radius, int yLevel) {
        int px = (int) mc.thePlayer.posX, pz = (int) mc.thePlayer.posZ;
        for (int x = px - radius; x <= px + radius; x++)
            for (int z = pz - radius; z <= pz + radius; z++) {
                BlockPos pos = new BlockPos(x, yLevel, z);
                if (mc.theWorld.getBlockState(pos).getBlock() == target) return pos;
            }
        return null;
    }

    private boolean isLeverPowered(BlockPos pos) {
        IBlockState s = mc.theWorld.getBlockState(pos);
        return BlockCompat.isLever(s.getBlock()) && s.getValue(BlockLever.POWERED);
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        reset();
    }

    private void reset() {
        inWaterRoom = false;
        patternId = -1;
        extendedSlots = null;
        lastOptimized = false;
        solutions = new HashMap<>();
        leverPositions = new HashMap<>();
        waterLeverPos = null;
        openedWaterTick = -1;
        prevWaterPowered = false;
        tickCounter = 0;
        scanTick = 0;
        clickCounts.clear();
        prevPowered.clear();
    }
}