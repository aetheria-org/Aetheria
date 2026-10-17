package io.hamlook.aetheria.utils.render;

import net.minecraft.entity.Entity;

//#if MC < 1.21
import net.minecraft.client.renderer.culling.Frustum;
//#else
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.render.Frustum;
//$$ import net.minecraft.client.render.WorldRenderer;
//$$ import net.minecraft.util.math.Box;
//#endif

public final class FrustumUtils {

    //#if MC < 1.21
    private static final Frustum FRUSTUM = new Frustum();
    //#endif

    private FrustumUtils() {
    }

    public static boolean isVisible(Entity entity) {
        //#if MC < 1.21
        double[] vp = WorldRenderUtils.viewerPos();
        FRUSTUM.setPosition(vp[0], vp[1], vp[2]);
        return FRUSTUM.isBoundingBoxInFrustum(entity.getEntityBoundingBox());
        //#else
        //$$ WorldRenderer renderer = (WorldRenderer) MinecraftClient.getInstance().worldRenderer;
        //$$ Frustum frustum = renderer.frustum;
        //$$ if (frustum == null) return true;
        //$$ return frustum.isVisible(entity.getBoundingBox());
        //#endif
    }

    public static boolean isVisible(double x0, double y0, double z0,
                                    double x1, double y1, double z1) {
        //#if MC < 1.21
        double[] vp = WorldRenderUtils.viewerPos();
        FRUSTUM.setPosition(vp[0], vp[1], vp[2]);
        return FRUSTUM.isBoxInFrustum(x0, y0, z0, x1, y1, z1);
        //#else
        //$$ WorldRenderer renderer = (WorldRenderer) MinecraftClient.getInstance().worldRenderer;
        //$$ Frustum frustum = renderer.frustum;
        //$$ if (frustum == null) return true;
        //$$ return frustum.isVisible(new Box(x0, y0, z0, x1, y1, z1));
        //#endif
    }
}
