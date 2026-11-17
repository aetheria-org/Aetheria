package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//#if MC < 1.21
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
//#endif

//#if MC >= 1.21
//$$ import net.minecraft.block.BlockState;
//$$ import net.minecraft.client.render.model.BakedModel;
//$$ import net.minecraft.client.render.model.BakedQuad;
//$$ import net.minecraft.util.math.Direction;
//$$ import net.minecraft.util.math.random.Random;
//$$
//$$ import java.util.HashMap;
//#endif

public final class SolidRender {

    private static final int MAX_CACHE_SIZE = 64;

    //#if MC < 1.21
    private static final Map<IBakedModel, float[]> CACHE = new LinkedHashMap<IBakedModel, float[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<IBakedModel, float[]> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    //#else
    //$$ private static final Map<BlockState, float[]> CACHE = new HashMap<>();
    //#endif

    private SolidRender() {
    }

    public static void block(BlockPos pos, Color color, boolean throughWalls) {
        World world = MinecraftCompat.getLocalWorld();
        if (world == null) return;

        float[] vertices = quads(world, pos);
        if (vertices.length == 0) {
            WorldRenderUtils.drawFilledBlocks(Collections.singletonList(new AxisAlignedBB(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)),
                    color, true, throughWalls);
            return;
        }

        WorldRenderUtils.drawBlockQuads(pos, vertices, color, throughWalls);
    }

    public static void cube(AxisAlignedBB box, Color color, boolean throughWalls) {
        WorldRenderUtils.drawFilledBlocks(Collections.singletonList(box), color, true, throughWalls);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    //#if MC < 1.21
    private static float[] quads(World world, BlockPos pos) {
        IBakedModel model = MinecraftCompat.getMinecraft().getBlockRendererDispatcher()
                .getModelFromBlockState(world.getBlockState(pos), world, pos);
        if (model == null) return new float[0];

        float[] cached = CACHE.get(model);
        if (cached != null) return cached;

        List<BakedQuad> all = new ArrayList<>();
        List<BakedQuad> general = model.getGeneralQuads();
        if (general != null) all.addAll(general);
        for (EnumFacing facing : EnumFacing.values()) {
            List<BakedQuad> face = model.getFaceQuads(facing);
            if (face != null) all.addAll(face);
        }

        float[] vertices = flatten(all);
        CACHE.put(model, vertices);
        return vertices;
    }
    //#else
    //$$ private static float[] quads(World world, BlockPos pos) {
    //$$     BlockState state = world.getBlockState(pos);
    //$$     float[] cached = CACHE.get(state);
    //$$     if (cached != null) return cached;
    //$$
    //$$     BakedModel model = MinecraftCompat.getMinecraft().getBlockRenderManager().getModel(state);
    //$$     if (model == null) return new float[0];
    //$$
    //$$     List<BakedQuad> all = new ArrayList<>(model.getQuads(state, null, Random.create()));
    //$$     for (Direction facing : Direction.values()) {
    //$$         all.addAll(model.getQuads(state, facing, Random.create()));
    //$$     }
    //$$
    //$$     float[] vertices = flatten(all);
    //$$     if (CACHE.size() > MAX_CACHE_SIZE) CACHE.clear();
    //$$     CACHE.put(state, vertices);
    //$$     return vertices;
    //$$ }
    //#endif

    // only the vertex positions are kept: the highlight is one flat colour, the model's own colours are unused
    private static float[] flatten(List<BakedQuad> quads) {
        float[] vertices = new float[quads.size() * 12];
        int out = 0;
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertexData();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * stride;
                vertices[out++] = Float.intBitsToFloat(data[base]);
                vertices[out++] = Float.intBitsToFloat(data[base + 1]);
                vertices[out++] = Float.intBitsToFloat(data[base + 2]);
            }
        }
        return vertices;
    }
}
