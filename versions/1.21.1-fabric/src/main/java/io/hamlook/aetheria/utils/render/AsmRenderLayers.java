package io.hamlook.aetheria.utils.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public final class AsmRenderLayers {
    // LINES format carries the Normal element LINES_PROGRAM expands with.
    public static final RenderLayer ESP_LINES = RenderLayer.of(
            "asm_esp_lines", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 1536, false, true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false));

    // Quads mirror vanilla's debug filled box layer.
    public static final RenderLayer ESP_QUADS = RenderLayer.of(
            "asm_esp_quads", VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 1536, false, true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false));

    private AsmRenderLayers() {
    }
}
